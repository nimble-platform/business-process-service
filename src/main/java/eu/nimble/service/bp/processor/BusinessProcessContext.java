package eu.nimble.service.bp.processor;

import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.resource.EntityIdAwareRepositoryWrapper;
import eu.nimble.utility.persistence.resource.ResourceValidationUtil;

public class BusinessProcessContext {

    private ProcessInstanceInputMessageDAO messageDAO;
    private ProcessDocumentMetadataDAO metadataDAO;
    private ProcessInstanceDAO processInstanceDAO;
    private ProcessInstanceGroupDAO processInstanceGroupDAO1;
    private ProcessInstanceGroupDAO processInstanceGroupDAO2;
    private ProcessInstanceGroupDAO sourceGroup;
    private ProcessInstanceGroupDAO associatedGroup;
    private ProcessInstanceGroupDAO targetGroup;
    private ProcessInstanceStatus previousStatus;
    private ProcessDocumentStatus previousDocumentMetadataStatus;
    private ProcessDocumentMetadataDAO updatedDocumentMetadata;
    private ProcessInstanceGroupDAO updatedAssociatedGroup;
    private Object previousDocument;
    private Object document;
    private String id;

    public void handleExceptions() {
        if (messageDAO != null) {
//            HibernateUtilityRef.getInstance("bp-data-model").delete(messageDAO);
            SpringBridge.getInstance().getBusinessProcessRepository().deleteEntity(messageDAO);
        }
        if (metadataDAO != null) {
            if (previousDocumentMetadataStatus != null) {
                updatedDocumentMetadata.setStatus(previousDocumentMetadataStatus);
//                HibernateUtilityRef.getInstance("bp-data-model").update(updatedDocumentMetadata);
                SpringBridge.getInstance().getBusinessProcessRepository().updateEntity(updatedDocumentMetadata);
            }
//            HibernateUtilityRef.getInstance("bp-data-model").delete(metadataDAO);
            SpringBridge.getInstance().getBusinessProcessRepository().deleteEntity(metadataDAO);
        }
        if (document != null) {
            EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper((GenericJPARepository) SpringBridge.getInstance().getCatalogueRepository(), metadataDAO.getInitiatorID());
            repositoryWrapper.deleteEntity(document);
            ResourceValidationUtil.removeHjidsForObject(document, Configuration.Standard.UBL.toString());
        }
        if (previousDocument != null){
//            HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(previousDocument);
            SpringBridge.getInstance().getBusinessProcessRepository().persistEntity(previousDocument);
        }
        if (previousStatus == null && processInstanceDAO != null) {
//            HibernateUtilityRef.getInstance("bp-data-model").delete(processInstanceDAO);
            SpringBridge.getInstance().getBusinessProcessRepository().deleteEntity(processInstanceDAO);
        }

        if (processInstanceGroupDAO1 != null) {
//            HibernateUtilityRef.getInstance("bp-data-model").delete(processInstanceGroupDAO1);
            SpringBridge.getInstance().getBusinessProcessRepository().deleteEntity(processInstanceGroupDAO1);
        }
        if (processInstanceGroupDAO2 != null) {
//            HibernateUtilityRef.getInstance("bp-data-model").delete(processInstanceGroupDAO2);
            SpringBridge.getInstance().getBusinessProcessRepository().deleteEntity(processInstanceGroupDAO2);
        }
        if (sourceGroup != null) {
            for (ProcessInstanceGroupDAO.ProcessInstanceGroupDAOProcessInstanceIDsItem p : sourceGroup.getProcessInstanceIDsItems()) {
                if (p.getItem().equals(processInstanceDAO.getProcessInstanceID())) {
//                    HibernateUtilityRef.getInstance("bp-data-model").delete(p);
                    SpringBridge.getInstance().getBusinessProcessRepository().deleteEntity(p);
                }
            }
            if (targetGroup != null) {
                for (ProcessInstanceGroupDAO.ProcessInstanceGroupDAOAssociatedGroupsItem p : sourceGroup.getAssociatedGroupsItems()) {
                    if (p.getItem().equals(targetGroup.getID())) {
//                        HibernateUtilityRef.getInstance("bp-data-model").delete(p);
                        SpringBridge.getInstance().getBusinessProcessRepository().deleteEntity(p);
                    }
                }
            }
        }
        if (associatedGroup != null) {
            for (ProcessInstanceGroupDAO.ProcessInstanceGroupDAOProcessInstanceIDsItem p : associatedGroup.getProcessInstanceIDsItems()) {
                if (p.getItem().equals(processInstanceDAO.getProcessInstanceID())) {
//                    HibernateUtilityRef.getInstance("bp-data-model").delete(p);
                    SpringBridge.getInstance().getBusinessProcessRepository().deleteEntity(p);
                }
            }
        }
        if (targetGroup != null) {
//            HibernateUtilityRef.getInstance("bp-data-model").delete(targetGroup);
            SpringBridge.getInstance().getBusinessProcessRepository().deleteEntity(targetGroup);
        }
        if (previousStatus != null && processInstanceDAO != null) {
            processInstanceDAO.setStatus(ProcessInstanceStatus.fromValue(previousStatus.toString()));
//            HibernateUtilityRef.getInstance("bp-data-model").update(processInstanceDAO);
            SpringBridge.getInstance().getBusinessProcessRepository().updateEntity(processInstanceDAO);
        }
        if (updatedAssociatedGroup != null) {
//            HibernateUtilityRef.getInstance("bp-data-model").delete(updatedAssociatedGroup);
            SpringBridge.getInstance().getBusinessProcessRepository().deleteEntity(updatedAssociatedGroup);
        }
    }

    // Getters and Setters
    public ProcessInstanceInputMessageDAO getMessageDAO() {
        return messageDAO;
    }

    public void setMessageDAO(ProcessInstanceInputMessageDAO messageDAO) {
        this.messageDAO = messageDAO;
    }

    public ProcessDocumentMetadataDAO getMetadataDAO() {
        return metadataDAO;
    }

    public void setMetadataDAO(ProcessDocumentMetadataDAO metadataDAO) {
        this.metadataDAO = metadataDAO;
    }

    public Object getDocument() {
        return document;
    }

    public void setDocument(Object document) {
        this.document = document;
    }

    public ProcessInstanceDAO getProcessInstanceDAO() {
        return processInstanceDAO;
    }

    public void setProcessInstanceDAO(ProcessInstanceDAO processInstanceDAO) {
        this.processInstanceDAO = processInstanceDAO;
    }

    public ProcessInstanceGroupDAO getProcessInstanceGroupDAO1() {
        return processInstanceGroupDAO1;
    }

    public void setProcessInstanceGroupDAO1(ProcessInstanceGroupDAO processInstanceGroupDAO1) {
        this.processInstanceGroupDAO1 = processInstanceGroupDAO1;
    }

    public ProcessInstanceGroupDAO getProcessInstanceGroupDAO2() {
        return processInstanceGroupDAO2;
    }

    public void setProcessInstanceGroupDAO2(ProcessInstanceGroupDAO processInstanceGroupDAO2) {
        this.processInstanceGroupDAO2 = processInstanceGroupDAO2;
    }

    public ProcessInstanceGroupDAO getSourceGroup() {
        return sourceGroup;
    }

    public void setSourceGroup(ProcessInstanceGroupDAO sourceGroup) {
        this.sourceGroup = sourceGroup;
    }

    public ProcessInstanceGroupDAO getAssociatedGroup() {
        return associatedGroup;
    }

    public void setAssociatedGroup(ProcessInstanceGroupDAO associatedGroup) {
        this.associatedGroup = associatedGroup;
    }

    public ProcessInstanceGroupDAO getTargetGroup() {
        return targetGroup;
    }

    public void setTargetGroup(ProcessInstanceGroupDAO targetGroup) {
        this.targetGroup = targetGroup;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ProcessInstanceStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(ProcessInstanceStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public ProcessInstanceGroupDAO getUpdatedAssociatedGroup() {
        return updatedAssociatedGroup;
    }

    public void setUpdatedAssociatedGroup(ProcessInstanceGroupDAO updatedAssociatedGroup) {
        this.updatedAssociatedGroup = updatedAssociatedGroup;
    }

    public ProcessDocumentStatus getPreviousDocumentMetadataStatus() {
        return previousDocumentMetadataStatus;
    }

    public void setPreviousDocumentMetadataStatus(ProcessDocumentStatus previousDocumentMetadataStatus) {
        this.previousDocumentMetadataStatus = previousDocumentMetadataStatus;
    }

    public ProcessDocumentMetadataDAO getUpdatedDocumentMetadata() {
        return updatedDocumentMetadata;
    }

    public void setUpdatedDocumentMetadata(ProcessDocumentMetadataDAO updatedDocumentMetadata) {
        this.updatedDocumentMetadata = updatedDocumentMetadata;
    }

    public Object getPreviousDocument() {
        return previousDocument;
    }

    public void setPreviousDocument(Object previousDocument) {
        this.previousDocument = previousDocument;
    }
}
