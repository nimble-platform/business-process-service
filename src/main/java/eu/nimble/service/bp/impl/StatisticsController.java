package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.impl.util.camunda.CamundaEngine;
import eu.nimble.service.bp.impl.util.controller.HttpResponseUtil;
import eu.nimble.service.bp.impl.util.controller.InputValidatorUtil;
import eu.nimble.service.bp.impl.util.controller.ValidationResponse;
import eu.nimble.service.bp.impl.util.persistence.DAOUtility;
import eu.nimble.service.bp.impl.util.persistence.StatisticsDAOUtility;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroup;
import eu.nimble.service.bp.swagger.model.Transaction;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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

    @ApiOperation(value = "Gets the total number (active / completed) of specified business process.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the number of processes successfully")
    })
    @RequestMapping(value = "/total-number/business-process",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getDocumentCount(@ApiParam(value = "Business process type. ", required = false) @RequestParam(value = "businessProcessType", required = false) String businessProcessType,
                                           @ApiParam(value = "Start date", required = false) @RequestParam(value = "startDate", required = false) String startDateStr,
                                           @ApiParam(value = "End date", required = false) @RequestParam(value = "endDate", required = false) String endDateStr,
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

    @ApiOperation(value = "Gets the trading volume")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the trading volume successfully")
    })
    @RequestMapping(value = "/trading-volume",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getProcessCount(@ApiParam(value = "Start date", required = false) @RequestParam(value = "startDate", required = false) String startDateStr,
                                          @ApiParam(value = "End date", required = false) @RequestParam(value = "endDate", required = false) String endDateStr,
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
}
