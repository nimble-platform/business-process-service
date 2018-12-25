package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.impl.model.trust.NegotiationRatings;
import eu.nimble.service.bp.impl.persistence.catalogue.CatalogueRepository;
import eu.nimble.service.bp.impl.persistence.util.CatalogueDAOUtility;
import eu.nimble.service.bp.impl.persistence.util.DAOUtility;
import eu.nimble.service.bp.impl.persistence.util.DocumentMetadataDAOUtility;
import eu.nimble.service.bp.impl.persistence.util.TrustUtility;
import eu.nimble.service.bp.impl.util.serialization.Serializer;
import eu.nimble.service.bp.messaging.KafkaSender;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.utility.HttpResponseUtil;
import io.swagger.annotations.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by dogukan on 24.09.2018.
 */
@Controller
public class TrustServiceController {
    private final Logger logger = LoggerFactory.getLogger(TrustServiceController.class);

    @Autowired
    private KafkaSender kafkaSender;
    @Autowired
    private CatalogueRepository catalogueRepository;

    @ApiOperation(value = "", notes = "Create rating and reviews for the company")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Created rating and reviews for the company successfully"),
            @ApiResponse(code = 400, message = "Party specified with the partyID or the correspond trading party does not exists. OR process instance does not exist. OR specified party is not included in the process instance"),
            @ApiResponse(code = 500, message = "Failed to create rating and reviews for the company")
    })
    @RequestMapping(value = "/ratingsAndReviews",
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity createRatingAndReview(@ApiParam(value = "JSON string representing an array of EvidenceSupplied instances.") @RequestParam(value = "ratings", required = false) String ratingsString,
                                                @ApiParam(value = "JSON string representing an array of Comment instances.") @RequestParam(value = "reviews", required = false) String reviewsString,
                                                @ApiParam(value = "Identifier of the party for which a rating and reviews will be created") @RequestParam(value = "partyID") String partyID,
                                                @ApiParam(value = "Identifier of the process instance associated with the ratings and reviews") @RequestParam(value = "processInstanceID") String processInstanceID,
                                                @ApiParam(value = "", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        try {
            logger.info("Creating rating and reviews for the party with id: {} and process instance with id: {}", partyID, processInstanceID);
            /**
             * CHECKS
             */

            // check party
            QualifyingPartyType qualifyingParty = CatalogueDAOUtility.getQualifyingPartyType(partyID, bearerToken);
            if (qualifyingParty == null) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("No qualifying party exists for the given party id: %s", partyID), HttpStatus.BAD_REQUEST);
            }
            // check process instance id
            List<ProcessDocumentMetadataDAO> processDocumentMetadatas = DAOUtility.getProcessDocumentMetadataByProcessInstanceID(processInstanceID);
            if (processDocumentMetadatas.size() == 0) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("No process document metadata for the given process instance id: %s", processInstanceID), HttpStatus.BAD_REQUEST);
            }
            // check the trading partner existence
            String tradingPartnerId = DocumentMetadataDAOUtility.getTradingPartnerId(processDocumentMetadatas.get(0), partyID);
            PartyType tradingParty = CatalogueDAOUtility.getParty(tradingPartnerId);
            if(tradingParty == null) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("No party exists for the given party id: %s", tradingPartnerId), HttpStatus.BAD_REQUEST);
            }
            // check whether the party is included in the process
            if(!(processDocumentMetadatas.get(0).getInitiatorID().contentEquals(partyID) || processDocumentMetadatas.get(0).getResponderID().contentEquals(partyID))) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("Party: %s is not included in the process instance: {}", tradingPartnerId, processInstanceID), HttpStatus.BAD_REQUEST);
            }
            // check the values
            ObjectMapper objectMapper = Serializer.getDefaultObjectMapper();
            List<EvidenceSuppliedType> ratings = null;
            List<CommentType> reviews = null;
            if(ratingsString == null && reviewsString == null) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("One of the ratings or reviews parameters must be {}", tradingPartnerId, processInstanceID), HttpStatus.BAD_REQUEST);
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

            boolean completedTaskExist = TrustUtility.completedTaskExist(qualifyingParty, processInstanceID);
            if (!completedTaskExist) {
                TrustUtility.createCompletedTasksForBothParties(processDocumentMetadatas.get(0), bearerToken, "Completed");
                // get qualifyingParty (which contains the completed task) again
                qualifyingParty = CatalogueDAOUtility.getQualifyingPartyType(partyID, bearerToken);
            }
            CompletedTaskType completedTaskType = TrustUtility.fillCompletedTask(qualifyingParty, ratings, reviews, processInstanceID);
//            HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(qualifyingParty);
            catalogueRepository.updateEntity(qualifyingParty);

            // broadcast changes
            kafkaSender.broadcastRatingsUpdate(partyID, bearerToken);

            logger.info("Created rating and reviews for the party with id: {} and process instance with id: {}", partyID, processInstanceID);
            return ResponseEntity.ok(completedTaskType);
        } catch (Exception e) {
            logger.error("Failed to create rating and reviews for the party with id: {} and process instance with id: {}", partyID, processInstanceID, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @ApiOperation(value = "",notes = "Gets rating summary for the given company")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved rating summary successfully")
    })
    @RequestMapping(value = "/ratingsSummary",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity getRatingsSummary(@RequestParam(value = "partyID") String partyID,
                                            @ApiParam(value = "" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken){
        logger.info("Getting ratings summary for the party with id: {}",partyID);
        QualifyingPartyType qualifyingParty = CatalogueDAOUtility.getQualifyingPartyType(partyID,bearerToken);
        JSONObject jsonResponse = createJSONResponse(qualifyingParty.getCompletedTask());
        logger.info("Retrieved ratings summary for the party with id: {}",partyID);
        return ResponseEntity.ok(jsonResponse.toString());
    }

    @ApiOperation(value = "",notes = "Gets all individual ratings and review")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved all individual ratings and review successfully")
    })
    @RequestMapping(value = "/ratingsAndReviews",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity listAllIndividualRatingsAndReviews(@RequestParam(value = "partyID") String partyID,
                                                             @ApiParam(value = "" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken){
        try {
            logger.info("Getting all individual ratings and review for the party with id: {}",partyID);
            QualifyingPartyType qualifyingParty = CatalogueDAOUtility.getQualifyingPartyType(partyID,bearerToken);
            List<NegotiationRatings> negotiationRatings = TrustUtility.createNegotiationRatings(qualifyingParty.getCompletedTask());
            String ratingsAndReviews = new ObjectMapper().writeValueAsString(negotiationRatings);
            logger.info("Retrieved all individual ratings and review for the party with id: {}",partyID);
            return ResponseEntity.ok(ratingsAndReviews);
        }
        catch (Exception e){
            logger.error("Unexpected error while getting negotiation ratings and reviews for the party: {}",partyID,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
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



}