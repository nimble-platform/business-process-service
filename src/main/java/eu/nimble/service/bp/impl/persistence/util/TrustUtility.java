package eu.nimble.service.bp.impl.persistence.util;

import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.impl.model.trust.NegotiationRatings;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrustUtility {
    private static final Logger logger = LoggerFactory.getLogger(TrustUtility.class);

    public static boolean completedTaskExist(QualifyingPartyType qualifyingParty,String processInstanceID){
        for (CompletedTaskType completedTask:qualifyingParty.getCompletedTask()){
            if(completedTask.getAssociatedProcessInstanceID().equals(processInstanceID)){
                return true;
            }
        }
        return false;
    }

    public static CompletedTaskType fillCompletedTask(QualifyingPartyType qualifyingParty, List<EvidenceSuppliedType> ratings, List<CommentType> reviews, String processInstanceID){
        for (CompletedTaskType completedTask:qualifyingParty.getCompletedTask()){
            if(completedTask.getAssociatedProcessInstanceID().equals(processInstanceID)){
                completedTask.setComment(reviews);
                completedTask.setEvidenceSupplied(ratings);
                return completedTask;
            }
        }
        return null;
    }

    public static void createCompletedTask(String partyID,String processInstanceID,String bearerToken,String status) {
        /**
         * IMPORTANT:
         * {@link QualifyingPartyType}ies should be existing when a {@link CompletedTaskType} is about to be associated to it
         */
        QualifyingPartyType qualifyingParty = CatalogueDAOUtility.getQualifyingPartyType(partyID,bearerToken);
        CompletedTaskType completedTask = new CompletedTaskType();
        completedTask.setAssociatedProcessInstanceID(processInstanceID);
        completedTask.setDescription(Arrays.asList(status));
        PeriodType periodType = new PeriodType();

        ProcessDocumentMetadata responseMetadata = DocumentDAOUtility.getResponseMetadata(processInstanceID);
        // TODO: End time and date are NULL for cancelled process for now
        try {
            if (responseMetadata != null) {
                periodType.setEndDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(responseMetadata.getSubmissionDate()));
                periodType.setEndTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(responseMetadata.getSubmissionDate()));
            }
            ProcessDocumentMetadata requestMetadata = DocumentDAOUtility.getRequestMetadata(DAOUtility.getAllProcessInstanceIDs(processInstanceID).get(0));
            periodType.setStartDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(requestMetadata.getSubmissionDate()));
            periodType.setStartTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(requestMetadata.getSubmissionDate()));
            completedTask.setPeriod(periodType);

        } catch (DatatypeConfigurationException e) {
            String msg = "Date format exception";
            logger.error(msg);
            throw new RuntimeException(msg, e);
        }

        qualifyingParty.getCompletedTask().add(completedTask);
//        HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(qualifyingParty);
        SpringBridge.getInstance().getCatalogueRepository().updateEntity(qualifyingParty);
    }

    public static void createCompletedTasksForBothParties(String processInstanceID,String bearerToken,String status) {
        List<ProcessDocumentMetadataDAO> processDocumentMetadatas= DAOUtility.getProcessDocumentMetadataByProcessInstanceID(processInstanceID);
        createCompletedTasksForBothParties(processDocumentMetadatas.get(0), bearerToken, status);
    }

    public static void createCompletedTasksForBothParties(ProcessDocumentMetadataDAO processDocumentMetadata,String bearerToken,String status) {
        String initiatorID = processDocumentMetadata.getInitiatorID();
        String responderID = processDocumentMetadata.getResponderID();

        TrustUtility.createCompletedTask(initiatorID,processDocumentMetadata.getProcessInstanceID(),bearerToken,status);
        TrustUtility.createCompletedTask(responderID,processDocumentMetadata.getProcessInstanceID(),bearerToken,status);
    }


    public static List<NegotiationRatings> createNegotiationRatings(List<CompletedTaskType> completedTasks){
        List<NegotiationRatings> negotiationRatings = new ArrayList<>();

        for (CompletedTaskType completedTask:completedTasks){

            // consider only Completed tasks
            if(completedTask.getDescription().get(0).equals("Completed")){
                List<EvidenceSuppliedType> ratings = new ArrayList<>();
                List<CommentType> reviews = new ArrayList<>();

                // ratings
                for (EvidenceSuppliedType evidenceSupplied:completedTask.getEvidenceSupplied()){
                    ratings.add(evidenceSupplied);
                }
                // reviews
                for(CommentType comment:completedTask.getComment()){
                    reviews.add(comment);
                }
                negotiationRatings.add(new NegotiationRatings(completedTask.getAssociatedProcessInstanceID(),ratings,reviews));
            }
        }

        return negotiationRatings;
    }
}
