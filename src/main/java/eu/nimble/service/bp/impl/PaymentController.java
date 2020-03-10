package eu.nimble.service.bp.impl;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.util.persistence.catalogue.PaymentPersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.invoice.InvoiceType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.validation.IValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Arrays;

@Controller
public class PaymentController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ExecutionContext executionContext;

    @ApiOperation(value = "",notes = "Checks whether the payment is done for the given order or not")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Checked the payment successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "There does not exist an order with the given id")
    })
    @RequestMapping(value = "/paymentDone/{orderId}",
            method = RequestMethod.GET)
    public ResponseEntity isPaymentDone(@ApiParam(value = "Identifier of the order for which the payment is to be checked", required = true) @PathVariable(value = "orderId", required = true) String orderId,
                                        @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to check whether the payment is done or not for order: %s", orderId);
        executionContext.setRequestLog(requestLog);

        logger.info(requestLog);

        if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        OrderType order = (OrderType) DocumentPersistenceUtility.getUBLDocument(orderId);
        if(order == null){
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_ORDER.toString(), Arrays.asList(orderId));
        }

        boolean isPaymentDone = PaymentPersistenceUtility.isPaymentDoneForOrder(orderId);
        logger.info("Completed request to check whether the payment is done or not for order: {}", orderId);
        return ResponseEntity.ok(isPaymentDone);
    }

    @ApiOperation(value = "",notes = "Creates an Invoice for the given order")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Created Invoice successfully"),
            @ApiResponse(code = 400, message = "The payment is already done for the given order"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "There does not exist an order with the given id")
    })
    @RequestMapping(value = "/paymentDone/{orderId}",
            method = RequestMethod.POST)
    public ResponseEntity paymentDone(@ApiParam(value = "Identifier of the order for which the payment is to be done", required = true) @PathVariable(value = "orderId", required = true) String orderId,
                                      @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Creating an Invoice for order: %s", orderId);
        executionContext.setRequestLog(requestLog);

        logger.info(requestLog);

        if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        OrderType order = (OrderType) DocumentPersistenceUtility.getUBLDocument(orderId);
        if(order == null){
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_ORDER.toString(), Arrays.asList(orderId));
        }

        boolean isPaymentDoneBefore = PaymentPersistenceUtility.isPaymentDoneForOrder(orderId);
        if(isPaymentDoneBefore){
            throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_PAYMENT_DONE.toString(), Arrays.asList(orderId));
        }

        GenericJPARepository catalogueRepository = new JPARepositoryFactory().forCatalogueRepository();
        InvoiceType invoice = PaymentPersistenceUtility.createInvoiceForOrder(orderId);

        catalogueRepository.persistEntity(invoice);

//        if(validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_TO_LOG_PAYMENTS)){
            String logstashUrl = SpringBridge.getInstance().getGenericConfig().getEfactoryLogstashUrl();
            if(logstashUrl.contentEquals("")){
                logger.info("Could not send payment log since no url set for efactory logstash");
            }
            else {
                try {
                    String body = PaymentPersistenceUtility.createJSONBodyForPayment(order);
                    logger.info("Body:{}",body);
                    HttpResponse<String> response = Unirest.post(logstashUrl)
                            .header("Content-Type", "application/json")
                            .header("accept", "*/*")
                            .body(body)
                            .asString();
                    if(response.getStatus() != 200 && response.getStatus() != 204){
                        throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_SEND_PAYMENT_LOG.toString(),Arrays.asList(logstashUrl,orderId));
                    }
                } catch (Exception e) {
                    throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_SEND_PAYMENT_LOG.toString(),Arrays.asList(logstashUrl,orderId),e);
                }
            }
//        }

        logger.info("Created an Invoice for order: {}", orderId);
        return ResponseEntity.ok(null);
    }
}
