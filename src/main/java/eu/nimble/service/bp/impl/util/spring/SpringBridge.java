package eu.nimble.service.bp.impl.util.spring;

import eu.nimble.common.rest.identity.IdentityClient;
import eu.nimble.common.rest.identity.IdentityClientTyped;
import eu.nimble.service.bp.config.GenericConfig;
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
}