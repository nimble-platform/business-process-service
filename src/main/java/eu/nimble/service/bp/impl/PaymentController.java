package eu.nimble.service.bp.impl;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.util.persistence.catalogue.PaymentPersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.invoice.InvoiceType;
import eu.nimble.service.model.ubl.order.OrderType;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@Controller
public class PaymentController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private IValidationUtil validationUtil;

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
        logger.info("Incoming request to check whether the payment is done or not for order: {}", orderId);

        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }

        OrderType order = (OrderType) DocumentPersistenceUtility.getUBLDocument(orderId);
        if(order == null){
            String msg = String.format("The order with id: %s does not exist",orderId);
            logger.error(msg);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
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
        logger.info("Creating an Invoice for order: {}", orderId);

        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }

        OrderType order = (OrderType) DocumentPersistenceUtility.getUBLDocument(orderId);
        if(order == null){
            String msg = String.format("The order with id: %s does not exist",orderId);
            logger.error(msg);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
        }

        boolean isPaymentDoneBefore = PaymentPersistenceUtility.isPaymentDoneForOrder(orderId);
        if(isPaymentDoneBefore){
            String msg = String.format("The payment is already done for the order: %s",orderId);
            logger.error(msg);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
        }

        GenericJPARepository catalogueRepository = new JPARepositoryFactory().forCatalogueRepository();
        InvoiceType invoice = PaymentPersistenceUtility.createInvoiceForOrder(orderId);

        catalogueRepository.persistEntity(invoice);

        if(validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_TO_LOG_PAYMENTS)){
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
                        logger.error("Failed send payment log to url {} for order {}",logstashUrl,orderId);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                    }
                } catch (Exception e) {
                    logger.error("Failed send payment log to url {} for order {}",logstashUrl,orderId);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                }
            }
        }

        logger.info("Created an Invoice for order: {}", orderId);
        return ResponseEntity.ok(null);
    }
}
