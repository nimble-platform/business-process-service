package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.nimble.common.rest.identity.model.NegotiationSettings;
import eu.nimble.service.bp.bom.BPMessageGenerator;
import eu.nimble.service.bp.model.efactoryDemo.RFQSummary;
import eu.nimble.service.bp.model.hyperjaxb.DocumentType;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.util.UBLUtility;
import eu.nimble.service.bp.util.persistence.catalogue.CataloguePersistenceUtility;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.util.persistence.catalogue.PartyPersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
import eu.nimble.service.model.ubl.document.IDocument;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
    public ResponseEntity startRFQProcess(@RequestBody RFQSummary rfqSummary) throws Exception {
        logger.info("Getting request to start request for quotation process");
        try {
            logger.info("RFQSummary: {}",JsonSerializationUtility.getObjectMapper().writeValueAsString(rfqSummary));
        } catch (JsonProcessingException e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_SERIALIZE_RFQ_SUMMARY.toString(),e);
        }

        // fill missing fields of RFQSummary
        fillRfqSummaryWithPreviousDocument(rfqSummary);

        // retrieve the product details
        CatalogueLineType catalogueLine = CataloguePersistenceUtility.getCatalogueLine(rfqSummary.getProductID());
        // check the existence of catalogue line
        if(catalogueLine == null){
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_PRODUCT.toString(),Arrays.asList(rfqSummary.getProductID()));
        }
        // get seller negotiation settings
        NegotiationSettings sellerNegotiationSettings;
        try {
            sellerNegotiationSettings = SpringBridge.getInstance().getiIdentityClientTyped().getNegotiationSettings(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID());
        } catch (IOException e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_NEGOTIATION_SETTINGS.toString(),Arrays.asList(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID()),e);
        }
        // create quantity
        QuantityType quantity = new QuantityType();
        quantity.setValue(rfqSummary.getNumberOfProductsRequested());
        quantity.setUnitCode(catalogueLine.getRequiredItemLocationQuantity().getPrice().getBaseQuantity().getUnitCode());

        // create buyer party
        PartyType buyerParty;
        try {
            buyerParty = getBuyerParty(rfqSummary);
        } catch (IOException e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_CREATE_RFQ.toString(),Arrays.asList(rfqSummary.getProductID()),e);
        }


        // create RequestForQuotationLine
        RequestForQuotationType requestForQuotationType = null;
        try {
            requestForQuotationType = BPMessageGenerator.createRequestForQuotation(catalogueLine,quantity,sellerNegotiationSettings,buyerParty,rfqSummary.getPreviousDocumentId(), rfqSummary.getPricePerProduct(),StartWithDocumentController.token);
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_CREATE_RFQ.toString(),Arrays.asList(rfqSummary.getProductID()),e);
        }

        // serialize RFQ
        String serializedRFQ;
        try {
            serializedRFQ = JsonSerializationUtility.getObjectMapper().writeValueAsString(requestForQuotationType);
        } catch (JsonProcessingException e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_SERIALIZE_RFQ.toString(),e);
        }
        // start the process
        ProcessInstance processInstance = (ProcessInstance) startWithDocumentController.startProcessWithDocument(serializedRFQ, null ).getBody();
        logger.info("Completed the request to start request for quotation process");
        return ResponseEntity.ok(processInstance);
    }

    @ApiOperation(value = "",notes = "Creates an Order for the given product and buyer party. Then, it starts the process using the created Order document.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Started order successfully", response = ProcessInstance.class),
            @ApiResponse(code = 404, message = "There does not exist a product for the given id"),
            @ApiResponse(code = 500, message = "Unexpected error while creating order for the given product")
    })
    @RequestMapping(value = "/start-order",
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity startOrderProcess(@RequestBody RFQSummary rfqSummary) throws Exception {
        logger.info("Getting request to start order process");
        try {
            logger.info("RFQSummary: {}",JsonSerializationUtility.getObjectMapper().writeValueAsString(rfqSummary));
        } catch (JsonProcessingException e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_SERIALIZE_RFQ_SUMMARY.toString(),e);
        }

        // fill missing fields of RFQSummary
        fillRfqSummaryWithPreviousDocument(rfqSummary);

        // retrieve the product details
        CatalogueLineType catalogueLine = CataloguePersistenceUtility.getCatalogueLine(rfqSummary.getProductID());
        // check the existence of catalogue line
        if(catalogueLine == null){
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_PRODUCT.toString(),Arrays.asList(rfqSummary.getProductID()));
        }
        // get seller negotiation settings
        NegotiationSettings sellerNegotiationSettings;
        try {
            sellerNegotiationSettings = SpringBridge.getInstance().getiIdentityClientTyped().getNegotiationSettings(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID());
        } catch (IOException e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_NEGOTIATION_SETTINGS.toString(),Arrays.asList(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID()),e);
        }
        // create quantity
        QuantityType quantity = new QuantityType();
        quantity.setValue(rfqSummary.getNumberOfProductsRequested());
        quantity.setUnitCode(catalogueLine.getRequiredItemLocationQuantity().getPrice().getBaseQuantity().getUnitCode());

        // create buyer party
        PartyType buyerParty;
        try {
            buyerParty = getBuyerParty(rfqSummary);
        } catch (IOException e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_CREATE_ORDER.toString(),Arrays.asList(rfqSummary.getProductID()),e);
        }


        // create OrderLine
        OrderType order = null;
        try {
            order = BPMessageGenerator.createOrder(catalogueLine,quantity,sellerNegotiationSettings,buyerParty,rfqSummary.getPreviousDocumentId(),rfqSummary.getPricePerProduct(),StartWithDocumentController.token);
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_CREATE_ORDER.toString(),Arrays.asList(rfqSummary.getProductID()),e);
        }

        // serialize Order
        String serializedOrder;
        try {
            serializedOrder = JsonSerializationUtility.getObjectMapper().writeValueAsString(order);
        } catch (JsonProcessingException e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_SERIALIZE_ORDER.toString(),e);
        }
        // start the process
        ProcessInstance processInstance = (ProcessInstance) startWithDocumentController.startProcessWithDocument(serializedOrder, null).getBody();
        logger.info("Completed the request to start order process");
        return ResponseEntity.ok(processInstance);
    }

    private PartyType getBuyerParty(RFQSummary rfqSummary) throws IOException {
        // retrieve party info from the identity-service
        PartyType buyerParty = SpringBridge.getInstance().getiIdentityClientTyped().getParty(StartWithDocumentController.token,rfqSummary.getBuyerPartyId());
        // create the party if it does not exist
        if(buyerParty == null){
            // retrieve the party info from the database
            buyerParty = PartyPersistenceUtility.getParty(rfqSummary.getBuyerPartyId(),SpringBridge.getInstance().getFederationId(), true);
            boolean buyerPartyExists = buyerParty != null;
            // create the party
            if(!buyerPartyExists){
                buyerParty = new PartyType();
                PartyIdentificationType partyIdentification = new PartyIdentificationType();
                partyIdentification.setID(rfqSummary.getBuyerPartyId());
                buyerParty.setPartyIdentification(Arrays.asList(partyIdentification));
                buyerParty.setFederationInstanceID(SpringBridge.getInstance().getFederationId());

                PersonType person = new PersonType();
                person.setID("213");
                buyerParty.getPerson().add(person);
            }

            TextType partyName = new TextType();
            partyName.setValue(rfqSummary.getBuyerPartyName());
            partyName.setLanguageID("en");
            PartyNameType partyNameType = new PartyNameType();
            partyNameType.setName(partyName);
            buyerParty.setPartyName(Arrays.asList(partyNameType));

            ContactType contact = new ContactType();
            CommunicationType communicationType = new CommunicationType();
            CodeType codeType = new CodeType();
            codeType.setName("REST");
            codeType.setValue(rfqSummary.getEndpointOfTheBuyer());
            codeType.setListID(rfqSummary.getMessageName());
            codeType.setURI(rfqSummary.getProcessInstanceId());
            communicationType.setChannelCode(codeType);
            contact.setOtherCommunication(Arrays.asList(communicationType));
            buyerParty.setContact(contact);

            // if buyer party exists, update it
            if(buyerPartyExists){
                buyerParty = new JPARepositoryFactory().forCatalogueRepository(true).updateEntity(buyerParty);
            }
        }
        return buyerParty;
    }

    private void fillRfqSummaryWithPreviousDocument(RFQSummary rfqSummary){
        if(rfqSummary.getPreviousDocumentId() != null){
            IDocument iDocument = DocumentPersistenceUtility.getUBLDocument(rfqSummary.getPreviousDocumentId());

            BigDecimal numberOfProductsRequested;
            BigDecimal pricePerProduct;
            // QuotationType
            if(iDocument instanceof QuotationType){
                QuotationType quotation = (QuotationType) iDocument;
                pricePerProduct = quotation.getQuotationLine().get(0).getLineItem().getPrice().getPriceAmount().getValue();
                numberOfProductsRequested = quotation.getQuotationLine().get(0).getLineItem().getQuantity().getValue();
            }
            // OrderResponseSimpleType
            else {
                OrderResponseSimpleType orderResponse = (OrderResponseSimpleType) iDocument;
                OrderType order = (OrderType) DocumentPersistenceUtility.getUBLDocument(orderResponse.getOrderReference().getID(), DocumentType.ORDER);
                pricePerProduct = order.getOrderLine().get(0).getLineItem().getPrice().getPriceAmount().getValue();
                numberOfProductsRequested = order.getOrderLine().get(0).getLineItem().getQuantity().getValue();
            }
            CodeType communicationChannel = UBLUtility.getPartyCommunicationChannel(iDocument.getBuyerParty());

            if(rfqSummary.getBuyerPartyId() == null){
                rfqSummary.setBuyerPartyId(iDocument.getBuyerPartyId());
            }
            if(rfqSummary.getBuyerPartyName() == null){
                rfqSummary.setBuyerPartyName(iDocument.getBuyerPartyName().get(0).getName().getValue());
            }
            if(rfqSummary.getEndpointOfTheBuyer() == null && communicationChannel != null){
                rfqSummary.setEndpointOfTheBuyer(communicationChannel.getValue());
            }
            if(rfqSummary.getMessageName() == null && communicationChannel != null){
                rfqSummary.setMessageName(communicationChannel.getListID());
            }
            if(rfqSummary.getProcessInstanceId() == null && communicationChannel != null){
                rfqSummary.setProcessInstanceId(communicationChannel.getURI());
            }
            if(rfqSummary.getProductID() == null){
                rfqSummary.setProductID(CataloguePersistenceUtility.getCatalogueLineHjid(iDocument.getItemTypes().get(0).getCatalogueDocumentReference().getID(),iDocument.getItemTypes().get(0).getManufacturersItemIdentification().getID()).toString());
            }
            if(rfqSummary.getNumberOfProductsRequested() == null){
                rfqSummary.setNumberOfProductsRequested(numberOfProductsRequested);
            }
            if(rfqSummary.getPricePerProduct() == null){
                rfqSummary.setPricePerProduct(pricePerProduct);
            }

        }
    }
}
