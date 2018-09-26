package eu.nimble.service.bp.impl.util.persistence;

import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentMetadataDAO;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.utility.Configuration;

import javax.xml.datatype.DatatypeFactory;
import java.util.List;

public class TrustUtility {

    public static boolean completedTaskExist(QualifyingPartyType qualifyingParty,String processInstanceID){
        for (CompletedTaskType completedTask:qualifyingParty.getCompletedTask()){
            if(completedTask.getAssociatedProcessInstanceID().equals(processInstanceID)){
                return true;
            }
        }
        return false;
    }

    public static void fillCompletedTask(QualifyingPartyType qualifyingParty, List<EvidenceSuppliedType> ratings, List<CommentType> reviews, String processInstanceID){
        for (CompletedTaskType completedTask:qualifyingParty.getCompletedTask()){
            if(completedTask.getAssociatedProcessInstanceID().equals(processInstanceID)){
                completedTask.setComment(reviews);
                completedTask.setEvidenceSupplied(ratings);
            }
        }
    }

    public static void createCompletedTask(String partyID,String processInstanceID,String bearerToken) throws Exception{
        QualifyingPartyType qualifyingParty = CatalogueDAOUtility.getQualifyingPartyType(partyID,bearerToken);
        CompletedTaskType completedTask = new CompletedTaskType();
        completedTask.setAssociatedProcessInstanceID(processInstanceID);
        PeriodType periodType = new PeriodType();

        periodType.setEndDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(DocumentDAOUtility.getResponseMetadata(processInstanceID).getSubmissionDate()));
        periodType.setEndTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(DocumentDAOUtility.getResponseMetadata(processInstanceID).getSubmissionDate()));
        periodType.setStartDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(DocumentDAOUtility.getRequestMetadata(DAOUtility.getAllProcessInstanceIDs(processInstanceID).get(0)).getSubmissionDate()));
        periodType.setStartTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(DocumentDAOUtility.getRequestMetadata(DAOUtility.getAllProcessInstanceIDs(processInstanceID).get(0)).getSubmissionDate()));
        completedTask.setPeriod(periodType);

        qualifyingParty.getCompletedTask().add(completedTask);
        HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(qualifyingParty);
    }

    public static void createCompletedTasksForBothParties(String processInstanceID,String bearerToken) throws Exception{
        List<ProcessDocumentMetadataDAO> processDocumentMetadatas= DAOUtility.getProcessDocumentMetadataByProcessInstanceID(processInstanceID);
        String initiatorID = processDocumentMetadatas.get(0).getInitiatorID();
        String responderID = processDocumentMetadatas.get(0).getResponderID();

        TrustUtility.createCompletedTask(initiatorID,processInstanceID,bearerToken);
        TrustUtility.createCompletedTask(responderID,processInstanceID,bearerToken);
    }
}
