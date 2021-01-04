package eu.nimble.service.bp.util.email;

import com.fasterxml.jackson.core.type.TypeReference;
import eu.nimble.common.rest.identity.IIdentityClientTyped;
import eu.nimble.service.bp.exception.NimbleExceptionMessageCode;
import eu.nimble.service.bp.model.hyperjaxb.*;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.util.persistence.catalogue.PartyPersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.email.EmailService;
import eu.nimble.utility.validation.NimbleRole;
import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
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
    private MessageSource messageSource;

    @Autowired
    private IIdentityClientTyped iIdentityClientTyped;

    @Value("${nimble.frontend.url}")
    private String frontEndURL;
    @Value("${spring.mail.platformName}")
    private String platformName;
    @Value("${spring.mail.languages}")
    private String mailTemplateLanguages;

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
                    parties = SpringBridge.getInstance().getiIdentityClientTyped().getParties(bearerToken,Arrays.asList(partyId,tradingPartnerId),true);
                }
                else{
                    Response response = SpringBridge.getInstance().getDelegateClient().getParty(bearerToken,partyIds,true,federationIds);
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

            // get process metadata
            ProcessDocumentMetadataDAO processDocumentMetadataDAO = ProcessDocumentMetadataDAOUtility.findByProcessInstanceID(groupDAO.getProcessInstanceIDs().get(0)).get(0);
            // collect product name
            List<String> productNameList = processDocumentMetadataDAO.getRelatedProducts();
            StringBuilder productNames = new StringBuilder("");
            for (int i = 0; i < productNameList.size() - 1; i++) {
                productNames.append(productNameList.get(i)).append(", ");
            }
            productNames.append(productNameList.get(productNameList.size() - 1));

            // check whether the trading partner is buyer or not
            boolean isTradingPartnerBuyer = processDocumentMetadataDAO.getInitiatorID().contentEquals(tradingPartnerId) && processDocumentMetadataDAO.getInitiatorFederationID().contentEquals(tradingPartnerFederationID);
            if(processDocumentMetadataDAO.getType().equals(DocumentType.DESPATCHADVICE) || processDocumentMetadataDAO.getType().equals(DocumentType.RECEIPTADVICE)){
                isTradingPartnerBuyer = !isTradingPartnerBuyer;
            }
            // get dashboard url for the trading partner
            String url = getDashboardUrl(isTradingPartnerBuyer ? COLLABORATION_ROLE_BUYER : COLLABORATION_ROLE_SELLER);
            // collect name of the person cancelling the collaboration
            PersonType person;
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
            String personName = String.format("%s %s",person.getFirstName(),person.getFamilyName());
            // populate toEmail and toEmailTradingPartner lists
            List<String> toEmail = new ArrayList<>();
            List<String> toEmailTradingPartner = new ArrayList<>();

            String tradingPartnerRequiredRole = NimbleRole.SALES_OFFICER.getName();
            String requiredRole = NimbleRole.PURCHASER.getName();
            if(isTradingPartnerBuyer){
                tradingPartnerRequiredRole = NimbleRole.PURCHASER.getName() ;
                requiredRole = NimbleRole.SALES_OFFICER.getName() ;
            }

            for (PersonType p : tradingPartner.getPerson()) {
                if (p.getRole().contains(tradingPartnerRequiredRole) || p.getRole().contains(NimbleRole.MONITOR.getName())) {
                    toEmailTradingPartner.add(p.getContact().getElectronicMail());
                }
            }

            for (PersonType p : party.getPerson()) {
                if (p.getRole().contains(requiredRole) || p.getRole().contains(NimbleRole.MONITOR.getName())) {
                    toEmail.add(p.getContact().getElectronicMail());
                }
            }

            notifyPartyOnCollaborationStatus(toEmail.toArray(new String[0]), toEmailTradingPartner.toArray(new String[0]), personName, productNames.toString(), party.getPartyName().get(0).getName().getValue(),groupDAO.getStatus(),url,language);
            logger.info("Collaboration status mail sent to: {} for group: {} with status: {}", toEmail, groupDAO.getID(), groupDAO.getStatus().toString());
        }).start();
    }

    public void sendNewDeliveryDateEmail(String bearerToken, Date newDeliveryDate, String buyerPartyId,String buyerPartyFederationId,String sellerFederationId, String processInstanceId) {
        new Thread(() -> {
            // get date as a string
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String strDate = dateFormat.format(newDeliveryDate);

            // trading partner of the party in this collaboration
            PartyType tradingPartner = null;
            try {
                tradingPartner = PartyPersistenceUtility.getParty(buyerPartyId,buyerPartyFederationId,bearerToken);
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

    public void sendBusinessProcessStatusEmail(String bearerToken, String originalBearerToken, String clientFederationId, String documentId, String language) {
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
                    respondingParty = PartyPersistenceUtility.getParty(processDocumentMetadataDAO.getResponderID(),processDocumentMetadataDAO.getResponderFederationID(),bearerToken);
                    initiatingParty = PartyPersistenceUtility.getParty(processDocumentMetadataDAO.getInitiatorID(),processDocumentMetadataDAO.getInitiatorFederationID(),bearerToken);
                }else {
                    respondingParty = PartyPersistenceUtility.getParty(processDocumentMetadataDAO.getInitiatorID(),processDocumentMetadataDAO.getInitiatorFederationID(),bearerToken);
                    initiatingParty = PartyPersistenceUtility.getParty(processDocumentMetadataDAO.getResponderID(),processDocumentMetadataDAO.getResponderFederationID(),bearerToken);
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
                subject = getMailSubject(NimbleExceptionMessageCode.MAIL_SUBJECT_INFORMATION_REQUESTED, language, Arrays.asList(platformName,productName,initiatingPartyName));
            }else if(documentType.equals(DocumentType.REQUESTFORQUOTATION)) {
                subject = getMailSubject(NimbleExceptionMessageCode.MAIL_SUBJECT_QUOTATION_REQUESTED, language, Arrays.asList(platformName,productName,initiatingPartyName));
            }else if(documentType.equals(DocumentType.ORDER)) {
                subject = getMailSubject(NimbleExceptionMessageCode.MAIL_SUBJECT_ORDER_RECEIVED, language, Arrays.asList(platformName,productName,initiatingPartyName));
            }else if(documentType.equals(DocumentType.RECEIPTADVICE)) {
                subject = getMailSubject(NimbleExceptionMessageCode.MAIL_SUBJECT_RECEIPT_ADVICE_RECEIVED, language, Arrays.asList(platformName,productName,initiatingPartyName));
            }else if (processDocumentMetadataDAO.getType().equals(DocumentType.ITEMINFORMATIONRESPONSE)){
                initiatorIsBuyer = false;
                subject = getMailSubject(NimbleExceptionMessageCode.MAIL_SUBJECT_INFORMATION_RECEIVED, language, Arrays.asList(platformName,productName,initiatingPartyName));
            }else if (processDocumentMetadataDAO.getType().equals(DocumentType.QUOTATION)){
                initiatorIsBuyer = false;
                subject = getMailSubject(NimbleExceptionMessageCode.MAIL_SUBJECT_QUOTATION_RECEIVED, language, Arrays.asList(platformName,productName,initiatingPartyName));
            } else if (processDocumentMetadataDAO.getType().equals(DocumentType.ORDERRESPONSESIMPLE)){
                initiatorIsBuyer = false;
                subject = getMailSubject(NimbleExceptionMessageCode.MAIL_SUBJECT_ORDER_RESPONSE, language, Arrays.asList(platformName,productName,initiatingPartyName));
            }else if (processDocumentMetadataDAO.getType().equals(DocumentType.DESPATCHADVICE)){
                initiatorIsBuyer = false;
                subject = getMailSubject(NimbleExceptionMessageCode.MAIL_SUBJECT_DISPATCH_ADVICE_RECEIVED, language, Arrays.asList(platformName,productName,initiatingPartyName));
            }else {
                showURL = false;
            }

            if (initiatorIsBuyer) {
                buyerParty = initiatingParty;
                sellerParty = respondingParty;
                for (PersonType p : personTypeList) {
                    if (p.getRole().contains(NimbleRole.SALES_OFFICER.getName()) || p.getRole().contains(NimbleRole.MONITOR.getName())) {
                        emailList.add(p.getContact().getElectronicMail());
                    }
                }
            }else {
                buyerParty = respondingParty;
                sellerParty = initiatingParty;
                for (PersonType p : personTypeList) {
                    if (p.getRole().contains(NimbleRole.PURCHASER.getName()) || p.getRole().contains(NimbleRole.MONITOR.getName())) {
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

                notifyPartyOnBusinessProcess(emailList.toArray(new String[0]), initiatingPersonName, productName, initiatingPartyName, url, subject, respondingPartyName,language,processDocumentStatus);
            }
        }).start();
    }

    public void sendTrustScoreUpdateEmail(String partyID, String federationID, String bearerToken, String language) {
        new Thread(() -> {
            PartyType partyType = null;
            try {
                partyType = PartyPersistenceUtility.getParty(partyID,federationID,bearerToken);
            } catch (IOException e) {
                logger.error("Failed to get party with id: {} from identity service", partyID, e);
                return;
            }

            List<PersonType> personTypeList = partyType.getPerson();
            List<String> emailList = new ArrayList<>();
            for (PersonType p : personTypeList) {
                if (p.getRole().contains(NimbleRole.SALES_OFFICER.getName()) || p.getRole().contains(NimbleRole.MONITOR.getName())) {
                    emailList.add(p.getContact().getElectronicMail());
                }
            }

            if (emailList.size() != 0) {
                String subject = getMailSubject(NimbleExceptionMessageCode.MAIL_SUBJECT_TRUST_SCORE_UPDATED,language, new ArrayList<>());
                Context context = new Context();
                context.setVariable("partyName", partyType.getPartyName().get(0).getName().getValue());
                context.setVariable("url", URL_TEXT + frontEndURL + "/#/user-mgmt/company-rating");
                context.setVariable("platformName",platformName);
                emailService.send(emailList.toArray(new String[0]), subject, getTemplateName("trust_update",language), context);
            }
        }).start();
    }

    private void notifyPartyOnCollaborationStatus(String[] toEmail,String[] toEmailTradingPartner, String tradingPartnerPersonName, String productName, String tradingPartnerName, GroupStatus status,String url, String language){
        Context context = new Context();
        String subject;
        String template;

        context.setVariable("tradingPartnerPerson", tradingPartnerPersonName);
        context.setVariable("tradingPartner", tradingPartnerName);
        context.setVariable("product", productName);
        context.setVariable("platformName",platformName);

        if (!url.isEmpty()) {
            context.setVariable("url", url);
        }

        if(status.equals(GroupStatus.CANCELLED)){
            subject = getMailSubject(NimbleExceptionMessageCode.MAIL_SUBJECT_BUSINESS_PROCESS_CANCELLED,language,Arrays.asList(platformName));
            template = getTemplateName("cancelled_collaboration",language);
        }
        else{
            subject = getMailSubject(NimbleExceptionMessageCode.MAIL_SUBJECT_BUSINESS_PROCESS_FINISHED,language,Arrays.asList(platformName));
            template = getTemplateName("finished_collaboration",language);
        }

        if(toEmail.length > 0){
            try{
                emailService.send(toEmail, subject, template, context);
            } catch (Exception e){
                logger.error("Failed to send email for {} to notify collaboration status",toEmail,e);
            }
        }
        if(toEmailTradingPartner.length > 0){
            try{
                emailService.send(toEmailTradingPartner, subject, template, context);
            } catch (Exception e){
                logger.error("Failed to send email for {} to notify collaboration status",toEmailTradingPartner,e);
            }
        }
    }

    public void notifyPartyOnBusinessProcess(String[] toEmail, String initiatingPersonName, String productName, String initiatingPartyName,
                                           String url, String subject, String respondingPartyName, String language, ProcessDocumentStatus processDocumentStatus) {
        Context context = new Context();

        context.setVariable("initiatingPersonName", initiatingPersonName);
        context.setVariable("initiatingPartyName", initiatingPartyName);
        context.setVariable("respondingPartyName", respondingPartyName);
        context.setVariable("product", productName);
        context.setVariable("platformName",platformName);

        if (!url.isEmpty()) {
            context.setVariable("url", url);
        }

        String templateName = "continue_colloboration";
        String mailSubject = getMailSubject(NimbleExceptionMessageCode.MAIL_SUBJECT_BUSINESS_PROCESS_TRANSITION,language, Collections.singletonList(platformName));

        if (processDocumentStatus.equals(ProcessDocumentStatus.WAITINGRESPONSE)) {
            templateName = "action_pending";
            mailSubject = getMailSubject(NimbleExceptionMessageCode.MAIL_SUBJECT_ACTION_REQUIRED,language, Collections.singletonList(platformName));
        }

        if (subject.equals(EMPTY_TEXT)) {
            subject = mailSubject;
        }

        emailService.send(toEmail, subject, getTemplateName(templateName,language), context);
    }

    public void notifyPartyOnNewDeliveryDate(String toEmail,String productName, String respondingPartyName, String expectedDeliveryDate, String url) {
        Context context = new Context();

        context.setVariable("respondingPartyName", respondingPartyName);
        context.setVariable("expectedDeliveryDate", expectedDeliveryDate);
        context.setVariable("product", productName);
        context.setVariable("url", url);
        context.setVariable("platformName",platformName);

        List<String> languages = Arrays.asList(mailTemplateLanguages.split(","));
        String subject = getMailSubject(NimbleExceptionMessageCode.MAIL_SUBJECT_DELIVERY_DATE, languages.get(0),Arrays.asList(platformName));
        emailService.send(new String[]{toEmail},subject , getTemplateName("new_delivery_date",languages.get(0)), context);
    }

    /* Helper Methods */

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

    private String getMailSubject(NimbleExceptionMessageCode messageCode, String language, List<String> parameters){
        List<String> languages = Arrays.asList(mailTemplateLanguages.split(","));
        String mailSubjectLanguage = languages.contains(language) ? language :languages.get(0) ;
        Locale locale = new Locale(mailSubjectLanguage);
        return this.messageSource.getMessage(messageCode.toString(), parameters.toArray(), locale);
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
}
