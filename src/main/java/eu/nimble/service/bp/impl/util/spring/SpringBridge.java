package eu.nimble.service.bp.impl.util.spring;

import eu.nimble.common.rest.identity.IdentityClient;
import eu.nimble.common.rest.identity.IdentityClientTyped;
import eu.nimble.service.bp.config.BusinessProcessPersistenceConfig;
import eu.nimble.service.bp.config.GenericConfig;
import eu.nimble.service.bp.impl.persistence.bp.*;
import eu.nimble.service.bp.impl.persistence.catalogue.CatalogueRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Class to access Spring Beans from non-Spring-managed classes.
 * <p>
 * Created by suat on 17-May-18.
 */
@Component
public class SpringBridge implements ApplicationContextAware {
    private static ApplicationContext applicationContext;
    @Autowired
    private GenericConfig genericConfig;

    @Autowired
    private IdentityClient identityClient;

    @Autowired
    private IdentityClientTyped identityClientTyped;

    @Autowired
    private BusinessProcessPersistenceConfig bpConfig;

    @Autowired
    private CatalogueRepository catalogueRepository;

    @Autowired
    private BusinessProcessRepository businessProcessRepository;

    @Autowired
    private ProcessDocumentMetadataDAORepository processDocumentMetadataDAORepository;

    @Autowired
    private ProcessInstanceDAORepository processInstanceDAORepository;

    @Autowired
    private ProcessInstanceGroupDAORepository processInstanceGroupDAORepository;

    @Autowired
    private ProcessDAORepository processDAORepository;

    public static SpringBridge getInstance() {
        return applicationContext.getBean(SpringBridge.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }

    public GenericConfig getGenericConfig() {
        return this.genericConfig;
    }

    public IdentityClientTyped getIdentityClientTyped() {
        return this.identityClientTyped;
    }

    public IdentityClient getIdentityClient() {
        return this.identityClient;
    }

    public BusinessProcessPersistenceConfig getBpConfig() {
        return bpConfig;
    }

    public ProcessDocumentMetadataDAORepository getProcessDocumentMetadataDAORepository() {
        return processDocumentMetadataDAORepository;
    }

    public CatalogueRepository getCatalogueRepository() {
        return catalogueRepository;
    }

    public BusinessProcessRepository getBusinessProcessRepository() {
        return businessProcessRepository;
    }

    public ProcessInstanceDAORepository getProcessInstanceDAORepository() {
        return processInstanceDAORepository;
    }

    public ProcessDAORepository getProcessDAORepository() {
        return processDAORepository;
    }

    public ProcessInstanceGroupDAORepository getProcessInstanceGroupDAORepository() {
        return processInstanceGroupDAORepository;
    }
}