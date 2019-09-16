package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.nimble.common.rest.identity.model.NegotiationSettings;
import eu.nimble.service.bp.bom.BPMessageGenerator;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.util.persistence.catalogue.CataloguePersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
import eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType;
import eu.nimble.utility.JsonSerializationUtility;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import springfox.documentation.annotations.ApiIgnore;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;

@ApiIgnore
@Controller
public class EFactoryDemoController {

    private final Logger logger = LoggerFactory.getLogger(StartWithDocumentController.class);
    @Autowired
    private StartWithDocumentController startWithDocumentController;

    @ApiOperation(value = "",notes = "Creates a RFQ for the given product and buyer party. Then, it starts the process using the created RFQ document.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Started request for quotation successfully", response = ProcessInstance.class),
            @ApiResponse(code = 404, message = "There does not exist a product for the given id"),
            @ApiResponse(code = 500, message = "Unexpected error while creating request for quotation for the given product")
    })
    @RequestMapping(value = "/start-rfq",
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity startRFQProcess(@ApiParam(value = "Identifier (catalogueLine.hjid) of the product for which the request for quotation is created", required = true) @RequestParam(value = "productID",required = true) String productID,
                                          @ApiParam(value = "Number of the product requested", required = true) @RequestParam(value = "numberOfProductsRequested", required = true) BigDecimal numberOfProductsRequested,
                                          @ApiParam(value = "Endpoint of the buyer company which is used to send Quotation document", required = true) @RequestParam(value = "endpointOfTheBuyer",required = true) String endpointOfTheBuyer,
                                          @ApiParam(value = "Identifier of the buyer company", required = true) @RequestParam(value = "buyerPartyId",required = true) String buyerPartyId,
                                          @ApiParam(value = "Name of the buyer company", required = true) @RequestParam(value = "buyerPartyName",required = true) String buyerPartyName) {
        logger.info("Getting request to start request for quotation process");

        // retrieve the product details
        CatalogueLineType catalogueLine = CataloguePersistenceUtility.getCatalogueLine(productID);
        // check the existence of catalogue line
        if(catalogueLine == null){
            String msg = String.format("There does not exist a product for the given id: %s",productID);
            logger.error(msg);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
        }
        // get seller negotiation settings
        NegotiationSettings sellerNegotiationSettings;
        try {
            sellerNegotiationSettings = SpringBridge.getInstance().getiIdentityClientTyped().getNegotiationSettings(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID());
        } catch (IOException e) {
            String msg = String.format("Unexpected error while getting negotiation settings for the party: %s",catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID());
            logger.error(msg,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
        }
        // create quantity
        QuantityType quantity = new QuantityType();
        quantity.setValue(numberOfProductsRequested);
        quantity.setUnitCode(catalogueLine.getRequiredItemLocationQuantity().getPrice().getBaseQuantity().getUnitCode());

        // create buyer party
        PartyType buyerParty;
        try {
            buyerParty = SpringBridge.getInstance().getiIdentityClientTyped().getParty(StartWithDocumentController.token,buyerPartyId);
            // create the party if it does not exist
            if(buyerParty == null){
                buyerParty = new PartyType();
                PartyIdentificationType partyIdentification = new PartyIdentificationType();
                partyIdentification.setID(buyerPartyId);
                buyerParty.setPartyIdentification(Arrays.asList(partyIdentification));
                PersonType person = new PersonType();
                person.setID("213");
                buyerParty.getPerson().add(person);
                TextType partyName = new TextType();
                partyName.setValue(buyerPartyName);
                partyName.setLanguageID("en");
                PartyNameType partyNameType = new PartyNameType();
                partyNameType.setName(partyName);
                buyerParty.setPartyName(Arrays.asList(partyNameType));

                ContactType contact = new ContactType();
                CommunicationType communicationType = new CommunicationType();
                CodeType codeType = new CodeType();
                codeType.setName("REST");
                codeType.setValue(endpointOfTheBuyer);
                communicationType.setChannelCode(codeType);
                contact.setOtherCommunication(Arrays.asList(communicationType));
                buyerParty.setContact(contact);
            }
        } catch (IOException e) {
            String msg = String.format("Unexpected error while creating request for quotation for product: %s",catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID());
            logger.error(msg,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
        }


        // create RequestForQuotationLine
        RequestForQuotationType requestForQuotationType = null;
        try {
            requestForQuotationType = BPMessageGenerator.createRequestForQuotation(catalogueLine,quantity,sellerNegotiationSettings,buyerParty,StartWithDocumentController.token);
        } catch (IOException e) {
            String msg = String.format("Unexpected error while creating request for quotation for product: %s",productID);
            logger.error(msg,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
        }

        // serialize RFQ
        String serializedRFQ;
        try {
            serializedRFQ = JsonSerializationUtility.getObjectMapper().writeValueAsString(requestForQuotationType);
        } catch (JsonProcessingException e) {
            String msg = "Unexpected error while serializing the request for quotation";
            logger.error(msg,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
        }
        // start the process
        ProcessInstance processInstance = (ProcessInstance) startWithDocumentController.startProcessWithDocument(serializedRFQ, null).getBody();
        logger.info("Completed the request to start request for quotation process");
        return ResponseEntity.ok(processInstance);
    }

}
