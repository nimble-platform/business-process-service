package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.contract.ContractGenerator;
import eu.nimble.service.bp.model.hyperjaxb.DocumentType;
import eu.nimble.service.bp.util.HttpResponseUtil;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ClauseType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.validation.IValidationUtil;
import eu.nimble.utility.validation.ValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;
import java.util.zip.ZipOutputStream;

@Controller
public class ContractGeneratorController {
    private final Logger logger = LoggerFactory.getLogger(ContractGeneratorController.class);

    @Autowired
    private IValidationUtil validationUtil;

    @CrossOrigin(origins = {"*"})
    @ApiOperation(value = "",notes = "Generates a contract bundle for the given order id")
    @ApiResponses(value = {
            @ApiResponse(code = 200,message = "Generated the contract bundle successfully"),
            @ApiResponse(code = 401,message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404,message = "No order exists for the given id"),
            @ApiResponse(code = 500, message = "Unexpected error contract for the order with the given id")
    })
    @RequestMapping(value = "/contracts/create-bundle",
            method = RequestMethod.GET,
            produces = {"application/zip"})
    public void generateContract(@ApiParam(value = "Identifier of the order for which the contract will be generated", required = true) @RequestParam(value = "orderId", required = true) String orderId,
                                 @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken,HttpServletResponse response){
        try{
            logger.info("Generating contract for the order with id : {}",orderId);
            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES)) {
                eu.nimble.utility.HttpResponseUtil.writeMessageServletResponseAndLog(response, "Invalid role", HttpStatus.UNAUTHORIZED);
                return;
            }

            // check existence of Order
            OrderType order = (OrderType) DocumentPersistenceUtility.getUBLDocument(orderId, DocumentType.ORDER);
            if(order == null){
                String msg = String.format("No order exists for the given id : %s",orderId);
                logger.error(msg);
                response.setStatus(HttpStatus.NOT_FOUND.value());
                try{
                    response.getOutputStream().write(msg.getBytes());
                }
                catch (Exception e1){
                    logger.error("Failed to write the error message to the output stream",e1);
                }
            }
            else{
                ZipOutputStream zos = new ZipOutputStream(response.getOutputStream());

                ContractGenerator contractGenerator = new ContractGenerator();
                contractGenerator.generateContract(order,zos);

                response.flushBuffer();

                zos.close();

                logger.info("Generated contract for the order with id : {}",orderId);
            }
        }
        catch (Exception e){
            logger.error("Failed to generate contract for the order with id : {}",orderId,e);

            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            try{
                response.getOutputStream().write("Failed to generate contract".getBytes());
            }
            catch (Exception e1){
                logger.error("Failed to write the error message to the output stream",e1);
            }
        }

    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/contracts/terms-and-conditions",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.GET)
    public ResponseEntity getTermsAndConditions(@ApiParam(value = "Identifier of the order for which terms and conditions are generated", required = false) @RequestParam(value = "orderId", required = false) String orderId,
                                                @ApiParam(value = "Identifier of the request for quotation for which terms and conditions are generated", required = false) @RequestParam(value = "rfqId", required = false) String rfqId,
                                                @ApiParam(value = "Identifier of the seller party",required = true) @RequestParam(value = "sellerPartyId", required = true) String sellerPartyId,
                                                @ApiParam(value = "Identifier of the buyer party") @RequestParam(value = "buyerPartyId", required = false) String buyerPartyId,
                                                @ApiParam(value = "The selected incoterms while negotiating.<br>Example:DDP (Delivery Duty Paid)") @RequestParam(value = "incoterms", required = false) String incoterms,
                                                @ApiParam(value = "The selected trading term while negotiating.<br>Example:Cash_on_delivery") @RequestParam(value = "tradingTerm", required = false) String tradingTerm,
                                                @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken){
        logger.info("Generating Order Terms and Conditions clauses for the order : {}, rfq: {}, seller party: {}, buyer party: {}",orderId,rfqId,sellerPartyId, buyerPartyId);

        try {
            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES)) {
                return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
            }

            ContractGenerator contractGenerator = new ContractGenerator();

            List<ClauseType> clauses = contractGenerator.getTermsAndConditions(orderId,rfqId,sellerPartyId,buyerPartyId,incoterms,tradingTerm,bearerToken);

            logger.info("Generated Order Terms and Conditions clauses for the order : {}, rfq: {}, seller party: {}, buyer party: {}",orderId,rfqId,sellerPartyId, buyerPartyId);
            return ResponseEntity.ok(JsonSerializationUtility.getObjectMapper().writeValueAsString(clauses));
        }
        catch (Exception e){
            logger.error("Failed to generate Order Terms and Conditions clauses",e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to generate Order Terms and Conditions clauses");
        }
    }

}