package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.util.eFactory.AccountancyService;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.util.persistence.catalogue.PaymentPersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.bp.util.stripe.StripeClient;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.invoice.InvoiceType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.service.bp.exception.NimbleExceptionMessageCode;
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
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;

@Controller
public class PaymentController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ExecutionContext executionContext;
    @Autowired
    private AccountancyService accountancyService;
    @Autowired
    private StripeClient stripeClient;

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
                                      @ApiParam(value = "Identifier of the invoice", required = false) @RequestParam(value = "invoiceId", required = false) String invoiceId,
                                      @ApiParam(value = "Tracking URL of the invoice", required = false) @RequestParam(value = "invoiceUrl", required = false) String invoiceUrl,
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
        InvoiceType invoice = PaymentPersistenceUtility.createInvoiceForOrder(orderId,invoiceId,invoiceUrl);

        catalogueRepository.persistEntity(invoice);

//        if(validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_TO_LOG_PAYMENTS)){
            String body = PaymentPersistenceUtility.createJSONBodyForPayment(order);
            try {
                accountancyService.sendPaymentLog(body,orderId);
            } catch (Exception e) {
                throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_SEND_PAYMENT_LOG.toString(), Collections.singletonList(orderId),e);
            }
//        }

        logger.info("Created an Invoice for order: {}", orderId);
        return ResponseEntity.ok(null);
    }

    @ApiOperation(value = "",notes = "Retrieves the invoice for the specified order")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the invoice successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "There does not exist an order with the given id")
    })
    @RequestMapping(value = "/invoice/{orderId}",
            method = RequestMethod.GET)
    public ResponseEntity getInvoice(@ApiParam(value = "Identifier of the order for which the invoice is to be retrieved", required = true) @PathVariable(value = "orderId", required = true) String orderId,
                                        @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to retrieve invoice for order: %s", orderId);
        executionContext.setRequestLog(requestLog);

        logger.info(requestLog);

        if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        OrderType order = (OrderType) DocumentPersistenceUtility.getUBLDocument(orderId);
        if(order == null){
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_ORDER.toString(), Arrays.asList(orderId));
        }

        InvoiceType invoice = PaymentPersistenceUtility.getInvoiceForOrder(orderId);

        if(invoice == null){
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_INVOICE.toString(), Arrays.asList(orderId));
        }
        logger.info("Completed request to retrieve invoice for order: {}", orderId);

        String serializedInvoice = null;
        try {
            serializedInvoice = JsonSerializationUtility.serializeEntity(invoice);
        } catch (JsonProcessingException e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_SERIALIZE_INVOICE.toString());
        }
        return ResponseEntity.status(HttpStatus.OK).body(serializedInvoice);
    }

    @ApiOperation(value = "",notes = "Creates a Stripe payment intent for the given order")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Created the payment intent successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "There does not exist an order with the given id"),
            @ApiResponse(code = 500, message = "Unexpected error while creating the payment intent")
    })
    @RequestMapping(value = "/payment-intent/{orderId}",
            method = RequestMethod.POST)
    public ResponseEntity createPaymentIntent(@ApiParam(value = "Identifier of the order for which payment intent is to be created", required = true) @PathVariable(value = "orderId", required = true) String orderId,
                                              @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken){
        // set request log of ExecutionContext
        String requestLog = String.format("Incoming request to create payment intent for order: %s", orderId);
        executionContext.setRequestLog(requestLog);

        logger.info(requestLog);

        if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        try {
            OrderType order = (OrderType) DocumentPersistenceUtility.getUBLDocument(orderId);
            if(order == null){
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_ORDER.toString(), Collections.singletonList(orderId));
            }
            PartyType sellerParty = SpringBridge.getInstance().getiIdentityClientTyped().getParty(bearerToken,order.getSellerPartyId());
            if(sellerParty == null){
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_NO_PARTY.toString(), Collections.singletonList(order.getSellerPartyId()));
            }
            if(sellerParty.getStripeAccountId() == null){
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_NO_STRIPE_ACCOUNT.toString());
            }
            String clientSecret = this.stripeClient.createPaymentIntent(sellerParty.getStripeAccountId(),order);
            logger.info("Completed request to create payment intent for order: {}", orderId);
            return ResponseEntity.ok(clientSecret);
        } catch (Exception e){
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_CREATE_PAYMENT_INTENT.toString(), Collections.singletonList(orderId),e);
        }
    }
}
