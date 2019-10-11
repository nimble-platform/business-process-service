package eu.nimble.service.bp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

/**
 * Created by suat on 17-May-18.
 */
@Component
public class GenericConfig {

    @Value("${nimble.data-channel.url}")
    private String dataChannelServiceUrl;

    @Value("${nimble.delegate-service.url}")
    private String delegateServiceUrl;

    @Value("${nimble.federation.id}")
    private String federationId;

    public String getDataChannelServiceUrl() {
        return dataChannelServiceUrl;
    }

    public String getDelegateServiceUrl() {
        return delegateServiceUrl;
    }

    public String getFederationId() {
        return federationId;
    }
}
