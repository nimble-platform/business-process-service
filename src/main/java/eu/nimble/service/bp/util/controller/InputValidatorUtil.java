package eu.nimble.service.bp.util.controller;

import eu.nimble.service.bp.model.hyperjaxb.DocumentType;
import eu.nimble.service.bp.model.hyperjaxb.RoleType;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.utility.DateUtility;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngines;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by suat on 07-Jun-18.
 */
public class InputValidatorUtil {
    public static ValidationResponse checkBusinessProcessType(String bpType, boolean nullable) throws NimbleException {
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
            throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_BUSINESS_PROCESS_TYPE.toString(),Arrays.asList(bpType));
        }
        return validationResponse;
    }

    public static ValidationResponse checkDocumentType(String documentType, boolean nullable) throws NimbleException {
        ValidationResponse validationResponse = new ValidationResponse();
        if(permitNull(documentType, nullable)) {
            return validationResponse;
        }

        List<String> documentTypes = new ArrayList<>();
        Arrays.stream(DocumentType.values()).forEach(type -> documentTypes.add(type.toString()));

        documentType = documentType.toUpperCase();
        if (!documentTypes.contains(documentType)) {
            throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_DOCUMENT_TYPE.toString(),Arrays.asList(documentType));
        } else {
            validationResponse.setValidatedObject(documentType.toUpperCase());
        }
        return validationResponse;
    }

    public static void checkDate(String dateStr, boolean nullable) throws NimbleException {
        if(permitNull(dateStr, nullable)) {
            return;
        }

        if(!DateUtility.isValidDate(dateStr)) {
            throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_DATE.toString(),Arrays.asList(dateStr));
        }
    }

    public static ValidationResponse checkRole(String role, boolean nullable) throws NimbleException {
        ValidationResponse validationResponse = new ValidationResponse();
        if(permitNull(role, nullable)) {
            return validationResponse;
        }

        role = role.toUpperCase();
        if (role.equals(RoleType.BUYER.toString()) || role.equals(RoleType.SELLER.toString())) {
            validationResponse.setValidatedObject(role);
            return validationResponse;
        } else {
            throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_ROLE.toString(),Arrays.asList(role));
        }
    }

    public static ValidationResponse checkStatus(String status, boolean nullable) throws NimbleException {
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
            throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_INVALID_STATUS.toString(),Arrays.asList(status));
        }
    }

    private static boolean permitNull(Object object, boolean nullable) {
        if(object == null && nullable) {
            return true;
        }
        return false;
    }
}
