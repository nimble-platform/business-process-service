package eu.nimble.service.bp.messaging;

import eu.nimble.service.bp.config.KafkaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaSender {

    @Value("${nimble.kafka.topics.ratingsUpdates}")
    private String ratingsUpdatesTopic;

    @Autowired
    private KafkaTemplate<String, KafkaConfig.AuthorizedCompanyUpdate> kafkaTemplate;

    public void broadcastRatingsUpdate(String companyID, String accessToken) {
        accessToken = accessToken.replace("Bearer ", "");
        KafkaConfig.AuthorizedCompanyUpdate update = new KafkaConfig.AuthorizedCompanyUpdate(companyID, accessToken);
        kafkaTemplate.send(ratingsUpdatesTopic, update);
        System.out.println("Message: " + update + " sent to topic: " + ratingsUpdatesTopic);
    }
}
