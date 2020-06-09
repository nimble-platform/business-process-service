package eu.nimble.service.bp.impl.mock;

import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceGroupDAO;
import eu.nimble.service.bp.util.email.IEmailSenderUtil;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Date;

@Profile("test")
@Component
public class EmailSenderUtilMock implements IEmailSenderUtil {
    @Override
    public void notifyTrustScoreUpdate(String partyID, String federationID, String bearerToken, String language) {

    }

    @Override
    public void sendCollaborationStatusEmail(String bearerToken, String originalBearerToken, String clientFederationId, ProcessInstanceGroupDAO groupDAO, String language) {

    }

    @Override
    public void sendNewDeliveryDateEmail(String bearerToken, Date newDeliveryDate, String buyerPartyId, String buyerPartyFederationId, String sellerFederationId, String processInstanceId) {

    }

    @Override
    public void sendActionPendingEmail(String bearerToken, String originalBearerToken, String clientFederationId, String documentId, String language) {

    }

    @Override
    public void notifyPartyOnPendingCollaboration(String[] toEmail, String initiatingPersonName, String productName, String initiatingPartyName, String url, String subject, String respondingPartyName, String language) {

    }

    @Override
    public void notifyPartyOnCollaboration(String[] toEmail, String initiatingPersonName, String productName, String initiatingPartyName, String url, String subject, String respondingPartyName, String language) {

    }

    @Override
    public void notifyPartyOnNewDeliveryDate(String toEmail, String productName, String respondingPartyName, String expectedDeliveryDate, String url) {

    }
}
