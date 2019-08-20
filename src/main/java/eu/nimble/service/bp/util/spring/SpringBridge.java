package eu.nimble.service.bp.util.spring;

import eu.nimble.common.rest.datachannel.IDataChannelClient;
import eu.nimble.common.rest.identity.IIdentityClientTyped;
import eu.nimble.service.bp.config.BusinessProcessPersistenceConfig;
import eu.nimble.service.bp.config.GenericConfig;
import eu.nimble.service.bp.contract.FrameContractService;
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
    private IIdentityClientTyped iIdentityClientTyped;
    @Autowired
    private BusinessProcessPersistenceConfig bpConfig;
    @Autowired
    private ResourceValidationUtility resourceValidationUtil;
    @Autowired
    private BinaryContentService binaryContentService;
    @Autowired
    private FrameContractService frameContractService;
    @Autowired
    private IDataChannelClient dataChannelClient;

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

    public BinaryContentService getBinaryContentService() {
        return binaryContentService;
    }

    public FrameContractService getFrameContractService() {
        return frameContractService;
    }

    public IDataChannelClient getDataChannelClient() {
        return dataChannelClient;
    }
}