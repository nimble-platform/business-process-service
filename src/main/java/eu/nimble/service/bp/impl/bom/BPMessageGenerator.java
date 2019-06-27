package eu.nimble.service.bp.impl.bom;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.common.rest.identity.model.NegotiationSettings;
import eu.nimble.service.bp.impl.contract.ContractGenerator;
import eu.nimble.service.bp.impl.util.persistence.catalogue.ContractPersistenceUtility;
import eu.nimble.service.bp.impl.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.impl.util.persistence.catalogue.PartyPersistenceUtility;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import eu.nimble.service.bp.swagger.model.ProcessVariables;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import eu.nimble.service.model.ubl.digitalagreement.DigitalAgreementType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType;
import eu.nimble.utility.JsonSerializationUtility;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class BPMessageGenerator {

    private final static Logger logger = LoggerFactory.getLogger(BPMessageGenerator.class);

    private final static String orderProcessId = "Order";
    private final static String negotiationProcessId = "Negotiation";

    public static ProcessInstanceInputMessage createBPMessageForLineItem(LineItemType lineItem, Boolean useFrameContract, String partyId, NegotiationSettings negotiationSettings, String creatorUserId, String bearerToken) throws Exception {
        // if there is a valid frame contract and useFrameContract is True, then create an order for the line item using the details of frame contract
        // otherwise, start a negotiation process for the item
        if (useFrameContract) {
            DigitalAgreementType digitalAgreement = ContractPersistenceUtility.getFrameContractAgreementById(lineItem.getItem().getManufacturerParty().getPartyIdentification().get(0).getID(), partyId, lineItem.getItem().getManufacturersItemIdentification().getID());

            if (digitalAgreement != null) {
                // check whether frame contract is valid or not
                Date date = new Date();
                if (date.compareTo(digitalAgreement.getDigitalAgreementTerms().getValidityPeriod().getEndDateItem()) <= 0) {
                    // retrieve the quotation
                    QuotationType quotation = (QuotationType) DocumentPersistenceUtility.getUBLDocument(digitalAgreement.getQuotationReference().getID());
                    // create an order using the frame contract
                    OrderType order = createOrder(quotation, negotiationSettings, bearerToken);

                    return BPMessageGenerator.createProcessInstanceInputMessage(order, order.getID(), orderProcessId, order.getOrderLine().get(0).getLineItem(), negotiationSettings, creatorUserId);
                }
            }
        }

        RequestForQuotationType requestForQuotation = createRequestForQuotation(lineItem, negotiationSettings, bearerToken);

        return BPMessageGenerator.createProcessInstanceInputMessage(requestForQuotation, requestForQuotation.getID(), negotiationProcessId, lineItem, negotiationSettings, creatorUserId);
    }

    private static ProcessInstanceInputMessage createProcessInstanceInputMessage(Object document, String documentId, String processId, LineItemType lineItem, NegotiationSettings negotiationSettings, String creatorUserId) throws Exception {
        // get related product categories
        List<String> relatedProductCategories = new ArrayList<>();
        for (CommodityClassificationType commodityClassificationType : lineItem.getItem().getCommodityClassification()) {
            if (!relatedProductCategories.contains(commodityClassificationType.getItemClassificationCode().getName())) {
                relatedProductCategories.add(commodityClassificationType.getItemClassificationCode().getName());
            }
        }
        // serialize the document
        String rfqAsString = JsonSerializationUtility.getObjectMapper().writeValueAsString(document);

        // remove hjids
        JSONObject object = new JSONObject(rfqAsString);
        JsonSerializationUtility.removeHjidFields(object);
        rfqAsString = object.toString();

        // create process variables
        ProcessVariables processVariables = new ProcessVariables();

        processVariables.setContentUUID(documentId);
        processVariables.setContent(rfqAsString);
        processVariables.setProcessID(processId);
        processVariables.setCreatorUserID(creatorUserId);
        processVariables.setInitiatorID(negotiationSettings.getCompany().getPartyIdentification().get(0).getID());
        processVariables.setResponderID(lineItem.getItem().getManufacturerParty().getPartyIdentification().get(0).getID());
        processVariables.setRelatedProducts(Collections.singletonList(lineItem.getItem().getName().get(0).getValue()));
        processVariables.setRelatedProductCategories(relatedProductCategories);

        // create Process instance input message
        ProcessInstanceInputMessage processInstanceInputMessage = new ProcessInstanceInputMessage();
        processInstanceInputMessage.setProcessInstanceID("");
        processInstanceInputMessage.setVariables(processVariables);

        return processInstanceInputMessage;
    }

    private static OrderType createOrder(QuotationType quotation, NegotiationSettings negotiationSettings, String bearerToken) throws Exception {
        // get seller party
        PartyType sellerParty = SpringBridge.getInstance().getiIdentityClientTyped().getParty(bearerToken, quotation.getQuotationLine().get(0).getLineItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID());

        // retrieve request for quotation
        RequestForQuotationType requestForQuotation = (RequestForQuotationType) DocumentPersistenceUtility.getUBLDocument(quotation.getRequestForQuotationDocumentReference().getID());

        OrderType order = new OrderType();

        CustomerPartyType customerParty = new CustomerPartyType();
        customerParty.setParty(PartyPersistenceUtility.getParty(negotiationSettings.getCompany()));

        SupplierPartyType supplierParty = new SupplierPartyType();
        supplierParty.setParty(PartyPersistenceUtility.getParty(sellerParty));

        OrderLineType orderLine = new OrderLineType();
        orderLine.setLineItem(quotation.getQuotationLine().get(0).getLineItem());

        ContractType contract = new ContractType();
        contract.setID(UUID.randomUUID().toString());
        contract.setClause(quotation.getTermOrCondition());

        order.setID(UUID.randomUUID().toString());
        order.setBuyerCustomerParty(customerParty);
        order.setSellerSupplierParty(supplierParty);
        order.setOrderLine(Collections.singletonList(orderLine));
        order.getOrderLine().get(0).getLineItem().getDeliveryTerms().getDeliveryLocation().setAddress(requestForQuotation.getRequestForQuotationLine().get(0).getLineItem().getDeliveryTerms().getDeliveryLocation().getAddress());
        order.setPaymentMeans(quotation.getPaymentMeans());
        order.setPaymentTerms(quotation.getPaymentTerms());
        order.setContract(Collections.singletonList(contract));

        return order;
    }

    private static RequestForQuotationType createRequestForQuotation(LineItemType lineItem, NegotiationSettings negotiationSettings, String bearerToken) throws Exception {
        // get seller party
        PartyType sellerParty = SpringBridge.getInstance().getiIdentityClientTyped().getParty(bearerToken, lineItem.getItem().getManufacturerParty().getPartyIdentification().get(0).getID());

        // create request for quotation
        RequestForQuotationType requestForQuotation = new RequestForQuotationType();

        RequestForQuotationLineType requestForQuotationLine = new RequestForQuotationLineType();
        requestForQuotationLine.setLineItem(lineItem);

        CustomerPartyType customerParty = new CustomerPartyType();
        customerParty.setParty(PartyPersistenceUtility.getParty(negotiationSettings.getCompany()));

        SupplierPartyType supplierParty = new SupplierPartyType();
        supplierParty.setParty(PartyPersistenceUtility.getParty(sellerParty));

        PaymentTermsType paymentTermsType = new PaymentTermsType();
        paymentTermsType.setTradingTerms(getPaymentTerms(negotiationSettings.getPaymentTerms().size() > 0 ? negotiationSettings.getPaymentTerms().get(0) : ""));

        CodeType paymentMeansCode = new CodeType();
        paymentMeansCode.setValue(negotiationSettings.getPaymentMeans().size() > 0 ? negotiationSettings.getPaymentMeans().get(0) : "");

        PaymentMeansType paymentMeansType = new PaymentMeansType();
        paymentMeansType.setPaymentMeansCode(paymentMeansCode);

        PeriodType periodType = new PeriodType();

        DeliveryType deliveryType = new DeliveryType();
        deliveryType.setRequestedDeliveryPeriod(periodType);

        String uuid = UUID.randomUUID().toString();
        requestForQuotation.setID(uuid);
        requestForQuotation.setNote(Collections.singletonList(""));
        requestForQuotation.setDataMonitoringRequested(false);
        requestForQuotation.setBuyerCustomerParty(customerParty);
        requestForQuotation.setSellerSupplierParty(supplierParty);
        requestForQuotation.setDelivery(deliveryType);
        requestForQuotation.setRequestForQuotationLine(Collections.singletonList(requestForQuotationLine));
        requestForQuotation.setPaymentTerms(paymentTermsType);
        requestForQuotation.setPaymentMeans(paymentMeansType);
        requestForQuotation.setRequestForQuotationLine(Collections.singletonList(requestForQuotationLine));

        // if seller has some T&Cs, use them, otherwise use the default T&Cs
        if (sellerParty.getPurchaseTerms().getTermOrCondition().size() > 0) {
            requestForQuotation.setTermOrCondition(sellerParty.getPurchaseTerms().getTermOrCondition());
        } else {
            ContractGenerator contractGenerator = new ContractGenerator();
            List<ClauseType> clauses = contractGenerator.getTermsAndConditions(null, null, sellerParty.getPartyIdentification().get(0).getID(), negotiationSettings.getCompany().getPartyIdentification().get(0).getID(), lineItem.getDeliveryTerms().getIncoterms(), negotiationSettings.getPaymentTerms().size() > 0 ? negotiationSettings.getPaymentTerms().get(0) : "", bearerToken);
            requestForQuotation.setTermOrCondition(clauses);
        }

        return requestForQuotation;
    }

    private static List<TradingTermType> getPaymentTerms(String paymentTerm) {
        List<TradingTermType> tradingTerms = new ArrayList<>();
        InputStream inputStream = null;
        try {
            // read trading terms from the json file
            inputStream = BPMessageGenerator.class.getResourceAsStream("/tradingTerms/paymentTerms.json");

            String fileContent = IOUtils.toString(inputStream);

            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();

            // trading terms
            tradingTerms = objectMapper.readValue(fileContent, new TypeReference<List<TradingTermType>>() {
            });

            // set the value of selected trading term to true
            for (TradingTermType tradingTermType : tradingTerms) {
                String tradingTermName = tradingTermType.getTradingTermFormat() + " - " + tradingTermType.getDescription().get(0).getValue();
                if (tradingTermName.contentEquals(paymentTerm)) {
                    tradingTermType.getValue().getValue().get(0).setValue("true");
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to create payment terms", e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.error("Failed to close input stream", e);
                }
            }
        }
        return tradingTerms;
    }

}
