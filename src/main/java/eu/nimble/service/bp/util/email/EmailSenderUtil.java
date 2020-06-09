package eu.nimble.service.bp.util.email;

import com.fasterxml.jackson.core.type.TypeReference;
import eu.nimble.common.rest.identity.IIdentityClientTyped;
import eu.nimble.service.bp.model.hyperjaxb.*;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.email.EmailService;
import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by suat on 16-Oct-18.
 */
@Component
@Profile("!test")
public class EmailSenderUtil implements IEmailSenderUtil {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final static String COLLABORATION_ROLE_BUYER = "BUYER";
    private final static String COLLABORATION_ROLE_SELLER = "SELLER";
    private final static String EMPTY_TEXT = "";
    private final static String URL_TEXT = "Link : ";

    @Autowired
    private EmailService emailService;

    @Autowired
    private IIdentityClientTyped iIdentityClientTyped;

    @Value("${nimble.frontend.url}")
    private String frontEndURL;
    @Value("${spring.mail.platformName}")
    private String platformName;
    @Value("${spring.mail.languages}")
    private String mailTemplateLanguages;

    public void notifyTrustScoreUpdate(String partyID, String federationID, String bearerToken, String language) {
        new Thread(() -> {
            PartyType partyType = null;
            try {
                partyType = getParty(partyID,federationID,bearerToken);
            } catch (IOException e) {
                logger.error("Failed to get party with id: {} from identity service", partyID, e);
                return;
            }

            List<PersonType> personTypeList = partyType.getPerson();
            List<String> emailList = new ArrayList<>();
            for (PersonType p : personTypeList) {
                if (p.getRole().contains("sales_officer")) {
                    emailList.add(p.getContact().getElectronicMail());
                }
            }

            if (emailList.size() != 0) {
                String subject = "Trust Score has been updated!";
                Context context = new Context();
                context.setVariable("partyName", partyType.getPartyName().get(0).getName().getValue());
                context.setVariable("url", URL_TEXT + frontEndURL + "/#/user-mgmt/company-rating");
                context.setVariable("platformName",platformName);
                emailService.send(emailList.toArray(new String[0]), subject, getTemplateName("trust_update",language), context);
            }
        }).start();
    }

    // email sender thread
    public void sendCollaborationStatusEmail(String bearerToken,String originalBearerToken,String clientFederationId, ProcessInstanceGroupDAO groupDAO, String language) {
        new Thread(() -> {
            // Collect the trading partner name
            String partyId = groupDAO.getPartyID();
            // the party of the user finishing/cancelling the collaboration
            PartyType party = null;
            // trading partner of the party in this collaboration
            PartyType tradingPartner = null;
            // get parties
            String tradingPartnerId = ProcessDocumentMetadataDAOUtility.getTradingPartnerId(groupDAO.getProcessInstanceIDs().get(0), partyId);
            String tradingPartnerFederationID = ProcessDocumentMetadataDAOUtility.getTradingPartnerFederationId(groupDAO.getProcessInstanceIDs().get(0),partyId);
            String partyIds = partyId +","+tradingPartnerId;
            List<String> federationIds = Arrays.asList(groupDAO.getFederationID(),tradingPartnerFederationID);
            try {
                List<PartyType> parties;
                // parties in this instance
                if(groupDAO.getFederationID().contentEquals(SpringBridge.getInstance().getFederationId()) && tradingPartnerFederationID.contentEquals(SpringBridge.getInstance().getFederationId())){
                    parties = SpringBridge.getInstance().getiIdentityClientTyped().getParties(bearerToken,Arrays.asList(partyId,tradingPartnerId));
                }
                else{
                    Response response = SpringBridge.getInstance().getDelegateClient().getParty(bearerToken,partyIds,false,federationIds);
                    parties = JsonSerializationUtility.getObjectMapper().readValue(HttpResponseUtil.extractBodyFromFeignClientResponse(response),new TypeReference<List<PartyType>>() {
                    });
                }
                for (PartyType partyType : parties) {
                    if(partyType.getPartyIdentification().get(0).getID().contentEquals(partyId) && partyType.getFederationInstanceID().contentEquals(groupDAO.getFederationID())){
                        party = partyType;
                    }
                    else{
                        tradingPartner = partyType;
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to send email for group: {} with status: {}", groupDAO.getID(), groupDAO.getStatus().toString());
                logger.error("Failed to get parties with ids: {} from identity service", partyIds, e);
                return;
            }

            // collect product name
            List<String> productNameList = ProcessDocumentMetadataDAOUtility.findByProcessInstanceID(groupDAO.getProcessInstanceIDs().get(0)).get(0).getRelatedProducts();
            StringBuilder productNames = new StringBuilder("");
            for (int i = 0; i < productNameList.size() - 1; i++) {
                productNames.append(productNameList.get(i)).append(", ");
            }
            productNames.append(productNameList.get(productNameList.size() - 1));


            // Try to identify the recipient email
            //
            // If there are more than one instances in the group, simply select the first process instance and
            // use the creatorID of the document created by the trading party. Otherwise, use the email of the first
            // person associated with the trading partner
            String toEmail;
            if (groupDAO.getProcessInstanceIDs().size() > 1) {
                ProcessDocumentMetadataDAO documentMetadataDAO = ProcessDocumentMetadataDAOUtility.getDocumentOfTheOtherParty(groupDAO.getProcessInstanceIDs().get(0), partyId);
                // get person via the identity client
                String personId = documentMetadataDAO.getCreatorUserID();
                PersonType person;
                try {
                    if(tradingPartnerFederationID.contentEquals(SpringBridge.getInstance().getFederationId())){
                        person = iIdentityClientTyped.getPerson(bearerToken, personId);
                    } else {
                        Response response = SpringBridge.getInstance().getDelegateClient().getPerson(bearerToken,personId,tradingPartnerFederationID);
                        person = JsonSerializationUtility.getObjectMapper().readValue(HttpResponseUtil.extractBodyFromFeignClientResponse(response),PersonType.class);
                    }
                    toEmail = person.getContact().getElectronicMail();
                } catch (IOException e) {
                    logger.error("Failed to send email for group: {} with status: {}", groupDAO.getID(), groupDAO.getStatus().toString());
                    logger.error("Failed to get person with id: {} from identity service", personId, e);
                    return;
                }

            } else {
                toEmail = tradingPartner.getPerson().get(0).getContact().getElectronicMail();
            }

            // collect name of the person cancelling the collaboration
            PersonType person;
            String personName;
            try {
                if(originalBearerToken == null){
                    person = iIdentityClientTyped.getPerson(bearerToken);
                } else{
                    Response response = SpringBridge.getInstance().getDelegateClient().getPersonViaToken(bearerToken, originalBearerToken,clientFederationId);
                    person = JsonSerializationUtility.getObjectMapper().readValue(HttpResponseUtil.extractBodyFromFeignClientResponse(response),PersonType.class);
                }

            } catch (IOException e) {
                logger.error("Failed to send email for group: {} with status: {}", groupDAO.getID(), groupDAO.getStatus().toString());
                logger.error("Failed to get person with token: {} from identity service", bearerToken, e);
                return;
            }
            personName = new StringBuilder("").append(person.getFirstName()).append(" ").append(person.getFamilyName()).toString();

            notifyPartyOnCollaborationStatus(toEmail, personName, productNames.toString(), party.getPartyName().get(0).getName().getValue(),groupDAO.getStatus(),language);
            logger.info("Collaboration status mail sent to: {} for group: {} with status: {}", toEmail, groupDAO.getID(), groupDAO.getStatus().toString());
        }).start();
    }

    private void notifyPartyOnCollaborationStatus(String toEmail, String tradingPartnerPersonName, String productName, String tradingPartnerName, GroupStatus status, String language){
        Context context = new Context();
        String subject;
        String template;

        context.setVariable("tradingPartnerPerson", tradingPartnerPersonName);
        context.setVariable("tradingPartner", tradingPartnerName);
        context.setVariable("product", productName);
        context.setVariable("platformName",platformName);

        if(status.equals(GroupStatus.CANCELLED)){
            subject = platformName + ": Business process cancelled";
            template = getTemplateName("cancelled_collaboration",language);
        }
        else{
            subject = platformName + ": Business process finished";
            template = getTemplateName("finished_collaboration",language);
        }

        emailService.send(new String[]{toEmail}, subject, template, context);
    }

    public void sendNewDeliveryDateEmail(String bearerToken, Date newDeliveryDate, String buyerPartyId,String buyerPartyFederationId,String sellerFederationId, String processInstanceId) {
        new Thread(() -> {
            // get date as a string
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String strDate = dateFormat.format(newDeliveryDate);

            // trading partner of the party in this collaboration
            PartyType tradingPartner = null;
            try {
                tradingPartner = getParty(buyerPartyId,buyerPartyFederationId,bearerToken);
            } catch (IOException e) {
                logger.error("Failed to send the new delivery date information to buyer party: {}", buyerPartyId);
                logger.error("Failed to get party with id: {} from identity service", buyerPartyId, e);
                return;
            }

            // get process document metadata daos
            List<ProcessDocumentMetadataDAO> processDocumentMetadataDAOS = ProcessDocumentMetadataDAOUtility.findByProcessInstanceID(processInstanceId);
            // collect product name
            List<String> productNameList = processDocumentMetadataDAOS.get(0).getRelatedProducts();
            StringBuilder productNames = new StringBuilder("");
            for (int i = 0; i < productNameList.size() - 1; i++) {
                productNames.append(productNameList.get(i)).append(", ");
            }
            productNames.append(productNameList.get(productNameList.size() - 1));


            // get the recipient email
            String toEmail = null;
            for (ProcessDocumentMetadataDAO processDocumentMetadataDAO : processDocumentMetadataDAOS) {
                if(processDocumentMetadataDAO.getInitiatorID().contentEquals(buyerPartyId)){
                    PersonType person;
                    try {
                        person = getPerson(processDocumentMetadataDAO.getCreatorUserID(),processDocumentMetadataDAO.getInitiatorFederationID(),bearerToken);
                        toEmail = person.getContact().getElectronicMail();
                    } catch (IOException e) {
                        logger.error("Failed to send the new delivery date information to buyer party: {}", buyerPartyId);
                        logger.error("Failed to get person with id: {} from identity service", processDocumentMetadataDAO.getCreatorUserID(), e);
                        return;
                    }
                    break;
                }
            }

            if(toEmail == null){
                toEmail = tradingPartner.getPerson().get(0).getContact().getElectronicMail();
            }

            String url = getProcessUrl(processInstanceId,sellerFederationId);

            notifyPartyOnNewDeliveryDate(toEmail,productNames.toString(),tradingPartner.getPartyName().get(0).getName().getValue(),strDate,url);
            logger.info("New delivery date mail sent to: {} for process instance id: {}", toEmail, processInstanceId);
        }).start();
    }

    public void sendActionPendingEmail(String bearerToken, String originalBearerToken, String clientFederationId, String documentId,String language) {
        new Thread(() -> {
            // Get ProcessDocumentMetadataDAO for the given document id
            ProcessDocumentMetadataDAO processDocumentMetadataDAO = ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId);
            // if negotiation awaits response
            ProcessDocumentStatus processDocumentStatus = processDocumentMetadataDAO.getStatus();
            List<String> emailList = new ArrayList<>();
            PartyType respondingParty;
            PartyType initiatingParty;
            PartyType sellerParty = null;
            PartyType buyerParty = null;
            PersonType initiatingPerson;
            String respondingPartyName;
            String initiatingPartyName;
            String subject = EMPTY_TEXT;
            boolean showURL = true;
            boolean initiatorIsBuyer = true;

            try {
                if (processDocumentStatus.equals(ProcessDocumentStatus.WAITINGRESPONSE)) {
                    respondingParty = getParty(processDocumentMetadataDAO.getResponderID(),processDocumentMetadataDAO.getResponderFederationID(),bearerToken);
                    initiatingParty = getParty(processDocumentMetadataDAO.getInitiatorID(),processDocumentMetadataDAO.getInitiatorFederationID(),bearerToken);
                }else {
                    respondingParty = getParty(processDocumentMetadataDAO.getInitiatorID(),processDocumentMetadataDAO.getInitiatorFederationID(),bearerToken);
                    initiatingParty = getParty(processDocumentMetadataDAO.getResponderID(),processDocumentMetadataDAO.getResponderFederationID(),bearerToken);
                }

            } catch (IOException e) {
                logger.error("Failed to get party with id: {} from identity service", processDocumentMetadataDAO.getResponderID(), e);
                return;
            }

            try {
                if(originalBearerToken == null){
                    initiatingPerson = iIdentityClientTyped.getPerson(bearerToken);
                } else{
                    Response response = SpringBridge.getInstance().getDelegateClient().getPersonViaToken(bearerToken,originalBearerToken,clientFederationId);
                    initiatingPerson = JsonSerializationUtility.getObjectMapper().readValue(HttpResponseUtil.extractBodyFromFeignClientResponse(response),PersonType.class);
                }
            } catch (IOException e) {
                logger.error("Failed to get person with token: {} from identity service", bearerToken, e);
                return;
            }

            String initiatingPersonName = new StringBuilder("").append(initiatingPerson.getFirstName()).append(" ").append(initiatingPerson.getFamilyName()).toString();
            String productName = processDocumentMetadataDAO.getRelatedProducts().get(0);

            List<PersonType> personTypeList = respondingParty.getPerson();

            respondingPartyName = respondingParty.getPartyName().get(0).getName().getValue();
            initiatingPartyName = initiatingParty.getPartyName().get(0).getName().getValue();

            DocumentType documentType = processDocumentMetadataDAO.getType();
            if (documentType.equals(DocumentType.ITEMINFORMATIONREQUEST)) {
                subject = platformName + ": Information Requested for " + productName + " from " + initiatingPartyName;
            }else if(documentType.equals(DocumentType.REQUESTFORQUOTATION)) {
                subject = platformName + ": Quotation Requested for " + productName + " from " + initiatingPartyName;
            }else if(documentType.equals(DocumentType.ORDER)) {
                subject = platformName + ": Order Received for " + productName + " from " + initiatingPartyName;
            }else if(documentType.equals(DocumentType.RECEIPTADVICE)) {
                subject = platformName + ": Receipt Advice Received for " + productName + " from " + initiatingPartyName;
            }else if (processDocumentMetadataDAO.getType().equals(DocumentType.ITEMINFORMATIONRESPONSE)){
                initiatorIsBuyer = false;
                subject = platformName + ": Information Received for " + productName + " from " + initiatingPartyName;
            }else if (processDocumentMetadataDAO.getType().equals(DocumentType.QUOTATION)){
                initiatorIsBuyer = false;
                subject = platformName + ": Quotation Received for " + productName + " from " + initiatingPartyName;
            } else if (processDocumentMetadataDAO.getType().equals(DocumentType.ORDERRESPONSESIMPLE)){
                initiatorIsBuyer = false;
                subject = platformName + ": Order Response for " + productName + " from " + initiatingPartyName;
            }else if (processDocumentMetadataDAO.getType().equals(DocumentType.DESPATCHADVICE)){
                initiatorIsBuyer = false;
                subject = platformName + ": Dispatch Advice Received for " + productName + " from " + initiatingPartyName;
            }else {
                showURL = false;
            }

            if (initiatorIsBuyer) {
                buyerParty = initiatingParty;
                sellerParty = respondingParty;
                for (PersonType p : personTypeList) {
                    if (p.getRole().contains("sales_officer") || p.getRole().contains("monitor")) {
                        emailList.add(p.getContact().getElectronicMail());
                    }
                }
            }else {
                buyerParty = respondingParty;
                sellerParty = initiatingParty;
                for (PersonType p : personTypeList) {
                    if (p.getRole().contains("purchaser") || p.getRole().contains("monitor")) {
                        emailList.add(p.getContact().getElectronicMail());
                    }
                }
            }

            String url = EMPTY_TEXT;

            if (emailList.size() != 0) {
                if (showURL) {
                    if (processDocumentMetadataDAO.getResponderID().equals(String.valueOf(sellerParty.getPartyIdentification().get(0).getID()))) {
                        if (processDocumentStatus.equals(ProcessDocumentStatus.WAITINGRESPONSE)) {
                            url = getDashboardUrl(COLLABORATION_ROLE_SELLER);
                        }else {
                            url = getDashboardUrl(COLLABORATION_ROLE_BUYER);
                        }
                    } else if (processDocumentMetadataDAO.getResponderID().equals(String.valueOf(buyerParty.getPartyIdentification().get(0).getID()))) {
                        if (processDocumentStatus.equals(ProcessDocumentStatus.WAITINGRESPONSE)) {
                            url = getDashboardUrl(COLLABORATION_ROLE_BUYER);
                        }else {
                            url = getDashboardUrl(COLLABORATION_ROLE_SELLER);
                        }
                    }
                }

                if (processDocumentStatus.equals(ProcessDocumentStatus.WAITINGRESPONSE)) {
                    notifyPartyOnPendingCollaboration(emailList.toArray(new String[0]), initiatingPersonName, productName, initiatingPartyName, url, subject, respondingPartyName,language);
                } else {
                    notifyPartyOnCollaboration(emailList.toArray(new String[0]), initiatingPersonName, productName, initiatingPartyName, url, subject, respondingPartyName,language);
                }
            }
        }).start();
    }

    private String getDashboardUrl(String collaborationRole) {
        if (COLLABORATION_ROLE_SELLER.equals(collaborationRole)) {
            return URL_TEXT + frontEndURL + "/#/dashboard?tab=SALES";
        } else if (COLLABORATION_ROLE_BUYER.equals(collaborationRole)) {
            return URL_TEXT + frontEndURL + "/#/dashboard?tab=PUCHASES";
        }
        return EMPTY_TEXT;
    }

    private String getProcessUrl(String processInstanceId,String sellerFederationId) {
        return URL_TEXT + frontEndURL + "/#/bpe/bpe-exec/" + processInstanceId + "/" + sellerFederationId;
    }

    public void notifyPartyOnPendingCollaboration(String[] toEmail, String initiatingPersonName, String productName,
                                                  String initiatingPartyName, String url, String subject, String respondingPartyName, String language) {
        Context context = new Context();

        context.setVariable("initiatingPersonName", initiatingPersonName);
        context.setVariable("initiatingPartyName", initiatingPartyName);
        context.setVariable("respondingPartyName", respondingPartyName);
        context.setVariable("product", productName);
        context.setVariable("platformName",platformName);

        if (!url.isEmpty()) {
            context.setVariable("url", url);
        }

        if (subject.equals(EMPTY_TEXT)) {
            subject = platformName + ": Action Required for the Business Process";
        }

        emailService.send(toEmail, subject, getTemplateName("action_pending",language), context);
    }

    public void notifyPartyOnCollaboration(String[] toEmail, String initiatingPersonName, String productName, String initiatingPartyName,
                                           String url, String subject, String respondingPartyName, String language) {
        Context context = new Context();

        context.setVariable("initiatingPersonName", initiatingPersonName);
        context.setVariable("initiatingPartyName", initiatingPartyName);
        context.setVariable("respondingPartyName", respondingPartyName);
        context.setVariable("product", productName);
        context.setVariable("platformName",platformName);

        if (!url.isEmpty()) {
            context.setVariable("url", url);
        }

        if (subject.equals(EMPTY_TEXT)) {
            subject = platformName + ": Transition of the Business Process";
        }

        emailService.send(toEmail, subject, getTemplateName("continue_colloboration",language), context);
    }

    public void notifyPartyOnNewDeliveryDate(String toEmail,String productName, String respondingPartyName, String expectedDeliveryDate, String url) {
        Context context = new Context();

        context.setVariable("respondingPartyName", respondingPartyName);
        context.setVariable("expectedDeliveryDate", expectedDeliveryDate);
        context.setVariable("product", productName);
        context.setVariable("url", url);
        context.setVariable("platformName",platformName);

        List<String> languages = Arrays.asList(mailTemplateLanguages.split(","));
        emailService.send(new String[]{toEmail}, platformName + ": New Delivery Date", getTemplateName("new_delivery_date",languages.get(0)), context);
    }

    private PartyType getParty(String partyId,String federationId,String bearerToken) throws IOException {
        PartyType party = null;
        if(federationId.contentEquals(SpringBridge.getInstance().getFederationId())){
            party = iIdentityClientTyped.getParty(bearerToken, partyId);
        }
        else {
            Response response = SpringBridge.getInstance().getDelegateClient().getParty(bearerToken, Long.valueOf(partyId),false,federationId);
            party = JsonSerializationUtility.getObjectMapper().readValue(HttpResponseUtil.extractBodyFromFeignClientResponse(response),PartyType.class);
        }
        return party;
    }

    private PersonType getPerson(String personId,String federationId,String bearerToken) throws IOException {
        PersonType person = null;
        if(federationId.contentEquals(SpringBridge.getInstance().getFederationId())){
            person = iIdentityClientTyped.getPerson(bearerToken, personId);
        }
        else {
            Response response = SpringBridge.getInstance().getDelegateClient().getPerson(bearerToken, personId,federationId);
            person = JsonSerializationUtility.getObjectMapper().readValue(HttpResponseUtil.extractBodyFromFeignClientResponse(response),PersonType.class);
        }
        return person;
    }

    private String getTemplateName(String templateName,String language){
        List<String> languages = Arrays.asList(mailTemplateLanguages.split(","));
        if(languages.contains(language)){
            return String.format("%s_%s",templateName,language);
        }
        return String.format("%s_%s",templateName,languages.get(0));
    }
}
