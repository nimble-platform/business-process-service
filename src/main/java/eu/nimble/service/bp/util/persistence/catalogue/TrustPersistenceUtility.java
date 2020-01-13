package eu.nimble.service.bp.util.persistence.catalogue;

import eu.nimble.service.bp.model.hyperjaxb.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceDAO;
import eu.nimble.service.bp.model.trust.NegotiationRatings;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.util.persistence.bp.ProcessInstanceDAOUtility;
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
            "WHERE partyIdentification.ID = :partyId AND qParty.party.federationInstanceID = :federationId AND completedTask.associatedProcessInstanceID = :processInstanceId and (size(completedTask.evidenceSupplied) > 0 or size(completedTask.comment) > 0) ";

    public static boolean processInstanceIsRated(String partyId,String federationId, String processInstanceId) {
        int sizeOfCompletedTasks =  ((Long)new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_PROCESS_INSTANCE_IS_RATED,
                new String[]{"partyId","federationId", "processInstanceId"}, new Object[]{partyId, federationId, processInstanceId})).intValue();
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

    public static void createCompletedTask(String partyID,String federationId,String processInstanceID,String bearerToken,String status, String businessContextId) {
        /**
         * IMPORTANT:
         * {@link QualifyingPartyType}ies should be existing when a {@link CompletedTaskType} is about to be associated to it
         */
        QualifyingPartyType qualifyingParty = PartyPersistenceUtility.getQualifyingPartyType(partyID,federationId,bearerToken,businessContextId);
        CompletedTaskType completedTask = new CompletedTaskType();
        completedTask.setAssociatedProcessInstanceID(processInstanceID);
        TextType textType = new TextType();
        textType.setValue(status);
        textType.setLanguageID("en");
        completedTask.setDescription(Arrays.asList(textType));
        PeriodType periodType = new PeriodType();

        ProcessDocumentMetadata responseMetadata = businessContextId != null ? ProcessDocumentMetadataDAOUtility.getResponseMetadata(processInstanceID,BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(businessContextId).getBpRepository()) : ProcessDocumentMetadataDAOUtility.getResponseMetadata(processInstanceID);
        // TODO: End time and date are NULL for cancelled process for now
        try {
            if (responseMetadata != null) {
                periodType.setEndDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(responseMetadata.getSubmissionDate()));
                periodType.setEndTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(responseMetadata.getSubmissionDate()));
            }
            List<ProcessInstanceDAO> processInstanceDAOS;
            ProcessDocumentMetadata requestMetadata;
            if(businessContextId != null){
                processInstanceDAOS = ProcessInstanceDAOUtility.getAllProcessInstancesInCollaborationHistory(processInstanceID,BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(businessContextId).getBpRepository()) ;
                requestMetadata = ProcessDocumentMetadataDAOUtility.getRequestMetadata(processInstanceDAOS.get(0).getProcessInstanceID(),BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(businessContextId).getBpRepository());
            }
            else{
                processInstanceDAOS = ProcessInstanceDAOUtility.getAllProcessInstancesInCollaborationHistory(processInstanceID);
                requestMetadata = ProcessDocumentMetadataDAOUtility.getRequestMetadata(processInstanceDAOS.get(0).getProcessInstanceID());

            }
            periodType.setStartDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(requestMetadata.getSubmissionDate()));
            periodType.setStartTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(requestMetadata.getSubmissionDate()));
            completedTask.setPeriod(periodType);

        } catch (DatatypeConfigurationException e) {
            String msg = "Date format exception";
            logger.error(msg);
            throw new RuntimeException(msg, e);
        }

        qualifyingParty.getCompletedTask().add(completedTask);
        if(businessContextId == null){
            new JPARepositoryFactory().forCatalogueRepository().updateEntity(qualifyingParty);
        }
        else {
            BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(businessContextId).getCatalogRepository().updateEntity(qualifyingParty);
        }
    }

    public static void createCompletedTasksForBothParties(String processInstanceID,String bearerToken,String status, String businessContextId) {
        List<ProcessDocumentMetadataDAO> processDocumentMetadatas;
        if(businessContextId != null){
            processDocumentMetadatas = ProcessDocumentMetadataDAOUtility.findByProcessInstanceID(processInstanceID, BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(businessContextId).getBpRepository());
        }
        else{
            processDocumentMetadatas = ProcessDocumentMetadataDAOUtility.findByProcessInstanceID(processInstanceID);
        }

        createCompletedTasksForBothParties(processDocumentMetadatas.get(0), bearerToken, status,businessContextId);
    }

    public static void createCompletedTasksForBothParties(String processInstanceID,String bearerToken,String status) {
        createCompletedTasksForBothParties(processInstanceID,bearerToken,status,null);
    }

    public static void createCompletedTasksForBothParties(ProcessDocumentMetadataDAO processDocumentMetadata,String bearerToken,String status, String businessContextId) {
        String initiatorID = processDocumentMetadata.getInitiatorID();
        String responderID = processDocumentMetadata.getResponderID();
        String initiatorFederationId = processDocumentMetadata.getInitiatorFederationID();
        String responderFederationId = processDocumentMetadata.getResponderFederationID();

        TrustPersistenceUtility.createCompletedTask(initiatorID,initiatorFederationId,processDocumentMetadata.getProcessInstanceID(),bearerToken,status,businessContextId);
        TrustPersistenceUtility.createCompletedTask(responderID,responderFederationId,processDocumentMetadata.getProcessInstanceID(),bearerToken,status,businessContextId);
    }


    public static List<NegotiationRatings> createNegotiationRatings(List<CompletedTaskType> completedTasks){
        List<NegotiationRatings> negotiationRatings = new ArrayList<>();

        for (CompletedTaskType completedTask:completedTasks){

            // consider only Completed tasks
            if(completedTask.getDescription().get(0).getValue().contentEquals("Completed")){
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
