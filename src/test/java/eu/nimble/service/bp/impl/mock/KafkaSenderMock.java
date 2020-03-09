package eu.nimble.service.bp.impl.mock;

import eu.nimble.service.bp.messaging.IKafkaSender;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("test")
@Component
public class KafkaSenderMock implements IKafkaSender {
    @Override
    public void broadcastRatingsUpdate(String companyId, String accessToken) {
        // do nothing
    }
}
