package eu.nimble.service.bp.messaging;

public interface IKafkaSender {
    void broadcastRatingsUpdate(String companyId, String accessToken);
}
