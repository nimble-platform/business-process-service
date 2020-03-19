package eu.nimble.service.bp.util.migration.r14;

import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType;
import eu.nimble.service.model.ubl.transportexecutionplanrequest.TransportExecutionPlanRequestType;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

@ApiIgnore
@Controller
public class R14MigrationController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ExecutionContext executionContext;

    @ApiOperation(value = "", notes = "Updates business process documents (Request for Quotation, Quotation, Order and Transport Execution Plan) to handle the changes on LineItemType model.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Updated documents to handle line item updates successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while updating documents to handle line item updates")
    })
    @RequestMapping(value = "/r14/migration/line-item-updates",
            produces = {"application/json"},
            method = RequestMethod.PATCH)
    public ResponseEntity adaptDocumentsForLineItemUpdates(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken
    ) {
        // set request log of ExecutionContext
        String requestLog = "Incoming request to adapt documents to handle line item updates";
        executionContext.setRequestLog(requestLog);

        logger.info(requestLog);

        // validate role
        if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        GenericJPARepository catalogueRepository = new JPARepositoryFactory().forCatalogueRepositoryMultiTransaction(true);
        try{
            logger.info("Updating orders");
            List<OrderType> orders = catalogueRepository.getEntities(OrderType.class);
            logger.warn("There are {} orders.",orders.size());
            for (OrderType order : orders) {
                logger.info("Updating the order with id: {}",order.getID());

                order.getOrderLine().get(0).getLineItem().setPaymentMeans(order.getPaymentMeans());
                order.getOrderLine().get(0).getLineItem().setPaymentTerms(order.getPaymentTerms());

                order.setPaymentTerms(null);
                order.setPaymentMeans(null);

                order = catalogueRepository.updateEntity(order);
                // update document cache
                SpringBridge.getInstance().getCacheHelper().putDocument(order);
            }

            logger.info("Updating rfqs");
            List<RequestForQuotationType> rfqs = catalogueRepository.getEntities(RequestForQuotationType.class);
            logger.warn("There are {} rfqs.",rfqs.size());
            for (RequestForQuotationType rfq : rfqs) {
                logger.info("Updating the fields of rfq with id: {}",rfq.getID());

                boolean isDataMonitoringRequested = rfq.isDataMonitoringRequested() != null && rfq.isDataMonitoringRequested();
                rfq.getRequestForQuotationLine().get(0).getLineItem().setDataMonitoringRequested(isDataMonitoringRequested);
                rfq.getRequestForQuotationLine().get(0).getLineItem().setPaymentTerms(rfq.getPaymentTerms());
                rfq.getRequestForQuotationLine().get(0).getLineItem().setPaymentMeans(rfq.getPaymentMeans());

                rfq.getRequestForQuotationLine().get(0).getLineItem().getClause().clear();
                rfq.getRequestForQuotationLine().get(0).getLineItem().getTradingTerms().clear();

                rfq.getRequestForQuotationLine().get(0).getLineItem().getClause().addAll(rfq.getTermOrCondition());
                rfq.getRequestForQuotationLine().get(0).getLineItem().getTradingTerms().addAll(rfq.getTradingTerms());

                rfq.setPaymentTerms(null);
                rfq.setPaymentMeans(null);
                rfq.getTermOrCondition().clear();
                rfq.getTradingTerms().clear();

                if(rfq.getRequestForQuotationLine().get(0).getLineItem().getDelivery().size() > 0 && rfq.getRequestForQuotationLine().get(0).getLineItem().getDelivery().get(0).getShipment() != null){
                    rfq.getRequestForQuotationLine().get(0).getLineItem().getDelivery().get(0).getShipment().getGoodsItem().get(0).setQuantity(rfq.getRequestForQuotationLine().get(0).getLineItem().getDelivery().get(0).getShipment().getTotalTransportHandlingUnitQuantity());
                    rfq.getRequestForQuotationLine().get(0).getLineItem().getDelivery().get(0).getShipment().setTotalTransportHandlingUnitQuantity(null);
                }

                rfq = catalogueRepository.updateEntity(rfq);
                // update document cache
                SpringBridge.getInstance().getCacheHelper().putDocument(rfq);
            }

            logger.info("Updating quotations");
            List<QuotationType> quotations = catalogueRepository.getEntities(QuotationType.class);
            logger.warn("There are {} quotations.",quotations.size());
            for (QuotationType quotation : quotations) {
                logger.info("Updating the quotation with id: {}",quotation.getID());

                boolean isDataMonitoringPromised = quotation.isDataMonitoringPromised() != null && quotation.isDataMonitoringPromised();
                quotation.getQuotationLine().get(0).getLineItem().setDataMonitoringRequested(isDataMonitoringPromised);
                quotation.getQuotationLine().get(0).getLineItem().setPaymentTerms(quotation.getPaymentTerms());
                quotation.getQuotationLine().get(0).getLineItem().setPaymentMeans(quotation.getPaymentMeans());

                quotation.getQuotationLine().get(0).getLineItem().getClause().clear();
                quotation.getQuotationLine().get(0).getLineItem().getTradingTerms().clear();

                quotation.getQuotationLine().get(0).getLineItem().getClause().addAll(quotation.getTermOrCondition());
                quotation.getQuotationLine().get(0).getLineItem().getTradingTerms().addAll(quotation.getTradingTerms());

                quotation.setPaymentTerms(null);
                quotation.setPaymentMeans(null);
                quotation.getTermOrCondition().clear();
                quotation.getTradingTerms().clear();

                quotation = catalogueRepository.updateEntity(quotation);
                // update document cache
                SpringBridge.getInstance().getCacheHelper().putDocument(quotation);
            }

            logger.info("Updating transport execution plan requests");
            List<TransportExecutionPlanRequestType> tepRequests = catalogueRepository.getEntities(TransportExecutionPlanRequestType.class);
            logger.warn("There are {} transport execution plan requests.",tepRequests.size());
            for (TransportExecutionPlanRequestType transportExecutionPlanRequest : tepRequests) {
                logger.info("Updating transport execution plan request with id: {}",transportExecutionPlanRequest.getID());

                transportExecutionPlanRequest.getConsignment().get(0).getConsolidatedShipment().get(0).getGoodsItem().get(0).setQuantity(transportExecutionPlanRequest.getConsignment().get(0).getConsolidatedShipment().get(0).getTotalTransportHandlingUnitQuantity());
                transportExecutionPlanRequest.getConsignment().get(0).getConsolidatedShipment().get(0).setTotalTransportHandlingUnitQuantity(null);

                transportExecutionPlanRequest = catalogueRepository.updateEntity(transportExecutionPlanRequest);
                // update document cache
                SpringBridge.getInstance().getCacheHelper().putDocument(transportExecutionPlanRequest);
            }

            catalogueRepository.commit();
        }
        catch (Exception e){
            catalogueRepository.rollback();
            String msg = "Unexpected error while adapting documents to handle line item updates";
            logger.error(msg,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
        }

        logger.info("Completed request to adapt documents to handle line item updates");
        return ResponseEntity.ok(null);
    }
}
