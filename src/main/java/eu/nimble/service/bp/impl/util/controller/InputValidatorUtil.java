package eu.nimble.service.bp.impl.util.controller;

import eu.nimble.service.bp.hyperjaxb.model.DocumentType;
import eu.nimble.service.bp.hyperjaxb.model.RoleType;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngines;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by suat on 07-Jun-18.
 */
public class InputValidatorUtil {
    private static final Logger logger = LoggerFactory.getLogger(InputValidatorUtil.class);

    public static ValidationResponse checkBusinessProcessType(String bpType, boolean nullable) {
        ValidationResponse validationResponse = new ValidationResponse();
        if(permitNull(bpType, nullable)) {
            return validationResponse;
        }

        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();

        boolean validBpType = false;
        for (ProcessDefinition processDefinition : processDefinitions) {
            if (processDefinition.getKey().equalsIgnoreCase(bpType)) {
                validBpType = true;
                break;
            }
        }

        if (!validBpType) {
            validationResponse.setInvalidResponse(HttpResponseUtil.createResponseEntityAndLog(String.format("Invalid business process type: %s", bpType), HttpStatus.BAD_REQUEST));
        }
        return validationResponse;
    }

    public static ValidationResponse checkDocumentType(String documentType, boolean nullable) {
        ValidationResponse validationResponse = new ValidationResponse();
        if(permitNull(documentType, nullable)) {
            return validationResponse;
        }

        List<String> documentTypes = new ArrayList<>();
        Arrays.stream(DocumentType.values()).forEach(type -> documentTypes.add(type.toString()));

        documentType = documentType.toUpperCase();
        if (!documentTypes.contains(documentType)) {
            validationResponse.setInvalidResponse(HttpResponseUtil.createResponseEntityAndLog(String.format("Invalid document type: %s", documentType), HttpStatus.BAD_REQUEST));
        } else {
            validationResponse.setValidatedObject(documentType.toUpperCase());
        }
        return validationResponse;
    }

    public static ValidationResponse checkDate(String dateStr, boolean nullable) {
        ValidationResponse validationResponse = new ValidationResponse();
        if(permitNull(dateStr, nullable)) {
            return validationResponse;
        }

        DateTimeFormatter bpFormatter = DateTimeFormat.forPattern("dd-MM-yyyy");
        try {
            bpFormatter.parseDateTime(dateStr);
            return validationResponse;

        } catch (IllegalArgumentException e) {
            validationResponse.setInvalidResponse(HttpResponseUtil.createResponseEntityAndLog(String.format("Invalid date: %s", dateStr), e, HttpStatus.BAD_REQUEST));
            return validationResponse;
        }
    }

    public static ValidationResponse checkRole(String role, boolean nullable) {
        ValidationResponse validationResponse = new ValidationResponse();
        if(permitNull(role, nullable)) {
            return validationResponse;
        }

        role = role.toUpperCase();
        if (role.equals(RoleType.BUYER.toString()) || role.equals(RoleType.SELLER.toString())) {
            validationResponse.setValidatedObject(role);
            return validationResponse;
        } else {
            validationResponse.setInvalidResponse(HttpResponseUtil.createResponseEntityAndLog(String.format("Invalid role: %s", role), null, HttpStatus.BAD_REQUEST, LogLevel.INFO));
            return validationResponse;
        }
    }

    public static ValidationResponse checkStatus(String status, boolean nullable) {
        ValidationResponse validationResponse = new ValidationResponse();
        if(permitNull(status, nullable)) {
            return validationResponse;
        }

        status = status.toUpperCase();
        if (status.equals(ProcessDocumentMetadata.StatusEnum.APPROVED.toString()) ||
                status.equals(ProcessDocumentMetadata.StatusEnum.DENIED.toString()) ||
                status.equals(ProcessDocumentMetadata.StatusEnum.WAITINGRESPONSE.toString())) {

            validationResponse.setValidatedObject(status);
            return validationResponse;

        } else {
            validationResponse.setInvalidResponse(HttpResponseUtil.createResponseEntityAndLog(String.format("Invalid status: %s", status), null, HttpStatus.BAD_REQUEST, LogLevel.INFO));
            return validationResponse;
        }
    }

    private static boolean permitNull(Object object, boolean nullable) {
        if(object == null && nullable) {
            return true;
        }
        return false;
    }
}
