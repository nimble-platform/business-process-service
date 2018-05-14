package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.hyperjaxb.model.DocumentType;
import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceDAO;
import eu.nimble.service.bp.impl.util.persistence.ContractDAOUtility;
import eu.nimble.service.bp.impl.util.persistence.DAOUtility;
import eu.nimble.service.bp.impl.util.persistence.DocumentDAOUtility;
import eu.nimble.service.bp.impl.util.persistence.HibernateUtilityRef;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ClauseType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.transportexecutionplanrequest.TransportExecutionPlanRequestType;
import eu.nimble.utility.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Created by suat on 25-Apr-18.
 */
@Controller
public class ContractController {
    private final Logger logger = LoggerFactory.getLogger(ContractController.class);

    /**
     * Retrieves the {@link ClauseType} specified by the <code>clauseId</code>
     *
     * @param clauseId
     * @return <ul>
     * <li>200, if there is a clause, along with the clause</li>
     * <li>404, if there is not a clause</li>
     * <li>500, if there is an unexpected error</li>
     * </ul>
     */
    @RequestMapping(value = "/clauses/{clauseId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getClauseDetails(@PathVariable(value = "clauseId", required = true) String clauseId) {
        try {
            logger.info("Getting clause with id: {}", clauseId);
            ClauseType clause = ContractDAOUtility.getClause(clauseId);
            if (clause == null) {
                createResponseEntityAndLog(String.format("No clause for the given id: %s", clauseId), HttpStatus.NOT_FOUND);
            }
            logger.info("Retrieved clause with id: {}", clauseId);
            return ResponseEntity.ok().body(clause);

        } catch (Exception e) {
            return createResponseEntityAndLog(String.format("Unexpected error while getting the clause with id: %s", clauseId), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/clauses/{clauseId}",
            method = RequestMethod.PUT)
    public ResponseEntity updateClause(@PathVariable(value = "clauseId") String clauseId,
                                       @RequestBody() String deserializedClause) {

        try {
            logger.info("Updating clause with id: {}", clauseId);
            // parse the base clause object to get the type
            ObjectMapper objectMapper = new ObjectMapper().
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).
                    configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            ClauseType clause;
            try {
                clause = objectMapper.readValue(deserializedClause, ClauseType.class);
            } catch (IOException e) {
                return createResponseEntityAndLog("Failed to deserialize the clause: " + deserializedClause, e, HttpStatus.BAD_REQUEST);
            }

            // parse the derived clause object
            Object clauseObject;
            if (clause.getType().contentEquals(eu.nimble.service.bp.impl.model.ClauseType.DATA_MONITORING.toString())) {
                try {
                    clauseObject = objectMapper.readValue(deserializedClause, DataMonitoringClauseType.class);
                } catch (IOException e) {
                    return createResponseEntityAndLog("Failed to deserialize data monitoring clause: " + deserializedClause, e, HttpStatus.BAD_REQUEST);
                }

            } else {
                try {
                    clauseObject = objectMapper.readValue(deserializedClause, DocumentClauseType.class);
                } catch (IOException e) {
                    return createResponseEntityAndLog("Failed to deserialize document clause: " + deserializedClause, e, HttpStatus.BAD_REQUEST);
                }
            }

            // update clause
            try {
                clauseObject = HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(clauseObject);
            } catch (Exception e) {
                return createResponseEntityAndLog("Failed to update the clause: " + clauseId, e, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            logger.info("Updated clause with id: {}", clauseId);
            return ResponseEntity.ok().body(clauseObject);

        } catch (Exception e) {
            return createResponseEntityAndLog("Unexpected error in updating the clause: " + clauseId, e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/contracts",
            method = RequestMethod.GET)
    public ResponseEntity constructContractForProcessInstances(@RequestParam(value = "processInstanceId") String processInstanceId) {
        try {
            logger.info("Constructing contract starting from the process instance: {}", processInstanceId);
            // check existence and type of the process instance
            ProcessInstanceDAO processInstance = DAOUtility.getProcessIntanceDAOByID(processInstanceId);
            if (processInstance == null) {
                return createResponseEntityAndLog(String.format("Invalid process instance id: %s", processInstanceId), HttpStatus.BAD_REQUEST);
            }

            ContractType contract = ContractDAOUtility.constructContructForProcessInstances(processInstance);
            logger.info("Constructed contract starting from the process instance: {}", processInstanceId);

            ObjectMapper objectMapper = new ObjectMapper();
            return ResponseEntity.ok().body(objectMapper.writeValueAsString(contract));
        } catch (Exception e) {
            return createResponseEntityAndLog(String.format("Unexpected error while constructing contract for process: ", processInstanceId), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves all the {@link ClauseType}s for the contract specified with the given <code>contractId</code>
     *
     * @param contractId
     * @return <ul>
     * <li>200, along with the list of clauses</li>
     * <li>400, if there is not a contract with the specified id</li>
     * <li>500, if there is an unexpected error</li>
     * </ul>
     */
    @RequestMapping(value = "/contracts/{contractId}/clauses",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<List<ClauseType>> getClausesOfContract(@PathVariable(value = "contractId") String contractId) {
        try {
            logger.info("Getting clauses for contract: {}", contractId);
            ContractType contract = ContractDAOUtility.getContract(contractId);
            if (contract == null) {
                createResponseEntityAndLog(String.format("No contract for the given id: %s", contractId), HttpStatus.BAD_REQUEST);
            }

            logger.info("Retrieved clauses for contract: {}", contractId);
            return ResponseEntity.ok().body(contract.getClause());

        } catch (Exception e) {
            return createResponseEntityAndLog(String.format("Unexpected error while getting clauses for contract: %s", contractId), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/contracts/{contractId}/clauses/{clauseId}",
            method = RequestMethod.DELETE)
    public ResponseEntity deleteClauseFromContract(@PathVariable(value = "contractId") String contractId,
                                                   @PathVariable(value = "clauseId") String clauseId) {
        try {
            logger.info("Deleting clause: {} from contract: {}", clauseId, contractId);

            // check existence of contract
            ContractType contract = ContractDAOUtility.getContract(contractId);
            if (contract == null) {
                return createResponseEntityAndLog("Invalid contract id: " + contractId, HttpStatus.BAD_REQUEST);
            }

            // check existence of clause
            ClauseType clause = ContractDAOUtility.getContractClause(contractId, clauseId);
            if (clause == null) {
                return createResponseEntityAndLog("Invalid clause id: " + clauseId + " for contract: " + contractId, HttpStatus.BAD_REQUEST);
            }

            // delete the clause
            try {
                ContractDAOUtility.deleteClause(clause);
            } catch (Exception e) {
                return createResponseEntityAndLog("Failed to delete clause: " + clauseId + " from contract: " + contractId, e, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            logger.info("Deleted clause: {} from contract: {}", clauseId, contractId);
            return ResponseEntity.ok().body(contract);

        } catch (Exception e) {
            return createResponseEntityAndLog("Unexpected error while deleting the clause: " + clauseId + " from contract: " + contractId, e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves the {@link ClauseType}s having the specified <code>clauseType</code> for the document specified with
     * <code>documentId</code>. The method assumes that clauses can only
     *
     * @param documentId
     * @param clauseType
     * @return
     * <ul>
     *     <li>400, if the specified document does not exists or have </li>
     * </ul>
     */
    @RequestMapping(value = "/documents/{documentId}/clauses",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<ClauseType> getClauseDetails(@PathVariable(value = "documentId", required = true) String documentId,
                                                       @RequestParam(value = "clauseType", required = true) eu.nimble.service.bp.impl.model.ClauseType clauseType) {
        try {
            logger.info("Getting clause for document: {}, type: {}", documentId, clauseType);
            // check existence and type of the document bound to the contract
            ProcessDocumentMetadataDAO documentMetadata = DAOUtility.getProcessDocumentMetadata(documentId);
            if (documentMetadata == null) {
                return createResponseEntityAndLog(String.format("Invalid bounded-document id: %s", documentMetadata.getDocumentID()), HttpStatus.BAD_REQUEST);
            }
            DocumentType documentType = documentMetadata.getType();
            if (!(documentType == DocumentType.ORDER || documentType == DocumentType.TRANSPORTEXECUTIONPLANREQUEST)) {
                return createResponseEntityAndLog(String.format("Invalid bounded-document type: %s", documentType), HttpStatus.BAD_REQUEST);
            }

            ClauseType clause = ContractDAOUtility.getClause(documentMetadata.getDocumentID(), documentType, clauseType);
            if (clause == null) {
                createResponseEntityAndLog(String.format("No clause for the document: %s, clause type: %s", documentId, clauseType), HttpStatus.NOT_FOUND);
            }
            logger.info("Retrieved clause with for document: {}, type: {}", documentId, clauseType);
            return ResponseEntity.ok().body(clause);

        } catch (Exception e) {
            return createResponseEntityAndLog(String.format("Unexpected error while getting the clause details for document id: %s, clause type: %s", documentId, clauseType), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @RequestMapping(value = "/documents/{documentId}/contract",
            produces = {"application/json"},
            method = RequestMethod.PATCH)
    public ResponseEntity addDocumentClauseToContract(@PathVariable(value = "documentId") String documentId,
                                                      @RequestParam(value = "clauseType") String clauseType,
                                                      @RequestParam(value = "clauseDocumentId") String clauseDocumentId) {
        try {
            logger.info("Adding document clause to contract. Bounded-document id: {}, clause type: {}, clause document id: {}", documentId, clauseType, clauseDocumentId);

            // check the passed parameters
            // check clause type
            eu.nimble.service.bp.impl.model.ClauseType clauseTypeEnum;
            try {
                clauseTypeEnum = eu.nimble.service.bp.impl.model.ClauseType.valueOf(clauseType);
            } catch (Exception e) {
                return createResponseEntityAndLog(String.format("Invalid clause type: %s. Possible values are: ", clauseType, eu.nimble.service.bp.impl.model.ClauseType.values().toString()), e, HttpStatus.BAD_REQUEST);
            }

            // check existence and type of the document bound to the contract
            ResponseEntity response = validateDocumentExistence(documentId);
            if (response != null) {
                return response;
            }

            // check existence and type of the clause document
            ProcessDocumentMetadataDAO clauseDocumentMetadata = DAOUtility.getProcessDocumentMetadata(documentId);
            if (clauseDocumentMetadata == null) {
                return createResponseEntityAndLog(String.format("Invalid clause document id: %s", clauseDocumentId), HttpStatus.BAD_REQUEST);
            }

            ProcessDocumentMetadataDAO documentMetadata = DAOUtility.getProcessDocumentMetadata(documentId);
            Object document = DocumentDAOUtility.getUBLDocument(documentId, documentMetadata.getType());
            ContractType contract = checkDocumentContract(document);
            DocumentClauseType clause = new DocumentClauseType();
            DocumentReferenceType docRef = new DocumentReferenceType();

            contract.getClause().add(clause);
            clause.setID(UUID.randomUUID().toString());
            clause.setType(clauseType);
            clause.setClauseDocumentRef(docRef);

            docRef.setID(clauseDocumentId);
            if (clauseTypeEnum == eu.nimble.service.bp.impl.model.ClauseType.ITEM_DETAILS) {
                docRef.setDocumentType(DocumentType.ITEMINFORMATIONRESPONSE.toString());
            } else if (clauseTypeEnum == eu.nimble.service.bp.impl.model.ClauseType.NEGOTIATION) {
                docRef.setDocumentType(DocumentType.QUOTATION.toString());
            } else if (clauseTypeEnum == eu.nimble.service.bp.impl.model.ClauseType.PPAP) {
                docRef.setDocumentType(DocumentType.PPAPRESPONSE.toString());
            }

            // persist the update
            try {
                document = HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(document);
            } catch (Exception e) {
                return createResponseEntityAndLog(String.format("Failed to add document clause to contract. Bounded-document id: %s , clause type: %s , clause document id: %s", documentId, clauseType, clauseDocumentId), e, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            logger.info("Added document clause to contract. Bounded-document id: {}, clause type: {}, clause document id: {}, clause id: {}", documentId, clauseType, clauseDocumentId, clause.getID());
            return ResponseEntity.ok(document);

        } catch (Exception e) {
            return createResponseEntityAndLog(String.format("Unexpected error while adding clause to contract. Bounded-document id: %s , clause type: %s , clause document id: %s", documentId, clauseType, clauseDocumentId), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/documents/{documentId}/contract",
            consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.PATCH)
    public ResponseEntity addDataMonitoringClauseToContract(@PathVariable(value = "documentId") String documentId,
                                                            @RequestBody() DataMonitoringClauseType dataMonitoringClause) {

        try {
            logger.info("Adding data monitoring clause to contract. Bounded-document id: {}", documentId);
            // check existence and type of the document bound to the contract
            ResponseEntity response = validateDocumentExistence(documentId);
            if (response != null) {
                return response;
            }

            // check contract of the document
            ProcessDocumentMetadataDAO documentMetadata = DAOUtility.getProcessDocumentMetadata(documentId);
            Object document = DocumentDAOUtility.getUBLDocument(documentId, documentMetadata.getType());
            ContractType contract = checkDocumentContract(document);

            dataMonitoringClause.setID(UUID.randomUUID().toString());
            contract.getClause().add(dataMonitoringClause);

            // persist the update
            try {
                document = HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(document);
            } catch (Exception e) {
                return createResponseEntityAndLog("Failed to add monitoring clause to contract. Bounded-document id: " + documentId, e, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            logger.info("Added data monitoring clause to contract. Bounded-document id: {}, clause id: {}", documentId, dataMonitoringClause.getID());
            return ResponseEntity.ok(document);

        } catch (Exception e) {
            return createResponseEntityAndLog("Unexpected error while adding data monitoring clause to contract. Bounded-document id: " + documentId, e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ContractType checkDocumentContract(Object document) {
        ContractType contract = null;
        if (document instanceof OrderType) {
            OrderType order = (OrderType) document;
            if (order.getContract() == null || order.getContract().size() == 0) {
                contract = new ContractType();
                order.getContract().add(contract);
                contract.setID(UUID.randomUUID().toString());
            } else {
                contract = order.getContract().get(0);
            }

        } else if (document instanceof TransportExecutionPlanRequestType) {
            TransportExecutionPlanRequestType transportExecutionPlanRequest = (TransportExecutionPlanRequestType) document;
            contract = transportExecutionPlanRequest.getTransportContract();
            if (contract == null) {
                contract = new ContractType();
                contract.setID(UUID.randomUUID().toString());
                transportExecutionPlanRequest.setTransportContract(contract);
            }
        }

        return contract;
    }

    private ResponseEntity validateDocumentExistence(ProcessDocumentMetadataDAO documentMetadata) {
        if (documentMetadata == null) {
            return createResponseEntityAndLog("Invalid bounded-document id: " + documentMetadata.getDocumentID(), HttpStatus.BAD_REQUEST);
        }
        DocumentType documentType = documentMetadata.getType();
        if (!(documentType == DocumentType.ORDER || documentType == DocumentType.TRANSPORTEXECUTIONPLANREQUEST)) {
            return createResponseEntityAndLog("Invalid bounded-document type: " + documentType, HttpStatus.BAD_REQUEST);
        }

        return null;
    }

    private ResponseEntity validateDocumentExistence(String documentId) {
        ProcessDocumentMetadataDAO documentMetadata = DAOUtility.getProcessDocumentMetadata(documentId);
        return validateDocumentExistence(documentMetadata);
    }

    private ResponseEntity createResponseEntityAndLog(String msg, HttpStatus httpStatus) {
        return createResponseEntityAndLog(msg, null, httpStatus, LogLevel.WARN);
    }

    private ResponseEntity createResponseEntityAndLog(String msg, Exception e, HttpStatus httpStatus) {
        if (httpStatus == HttpStatus.BAD_REQUEST) {
            return createResponseEntityAndLog(msg, e, httpStatus, LogLevel.WARN);
        } else {
            return createResponseEntityAndLog(msg, e, httpStatus, LogLevel.ERROR);
        }
    }


    private ResponseEntity createResponseEntityAndLog(String msg, Exception e, HttpStatus httpStatus, LogLevel logLevel) {
        if (logLevel == null || logLevel == LogLevel.WARN) {
            if (e != null) {
                logger.warn(msg, e);
            } else {
                logger.warn(msg);
            }
        } else if (logLevel == LogLevel.ERROR) {
            if (e != null) {
                logger.error(msg, e);
            } else {
                logger.error(msg);
            }
        }
        return ResponseEntity.status(httpStatus).body(msg);
    }
}
