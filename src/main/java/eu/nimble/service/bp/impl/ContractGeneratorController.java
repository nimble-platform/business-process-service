package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.contract.ContractGenerator;
import eu.nimble.service.bp.model.hyperjaxb.DocumentType;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ClauseType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.service.bp.exception.NimbleExceptionMessageCode;
import eu.nimble.utility.validation.IValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipOutputStream;

@Controller
public class ContractGeneratorController {
    private final Logger logger = LoggerFactory.getLogger(ContractGeneratorController.class);

    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ExecutionContext executionContext;

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
                                 @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken,HttpServletResponse response) throws Exception {
        try{
            // set request log of ExecutionContext
            String requestLog = String.format("Generating contract for the order with id : %s",orderId);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString(),true);
            }

            // check existence of Order
            OrderType order = (OrderType) DocumentPersistenceUtility.getUBLDocument(orderId, DocumentType.ORDER);
            if(order == null){
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_ORDER.toString(),Arrays.asList(orderId),true);
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
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GENERATE_CONTRACT.toString(),Arrays.asList(orderId),e,true);
        }

    }

    @CrossOrigin(origins = {"*"})
    @RequestMapping(value = "/contracts/terms-and-conditions",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.GET)
    public ResponseEntity getTermsAndConditions(@ApiParam(value = "Identifier of the seller party",required = true) @RequestParam(value = "sellerPartyId", required = true) String sellerPartyId,
                                                @ApiParam(value = "Identifier of the buyer party") @RequestParam(value = "buyerPartyId", required = false) String buyerPartyId,
                                                @ApiParam(value = "The selected incoterms while negotiating.<br>Example:DDP (Delivery Duty Paid)") @RequestParam(value = "incoterms", required = false) String incoterms,
                                                @ApiParam(value = "The selected trading term while negotiating.<br>Example:Cash_on_delivery") @RequestParam(value = "tradingTerm", required = false) String tradingTerm,
                                                @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
                                                @ApiParam(value = "" ,required=true ) @RequestHeader(value="initiatorFederationId", required=true) String initiatorFederationId) throws NimbleException {
        // set request log of ExecutionContext
        String requestLog = String.format("Generating Order Terms and Conditions clauses for seller party: %s, buyer party: %s",sellerPartyId, buyerPartyId);
        executionContext.setRequestLog(requestLog);

        logger.info(requestLog);

        try {
            // validate role
            if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            ContractGenerator contractGenerator = new ContractGenerator();

            List<ClauseType> clauses = contractGenerator.getTermsAndConditions(sellerPartyId,buyerPartyId,initiatorFederationId,incoterms,tradingTerm,bearerToken);

            logger.info("Generated Order Terms and Conditions clauses for seller party: {}, buyer party: {}",sellerPartyId, buyerPartyId);
            return ResponseEntity.ok(JsonSerializationUtility.getObjectMapper().writeValueAsString(clauses));
        }
        catch (Exception e){
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_GENERATE_ORDER_TERMS.toString(),e);
        }
    }

}