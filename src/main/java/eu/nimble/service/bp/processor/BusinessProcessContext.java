package eu.nimble.service.bp.processor;

import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import eu.nimble.service.bp.util.camunda.CamundaEngine;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.resource.EntityIdAwareRepositoryWrapper;

import java.util.Collections;

/**
 * Makes sure that all operations in business process and catalogue repositories are performed in a single transaction.
 * */
public class BusinessProcessContext {

    /**
     * Identifier of the BusinessProcessContext
     * */
    private String id;
    /**
     * Repositories
     * */
    // lazy loading disabled business process repository
    private GenericJPARepository bpRepository;
    // lazy loading disabled catalogue repository
    private GenericJPARepository catalogRepository;
    private EntityIdAwareRepositoryWrapper entityIdAwareRepository;
    /**
     * The identifier of process instance which is started in {@link eu.nimble.service.bp.impl.StartController#startProcessInstance(String, ProcessInstanceInputMessage, String, String, String)} service
     * We use this id to rollback the changes related to this process instance in Camunda.
     * */
    private String processInstanceId;

    public GenericJPARepository getBpRepository() {
        if(bpRepository == null){
            bpRepository = new JPARepositoryFactory().forBpRepositoryMultiTransaction(true);
        }
        return bpRepository;
    }

    public GenericJPARepository getCatalogRepository() {
        if(catalogRepository == null){
            catalogRepository = new JPARepositoryFactory().forCatalogueRepositoryMultiTransaction(true);
        }
        return catalogRepository;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public EntityIdAwareRepositoryWrapper getEntityIdAwareRepository(String partyId) {
        if(entityIdAwareRepository == null){
            entityIdAwareRepository = new EntityIdAwareRepositoryWrapper(partyId, getCatalogRepository());
            entityIdAwareRepository.beginTransaction();

        }
        return entityIdAwareRepository;
    }

    public String getId() {
        return id;
    }

    public void commitDbUpdates() {
        if(bpRepository != null){
            bpRepository.commit();
        }
        if(catalogRepository != null){
            catalogRepository.commit();
        }
        if(entityIdAwareRepository != null){
            entityIdAwareRepository.commit();
        }
    }

    public void rollbackDbUpdates() {
        if(bpRepository != null){
            bpRepository.rollback();
        }
        if(catalogRepository != null){
            catalogRepository.rollback();
        }
        if(entityIdAwareRepository != null){
            entityIdAwareRepository.rollback();
        }
        if(processInstanceId != null){
            CamundaEngine.deleteProcessInstances(Collections.singleton(processInstanceId));
        }
    }

    public void setId(String id) {
        this.id = id;
    }
}