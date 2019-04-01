package eu.nimble.service.bp.impl.util.spring;

import eu.nimble.common.rest.identity.IdentityClientTyped;
import eu.nimble.service.bp.config.BusinessProcessPersistenceConfig;
import eu.nimble.service.bp.config.GenericConfig;
import eu.nimble.utility.persistence.binary.BinaryContentService;
import eu.nimble.utility.persistence.resource.ResourceValidationUtility;
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
    private IdentityClientTyped identityClientTyped;

    @Autowired
    private BusinessProcessPersistenceConfig bpConfig;

    @Autowired
    private ResourceValidationUtility resourceValidationUtil;

    @Autowired
    private BinaryContentService binaryContentService;

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

    public BusinessProcessPersistenceConfig getBpConfig() {
        return bpConfig;
    }

    public ResourceValidationUtility getResourceValidationUtil() {
        return resourceValidationUtil;
    }

    public BinaryContentService getBinaryContentService() {
        return binaryContentService;
    }
}