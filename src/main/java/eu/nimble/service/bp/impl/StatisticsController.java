package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.impl.model.statistics.BusinessProcessCount;
import eu.nimble.service.bp.impl.model.statistics.NonOrderedProducts;
import eu.nimble.service.bp.impl.model.statistics.OverallStatistics;
import eu.nimble.service.bp.impl.util.camunda.CamundaEngine;
import eu.nimble.service.bp.impl.util.controller.HttpResponseUtil;
import eu.nimble.service.bp.impl.util.controller.InputValidatorUtil;
import eu.nimble.service.bp.impl.util.controller.ValidationResponse;
import eu.nimble.service.bp.impl.util.persistence.DAOUtility;
import eu.nimble.service.bp.impl.util.persistence.StatisticsDAOUtility;
import eu.nimble.service.bp.impl.util.serialization.Serializer;
import eu.nimble.service.bp.swagger.model.Transaction;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
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

import java.util.ArrayList;
import java.util.List;

@Api(value = "statistics", description = "The statistics API")
@RequestMapping(value = "/statistics")
@Controller
public class StatisticsController {
    private final Logger logger = LoggerFactory.getLogger(StatisticsController.class);

    @ApiOperation(value = "",notes = "Get the total number (active / completed) of specified business process")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the number of processes successfully",response = int.class)
    })
    @RequestMapping(value = "/total-number/business-process",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getProcessCount(@ApiParam(value = "Business process type. ", required = false) @RequestParam(value = "businessProcessType", required = false) String businessProcessType,
                                          @ApiParam(value = "Start date (DD-MM-YYYY)", required = false) @RequestParam(value = "startDate", required = false) String startDateStr,
                                          @ApiParam(value = "End date (DD-MM-YYYY)", required = false) @RequestParam(value = "endDate", required = false) String endDateStr,
                                          @ApiParam(value = "Company ID", required = false) @RequestParam(value = "companyId", required = false) Integer companyId,
                                          @ApiParam(value = "Role in business process. Can be seller or buyer", required = false) @RequestParam(value = "role", required = false, defaultValue = "seller") String role,
                                          @ApiParam(value = "State of transaction. Can be WaitingResponse, Approved or Denied", required = false) @RequestParam(value = "status", required = false) String status) {

        try {
            logger.info("Getting total number of documents for start date: {}, end date: {}, type: {}, company id: {}, role: {}, state: {}", startDateStr, endDateStr, businessProcessType, companyId, role, status);
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
                documentTypes.add(CamundaEngine.getInitialDocumentForProcess(businessProcessType).toString());

                // if there is no process specified, get the document list for all business processes
            } else {
                List<Transaction.DocumentTypeEnum> initialDocuments = CamundaEngine.getInitialDocumentsForAllProcesses();
                initialDocuments.stream().forEach(type -> documentTypes.add(type.toString()));
            }

            // check status
            response = InputValidatorUtil.checkStatus(status, true);
            if (response.getInvalidResponse() != null) {
                return response.getInvalidResponse();
            }
            status = response.getValidatedObject() != null ? (String) response.getValidatedObject() : null;

            int count = DAOUtility.getTransactionCount(companyId, documentTypes, role, startDateStr, endDateStr, status);

            logger.info("Number of business process for start date: {}, end date: {}, type: {}, company id: {}, role: {}, state: {}", startDateStr, endDateStr, businessProcessType, companyId, role, status);
            return ResponseEntity.ok().body(count);

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting the total number for business process type: %s, start date: %s, end date: %s, company id: %s, role: %s, state: %s", businessProcessType, startDateStr, endDateStr, companyId, role, status), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "",notes = "Get the total number (active / completed) of specified business process")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the number of processes successfully",response = BusinessProcessCount.class)
    })
    @RequestMapping(value = "/total-number/business-process/break-down",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getProcessCountBreakDown(@ApiParam(value = "Start date (DD-MM-YYYY)", required = false) @RequestParam(value = "startDate", required = false) String startDateStr,
                                                   @ApiParam(value = "End date (DD-MM-YYYY)", required = false) @RequestParam(value = "endDate", required = false) String endDateStr,
                                                   @ApiParam(value = "Company ID", required = false) @RequestParam(value = "companyId", required = false) Integer companyId,
                                                   @ApiParam(value = "Role in business process. Can be seller or buyer",required = true) @RequestParam(value = "role",required = true,defaultValue = "seller") String role,
                                                   @ApiParam(value = "" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {

        try {
            logger.info("Getting total number of documents for start date: {}, end date: {}, company id: {}, role: {}", startDateStr, endDateStr, companyId, role);
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

            BusinessProcessCount counts = DAOUtility.getGroupTransactionCounts(companyId, startDateStr, endDateStr,role,bearerToken);
            logger.info("Number of business process for start date: {}, end date: {}, company id: {}, role: {}", startDateStr, endDateStr, companyId, role);
            return ResponseEntity.ok().body(counts);

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting the total number for start date: %s, end date: %s, company id: %s, role: %s", startDateStr, endDateStr, companyId, role), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "",notes = "Get the products that are not ordered")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the products that are not ordered",response = String.class)
    })
    @RequestMapping(value = "/non-ordered",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getNonOrderedProducts(@ApiParam(value = "Company ID", required = false) @RequestParam(value = "companyId", required = false) Integer companyId) {
        try {
            logger.info("Getting non-ordered products for company id: {}", companyId);

            NonOrderedProducts nonOrderedProducts = StatisticsDAOUtility.getNonOrderedProducts(companyId);
            String serializedResponse = Serializer.getDefaultObjectMapperForFilledFields().writeValueAsString(nonOrderedProducts);
            logger.info("Retrieved the products that are not ordered for company id: {}", companyId);
            return ResponseEntity.ok().body(serializedResponse);

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting the total number for company id: %s", companyId), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "",notes = "Get the trading volume")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the trading volume successfully",response = double.class)
    })
    @RequestMapping(value = "/trading-volume",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getTradingVolume(@ApiParam(value = "Start date (DD-MM-YYYY)", required = false) @RequestParam(value = "startDate", required = false) String startDateStr,
                                          @ApiParam(value = "End date (DD-MM-YYYY)", required = false) @RequestParam(value = "endDate", required = false) String endDateStr,
                                          @ApiParam(value = "Company ID", required = false) @RequestParam(value = "companyId", required = false) Integer companyId,
                                          @ApiParam(value = "Role in business process. Can be SELLER or BUYER", required = false) @RequestParam(value = "role", required = false, defaultValue = "SELLER") String role,
                                          @ApiParam(value = "State of transaction. Can be WaitingResponse, Approved or Denied", required = false) @RequestParam(value = "status", required = false) String status) {
        try {
            logger.info("Getting total number of documents for start date: {}, end date: {}, company id: {}, role: {}, state: {}", startDateStr, endDateStr, companyId, role, status);
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

            double tradingVolume = StatisticsDAOUtility.getTradingVolume(companyId, role, startDateStr, endDateStr, status);

            logger.info("Number of business process for start date: {}, end date: {}, company id: {}, role: {}, state: {}", startDateStr, endDateStr, companyId, role, status);
            return ResponseEntity.ok().body(tradingVolume);

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting the total number for start date: %s, end date: %s, company id: %s, role: %s, state: %s", startDateStr, endDateStr, companyId, role, status), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "Gets the inactive companies. (Companies that have not initiated a business process)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the inactive companies")
    })
    @RequestMapping(value = "/inactive-companies",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getInactiveCompanies(@ApiParam(value = "Start date", required = false) @RequestParam(value = "startDate", required = false) String startDateStr,
                                               @ApiParam(value = "End date", required = false) @RequestParam(value = "endDate", required = false) String endDateStr,
                                               @ApiParam(value = "" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        try {
            logger.info("Getting inactive companies for start date: {}, end date: {}", startDateStr, endDateStr);
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

            List<PartyType> inactiveCompanies = StatisticsDAOUtility.getInactiveCompanies(startDateStr, endDateStr, bearerToken);
            String serializedResponse = Serializer.getDefaultObjectMapperForFilledFields().writeValueAsString(inactiveCompanies);
            logger.info("Retrieved the inactive companies for start date: {}, end date: {}", startDateStr, endDateStr);
            return ResponseEntity.ok().body(serializedResponse);

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting the inactive companies for start date: %s, end date: %s", startDateStr, endDateStr), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "Gets average response time for the party in terms of days")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved average response time for the party")
    })
    @RequestMapping(value = "/response-time",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getAverageResponseTime(@RequestParam(value = "partyID") String partyID,
                                                 @ApiParam(value = "" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken){
        logger.info("Getting average response time for the party with id: {}",partyID);
        double averageResponseTime;
        try {
            averageResponseTime = StatisticsDAOUtility.calculateAverageResponseTime(partyID,bearerToken);
        }
        catch (Exception e){
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting average response time for the party with id: %s", partyID), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        logger.info("Retrieved average response time for the party with id: {}",partyID);
        return ResponseEntity.ok(averageResponseTime);
    }

    @ApiOperation(value = "Gets average negotiation time for the party in terms of days")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved average negotiation time for the party")
    })
    @RequestMapping(value = "/negotiation-time",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getAverageNegotiationTime(@RequestParam(value = "partyID") String partyID,
                                                    @ApiParam(value = "" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken){
        logger.info("Getting average negotiation time for the party with id: {}",partyID);
        double averageNegotiationTime = StatisticsDAOUtility.calculateAverageNegotiationTime(partyID,bearerToken);
        logger.info("Retrieved average negotiation time for the party with id: {}",partyID);
        return ResponseEntity.ok(averageNegotiationTime);
    }

    @ApiOperation(value = "Gets statistics (average negotiation time,average response time,trading volume and number of transactions) for the party")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved average negotiation time for the party",response = OverallStatistics.class)
    })
    @RequestMapping(value = "/overall",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getStatistics(@RequestParam(value = "partyID") String partyID,
                                        @ApiParam(value = "Role in business process. Can be SELLER or BUYER", required = false) @RequestParam(value = "role", required = false, defaultValue = "SELLER") String role,
                                        @ApiParam(value = "" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken){
        logger.info("Getting statistics for the party with id: {}",partyID);
        OverallStatistics statistics = new OverallStatistics();
        try {
            statistics.setAverageNegotiationTime((double)getAverageNegotiationTime(partyID,bearerToken).getBody());
            statistics.setAverageResponseTime((double)getAverageResponseTime(partyID,bearerToken).getBody());
            statistics.setTradingVolume((double) getTradingVolume(null,null,Integer.valueOf(partyID), role,null).getBody());
            statistics.setNumberOfTransactions((int)getProcessCount(null,null,null,Integer.valueOf(partyID),role,null).getBody());
        }
        catch (Exception e){
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting statistics for the party with id: %s", partyID), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        logger.info("Retrieved statistics for the party with id: {}",partyID);
        return ResponseEntity.ok(statistics);
    }
}
