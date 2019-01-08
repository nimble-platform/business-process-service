package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.impl.model.statistics.BusinessProcessCount;
import eu.nimble.service.bp.impl.model.statistics.NonOrderedProducts;
import eu.nimble.service.bp.impl.model.statistics.OverallStatistics;
import eu.nimble.service.bp.impl.util.bp.BusinessProcessUtility;
import eu.nimble.service.bp.impl.util.camunda.CamundaEngine;
import eu.nimble.service.bp.impl.util.controller.InputValidatorUtil;
import eu.nimble.service.bp.impl.util.controller.ValidationResponse;
import eu.nimble.service.bp.impl.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.impl.util.persistence.catalogue.StatisticsPersistenceUtility;
import eu.nimble.service.bp.swagger.model.Transaction;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JsonSerializationUtility;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.ws.rs.HEAD;
import java.util.ArrayList;
import java.util.List;

@Api(value = "statistics", description = "The statistics API")
@RequestMapping(value = "/statistics")
@Controller
public class StatisticsController {
    private final Logger logger = LoggerFactory.getLogger(StatisticsController.class);

    @ApiOperation(value = "",notes = "Get the total number of process instances which require an action")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the total number of process instances which require an action successfully",response = int.class),
            @ApiResponse(code = 400, message = "Invalid role")
    })
    @RequestMapping(value = "/total-number/business-process/action-required",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getActionRequiredProcessCount(@ApiParam(value = "The identifier of the party whose action required process count will be received", required = true) @RequestParam(value = "partyId", required = true) Integer partyId,
                                                        @ApiParam(value = "Whether the group which contains process instances is archived or not.", defaultValue = "false") @RequestParam(value = "archived", required = true, defaultValue="false") Boolean archived,
                                                        @ApiParam(value = "Role of the party in the business process.\nPossible values: seller,buyer", required = true) @RequestParam(value = "role", required = true, defaultValue = "seller") String role){
        logger.info("Getting total number of process instances which require an action for party id:{},archived: {}, role: {}",partyId,archived,role);

        // check role
        ValidationResponse response = InputValidatorUtil.checkRole(role, false);
        if (response.getInvalidResponse() != null) {
            return response.getInvalidResponse();
        }

        long count = StatisticsPersistenceUtility.getActionRequiredProcessCount(String.valueOf(partyId),role,archived);
        logger.info("Retrieved total number of process instances which require an action for company id:{},archived: {}, role: {}",partyId,archived,role);
        return ResponseEntity.ok(count);
    }

    @ApiOperation(value = "",notes = "Get the total number (active / completed) of specified business process")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the number of processes successfully",response = int.class)
    })
    @RequestMapping(value = "/total-number/business-process",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getProcessCount(@ApiParam(value = "Business process type.\nExamples:ORDER,NEGOTIATION,ITEM_INFORMATION_REQUEST", required = false) @RequestParam(value = "businessProcessType", required = false) String businessProcessType,
                                          @ApiParam(value = "Start date (DD-MM-YYYY) of the process", required = false) @RequestParam(value = "startDate", required = false) String startDateStr,
                                          @ApiParam(value = "End date (DD-MM-YYYY) of the process", required = false) @RequestParam(value = "endDate", required = false) String endDateStr,
                                          @ApiParam(value = "Identifier of the party as specified by the identity service", required = false) @RequestParam(value = "partyId", required = false) Integer partyId,
                                          @ApiParam(value = "Role of the party in the business process.\nPossible values:seller,buyer", required = false) @RequestParam(value = "role", required = false, defaultValue = "seller") String role,
                                          @ApiParam(value = "State of the transaction.\nPossible values:WaitingResponse,Approved,Denied", required = false) @RequestParam(value = "status", required = false) String status) {

        try {
            logger.info("Getting total number of documents for start date: {}, end date: {}, type: {}, party id: {}, role: {}, state: {}", startDateStr, endDateStr, businessProcessType, partyId, role, status);
            ValidationResponse response;

            // check start date
            response = InputValidatorUtil.checkDate(startDateStr, true);
            if (response.getInvalidResponse() != null) {
                return response.getInvalidResponse();
            }

            // check end date
            response = InputValidatorUtil.checkDate(endDateStr, true);
            if (response.getInvalidResponse() != null) {
                return response.getInvalidResponse();
            }

            // check role
            response = InputValidatorUtil.checkRole(role, true);
            if (response.getInvalidResponse() != null) {
                return response.getInvalidResponse();
            }
            role = response.getValidatedObject() != null ? (String) response.getValidatedObject() : null;

            // check business process type
            response = InputValidatorUtil.checkBusinessProcessType(businessProcessType, true);
            if (response.getInvalidResponse() != null) {
                return response.getInvalidResponse();
            }

            // get initiating document for the business process
            List<String> documentTypes = new ArrayList<>();
            if (businessProcessType != null) {
                documentTypes.add(BusinessProcessUtility.getInitialDocumentForProcess(businessProcessType).toString());

                // if there is no process specified, get the document list for all business processes
            } else {
                List<Transaction.DocumentTypeEnum> initialDocuments = BusinessProcessUtility.getInitialDocumentsForAllProcesses();
                initialDocuments.stream().forEach(type -> documentTypes.add(type.toString()));
            }

            // check status
            response = InputValidatorUtil.checkStatus(status, true);
            if (response.getInvalidResponse() != null) {
                return response.getInvalidResponse();
            }
            status = response.getValidatedObject() != null ? (String) response.getValidatedObject() : null;

            int count = ProcessDocumentMetadataDAOUtility.getTransactionCount(partyId, documentTypes, role, startDateStr, endDateStr, status);

            logger.info("Number of business process for start date: {}, end date: {}, type: {}, party id: {}, role: {}, state: {}", startDateStr, endDateStr, businessProcessType, partyId, role, status);
            return ResponseEntity.ok().body(count);

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting the total number for business process type: %s, start date: %s, end date: %s, partyId id: %s, role: %s, state: %s", businessProcessType, startDateStr, endDateStr, partyId, role, status), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "",notes = "Get the total number (active / completed) of specified business process")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the number of processes successfully",response = BusinessProcessCount.class)
    })
    @RequestMapping(value = "/total-number/business-process/break-down",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getProcessCountBreakDown(@ApiParam(value = "Start date (DD-MM-YYYY) of the process", required = false) @RequestParam(value = "startDate", required = false) String startDateStr,
                                                   @ApiParam(value = "End date (DD-MM-YYYY) of the process", required = false) @RequestParam(value = "endDate", required = false) String endDateStr,
                                                   @ApiParam(value = "Identifier of the party as specified by the identity service", required = false) @RequestParam(value = "partyId", required = false) Integer partyId,
                                                   @ApiParam(value = "Role of the party in the business process.\nPossible values:seller,buyer",required = true) @RequestParam(value = "role",required = true,defaultValue = "seller") String role,
                                                   @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {

        try {
            logger.info("Getting total number of documents for start date: {}, end date: {}, party id: {}, role: {}", startDateStr, endDateStr, partyId, role);
            ValidationResponse response;

            // check start date
            response = InputValidatorUtil.checkDate(startDateStr, true);
            if (response.getInvalidResponse() != null) {
                return response.getInvalidResponse();
            }

            // check end date
            response = InputValidatorUtil.checkDate(endDateStr, true);
            if (response.getInvalidResponse() != null) {
                return response.getInvalidResponse();
            }

            BusinessProcessCount counts = ProcessDocumentMetadataDAOUtility.getGroupTransactionCounts(partyId, startDateStr, endDateStr,role,bearerToken);
            logger.info("Number of business process for start date: {}, end date: {}, company id: {}, role: {}", startDateStr, endDateStr, partyId, role);
            return ResponseEntity.ok().body(counts);

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting the total number for start date: %s, end date: %s, party id: %s, role: %s", startDateStr, endDateStr, partyId, role), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "",notes = "Get the products that are not ordered")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the products that are not ordered",response = String.class)
    })
    @RequestMapping(value = "/non-ordered",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getNonOrderedProducts(@ApiParam(value = "Identifier of the party as specified by the identity service", required = false) @RequestParam(value = "partyId", required = false) Integer partyId) {
        try {
            logger.info("Getting non-ordered products for party id: {}", partyId);

            NonOrderedProducts nonOrderedProducts = StatisticsPersistenceUtility.getNonOrderedProducts(partyId);
            String serializedResponse = JsonSerializationUtility.getObjectMapperForFilledFields().writeValueAsString(nonOrderedProducts);
            logger.info("Retrieved the products that are not ordered for company id: {}", partyId);
            return ResponseEntity.ok().body(serializedResponse);

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting the total number for party id: %s", partyId), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "",notes = "Get the trading volume")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the trading volume successfully",response = double.class)
    })
    @RequestMapping(value = "/trading-volume",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getTradingVolume(@ApiParam(value = "Start date (DD-MM-YYYY) of the transaction", required = false) @RequestParam(value = "startDate", required = false) String startDateStr,
                                          @ApiParam(value = "End date (DD-MM-YYYY) of the transaction", required = false) @RequestParam(value = "endDate", required = false) String endDateStr,
                                          @ApiParam(value = "Identifier of the party as specified by the identity service", required = false) @RequestParam(value = "partyId", required = false) Integer partyId,
                                          @ApiParam(value = "Role of the party in the business process.\nPossible values: SELLER,BUYER", required = false) @RequestParam(value = "role", required = false, defaultValue = "SELLER") String role,
                                          @ApiParam(value = "State of the transaction.\nPossible values:WaitingResponse,Approved,Denied", required = false) @RequestParam(value = "status", required = false) String status) {
        try {
            logger.info("Getting total number of documents for start date: {}, end date: {}, party id: {}, role: {}, state: {}", startDateStr, endDateStr, partyId, role, status);
            ValidationResponse response;

            // check start date
            response = InputValidatorUtil.checkDate(startDateStr, true);
            if (response.getInvalidResponse() != null) {
                return response.getInvalidResponse();
            }

            // check end date
            response = InputValidatorUtil.checkDate(endDateStr, true);
            if (response.getInvalidResponse() != null) {
                return response.getInvalidResponse();
            }

            // check role
            response = InputValidatorUtil.checkRole(role, true);
            if (response.getInvalidResponse() != null) {
                return response.getInvalidResponse();
            }
            role = response.getValidatedObject() != null ? (String) response.getValidatedObject() : null;

            // check status
            response = InputValidatorUtil.checkStatus(status, true);
            if (response.getInvalidResponse() != null) {
                return response.getInvalidResponse();
            }
            status = response.getValidatedObject() != null ? (String) response.getValidatedObject() : null;

            double tradingVolume = StatisticsPersistenceUtility.getTradingVolume(partyId, role, startDateStr, endDateStr, status);

            logger.info("Number of business process for start date: {}, end date: {}, party id: {}, role: {}, state: {}", startDateStr, endDateStr, partyId, role, status);
            return ResponseEntity.ok().body(tradingVolume);

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting the total number for start date: %s, end date: %s, party id: %s, role: %s, state: %s", startDateStr, endDateStr, partyId, role, status), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "Gets the inactive companies. (Companies that have not initiated a business process)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the inactive companies")
    })
    @RequestMapping(value = "/inactive-companies",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getInactiveCompanies(@ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        try {
            logger.info("Getting inactive companies");

            List<PartyType> inactiveCompanies = StatisticsPersistenceUtility.getInactiveCompanies(bearerToken);
            String serializedResponse = JsonSerializationUtility.getObjectMapperForFilledFields().writeValueAsString(inactiveCompanies);
            logger.info("Retrieved the inactive companies");
            return ResponseEntity.ok().body(serializedResponse);

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting the inactive companies"), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "Gets average response time for the party in terms of days")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved average response time for the party")
    })
    @RequestMapping(value = "/response-time",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getAverageResponseTime(@ApiParam(value = "Identifier of the party as specified by the identity service") @RequestParam(value = "partyId") String partyId,
                                                 @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken){
        logger.info("Getting average response time for the party with id: {}",partyId);
        double averageResponseTime;
        try {
            averageResponseTime = StatisticsPersistenceUtility.calculateAverageResponseTime(partyId);
        }
        catch (Exception e){
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting average response time for the party with id: %s", partyId), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        logger.info("Retrieved average response time for the party with id: {}",partyId);
        return ResponseEntity.ok(averageResponseTime);
    }

    @ApiOperation(value = "Gets average negotiation time for the party in terms of days")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved average negotiation time for the party")
    })
    @RequestMapping(value = "/negotiation-time",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getAverageNegotiationTime(@ApiParam(value = "Identifier of the party as specified by the identity service") @RequestParam(value = "partyID") String partyId,
                                                    @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken){
        logger.info("Getting average negotiation time for the party with id: {}",partyId);
        double averageNegotiationTime = StatisticsPersistenceUtility.calculateAverageNegotiationTime(partyId,bearerToken);
        logger.info("Retrieved average negotiation time for the party with id: {}",partyId);
        return ResponseEntity.ok(averageNegotiationTime);
    }

    @ApiOperation(value = "Gets statistics (average negotiation time,average response time,trading volume and number of transactions) for the party")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved average negotiation time for the party",response = OverallStatistics.class)
    })
    @RequestMapping(value = "/overall",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getStatistics(@ApiParam(value = "Identifier of the party as specified by the identity service") @RequestParam(value = "partyId") String partyId,
                                        @ApiParam(value = "Role of the party in the business process.\nPossible values:SELLER,BUYER", required = false) @RequestParam(value = "role", required = false, defaultValue = "SELLER") String role,
                                        @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken){
        logger.info("Getting statistics for the party with id: {}",partyId);
        OverallStatistics statistics = new OverallStatistics();
        try {
            statistics.setAverageNegotiationTime((double)getAverageNegotiationTime(partyId,bearerToken).getBody());
            statistics.setAverageResponseTime((double)getAverageResponseTime(partyId,bearerToken).getBody());
            statistics.setTradingVolume((double) getTradingVolume(null,null,Integer.valueOf(partyId), role,null).getBody());
            statistics.setNumberOfTransactions((int)getProcessCount(null,null,null,Integer.valueOf(partyId),role,null).getBody());
        }
        catch (Exception e){
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting statistics for the party with id: %s", partyId), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        logger.info("Retrieved statistics for the party with id: {}",partyId);
        return ResponseEntity.ok(statistics);
    }
}
