package eu.nimble.service.bp.util.trust;

import eu.nimble.service.bp.util.bp.ClassProcessTypeMap;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CompletedTaskType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.EvidenceSuppliedType;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TrustUtility {

    /**
     * Returns the rating summary as json
     *
     * @param completedTasks the completed tasks of company
     * @param processIds     the identifiers of business processes applicable for the company
     * @return JSON consisting of the total number of ratings and average values for sub-ratings
     */
    public static JSONObject createRatingSummaryJson(List<CompletedTaskType> completedTasks, List<String> processIds) {
        // rating summary for the company
        BigDecimal totalNumberOfRatings = BigDecimal.ZERO;
        List<BigDecimal> qualityOfNegotiationProcess = new ArrayList<>();
        List<BigDecimal> qualityOfOrderingProcess = new ArrayList<>();
        List<BigDecimal> responseTimeRating = new ArrayList<>();
        List<BigDecimal> listingAccuracy = new ArrayList<>();
        List<BigDecimal> conformanceToContractualTerms = new ArrayList<>();
        List<BigDecimal> deliveryAndPackaging = new ArrayList<>();

        // get applicable sub-ratings for the process ids
        List<String> applicableRatings = getSubRatingsForBusinessProcesses(processIds);

        for (CompletedTaskType completedTask : completedTasks) {
            if (completedTask.getEvidenceSupplied().size() == 0) {
                continue;
            }
            boolean incrementTotalNumberOfRatings = false;
            for (EvidenceSuppliedType evidenceSupplied : completedTask.getEvidenceSupplied()) {
                if (applicableRatings.contains(evidenceSupplied.getID())) {
                    incrementTotalNumberOfRatings = true;
                    switch (evidenceSupplied.getID()) {
                        case "QualityOfTheNegotiationProcess":
                            qualityOfNegotiationProcess.add(evidenceSupplied.getValueDecimal());
                            break;
                        case "QualityOfTheOrderingProcess":
                            qualityOfOrderingProcess.add(evidenceSupplied.getValueDecimal());
                            break;
                        case "ResponseTime":
                            responseTimeRating.add(evidenceSupplied.getValueDecimal());
                            break;
                        case "ProductListingAccuracy":
                            listingAccuracy.add(evidenceSupplied.getValueDecimal());
                            break;
                        case "ConformanceToOtherAgreedTerms":
                            conformanceToContractualTerms.add(evidenceSupplied.getValueDecimal());
                            break;
                        case "DeliveryAndPackaging":
                            deliveryAndPackaging.add(evidenceSupplied.getValueDecimal());
                            break;
                    }
                }
            }
            if (incrementTotalNumberOfRatings) {
                totalNumberOfRatings = totalNumberOfRatings.add(BigDecimal.ONE);
            }
        }
        // create JSON response
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("totalNumberOfRatings", totalNumberOfRatings);
        jsonResponse.put("qualityOfNegotiationProcess", qualityOfNegotiationProcess.stream().filter(TrustUtility::isNotNullOrZero).mapToLong(BigDecimal::longValue).average().orElse(0));
        jsonResponse.put("qualityOfOrderingProcess", qualityOfOrderingProcess.stream().filter(TrustUtility::isNotNullOrZero).mapToLong(BigDecimal::longValue).average().orElse(0));
        jsonResponse.put("responseTimeRating", responseTimeRating.stream().filter(TrustUtility::isNotNullOrZero).mapToLong(BigDecimal::longValue).average().orElse(0));
        jsonResponse.put("listingAccuracy", listingAccuracy.stream().filter(TrustUtility::isNotNullOrZero).mapToLong(BigDecimal::longValue).average().orElse(0));
        jsonResponse.put("conformanceToContractualTerms", conformanceToContractualTerms.stream().filter(TrustUtility::isNotNullOrZero).mapToLong(BigDecimal::longValue).average().orElse(0));
        jsonResponse.put("deliveryAndPackaging", deliveryAndPackaging.stream().filter(TrustUtility::isNotNullOrZero).mapToLong(BigDecimal::longValue).average().orElse(0));

        return jsonResponse;
    }

    /**
     * Returns the applicable sub-ratings for the given business processes
     *
     * @param processIds the business process ids
     * @return the list of applicable sub-ratings for the given business processes
     */
    private static List<String> getSubRatingsForBusinessProcesses(List<String> processIds) {
        List<String> ratings = new ArrayList<>();
        // if no process is specified, assume that all ratings are available
        if (processIds == null || processIds.size() == 0) {
            ratings.add("QualityOfTheNegotiationProcess");
            ratings.add("QualityOfTheOrderingProcess");
            ratings.add("ResponseTime");
            ratings.add("ProductListingAccuracy");
            ratings.add("ConformanceToOtherAgreedTerms");
            ratings.add("DeliveryAndPackaging");
        } else {
            if (processIds.contains(ClassProcessTypeMap.CAMUNDA_PROCESS_ID_PPAP) || processIds.contains(ClassProcessTypeMap.CAMUNDA_PROCESS_ID_ITEM_INFORMATION_REQUEST)) {
                ratings.add("ProductListingAccuracy");
                ratings.add("ConformanceToOtherAgreedTerms");
            }
            if (processIds.contains(ClassProcessTypeMap.CAMUNDA_PROCESS_ID_NEGOTIATION)) {
                ratings.add("QualityOfTheNegotiationProcess");
                ratings.add("ResponseTime");
            }
            if (processIds.contains(ClassProcessTypeMap.CAMUNDA_PROCESS_ID_ORDER) || processIds.contains(ClassProcessTypeMap.CAMUNDA_PROCESS_ID_TRANSPORT_EXECUTION_PLAN)) {
                ratings.add("QualityOfTheOrderingProcess");
            }
            if (processIds.contains(ClassProcessTypeMap.CAMUNDA_PROCESS_ID_FULFILMENT)) {
                ratings.add("DeliveryAndPackaging");
            }
        }
        return ratings;
    }

    private static boolean isNotNullOrZero(BigDecimal d) {
        return d != null && d.compareTo(BigDecimal.ZERO) != 0;
    }
}
