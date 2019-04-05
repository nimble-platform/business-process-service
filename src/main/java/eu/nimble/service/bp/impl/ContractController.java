package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.hyperjaxb.model.DocumentType;
import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceDAO;
import eu.nimble.service.bp.impl.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.impl.util.persistence.bp.ProcessInstanceDAOUtility;
import eu.nimble.service.bp.impl.util.persistence.catalogue.ContractPersistenceUtility;
import eu.nimble.service.bp.impl.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.transportexecutionplanrequest.TransportExecutionPlanRequestType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.resource.EntityIdAwareRepositoryWrapper;
import eu.nimble.utility.persistence.resource.ResourceValidationUtility;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog;

/**
 * Created by suat on 25-Apr-18.
 */
@Controller
public class ContractController {
    private final Logger logger = LoggerFactory.getLogger(ContractController.class);

    @Autowired
    private ResourceValidationUtility resourceValidationUtil;

    @ApiOperation(value = "",notes = "Retrieves the specified ClauseType")
    @ApiResponses(value = {
            @ApiResponse(code = 200,message = "Retrieved the clause successfully",response = ClauseType.class),
            @ApiResponse(code = 401,message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404,message = "No clause for the given id"),
            @ApiResponse(code = 500,message = "Unexpected error while getting the clause")
    })
    @RequestMapping(value = "/clauses/{clauseId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getClauseDetails(@ApiParam(value = "Unique identifier of the ClauseType to be retrieved (clause.id)", required = true) @PathVariable(value = "clauseId", required = true) String clauseId,
                                           @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        try {
            logger.info("Getting clause with id: {}", clauseId);
            // check token
            ResponseEntity tokenCheck = eu.nimble.service.bp.impl.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            ClauseType clause = ContractPersistenceUtility.getClause(clauseId);
            if (clause == null) {
                return createResponseEntityAndLog(String.format("No clause for the given id: %s", clauseId), HttpStatus.NOT_FOUND);
            }
            logger.info("Retrieved clause with id: {}", clauseId);
            return ResponseEntity.ok().body(clause);

        } catch (Exception e) {
            return createResponseEntityAndLog(String.format("Unexpected error while getting the clause with id: %s", clauseId), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "",notes = "Updates the ClauseType with the specifiedId")
    @ApiResponses(value = {
            @ApiResponse(code = 200,message = "Updated the clause successfully",response = ClauseType.class),
            @ApiResponse(code = 400,message = "Invalid Clause content"),
            @ApiResponse(code = 401,message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500,message = "Unexpected error while updating the clause")
    })
    @RequestMapping(value = "/clauses/{clauseId}",
            method = RequestMethod.PUT)
    public ResponseEntity updateClause(@ApiParam(value = "Identifier of the ClauseType to be updated (clause.id)", required = true) @PathVariable(value = "clauseId") String clauseId,
                                       @ApiParam(value = "Serialized form of the complete ClauseType", required = true) @RequestBody String serializedClause,
                                       @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value = "Authorization", required = true) String bearerToken) {

        try {
            logger.info("Updating clause with id: {}", clauseId);
            // check token
            ResponseEntity tokenCheck = eu.nimble.service.bp.impl.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            // get person using the given bearer token
            PersonType person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);
            // get party for the person
            PartyType party = SpringBridge.getInstance().getiIdentityClientTyped().getPartyByPersonID(person.getID()).get(0);

            // parse the base clause object to get the type
            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
            ClauseType clause;
            try {
                clause = objectMapper.readValue(serializedClause, ClauseType.class);
            } catch (IOException e) {
                return createResponseEntityAndLog("Failed to deserialize the clause: " + serializedClause, e, HttpStatus.BAD_REQUEST);
            }

            // parse the derived clause object
            Object clauseObject;
            if (clause.getType().contentEquals(eu.nimble.service.model.ubl.extension.ClauseType.DATA_MONITORING.toString())) {
                try {
                    clauseObject = objectMapper.readValue(serializedClause, DataMonitoringClauseType.class);
                } catch (IOException e) {
                    return createResponseEntityAndLog("Failed to deserialize data monitoring clause: " + serializedClause, e, HttpStatus.BAD_REQUEST);
                }

            } else {
                try {
                    clauseObject = objectMapper.readValue(serializedClause, DocumentClauseType.class);
                } catch (IOException e) {
                    return createResponseEntityAndLog("Failed to deserialize document clause: " + serializedClause, e, HttpStatus.BAD_REQUEST);
                }
            }

            // validate the entity ids
            boolean hjidsBelongToCompany = resourceValidationUtil.hjidsBelongsToParty(clauseObject, party.getPartyIdentification().get(0).getID(), Configuration.Standard.UBL.toString());
            if(!hjidsBelongToCompany) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("Some of the identifiers (hjid fields) do not belong to the party in the passed clause: %s", serializedClause), null, HttpStatus.BAD_REQUEST, LogLevel.INFO);
            }

            // update clause
            try {
                EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(party.getPartyIdentification().get(0).getID());
                clauseObject = repositoryWrapper.updateEntity(clauseObject);
            } catch (Exception e) {
                return createResponseEntityAndLog("Failed to update the clause: " + clauseId, e, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            logger.info("Updated clause with id: {}", clauseId);
            return ResponseEntity.ok().body(clauseObject);

        } catch (Exception e) {
            return createResponseEntityAndLog("Unexpected error in updating the clause: " + clauseId, e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

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
                                                               @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        try {
            logger.info("Constructing contract starting from the process instance: {}", processInstanceId);
            // check token
            ResponseEntity tokenCheck = eu.nimble.service.bp.impl.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            // check existence and type of the process instance
            ProcessInstanceDAO processInstance = ProcessInstanceDAOUtility.getById(processInstanceId);
            if (processInstance == null) {
                return createResponseEntityAndLog(String.format("Invalid process instance id: %s", processInstanceId), HttpStatus.NOT_FOUND);
            }

            ContractType contract = ContractPersistenceUtility.constructContractForProcessInstances(processInstance);
            logger.info("Constructed contract starting from the process instance: {}", processInstanceId);

            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
            return ResponseEntity.ok().body(objectMapper.writeValueAsString(contract));
        } catch (Exception e) {
            return createResponseEntityAndLog(String.format("Unexpected error while constructing contract for process: ", processInstanceId), e, HttpStatus.INTERNAL_SERVER_ERROR);
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
            logger.info("Getting clauses for contract: {}", contractId);
            // check token
            ResponseEntity tokenCheck = eu.nimble.service.bp.impl.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            ContractType contract = ContractPersistenceUtility.getContract(contractId);
            if (contract == null) {
                return createResponseEntityAndLog(String.format("No contract for the given id: %s", contractId), HttpStatus.NOT_FOUND);
            }

            ObjectMapper mapper = JsonSerializationUtility.getObjectMapper();

            logger.info("Retrieved clauses for contract: {}", contractId);
            return ResponseEntity.ok().body(mapper.writeValueAsString(contract.getClause()));

        } catch (Exception e) {
            return createResponseEntityAndLog(String.format("Unexpected error while getting clauses for contract: %s", contractId), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "",notes = "Deletes a clause from the contract")
    @ApiResponses(value = {
            @ApiResponse(code = 200,message = "Deleted clause from the contract and returned the updated contract successfully",response = ContractType.class),
            @ApiResponse(code = 401,message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404,message = "No contract or clause for the specified identifiers"),
            @ApiResponse(code = 500,message = "Unexpected error while deleting the clause from the contract")
    })
    @RequestMapping(value = "/contracts/{contractId}/clauses/{clauseId}",
            method = RequestMethod.DELETE)
    public ResponseEntity deleteClauseFromContract(@ApiParam(value = "Identifier of the contract (contract.id) from which the clause to be deleted", required = true) @PathVariable(value = "contractId") String contractId,
                                                   @ApiParam(value = "Identifier of the clause (clause.id) to be deleted", required = true) @PathVariable(value = "clauseId") String clauseId,
                                                   @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        try {
            logger.info("Deleting clause: {} from contract: {}", clauseId, contractId);
            // get person using the given bearer token
            PersonType person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);
            // get party for the person
            PartyType party = SpringBridge.getInstance().getiIdentityClientTyped().getPartyByPersonID(person.getID()).get(0);

            // check token
            ResponseEntity tokenCheck = eu.nimble.service.bp.impl.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            // check existence of contract
            if (!ContractPersistenceUtility.contractExists(contractId)) {
                return createResponseEntityAndLog("Invalid contract id: " + contractId, HttpStatus.NOT_FOUND);
            }

            // check existence of clause
            ClauseType clause = ContractPersistenceUtility.getContractClause(contractId, clauseId);
            if (clause == null) {
                return createResponseEntityAndLog("Invalid clause id: " + clauseId + " for contract: " + contractId, HttpStatus.NOT_FOUND);
            }

            // delete the clause
            try {
                EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(party.getPartyIdentification().get(0).getID());
                repositoryWrapper.deleteEntity(clause);
            } catch (Exception e) {
                return createResponseEntityAndLog("Failed to delete clause: " + clauseId + " from contract: " + contractId, e, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // return updated version
            ContractType contract = ContractPersistenceUtility.getContract(contractId);
            logger.info("Deleted clause: {} from contract: {}", clauseId, contractId);
            return ResponseEntity.ok().body(contract);

        } catch (Exception e) {
            return createResponseEntityAndLog("Unexpected error while deleting the clause: " + clauseId + " from contract: " + contractId, e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

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
                                           @ApiParam(value = "Type of the clauses to be retrieved. If no type specified all the clauses are retrieved.") @RequestParam(value = "clauseType") eu.nimble.service.model.ubl.extension.ClauseType clauseType,
                                           @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        try {
            logger.info("Getting clause for document: {}, type: {}", documentId, clauseType);
            // check token
            ResponseEntity tokenCheck = eu.nimble.service.bp.impl.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            // check existence and type of the document bound to the contract
            ProcessDocumentMetadataDAO documentMetadata = ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId);
            if (documentMetadata == null) {
                return createResponseEntityAndLog(String.format("No document for the specified id: %s", documentMetadata.getDocumentID()), HttpStatus.NOT_FOUND);
            }
            DocumentType documentType = documentMetadata.getType();
            if (!(documentType == DocumentType.ORDER || documentType == DocumentType.TRANSPORTEXECUTIONPLANREQUEST)) {
                return createResponseEntityAndLog(String.format("Invalid bounded-document type: %s", documentType), HttpStatus.NOT_FOUND);
            }

            List<ClauseType> clause = ContractPersistenceUtility.getClauses(documentMetadata.getDocumentID(), documentType, clauseType);
            if (clause == null || clause.size() == 0) {
                return createResponseEntityAndLog(String.format("No clause for the document: %s, clause type: %s", documentId, clauseType), HttpStatus.NO_CONTENT);
            }
            logger.info("Retrieved clause with for document: {}, type: {}", documentId, clauseType);
            return ResponseEntity.ok().body(clause);

        } catch (Exception e) {
            return createResponseEntityAndLog(String.format("Unexpected error while getting the clause details for document id: %s, clause type: %s", documentId, clauseType), e, HttpStatus.INTERNAL_SERVER_ERROR);
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
                                                      @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        try {
            logger.info("Adding document clause to contract. Bounded-document id: {}, clause document id: {}", documentId, clauseDocumentId);
            // check token
            ResponseEntity tokenCheck = eu.nimble.service.bp.impl.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            // check the passed parameters
            // check existence and type of the document bound to the contract
            ResponseEntity response = validateDocumentExistence(documentId);
            if (response != null) {
                return response;
            }

            // check existence and type of the clause document
            ProcessDocumentMetadataDAO clauseDocumentMetadata = ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId);
            if (clauseDocumentMetadata == null) {
                return createResponseEntityAndLog(String.format("Invalid clause document id: %s", clauseDocumentId), HttpStatus.NOT_FOUND);
            }

            // get person using the given bearer token
            PersonType person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);
            // get party for the person
            PartyType party = SpringBridge.getInstance().getiIdentityClientTyped().getPartyByPersonID(person.getID()).get(0);

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
                EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(party.getPartyIdentification().get(0).getID());
                document = repositoryWrapper.updateEntity(document);
            } catch (Exception e) {
                return createResponseEntityAndLog(String.format("Failed to add document clause to contract. Bounded-document id: %s, clause document id: %s", documentId, clauseDocumentId), e, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            logger.info("Added document clause to contract. Bounded-document id: {}, clause document id: {}, clause id: {}", documentId, clauseDocumentId, clause.getID());
            return ResponseEntity.ok(document);

        } catch (Exception e) {
            return createResponseEntityAndLog(String.format("Unexpected error while adding clause to contract. Bounded-document id: %s , clause document id: %s", documentId, clauseDocumentId), e, HttpStatus.INTERNAL_SERVER_ERROR);
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
                                                            @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {

        try {
            logger.info("Adding data monitoring clause to contract. Bounded-document id: {}", documentId);
            // check token
            ResponseEntity tokenCheck = eu.nimble.service.bp.impl.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            // check existence and type of the document bound to the contract
            ResponseEntity response = validateDocumentExistence(documentId);
            if (response != null) {
                return response;
            }

            // get person using the given bearer token
            PersonType person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);
            // get party for the person
            PartyType party = SpringBridge.getInstance().getiIdentityClientTyped().getPartyByPersonID(person.getID()).get(0);

            // check contract of the document
            ProcessDocumentMetadataDAO documentMetadata = ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId);
            Object document = DocumentPersistenceUtility.getUBLDocument(documentId, documentMetadata.getType());
            ContractType contract = checkDocumentContract(document);

            dataMonitoringClause.setID(UUID.randomUUID().toString());
            dataMonitoringClause.setType(eu.nimble.service.model.ubl.extension.ClauseType.DATA_MONITORING.toString());
            contract.getClause().add(dataMonitoringClause);

            // persist the update
            try {
                EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(party.getPartyIdentification().get(0).getID());
                document = repositoryWrapper.updateEntity(document);
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
            return createResponseEntityAndLog("No document found for the specified id: " + documentMetadata.getDocumentID(), HttpStatus.NOT_FOUND);
        }
        DocumentType documentType = documentMetadata.getType();
        if (!(documentType == DocumentType.ORDER || documentType == DocumentType.TRANSPORTEXECUTIONPLANREQUEST)) {
            return createResponseEntityAndLog("No contract available for the specified document type: " + documentType, HttpStatus.NOT_FOUND);
        }

        return null;
    }

    private ResponseEntity validateDocumentExistence(String documentId) {
        ProcessDocumentMetadataDAO documentMetadata = ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId);
        return validateDocumentExistence(documentMetadata);
    }
}
