package eu.nimble.service.bp.util.persistence.catalogue;

import eu.nimble.service.bp.model.hyperjaxb.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceDAO;
import eu.nimble.service.bp.model.trust.NegotiationRatings;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.util.persistence.bp.ProcessInstanceDAOUtility;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
import eu.nimble.utility.DateUtility;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrustPersistenceUtility {
    private static final Logger logger = LoggerFactory.getLogger(TrustPersistenceUtility.class);

    private static final String QUERY_GET_COMPLETED_TASK_BY_PROCESS_IDS = "SELECT completedTask.hjid FROM CompletedTaskType completedTask WHERE completedTask.associatedProcessInstanceID in :processInstanceIds";
    private static final String QUERY_PROCESS_INSTANCE_IS_RATED = "SELECT count(completedTask) FROM QualifyingPartyType qParty JOIN qParty.party.partyIdentification partyIdentification JOIN qParty.completedTask completedTask " +
            "WHERE partyIdentification.ID = :partyId AND qParty.party.federationInstanceID = :federationId AND completedTask.associatedProcessInstanceID = :processInstanceId and " +
            "(size(completedTask.evidenceSupplied) > 0 or (" +
            "SELECT count(comment) FROM completedTask.comment comment WHERE comment.typeCode.listID <> 'CANCELLATION_REASON'" +
            ") > 0) ";
    private static final String QUERY_GET_CANCELLATION_REASON_FOR_COLLABORATION = "SELECT comment.typeCode.value FROM CompletedTaskType completedTask JOIN completedTask.comment comment " +
            "WHERE completedTask.associatedProcessInstanceID = :processInstanceId AND comment.typeCode.listID = 'CANCELLATION_REASON'";
    private static final String QUERY_GET_COMPLETION_DATE_FOR_COLLABORATION = "SELECT DISTINCT completedTask.period.endDateItem,completedTask.period.endTimeItem  FROM CompletedTaskType completedTask " +
            "WHERE completedTask.associatedProcessInstanceID = :processInstanceId";
    public static boolean processInstanceIsRated(String partyId,String federationId, String processInstanceId) {
        int sizeOfCompletedTasks =  ((Long)new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_PROCESS_INSTANCE_IS_RATED,
                new String[]{"partyId","federationId", "processInstanceId"}, new Object[]{partyId, federationId, processInstanceId})).intValue();
        return sizeOfCompletedTasks > 0;
    }

    public static String getCancellationReasonForCollaboration(String processInstanceId) {
        return new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntity(QUERY_GET_CANCELLATION_REASON_FOR_COLLABORATION,
                new String[]{"processInstanceId"}, new Object[]{processInstanceId});
    }

    public static String getCompletionDateForCollaboration(String processInstanceId) {
        Object[] objects = new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntity(QUERY_GET_COMPLETION_DATE_FOR_COLLABORATION,
                new String[]{"processInstanceId"}, new Object[]{processInstanceId});
        if(objects != null && objects[0] != null && objects[1] != null){
            return objects[0] + " " + objects[1];
        }
        return null;
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

    /**
     * @param comment the cancellation reason for the cancelled collaborations
     * */
    private static void createCompletedTask(String partyID, String federationId, String processInstanceID, String bearerToken, String status, String comment, String completionDate) {
        /**
         * IMPORTANT:
         * {@link QualifyingPartyType}ies should be existing when a {@link CompletedTaskType} is about to be associated to it
         */
        QualifyingPartyType qualifyingParty = PartyPersistenceUtility.getQualifyingPartyType(partyID,federationId,bearerToken);
        CompletedTaskType completedTask = new CompletedTaskType();
        completedTask.setAssociatedProcessInstanceID(processInstanceID);
        TextType textType = new TextType();
        textType.setValue(status);
        textType.setLanguageID("en");
        completedTask.setDescription(Arrays.asList(textType));
        PeriodType periodType = new PeriodType();

        try {
            // set end date of collaboration
            periodType.setEndDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(completionDate));
            periodType.setEndTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(completionDate));
            // set start date of collaboration
            List<ProcessInstanceDAO> processInstanceDAOS;
            ProcessDocumentMetadata requestMetadata;
            processInstanceDAOS = ProcessInstanceDAOUtility.getAllProcessInstancesInCollaborationHistory(processInstanceID);
            requestMetadata = ProcessDocumentMetadataDAOUtility.getRequestMetadata(processInstanceDAOS.get(0).getProcessInstanceID());

            periodType.setStartDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(requestMetadata.getSubmissionDate()));
            periodType.setStartTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(requestMetadata.getSubmissionDate()));
            completedTask.setPeriod(periodType);

        } catch (DatatypeConfigurationException e) {
            String msg = "Date format exception";
            logger.error(msg);
            throw new RuntimeException(msg, e);
        }

        // for the cancelled collaborations, cancellation reason is provided as comment.
        // for the finished collaborations, no comment available
        if(comment != null){
            CommentType commentType = new CommentType();
            CodeType code = new CodeType();
            code.setValue(comment);
            code.setListID("CANCELLATION_REASON");

            commentType.setTypeCode(code);

            completedTask.getComment().add(commentType);
        }

        qualifyingParty.getCompletedTask().add(completedTask);
        new JPARepositoryFactory().forCatalogueRepository().updateEntity(qualifyingParty);
    }

    public static void createCompletedTasksForBothParties(String processInstanceID,String bearerToken,String status, String comment) throws IOException {
        List<ProcessDocumentMetadataDAO> processDocumentMetadatas = ProcessDocumentMetadataDAOUtility.findByProcessInstanceID(processInstanceID);
        ProcessDocumentMetadataDAO processDocumentMetadata = processDocumentMetadatas.get(0);
        String initiatorID = processDocumentMetadata.getInitiatorID();
        String responderID = processDocumentMetadata.getResponderID();
        String initiatorFederationId = processDocumentMetadata.getInitiatorFederationID();
        String responderFederationId = processDocumentMetadata.getResponderFederationID();

        // completion date
        String completionDate = DateUtility.convert(new DateTime());

        if(status.contentEquals("Cancelled")){
            // we need to add the provided comment (cancellation reason) to the completed task of party who have not cancelled the collaboration
            PersonType personCancelledTheProcess = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);
            // get the party which cancelled the collaboration
            PartyType partyCancelledTheProcess = SpringBridge.getInstance().getiIdentityClientTyped().getPartyByPersonID(personCancelledTheProcess.getID()).get(0);

            // initiator party cancelled the collaboration
            if(partyCancelledTheProcess.getPartyIdentification().get(0).getID().contentEquals(initiatorID) && partyCancelledTheProcess.getFederationInstanceID().contentEquals(initiatorFederationId)){
                TrustPersistenceUtility.createCompletedTask(initiatorID,initiatorFederationId,processDocumentMetadata.getProcessInstanceID(),bearerToken,status,null,completionDate);
                TrustPersistenceUtility.createCompletedTask(responderID,responderFederationId,processDocumentMetadata.getProcessInstanceID(),bearerToken,status,comment,completionDate);
            }
            // responder party cancelled the collaboration
            else{
                TrustPersistenceUtility.createCompletedTask(initiatorID,initiatorFederationId,processDocumentMetadata.getProcessInstanceID(),bearerToken,status,comment,completionDate);
                TrustPersistenceUtility.createCompletedTask(responderID,responderFederationId,processDocumentMetadata.getProcessInstanceID(),bearerToken,status,null,completionDate);
            }
        }
        else{
            TrustPersistenceUtility.createCompletedTask(initiatorID,initiatorFederationId,processDocumentMetadata.getProcessInstanceID(),bearerToken,status,null,completionDate);
            TrustPersistenceUtility.createCompletedTask(responderID,responderFederationId,processDocumentMetadata.getProcessInstanceID(),bearerToken,status,null,completionDate);
        }
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
