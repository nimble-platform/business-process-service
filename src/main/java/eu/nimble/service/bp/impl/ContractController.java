package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.exception.NimbleExceptionMessageCode;
import eu.nimble.service.bp.model.hyperjaxb.DocumentType;
import eu.nimble.service.bp.model.hyperjaxb.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceDAO;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.util.persistence.bp.ProcessInstanceDAOUtility;
import eu.nimble.service.bp.util.persistence.catalogue.ContractPersistenceUtility;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.digitalagreement.DigitalAgreementType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.transportexecutionplanrequest.TransportExecutionPlanRequestType;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.repository.BinaryContentAwareRepositoryWrapper;
import eu.nimble.utility.validation.IValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by suat on 25-Apr-18.
 */
@Controller
public class ContractController {
    private final Logger logger = LoggerFactory.getLogger(ContractController.class);

    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ExecutionContext executionContext;

//    @ApiOperation(value = "",notes = "Retrieves the specified ClauseType")
//    @ApiResponses(value = {
//            @ApiResponse(code = 200,message = "Retrieved the clause successfully",response = ClauseType.class),
//            @ApiResponse(code = 401,message = "Invalid token. No user was found for the provided token"),
//            @ApiResponse(code = 404,message = "No clause for the given id"),
//            @ApiResponse(code = 500,message = "Unexpected error while getting the clause")
//    })
//    @RequestMapping(value = "/clauses/{clauseId}",
//            produces = {"application/json"},
//            method = RequestMethod.GET)
//    public ResponseEntity getClauseDetails(@ApiParam(value = "Unique identifier of the ClauseType to be retrieved (clause.id)", required = true) @PathVariable(value = "clauseId", required = true) String clauseId,
//                                           @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
//        try {
//            logger.info("Getting clause with id: {}", clauseId);
//            // check token
//            ResponseEntity tokenCheck = HttpResponseUtil.checkToken(bearerToken);
//            if (tokenCheck != null) {
//                return tokenCheck;
//            }
//
//            ClauseType clause = ContractPersistenceUtility.getClause(clauseId);
//            if (clause == null) {
//                return createResponseEntityAndLog(String.format("No clause for the given id: %s", clauseId), HttpStatus.NOT_FOUND);
//            }
//            logger.info("Retrieved clause with id: {}", clauseId);
//            return ResponseEntity.ok().body(clause);
//
//        } catch (Exception e) {
//            return createResponseEntityAndLog(String.format("Unexpected error while getting the clause with id: %s", clauseId), e, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @ApiOperation(value = "",notes = "Updates the ClauseType with the specifiedId")
//    @ApiResponses(value = {
//            @ApiResponse(code = 200,message = "Updated the clause successfully",response = ClauseType.class),
//            @ApiResponse(code = 400,message = "Invalid Clause content"),
//            @ApiResponse(code = 401,message = "Invalid token. No user was found for the provided token"),
//            @ApiResponse(code = 500,message = "Unexpected error while updating the clause")
//    })
//    @RequestMapping(value = "/clauses/{clauseId}",
//            method = RequestMethod.PUT)
//    public ResponseEntity updateClause(@ApiParam(value = "Identifier of the ClauseType to be updated (clause.id)", required = true) @PathVariable(value = "clauseId") String clauseId,
//                                       @ApiParam(value = "Serialized form of the complete ClauseType", required = true) @RequestBody String serializedClause,
//                                       @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
//
//        try {
//            logger.info("Updating clause with id: {}", clauseId);
//            // check token
//            ResponseEntity tokenCheck = HttpResponseUtil.checkToken(bearerToken);
//            if (tokenCheck != null) {
//                return tokenCheck;
//            }
//
//            // get person using the given bearer token
//            PersonType person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);
//            // get party for the person
//            PartyType party = SpringBridge.getInstance().getiIdentityClientTyped().getPartyByPersonID(person.getID()).get(0);
//
//            // parse the base clause object to get the type
//            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
//            ClauseType clause;
//            try {
//                clause = objectMapper.readValue(serializedClause, ClauseType.class);
//            } catch (IOException e) {
//                return createResponseEntityAndLog("Failed to deserialize the clause: " + serializedClause, e, HttpStatus.BAD_REQUEST);
//            }
//
//            // parse the derived clause object
//            Object clauseObject;
//            if (clause.getType().contentEquals(eu.nimble.service.model.ubl.extension.ClauseType.DATA_MONITORING.toString())) {
//                try {
//                    clauseObject = objectMapper.readValue(serializedClause, DataMonitoringClauseType.class);
//                } catch (IOException e) {
//                    return createResponseEntityAndLog("Failed to deserialize data monitoring clause: " + serializedClause, e, HttpStatus.BAD_REQUEST);
//                }
//
//            } else {
//                try {
//                    clauseObject = objectMapper.readValue(serializedClause, DocumentClauseType.class);
//                } catch (IOException e) {
//                    return createResponseEntityAndLog("Failed to deserialize document clause: " + serializedClause, e, HttpStatus.BAD_REQUEST);
//                }
//            }
//
//            // validate the entity ids
//            boolean hjidsBelongToCompany = resourceValidationUtil.hjidsBelongsToParty(clauseObject, party.getPartyIdentification().get(0).getID(), Configuration.Standard.UBL.toString());
//            if(!hjidsBelongToCompany) {
//                return HttpResponseUtil.createResponseEntityAndLog(String.format("Some of the identifiers (hjid fields) do not belong to the party in the passed clause: %s", serializedClause), null, HttpStatus.BAD_REQUEST, LogLevel.INFO);
//            }
//
//            // update clause
//            try {
//                EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(party.getPartyIdentification().get(0).getID());
//                clauseObject = repositoryWrapper.updateEntity(clauseObject);
//            } catch (Exception e) {
//                return createResponseEntityAndLog("Failed to update the clause: " + clauseId, e, HttpStatus.INTERNAL_SERVER_ERROR);
//            }
//
//            logger.info("Updated clause with id: {}", clauseId);
//            return ResponseEntity.ok().body(clauseObject);
//
//        } catch (Exception e) {
//            return createResponseEntityAndLog("Unexpected error in updating the clause: " + clauseId, e, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }

    @ApiOperation(value = "",notes = "Constructs a contract for the business processes where the specified process instance" +
            " is the last business process. All the business processes until the initial business process are considered. ")
    @ApiResponses(value = {
            @ApiResponse(code = 200,message = "Constructed a contract successfully",response = ClauseType.class),
            @ApiResponse(code = 401,message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404,message = "No process instance found for the specified id"),
            @ApiResponse(code = 500,message = "Unexpected error while constructing the contract")
    })
    @RequestMapping(value = "/contracts",
            method = RequestMethod.GET)
    public ResponseEntity constructContractForProcessInstances(@ApiParam(value = "The identifier of the ProcessInstance from which the processes would be considered while constructing the contract", required = true) @RequestParam(value = "processInstanceId") String processInstanceId,
                                                               @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) throws Exception {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Constructing contract starting from the process instance: %s", processInstanceId);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }
            // check existence and type of the process instance
            ProcessInstanceDAO processInstance = ProcessInstanceDAOUtility.getById(processInstanceId);
            if (processInstance == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_INVALID_PROCESS_INSTANCE.toString(), Arrays.asList(processInstanceId));
            }

            ContractType contract = ContractPersistenceUtility.constructContractForProcessInstances(processInstanceId);
            logger.info("Constructed contract starting from the process instance: {}", processInstanceId);

            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
            return ResponseEntity.ok().body(objectMapper.writeValueAsString(contract));
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_CONSTRUCT_CONTRACT_FOR_PROCESS_INSTANCES.toString(),Arrays.asList(processInstanceId),e);
        }
    }

    @ApiOperation(value = "",notes = "Retrieves all clauses for the specified contract")
    @ApiResponses(value = {
            @ApiResponse(code = 200,message = "Retrieved all clauses for the contract"),
            @ApiResponse(code = 401,message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404,message = "No contract for the given id"),
            @ApiResponse(code = 500,message = "Unexpected error while getting clauses of contract")
    })
    @RequestMapping(value = "/contracts/{contractId}/clauses",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getClausesOfContract(@ApiParam(value = "Identifier of the contract (contract.id) of which clauses to be retrieved", required = true) @PathVariable(value = "contractId") String contractId,
                                               @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken
    ) {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Getting clauses for contract: %s", contractId);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            ContractType contract = ContractPersistenceUtility.getContract(contractId);
            if (contract == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CONTRACT.toString(),Arrays.asList(contractId));
            }

            ObjectMapper mapper = JsonSerializationUtility.getObjectMapper();

            logger.info("Retrieved clauses for contract: {}", contractId);
            return ResponseEntity.ok().body(mapper.writeValueAsString(contract.getClause()));

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_CLAUSES_FOR_PROCESS_INSTANCES.toString(),Arrays.asList(contractId),e);
        }
    }

//    @ApiOperation(value = "",notes = "Deletes a clause from the contract")
//    @ApiResponses(value = {
//            @ApiResponse(code = 200,message = "Deleted clause from the contract and returned the updated contract successfully",response = ContractType.class),
//            @ApiResponse(code = 401,message = "Invalid token. No user was found for the provided token"),
//            @ApiResponse(code = 404,message = "No contract or clause for the specified identifiers"),
//            @ApiResponse(code = 500,message = "Unexpected error while deleting the clause from the contract")
//    })
//    @RequestMapping(value = "/contracts/{contractId}/clauses/{clauseId}",
//            method = RequestMethod.DELETE)
//    public ResponseEntity deleteClauseFromContract(@ApiParam(value = "Identifier of the contract (contract.id) from which the clause to be deleted", required = true) @PathVariable(value = "contractId") String contractId,
//                                                   @ApiParam(value = "Identifier of the clause (clause.id) to be deleted", required = true) @PathVariable(value = "clauseId") String clauseId,
//                                                   @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
//        try {
//            logger.info("Deleting clause: {} from contract: {}", clauseId, contractId);
//            // get person using the given bearer token
//            PersonType person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);
//            // get party for the person
//            PartyType party = SpringBridge.getInstance().getiIdentityClientTyped().getPartyByPersonID(person.getID()).get(0);
//
//            // check token
//            ResponseEntity tokenCheck = HttpResponseUtil.checkToken(bearerToken);
//            if (tokenCheck != null) {
//                return tokenCheck;
//            }
//
//            // check existence of contract
//            if (!ContractPersistenceUtility.contractExists(contractId)) {
//                return createResponseEntityAndLog("Invalid contract id: " + contractId, HttpStatus.NOT_FOUND);
//            }
//
//            // check existence of clause
//            ClauseType clause = ContractPersistenceUtility.getContractClause(contractId, clauseId);
//            if (clause == null) {
//                return createResponseEntityAndLog("Invalid clause id: " + clauseId + " for contract: " + contractId, HttpStatus.NOT_FOUND);
//            }
//
//            // delete the clause
//            try {
//                EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(party.getPartyIdentification().get(0).getID());
//                repositoryWrapper.deleteEntity(clause);
//            } catch (Exception e) {
//                return createResponseEntityAndLog("Failed to delete clause: " + clauseId + " from contract: " + contractId, e, HttpStatus.INTERNAL_SERVER_ERROR);
//            }
//
//            // return updated version
//            ContractType contract = ContractPersistenceUtility.getContract(contractId);
//            logger.info("Deleted clause: {} from contract: {}", clauseId, contractId);
//            return ResponseEntity.ok().body(contract);
//
//        } catch (Exception e) {
//            return createResponseEntityAndLog("Unexpected error while deleting the clause: " + clauseId + " from contract: " + contractId, e, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }

    @ApiOperation(value = "",notes = "Retrieves ClauseType instances having the specified clauseType for the document specified with" +
            " documentId. The document is supposed to have a ContractType inside i.e. OrderType or TransportExecutionPlanRequestType.")
    @ApiResponses(value = {
            @ApiResponse(code = 200,message = "Retrieved clauses successfully",response = ClauseType.class),
            @ApiResponse(code = 204,message = "No clause exists for the specified contract"),
            @ApiResponse(code = 401,message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404,message = "No contract available for the specified document"),
            @ApiResponse(code = 500,message = "Unexpected error while getting clauses of the contract")
    })
    @RequestMapping(value = "/documents/{documentId}/clauses",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getClauseDetails(@ApiParam(value = "Identifier of the document for which the inner clauses to be retrieved", required = true) @PathVariable(value = "documentId", required = true) String documentId,
                                           @ApiParam(value = "Type of the clauses to be retrieved. If no type specified all the clauses are retrieved.", required = false) @RequestParam(value = "clauseType", required = false) eu.nimble.service.model.ubl.extension.ClauseType clauseType,
                                           @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) throws NimbleException {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Getting clause for document: %s, type: %s", documentId, clauseType);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // check existence and type of the document bound to the contract
            ProcessDocumentMetadataDAO documentMetadata = ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId);
            if (documentMetadata == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_PROCESS_DOCUMENT_METADATA.toString(),Arrays.asList(documentId));
            }
            DocumentType documentType = documentMetadata.getType();
            if (!(documentType == DocumentType.ORDER || documentType == DocumentType.TRANSPORTEXECUTIONPLANREQUEST)) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_INVALID_BOUNDED_DOCUMENT_TYPE.toString(),Arrays.asList(documentType.value()));
            }

            List<ClauseType> clause = ContractPersistenceUtility.getClauses(documentMetadata.getDocumentID(), documentType, clauseType);
            if (clause == null || clause.size() == 0) {
                throw new NimbleException(NimbleExceptionMessageCode.NO_CONTENT_NO_CLAUSE.toString(),Arrays.asList(documentId, clauseType.toString()));
            }
            logger.info("Retrieved clause with for document: {}, type: {}", documentId, clauseType);
            return ResponseEntity.ok().body(clause);

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_CLAUSE_DETAILS.toString(),Arrays.asList(documentId, clauseType.toString()),e);
        }
    }

    @ApiOperation(value = "",notes = "Adds a document clause to the contract included in the specified document")
    @ApiResponses(value = {
            @ApiResponse(code = 200,message = "Added the clause to the specified document and returned the updated document successfully",response = ClauseType.class),
            @ApiResponse(code = 401,message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404,message = "No contract available for the specified document or no clause for the specified id"),
            @ApiResponse(code = 500,message = "Unexpected error while adding the clause to the contract")
    })
    @RequestMapping(value = "/documents/{documentId}/contract/clause/document",
            produces = {"application/json"},
            method = RequestMethod.PATCH)
    public ResponseEntity addDocumentClauseToContract(@ApiParam(value = "Identifier of the document containing the contract to which the clause to be added", required = true) @PathVariable(value = "documentId") String documentId,
                                                      @ApiParam(value = "Identifier of the document referred by the clause", required = true) @RequestParam(value = "clauseDocumentId") String clauseDocumentId,
                                                      @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) throws Exception {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Adding document clause to contract. Bounded-document id: %s, clause document id: %s", documentId, clauseDocumentId);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // check the passed parameters
            // check existence and type of the document bound to the contract
            validateDocumentExistence(documentId);

            // check existence and type of the clause document
            ProcessDocumentMetadataDAO clauseDocumentMetadata = ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId);
            if (clauseDocumentMetadata == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_PROCESS_DOCUMENT_METADATA.toString(),Arrays.asList(clauseDocumentId));
            }

            // get contract of the specified document
            Object document = DocumentPersistenceUtility.getUBLDocument(documentId, clauseDocumentMetadata.getType());
            ContractType contract = checkDocumentContract(document);
            DocumentClauseType clause = new DocumentClauseType();
            DocumentReferenceType docRef = new DocumentReferenceType();

            contract.getClause().add(clause);
            clause.setID(UUID.randomUUID().toString());
            clause.setType(eu.nimble.service.model.ubl.extension.ClauseType.DOCUMENT.toString());
            clause.setClauseDocumentRef(docRef);

            docRef.setID(clauseDocumentId);
            docRef.setDocumentType(clauseDocumentMetadata.getType().toString());

            // persist the update
            try {
                BinaryContentAwareRepositoryWrapper repositoryWrapper = new BinaryContentAwareRepositoryWrapper();
                document = repositoryWrapper.updateEntity(document);
                // update document cache
                SpringBridge.getInstance().getCacheHelper().putDocument(document);
            } catch (Exception e) {
                throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_ADD_CLAUSE.toString(),Arrays.asList(documentId, clauseDocumentId),e);
            }

            logger.info("Added document clause to contract. Bounded-document id: {}, clause document id: {}, clause id: {}", documentId, clauseDocumentId, clause.getID());
            return ResponseEntity.ok(document);

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_ADD_DOCUMENT_CLAUSE_TO_CONTRACT.toString(),Arrays.asList(documentId, clauseDocumentId),e);
        }
    }

    @ApiOperation(value = "",notes = "Adds a data monitoring clause to the contract included in the specified document")
    @ApiResponses(value = {
            @ApiResponse(code = 200,message = "Added the clause to the specified document and returned the updated document successfully",response = ClauseType.class),
            @ApiResponse(code = 401,message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404,message = "No contract available for the specified document or no clause for the specified id"),
            @ApiResponse(code = 500,message = "Unexpected error while adding the clause to the contract")
    })
    @RequestMapping(value = "/documents/{documentId}/contract/clause/data-monitoring",
            consumes = {"application/json"},
            produces = {"application/json"},
            method = RequestMethod.PATCH)
    public ResponseEntity addDataMonitoringClauseToContract(@ApiParam(value = "Identifier of the document containing the contract to which the clause to be added", required = true) @PathVariable(value = "documentId") String documentId,
                                                            @ApiParam(value = "Serialized form of the DataMonitoringClauseType to be added", required = true) @RequestBody DataMonitoringClauseType dataMonitoringClause,
                                                            @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) throws NimbleException, IOException {

        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Adding data monitoring clause to contract. Bounded-document id: %s", documentId);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // check existence and type of the document bound to the contract
            validateDocumentExistence(documentId);

            // check contract of the document
            ProcessDocumentMetadataDAO documentMetadata = ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId);
            Object document = DocumentPersistenceUtility.getUBLDocument(documentId, documentMetadata.getType());
            ContractType contract = checkDocumentContract(document);

            dataMonitoringClause.setID(UUID.randomUUID().toString());
            dataMonitoringClause.setType(eu.nimble.service.model.ubl.extension.ClauseType.DATA_MONITORING.toString());
            contract.getClause().add(dataMonitoringClause);

            // persist the update
            try {
                BinaryContentAwareRepositoryWrapper repositoryWrapper = new BinaryContentAwareRepositoryWrapper();
                document = repositoryWrapper.updateEntity(document);
                // update document cache
                SpringBridge.getInstance().getCacheHelper().putDocument(document);
            } catch (Exception e) {
                throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_ADD_DATA_MONITORING_CLAUSE_TO_CONTRACT.toString(),Arrays.asList(documentId),e);
            }

            logger.info("Added data monitoring clause to contract. Bounded-document id: {}, clause id: {}", documentId, dataMonitoringClause.getID());
            return ResponseEntity.ok(document);

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_ADD_DATA_MONITORING_CLAUSE_TO_CONTRACT.toString(),Arrays.asList(documentId),e);
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

//    @ApiOperation(value = "",notes = "Saves a DigitalAgreement instance to be referred by a RequestForQuotation or Quotation")
//    @ApiResponses(value = {
//            @ApiResponse(code = 200,message = "Saved the passed instance of DigitalAgreement", response = DigitalAgreementType.class),
//            @ApiResponse(code = 401,message = "Invalid token. No user was found for the provided token"),
//            @ApiResponse(code = 500,message = "Unexpected error while saving the passed DigitalAgreement")
//    })
//    @RequestMapping(value = "/contract/digital-agreement",
//            consumes = {"application/json"},
//            produces = {"application/json"},
//            method = RequestMethod.POST)
//    public ResponseEntity saveFrameContract(@ApiParam(value = "Serialized form of the DigitalAgreement to be added", required = true) @RequestBody String digitalAgreementJson,
//                                            @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
//
//        try {
//            logger.info("Incoming request to save a DigitalAgreement instance");
//            // check token
//            ResponseEntity tokenCheck = HttpResponseUtil.checkToken(bearerToken);
//            if (tokenCheck != null) {
//                return tokenCheck;
//            }
//
//            DigitalAgreementType digitalAgreement = JsonSerializationUtility.getObjectMapper().readValue(digitalAgreementJson, DigitalAgreementType.class);
//
//            // get person using the given bearer token
//            PersonType person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);
//            // get party for the person
//            PartyType party = SpringBridge.getInstance().getiIdentityClientTyped().getPartyByPersonID(person.getID()).get(0);
//
//            // set an identifier to the document
//            digitalAgreement.setID(UUID.randomUUID().toString());
//
//            // persist the update
//            try {
//                DataIntegratorUtil.checkExistingParties(digitalAgreement);
//                EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(party.getPartyIdentification().get(0).getID());
//                digitalAgreement = repositoryWrapper.updateEntityForPersistCases(digitalAgreement);
//            } catch (Exception e) {
//                return createResponseEntityAndLog(String.format("Failed to save DigitalAgreement: %s", digitalAgreementJson), e, HttpStatus.INTERNAL_SERVER_ERROR);
//            }
//
//            logger.info("Saved DigitalAgreement successfully");
//            return ResponseEntity.ok(JsonSerializationUtility.getObjectMapper().writeValueAsString(digitalAgreement));
//
//        } catch (Exception e) {
//            return createResponseEntityAndLog(String.format("Unexpected error while saving DigitalAgreement: %s", digitalAgreementJson), e, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @ApiOperation(value = "",notes = "Replace the passed DigitalAgreement with the existing DigitalAgreement. The existing " +
//            "instance is resolved via the hjid field of the passed instance")
//    @ApiResponses(value = {
//            @ApiResponse(code = 200,message = "Retrieved the specified DigitalAgreement successfully", response = DigitalAgreementType.class),
//            @ApiResponse(code = 401,message = "Invalid token. No user was found for the provided token"),
//            @ApiResponse(code = 500,message = "Unexpected error while retriving the passed DigitalAgreement")
//    })
//    @RequestMapping(value = "/contract/digital-agreement/{id}",
//            consumes = MediaType.APPLICATION_JSON_VALUE,
//            produces = MediaType.APPLICATION_JSON_VALUE,
//            method = RequestMethod.PUT)
//    public ResponseEntity updateDigitalAgreement(@ApiParam(value = "Updated DigitalAgreement to be replaced with the existing one", required = true) @RequestBody String digitalAgreementJson,
//                                                @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
//
//        try {
//            logger.info("Incoming request to update a DigitalAgreement.");
//            // check token
//            ResponseEntity tokenCheck = HttpResponseUtil.checkToken(bearerToken);
//            if (tokenCheck != null) {
//                return tokenCheck;
//            }
//
//            // parse the request body
//            DigitalAgreementType digitalAgreement = JsonSerializationUtility.getObjectMapper().readValue(digitalAgreementJson, DigitalAgreementType.class);
//
//            // get person using the given bearer token
//            PersonType person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);
//            // get party for the person
//            PartyType party = SpringBridge.getInstance().getiIdentityClientTyped().getPartyByPersonID(person.getID()).get(0);
//
//            // validate the entity ids
//            boolean hjidsBelongToCompany = resourceValidationUtil.hjidsBelongsToParty(digitalAgreement, party.getPartyIdentification().get(0).getID(), Configuration.Standard.UBL.toString());
//            if (!hjidsBelongToCompany) {
//                return HttpResponseUtil.createResponseEntityAndLog(String.format("Some of the identifiers (hjid fields) do not belong to the party in the passed catalogue: %s", digitalAgreementJson), null, HttpStatus.BAD_REQUEST, LogLevel.INFO);
//            }
//
//            // update
//            try {
//                EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(party.getPartyIdentification().get(0).getID());
//                repositoryWrapper.updateEntity(digitalAgreement);
//            } catch (Exception e) {
//                return createResponseEntityAndLog(String.format("Failed to save DigitalAgreement: %s", digitalAgreementJson), e, HttpStatus.INTERNAL_SERVER_ERROR);
//            }
//
//            logger.info("Updated DigitalAgreement successfully.");
//            return ResponseEntity.ok(JsonSerializationUtility.getObjectMapper().writeValueAsString(digitalAgreement));
//
//        } catch (Exception e) {
//            return createResponseEntityAndLog(String.format("Unexpected error while updating DigitalAgreement: %s", digitalAgreementJson), e, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }

    @ApiOperation(value = "", notes = "Gets the DigitalAgreement specified by the buyer, seller and product ids")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the specified DigitalAgreement successfully", response = DigitalAgreementType.class),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No DigitalAgreement found for the given id"),
            @ApiResponse(code = 500, message = "Unexpected error while retriving the passed DigitalAgreement")
    })
    @RequestMapping(value = "/contract/digital-agreement/{id}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getDigitalAgreementForPartiesAndProduct(@ApiParam(value = "Identifier (digitalAgreement.hjid) of the DigitalAgreement", required = true) @PathVariable(value = "id") Long contractId,
                                                                  @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) throws Exception {

        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to retrieve a DigitalAgreement. hjid: %s", contractId);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            DigitalAgreementType digitalAgreement = new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntityByHjid(DigitalAgreementType.class, contractId);
            if(digitalAgreement == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_DIGITAL_AGREEMENT.toString(),Arrays.asList(contractId.toString()));
            }

            logger.info("Retrieved DigitalAgreement. hjid: {}", contractId);
            return ResponseEntity.ok(JsonSerializationUtility.getObjectMapper().writeValueAsString(digitalAgreement));

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_DIGITAL_AGREEMENT_FOR_PARTIES_AND_PRODUCT.toString(),Arrays.asList(contractId.toString()),e);
        }
    }

    @ApiOperation(value = "", notes = "Deletes the given DigitalAgreement")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted the specified DigitalAgreement successfully"),
            @ApiResponse(code = 400, message = "No DigitalAgreement found for the given id"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while deleting the passed DigitalAgreement")
    })
    @RequestMapping(value = "/contract/digital-agreement/{id}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    public ResponseEntity deleteDigitalAgreement(@ApiParam(value = "Identifier (digitalAgreement.hjid) of the DigitalAgreement", required = true) @PathVariable(value = "id") Long contractId,
                                                 @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) throws NimbleException {

        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to delete a DigitalAgreement. hjid: %s", contractId);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            GenericJPARepository catalogueRepository = new JPARepositoryFactory().forCatalogueRepository();
            DigitalAgreementType digitalAgreement = catalogueRepository.getSingleEntityByHjid(DigitalAgreementType.class, contractId);
            if(digitalAgreement == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_DIGITAL_AGREEMENT.toString(),Arrays.asList(contractId.toString()));
            }
            catalogueRepository.deleteEntity(digitalAgreement);

            logger.info("Deleted DigitalAgreement. hjid: {}", contractId);
            return ResponseEntity.ok(null);

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_DELETE_DIGITAL_AGREEMENT.toString(),Arrays.asList(contractId.toString()),e);
        }
    }

    @ApiOperation(value = "", notes = "Gets the DigitalAgreement specified by the buyer, seller and product ids")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the specified DigitalAgreement successfully", response = DigitalAgreementType.class, responseContainer = "List"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No DigitalAgreement found for the given parameters"),
            @ApiResponse(code = 500, message = "Unexpected error while retriving the passed DigitalAgreement")
    })
    @RequestMapping(value = "/contract/digital-agreement",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getDigitalAgreementForPartiesAndProducts(@ApiParam(value = "Identifier of the buyer company participating in the DigitalAgreement", required = true) @RequestParam(value = "buyerId") String buyerId,
                                                                  @ApiParam(value = "Identifier of the seller company participating in the DigitalAgreement", required = true) @RequestParam(value = "sellerId") String sellerId,
                                                                  @ApiParam(value = "Manufacturer item identification of the products being the subject of the DigitalAgreement", required = true) @RequestParam(value = "productIds") List<String> manufacturersItemIds,
                                                                  @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
                                                                   @ApiParam(value = "" ,required=true ) @RequestHeader(value="initiatorFederationId", required=true) String initiatorFederationId,
                                                                   @ApiParam(value = "" ,required=true ) @RequestHeader(value="responderFederationId", required=true) String responderFederationId) throws Exception {

        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to retrieve a DigitalAgreement. seller id: %s, buyer id: %s, product hjids: %s", sellerId, buyerId, manufacturersItemIds);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            List<DigitalAgreementType> digitalAgreements = ContractPersistenceUtility.getFrameContractAgreementByIds(sellerId,responderFederationId, buyerId,initiatorFederationId, manufacturersItemIds);
            // removed the expired ones from the list
            List<DigitalAgreementType> expiredDigitalAgreements = new ArrayList<>();
            for (DigitalAgreementType digitalAgreement : digitalAgreements) {
                if(isDigitalAgreementExpired(digitalAgreement)){
                    expiredDigitalAgreements.add(digitalAgreement);
                }
            }

            digitalAgreements.removeAll(expiredDigitalAgreements);

            if(digitalAgreements.size() == 0) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_DIGITAL_AGREEMENT_FOR_PARAMETERS.toString(),Arrays.asList(sellerId, buyerId, manufacturersItemIds.toString()));
            }

            logger.info("Retrieved DigitalAgreement. seller id: {}, buyer id: {}, product hjids: {}", sellerId, buyerId, manufacturersItemIds);
            return ResponseEntity.ok(JsonSerializationUtility.getObjectMapper().writeValueAsString(digitalAgreements));

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_DIGITAL_AGREEMENT_FOR_PARTIES_AND_PRODUCTS.toString(),Arrays.asList(sellerId, buyerId, manufacturersItemIds.toString()),e);
        }
    }

    @ApiOperation(value = "", notes = "Gets the DigitalAgreement specified by the buyer, seller and product ids")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the specified DigitalAgreement successfully", response = DigitalAgreementType.class),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while retriving the passed DigitalAgreement")
    })
    @RequestMapping(value = "/contract/digital-agreement/all",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getDigitalAgreementForPartiesAndProduct(@ApiParam(value = "Identifier of the company participating in the DigitalAgreement", required = true) @RequestParam(value = "partyId") String partyId,
                                                                  @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
                                                                  @ApiParam(value = "", required = true) @RequestHeader(value = "federationId", required = true) String federationId) {

        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to retrieve a DigitalAgreements for party: %s", partyId);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            List<DigitalAgreementType> digitalAgreements = ContractPersistenceUtility.getFrameContractsByPartyId(partyId,federationId);
            // remove expired digital agreements
            List<DigitalAgreementType> expiredDigitalAgreements = new ArrayList<>();
            for (DigitalAgreementType digitalAgreement : digitalAgreements) {
                if(isDigitalAgreementExpired(digitalAgreement)){
                    expiredDigitalAgreements.add(digitalAgreement);
                }
            }
            digitalAgreements.removeAll(expiredDigitalAgreements);

            logger.info("Completed request to retrieve all DigitalAgreement for party: {}", partyId);
            return ResponseEntity.ok(JsonSerializationUtility.getObjectMapper().writeValueAsString(digitalAgreements));

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_ALL_DIGITAL_AGREEMENTS_FOR_PARTIES_AND_PRODUCTS.toString(),Arrays.asList(partyId),e);
        }
    }

    private void validateDocumentExistence(String documentId,ProcessDocumentMetadataDAO documentMetadata) {
        if (documentMetadata == null) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_PROCESS_DOCUMENT_METADATA.toString(),Arrays.asList(documentId));
        }
        DocumentType documentType = documentMetadata.getType();
        if (!(documentType == DocumentType.ORDER || documentType == DocumentType.TRANSPORTEXECUTIONPLANREQUEST)) {
            throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_CONTRACT_FOR_DOCUMENT_TYPE.toString(),Arrays.asList(documentType.value()));
        }
    }

    private void validateDocumentExistence(String documentId) {
        ProcessDocumentMetadataDAO documentMetadata = ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId);
        validateDocumentExistence(documentId,documentMetadata);
    }

    // TODO: Remove this method later.
    private boolean isDigitalAgreementExpired(DigitalAgreementType digitalAgreement){
        DateTime endDate = new DateTime(digitalAgreement.getDigitalAgreementTerms().getValidityPeriod().getEndDate().toGregorianCalendar().getTime());
        DateTime currentDate = new DateTime();
        if(Days.daysBetween(endDate, currentDate).getDays() > 0) {
            return true;
        }
        return false;
    }
}
