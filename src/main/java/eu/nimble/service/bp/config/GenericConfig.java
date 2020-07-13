package eu.nimble.service.bp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by suat on 17-May-18.
 */
@Component
public class GenericConfig {

    @Value("${nimble.data-channel.url}")
    private String dataChannelServiceUrl;

    @Value("${nimble.tracking-analysis.url}")
    private String trackingAnalysisServiceUrl;

    @Value("${nimble.delegate-service.url}")
    private String delegateServiceUrl;

    @Value("${nimble.federation-instance-id}")
    private String federationInstanceId;

    public String getDataChannelServiceUrl() {
        return dataChannelServiceUrl;
    }

    public String getTrackingAnalysisServiceUrl() {
        return trackingAnalysisServiceUrl;
    }

    public String getDelegateServiceUrl() {
        return delegateServiceUrl;
    }

    public String getFederationInstanceId() {
        return federationInstanceId;
    }
}
