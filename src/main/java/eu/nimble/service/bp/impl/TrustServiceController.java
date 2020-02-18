package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.messaging.IKafkaSender;
import eu.nimble.service.bp.model.hyperjaxb.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.model.trust.NegotiationRatings;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.util.persistence.catalogue.PartyPersistenceUtility;
import eu.nimble.service.bp.util.persistence.catalogue.TrustPersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.validation.IValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Created by dogukan on 24.09.2018.
 */
@Controller
public class TrustServiceController {
    private final Logger logger = LoggerFactory.getLogger(TrustServiceController.class);

    @Autowired
    private IKafkaSender kafkaSender;
    @Autowired
    private IValidationUtil validationUtil;

    @ApiOperation(value = "", notes = "Create rating and reviews for the company. A CompletedTaskType is created as a result" +
            "of this operation.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Created rating and reviews for the company successfully"),
            @ApiResponse(code = 400, message = "Party specified with the partyId or the correspond trading party does not exists. OR process instance does not exist. OR specified party is not included in the process instance"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Failed to create rating and reviews for the company")
    })
    @RequestMapping(value = "/ratingsAndReviews",
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity createRatingAndReview(@ApiParam(value = "JSON string representing an array of EvidenceSupplied instances.<br>Example:<br>[{\"id\":\"QualityOfTheNegotiationProcess\",\"valueDecimal\":5},{\"id\":\"QualityOfTheOrderingProcess\",\"valueDecimal\":3},{\"id\":\"ResponseTime\",\"valueDecimal\":4},{\"id\":\"ProductListingAccuracy\",\"valueDecimal\":2},{\"id\":\"ConformanceToOtherAgreedTerms\",\"valueDecimal\":5},{\"id\":\"DeliveryAndPackaging\",\"valueDecimal\":3}]", required = true) @RequestParam(value = "ratings", required = true) String ratingsString,
                                                @ApiParam(value = "JSON string representing an array of Comment instances.<br>Example:<br>[{\"comment\":\"Awesome trading partner\",\"typeCode\":{\"value\":\"\",\"name\":\"\",\"uri\":\"\",\"listID\":\"\",\"listURI\":\"\"}},{\"comment\":\"Perfect collaboration\",\"typeCode\":{\"value\":\"\",\"name\":\"\",\"uri\":\"\",\"listID\":\"\",\"listURI\":\"\"}}]") @RequestParam(value = "reviews", required = false) String reviewsString,
                                                @ApiParam(value = "Identifier of the party for which a rating and reviews will be created", required = true) @RequestParam(value = "partyId") String partyId,
                                                @ApiParam(value = "Identifier of the process instance associated with the ratings and reviews.Usually,it is the identifier of the last process instance which concludes the collaboration", required = true) @RequestParam(value = "processInstanceID") String processInstanceID,
                                                @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
                                                @ApiParam(value = "", required = true) @RequestHeader(value = "federationId", required = true) String federationId) {
        try {
            logger.info("Creating rating and reviews for the party with id: {} and process instance with id: {}", partyId, processInstanceID);
            /**
             * CHECKS
             */

            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // check party
            QualifyingPartyType qualifyingParty = PartyPersistenceUtility.getQualifyingParty(partyId,federationId);
            if (qualifyingParty == null) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_NO_QUALIFYING_PARTY.toString(), Arrays.asList(partyId));
            }
            // check process instance id
            List<ProcessDocumentMetadataDAO> processDocumentMetadatas = ProcessDocumentMetadataDAOUtility.findByProcessInstanceID(processInstanceID);
            if (processDocumentMetadatas.size() == 0) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_NO_PROCESS_DOCUMENT_METADATA.toString(), Arrays.asList(processInstanceID));
            }
            // check the trading partner existence
            String tradingPartnerId = ProcessDocumentMetadataDAOUtility.getTradingPartnerId(processDocumentMetadatas.get(0), partyId);
            String tradingPartnerFederationId = ProcessDocumentMetadataDAOUtility.getTradingPartnerFederationId(processDocumentMetadatas.get(0),partyId);
            PartyType tradingParty = PartyPersistenceUtility.getParty(tradingPartnerId,tradingPartnerFederationId);
            if(tradingParty == null) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_NO_PARTY.toString(), Arrays.asList(tradingPartnerId));
            }
            // check whether the party is included in the process
            if(!((processDocumentMetadatas.get(0).getInitiatorID().contentEquals(partyId) && processDocumentMetadatas.get(0).getInitiatorFederationID().contentEquals(federationId)) || (processDocumentMetadatas.get(0).getResponderID().contentEquals(partyId) && processDocumentMetadatas.get(0).getResponderFederationID().contentEquals(federationId)))) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_PARTY_NOT_INCLUDED_IN_PROCESS.toString(), Arrays.asList(tradingPartnerId, processInstanceID));
            }
            // check the values
            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
            List<EvidenceSuppliedType> ratings = null;
            List<CommentType> reviews = null;
            if(ratingsString == null && reviewsString == null) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_MISSING_RATING_PARAMETER.toString(), Arrays.asList(tradingPartnerId, processInstanceID));
            }
            if(ratingsString != null) {
                ratings = objectMapper.readValue(ratingsString, new TypeReference<List<EvidenceSuppliedType>>() {});
            }
            if(reviewsString != null) {
                reviews = objectMapper.readValue(reviewsString, new TypeReference<List<CommentType>>() {});
            }

            /**
             * LOGIC
             */

            boolean completedTaskExist = TrustPersistenceUtility.completedTaskExist(qualifyingParty, processInstanceID);
            if (!completedTaskExist) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_NO_COMPLETED_TASK.toString(), Arrays.asList(partyId, processInstanceID));
            }
            CompletedTaskType completedTaskType = TrustPersistenceUtility.fillCompletedTask(qualifyingParty, ratings, reviews, processInstanceID);
            new JPARepositoryFactory().forCatalogueRepository().updateEntity(qualifyingParty);

            // broadcast changes
            kafkaSender.broadcastRatingsUpdate(partyId, bearerToken);

            logger.info("Created rating and reviews for the party with id: {} and process instance with id: {}", partyId, processInstanceID);
            return ResponseEntity.ok(completedTaskType);

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_CREATE_RATING_AND_REVIEW.toString(),Arrays.asList(partyId, processInstanceID),e);
        }
    }

    @ApiOperation(value = "",notes = "Gets rating summary for the given company")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved rating summary successfully"),
            @ApiResponse(code = 400, message = "No qualifying party exists for the given party id"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token")
    })
    @RequestMapping(value = "/ratingsSummary",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getRatingsSummary(@ApiParam(value = "Identifier of the party whose ratings will be received", required = true) @RequestParam(value = "partyId") String partyId,
                                            @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken,
                                            @ApiParam(value = "" ,required=false ) @RequestHeader(value="federationId", required=false) String federationId){
        logger.info("Getting ratings summary for the party with id: {}",partyId);

        // validate federation id header
        federationId = validateFederationIdHeader(federationId);

        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        // check party
        QualifyingPartyType qualifyingParty = PartyPersistenceUtility.getQualifyingParty(partyId,federationId);
        if (qualifyingParty == null) {
            throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_NO_QUALIFYING_PARTY.toString(), Arrays.asList(partyId));
        }
        JSONObject jsonResponse = createJSONResponse(qualifyingParty.getCompletedTask());
        logger.info("Retrieved ratings summary for the party with id: {}",partyId);
        return ResponseEntity.ok(jsonResponse.toString());
    }

    @ApiOperation(value = "",notes = "Gets all individual ratings and review")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved all individual ratings and review successfully"),
            @ApiResponse(code = 400, message = "No qualifying party exists for the given party id"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while getting ratings and reviews")
    })
    @RequestMapping(value = "/ratingsAndReviews",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity listAllIndividualRatingsAndReviews(@ApiParam(value = "Identifier of the party whose individual ratings and reviews will be received", required = true) @RequestParam(value = "partyId") String partyId,
                                                             @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken,
                                                             @ApiParam(value = "" ,required=false ) @RequestHeader(value="federationId", required=false) String federationId) throws Exception{
        try {
            // validate federation id header
            federationId = validateFederationIdHeader(federationId);

            logger.info("Getting all individual ratings and review for the party with id: {}",partyId);
            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // check party
            QualifyingPartyType qualifyingParty = PartyPersistenceUtility.getQualifyingParty(partyId,federationId);
            if (qualifyingParty == null) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_NO_QUALIFYING_PARTY.toString(), Arrays.asList(partyId));
            }
            List<NegotiationRatings> negotiationRatings = TrustPersistenceUtility.createNegotiationRatings(qualifyingParty.getCompletedTask());
            String ratingsAndReviews = JsonSerializationUtility.getObjectMapper().writeValueAsString(negotiationRatings);
            logger.info("Retrieved all individual ratings and review for the party with id: {}",partyId);
            return ResponseEntity.ok(ratingsAndReviews);
        }
        catch (Exception e){
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_LIST_ALL_INDIVIDUAL_RATINGS_AND_REVIEWS.toString(), Arrays.asList(partyId),e);
        }
    }

    private JSONObject createJSONResponse(List<CompletedTaskType> completedTasks){
        // rating summary for the company
        BigDecimal totalNumberOfRatings = BigDecimal.ZERO;
        BigDecimal qualityOfNegotiationProcess = BigDecimal.ZERO;
        BigDecimal qualityOfOrderingProcess = BigDecimal.ZERO;
        BigDecimal responseTimeRating = BigDecimal.ZERO;
        BigDecimal listingAccuracy = BigDecimal.ZERO;
        BigDecimal conformanceToContractualTerms = BigDecimal.ZERO;
        BigDecimal deliveryAndPackaging = BigDecimal.ZERO;
        for(CompletedTaskType completedTask:completedTasks){
            if(completedTask.getEvidenceSupplied().size() == 0){
                continue;
            }
            else{
                totalNumberOfRatings = totalNumberOfRatings.add(BigDecimal.ONE);
            }
            for(EvidenceSuppliedType evidenceSupplied:completedTask.getEvidenceSupplied()){
                if(evidenceSupplied.getID().equals("QualityOfTheNegotiationProcess")){
                    qualityOfNegotiationProcess = qualityOfNegotiationProcess.add(evidenceSupplied.getValueDecimal());
                }
                else if(evidenceSupplied.getID().equals("QualityOfTheOrderingProcess")){
                    qualityOfOrderingProcess = qualityOfOrderingProcess.add(evidenceSupplied.getValueDecimal());
                }
                else if(evidenceSupplied.getID().equals("ResponseTime")){
                    responseTimeRating = responseTimeRating.add(evidenceSupplied.getValueDecimal());
                }
                else if(evidenceSupplied.getID().equals("ProductListingAccuracy")){
                    listingAccuracy = listingAccuracy.add(evidenceSupplied.getValueDecimal());
                }
                else if(evidenceSupplied.getID().equals("ConformanceToOtherAgreedTerms")){
                    conformanceToContractualTerms = conformanceToContractualTerms.add(evidenceSupplied.getValueDecimal());
                }
                else if(evidenceSupplied.getID().equals("DeliveryAndPackaging")){
                    deliveryAndPackaging = deliveryAndPackaging.add(evidenceSupplied.getValueDecimal());
                }
            }
        }
        // create JSON response
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("totalNumberOfRatings",totalNumberOfRatings);
        jsonResponse.put("qualityOfNegotiationProcess",qualityOfNegotiationProcess);
        jsonResponse.put("qualityOfOrderingProcess",qualityOfOrderingProcess);
        jsonResponse.put("responseTimeRating",responseTimeRating);
        jsonResponse.put("listingAccuracy",listingAccuracy);
        jsonResponse.put("conformanceToContractualTerms",conformanceToContractualTerms);
        jsonResponse.put("deliveryAndPackaging",deliveryAndPackaging);

        return jsonResponse;
    }

    private String validateFederationIdHeader(String federationIdHeader){
        if(federationIdHeader == null){
            federationIdHeader = SpringBridge.getInstance().getFederationId();
        }
        return federationIdHeader;
    }

}