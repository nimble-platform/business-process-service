package eu.nimble.service.bp.messaging;

import eu.nimble.service.bp.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaSender {

    private final Logger logger = LoggerFactory.getLogger(KafkaSender.class);

    @Value("${nimble.kafka.topics.businessProcessUpdates}")
    private String businessProcessUpdatesTopic;

    @Autowired
    private KafkaTemplate<String, KafkaConfig.AuthorizedCompanyUpdate> kafkaTemplate;

    public void broadcastRatingsUpdate(String companyID, String accessToken) {
        accessToken = accessToken.replace("Bearer ", "");
        KafkaConfig.AuthorizedCompanyUpdate update = new KafkaConfig.AuthorizedCompanyUpdate(companyID, accessToken);
        kafkaTemplate.send(businessProcessUpdatesTopic, update);
        logger.info("Message {} sent to topic: {}", update, businessProcessUpdatesTopic);
    }
}
