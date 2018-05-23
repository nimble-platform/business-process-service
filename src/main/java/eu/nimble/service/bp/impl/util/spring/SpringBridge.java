package eu.nimble.service.bp.impl.util.spring;

import eu.nimble.service.bp.config.GenericConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Class to access Spring Beans from non-Spring-managed classes.
 *
 * Created by suat on 17-May-18.
 */
@Component
public class SpringBridge implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    public static SpringBridge getInstance() {
        return applicationContext.getBean(SpringBridge.class);
    }

    @Autowired
    private GenericConfig genericConfig;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }

    public GenericConfig getGenericConfig() {
        return this.genericConfig;
    }
}
