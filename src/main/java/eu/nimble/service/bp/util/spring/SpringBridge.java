package eu.nimble.service.bp.util.spring;

import eu.nimble.common.rest.datachannel.IDataChannelClient;
import eu.nimble.common.rest.delegate.IDelegateClient;
import eu.nimble.common.rest.identity.IIdentityClientTyped;
import eu.nimble.service.bp.config.BusinessProcessPersistenceConfig;
import eu.nimble.service.bp.config.GenericConfig;
import eu.nimble.service.bp.contract.FrameContractService;
import eu.nimble.utility.persistence.resource.ResourceValidationUtility;
import feign.Response;
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
    private IIdentityClientTyped iIdentityClientTyped;
    @Autowired
    private BusinessProcessPersistenceConfig bpConfig;
    @Autowired
    private ResourceValidationUtility resourceValidationUtil;
    @Autowired
    private FrameContractService frameContractService;
    @Autowired
    private IDataChannelClient dataChannelClient;
    @Autowired
    private IDelegateClient delegateClient;

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

    public IIdentityClientTyped getiIdentityClientTyped() {
        return iIdentityClientTyped;
    }

    public BusinessProcessPersistenceConfig getBpConfig() {
        return bpConfig;
    }

    public ResourceValidationUtility getResourceValidationUtil() {
        return resourceValidationUtil;
    }

    public FrameContractService getFrameContractService() {
        return frameContractService;
    }

    public IDataChannelClient getDataChannelClient() {
        return dataChannelClient;
    }

    public IDelegateClient getDelegateClient() {
        return delegateClient;
    }

    public String getFederationId() {
        return getGenericConfig().getFederationInstanceId();
    }

    public boolean isDelegateServiceRunning(){
        try {
            Response response = delegateClient.getFederationId();
            if(response != null && response.status() == 200){
                return true;
            }
        }
        catch (Exception e){
            return false;
        }
        return false;
    }
}