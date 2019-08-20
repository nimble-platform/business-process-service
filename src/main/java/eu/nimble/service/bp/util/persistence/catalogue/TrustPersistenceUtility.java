package eu.nimble.service.bp.util.persistence.catalogue;

import eu.nimble.service.bp.model.hyperjaxb.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceDAO;
import eu.nimble.service.bp.model.trust.NegotiationRatings;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.bp.util.persistence.bp.ProcessInstanceDAOUtility;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrustPersistenceUtility {
    private static final Logger logger = LoggerFactory.getLogger(TrustPersistenceUtility.class);

    private static final String QUERY_GET_COMPLETED_TASK_BY_PROCESS_IDS = "SELECT completedTask.hjid FROM CompletedTaskType completedTask WHERE completedTask.associatedProcessInstanceID in :processInstanceIds";
    private static final String QUERY_PROCESS_INSTANCE_IS_RATED = "SELECT count(completedTask) FROM QualifyingPartyType qParty JOIN qParty.party.partyIdentification partyIdentification JOIN qParty.completedTask completedTask " +
            "WHERE partyIdentification.ID = :partyId AND completedTask.associatedProcessInstanceID = :processInstanceId and (size(completedTask.evidenceSupplied) > 0 or size(completedTask.comment) > 0) ";

    public static boolean processInstanceIsRated(String partyId, String processInstanceId) {
        int sizeOfCompletedTasks =  ((Long)new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_PROCESS_INSTANCE_IS_RATED,
                new String[]{"partyId", "processInstanceId"}, new Object[]{partyId, processInstanceId})).intValue();
        return sizeOfCompletedTasks > 0;
    }

    public static boolean completedTaskExist(List<String> processInstanceIDs){
        List<Long> hjids = new JPARepositoryFactory().forCatalogueRepository().getEntities(QUERY_GET_COMPLETED_TASK_BY_PROCESS_IDS, new String[]{"processInstanceIds"}, new Object[]{processInstanceIDs});
        if(hjids.size() > 0){
            return true;
        }
        return false;
    }

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

    public static void deleteCompletedTasks(List<String> processInstanceIds){
        GenericJPARepository repo = new JPARepositoryFactory().forCatalogueRepository();
        List<Long> hjids = repo.getEntities(QUERY_GET_COMPLETED_TASK_BY_PROCESS_IDS, new String[]{"processInstanceIds"}, new Object[]{processInstanceIds});
        for(Long hjid: hjids){
            repo.deleteEntityByHjid(CompletedTaskType.class,hjid);
        }
    }

    public static void createCompletedTask(String partyID,String processInstanceID,String bearerToken,String status) {
        /**
         * IMPORTANT:
         * {@link QualifyingPartyType}ies should be existing when a {@link CompletedTaskType} is about to be associated to it
         */
        QualifyingPartyType qualifyingParty = PartyPersistenceUtility.getQualifyingPartyType(partyID,bearerToken);
        CompletedTaskType completedTask = new CompletedTaskType();
        completedTask.setAssociatedProcessInstanceID(processInstanceID);
        TextType textType = new TextType();
        textType.setValue(status);
        textType.setLanguageID("en");
        completedTask.setDescription(Arrays.asList(textType));
        PeriodType periodType = new PeriodType();

        ProcessDocumentMetadata responseMetadata = ProcessDocumentMetadataDAOUtility.getResponseMetadata(processInstanceID);
        // TODO: End time and date are NULL for cancelled process for now
        try {
            if (responseMetadata != null) {
                periodType.setEndDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(responseMetadata.getSubmissionDate()));
                periodType.setEndTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(responseMetadata.getSubmissionDate()));
            }
            List<ProcessInstanceDAO> processInstanceDAOS = ProcessInstanceDAOUtility.getAllProcessInstancesInCollaborationHistory(processInstanceID);
            ProcessDocumentMetadata requestMetadata = ProcessDocumentMetadataDAOUtility.getRequestMetadata(processInstanceDAOS.get(0).getProcessInstanceID());
            periodType.setStartDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(requestMetadata.getSubmissionDate()));
            periodType.setStartTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(requestMetadata.getSubmissionDate()));
            completedTask.setPeriod(periodType);

        } catch (DatatypeConfigurationException e) {
            String msg = "Date format exception";
            logger.error(msg);
            throw new RuntimeException(msg, e);
        }

        qualifyingParty.getCompletedTask().add(completedTask);
        new JPARepositoryFactory().forCatalogueRepository().updateEntity(qualifyingParty);
    }

    public static void createCompletedTasksForBothParties(String processInstanceID,String bearerToken,String status) {
        List<ProcessDocumentMetadataDAO> processDocumentMetadatas= ProcessDocumentMetadataDAOUtility.findByProcessInstanceID(processInstanceID);
        createCompletedTasksForBothParties(processDocumentMetadatas.get(0), bearerToken, status);
    }

    public static void createCompletedTasksForBothParties(ProcessDocumentMetadataDAO processDocumentMetadata,String bearerToken,String status) {
        String initiatorID = processDocumentMetadata.getInitiatorID();
        String responderID = processDocumentMetadata.getResponderID();

        TrustPersistenceUtility.createCompletedTask(initiatorID,processDocumentMetadata.getProcessInstanceID(),bearerToken,status);
        TrustPersistenceUtility.createCompletedTask(responderID,processDocumentMetadata.getProcessInstanceID(),bearerToken,status);
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
