package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.impl.model.trust.NegotiationRatings;
import eu.nimble.service.bp.impl.util.persistence.*;
import eu.nimble.service.bp.impl.util.serialization.Serializer;
import eu.nimble.service.bp.messaging.KafkaSender;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.utility.Configuration;
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

    @ApiOperation(value = "",notes = "Create rating and reviews for the company")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Created rating and reviews for the company successfully"),
            @ApiResponse(code = 500, message = "Failed to create rating and reviews for the company")
    })
    @RequestMapping(value = "/ratingsAndReviews",
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity createRatingAndReview(@ApiParam(value = "JSON string which represents ratings") @RequestParam(value = "ratings",required = true) String ratingsString,
                                                @ApiParam(value = "JSON string which represents comments") @RequestParam(value = "reviews",required = true) String reviewsString,
                                                @RequestParam(value = "partyID") String partyID,
                                                @RequestParam(value = "processInstanceID") String processInstanceID,
                                                @ApiParam(value = "" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken){
        logger.info("Creating rating and reviews for the party with id: {} and process instance with id: {}",partyID,processInstanceID);
        QualifyingPartyType qualifyingParty = CatalogueDAOUtility.getQualifyingPartyType(partyID,bearerToken);
        try {
            ObjectMapper objectMapper = Serializer.getDefaultObjectMapper();
            List<EvidenceSuppliedType> ratings = objectMapper.readValue(ratingsString,new TypeReference<List<EvidenceSuppliedType>>(){});
            List<CommentType> reviews = objectMapper.readValue(reviewsString,new TypeReference<List<CommentType>>(){});

            boolean completedTaskExist = TrustUtility.completedTaskExist(qualifyingParty,processInstanceID);
            if(!completedTaskExist){
                TrustUtility.createCompletedTasksForBothParties(processInstanceID,bearerToken,"Completed");
                // get qualifyingParty (which contains the completed task) again
                qualifyingParty = CatalogueDAOUtility.getQualifyingPartyType(partyID,bearerToken);
            }
            TrustUtility.fillCompletedTask(qualifyingParty,ratings,reviews,processInstanceID);
            HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(qualifyingParty);

            // broadcast changes
            kafkaSender.broadcastRatingsUpdate(partyID,bearerToken);
        }
        catch (Exception e){
            logger.error("Failed to create rating and reviews for the party with id: {} and process instance with id: {}",partyID,processInstanceID,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        logger.info("Created rating and reviews for the party with id: {} and process instance with id: {}",partyID,processInstanceID);
        return ResponseEntity.ok(null);
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
            @ApiResponse(code = 200, message = "Retrieved all individual ratings and review successfully",response = NegotiationRatings.class)
    })
    @RequestMapping(value = "/ratingsAndReviews",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity listAllIndividualRatingsAndReviews(@RequestParam(value = "partyID") String partyID,
                                                             @ApiParam(value = "" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken){
        logger.info("Getting all individual ratings and review for the party with id: {}",partyID);
        QualifyingPartyType qualifyingParty = CatalogueDAOUtility.getQualifyingPartyType(partyID,bearerToken);
        NegotiationRatings negotiationRatings = TrustUtility.createNegotiationRatings(qualifyingParty.getCompletedTask());
        logger.info("Retrieved all individual ratings and review for the party with id: {}",partyID);
        return ResponseEntity.ok(negotiationRatings);
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