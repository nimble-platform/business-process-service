package eu.nimble.service.bp.util;

import eu.nimble.service.model.ubl.commonaggregatecomponents.CommunicationType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.service.model.ubl.transportexecutionplan.TransportExecutionPlanType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Created by yildiray on 6/8/2017.
 */
public class UBLUtility {
    private static Logger logger = LoggerFactory.getLogger(UBLUtility.class);

    public static String getDocumentId(Object document) {
        try {
            Field f = document.getClass().getDeclaredField("id");
            f.setAccessible(true);
            String id = (String) f.get(document);
            f.setAccessible(false);
            return id;

        } catch (Exception e) {
            String msg = String.format("Failed to get id of the document: %s", document.getClass());
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public static boolean documentIndicatesPositiveResponse(Object document) {
        boolean result = true;
        if(document instanceof OrderResponseSimpleType) {
            OrderResponseSimpleType orderResponse = (OrderResponseSimpleType) document;
            result = orderResponse.isAcceptedIndicator();

        }else if(document instanceof QuotationType) {
            QuotationType quotation = (QuotationType) document;
            result = quotation.getDocumentStatusCode().getName().equals("Accepted");

        } else if(document instanceof TransportExecutionPlanType) {
            TransportExecutionPlanType transportExecutionPlanType = (TransportExecutionPlanType) document;
            result = transportExecutionPlanType.getDocumentStatusCode().getName().equals("Accepted");
        }
        return result;
    }

    public static String getPartyRestEndpoint(PartyType party){
        if(party != null && party.getContact() != null && party.getContact().getOtherCommunication() != null){
            for (CommunicationType communicationType : party.getContact().getOtherCommunication()) {
                CodeType code = communicationType.getChannelCode();
                if(code != null && code.getName() != null && code.getName().contentEquals("REST")){
                    return code.getValue();
                }
            }
        }
        return null;
    }

//    private static Logger logger = LoggerFactory.getLogger(UBLUtility.class);
//
//    private static PartyType getPartyType(String partyID) {
//        /*String query = "SELECT party FROM PartyType party WHERE party.ID='" + partyID + "' ";
//        List<PartyType> resultSet = (List<PartyType>) GenericJPARepositoryImpl.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
//                .loadAll(query);
//        PartyType party = null;
//        if(resultSet.size() == 0) { */
//        // TODO: receive this from IdentityService
//        PartyType party = new PartyType();
//        party.setID(partyID);
//        /*} else {
//            party = resultSet.get(0);
//        }*/
//
//        return party;
//    }
//
//    private static TaxTotalType createTaxTotal(List<OrderLineType> orderLines) {
//        // first collect the TaxSubTotals in the line items
//        List<TaxSubtotalType> taxSubtotals = new ArrayList<>();
//        for (OrderLineType orderLine : orderLines) {
//            List<TaxSubtotalType> lineTaxSubTotals = orderLine.getLineItem().getTaxTotal().getTaxSubtotal();
//            taxSubtotals.addAll(lineTaxSubTotals);
//        }
//
//        // examine all the tax totals based on taxtypecode and percent. The same ones should be collected into the same taxsubtotal
//        Map<String, List<TaxSubtotalType>> subtotalMap = new HashMap<>();
//        for (TaxSubtotalType subtotal : taxSubtotals) {
//            BigDecimal percent = subtotal.getPercent();
//            String taxTypeCode = subtotal.getTaxCategory().getTaxScheme() != null ? subtotal.getTaxCategory().getTaxScheme().getTaxTypeCode().getValue() : "S";
//            logger.debug(" $$$ tax type code {} and percent {}: ", taxTypeCode, percent);
//
//            List<TaxSubtotalType> subtotalList = subtotalMap.get(taxTypeCode + "-" + percent.intValue());
//            if (subtotalList == null) {
//                subtotalList = new ArrayList<>();
//            }
//            subtotalList.add(subtotal);
//            subtotalMap.put(taxTypeCode + "-" + percent.intValue(), subtotalList);
//        }
//        // sum the taxsubtotals and obtain the document level tax subtotals
//        List<TaxSubtotalType> documentLevelTaxSubtotals = new ArrayList<>();
//        for (Map.Entry<String, List<TaxSubtotalType>> entry : subtotalMap.entrySet()) {
//            String taxTypeCode = entry.getKey().split("-")[0];
//            BigDecimal percent = new BigDecimal(entry.getKey().split("-")[1]);
//            // create new document level tax subtotal
//            TaxSubtotalType documentLevelTaxSubtotal = new TaxSubtotalType();
//            documentLevelTaxSubtotal.setPercent(percent);
//            TaxCategoryType taxCategory = new TaxCategoryType();
//            TaxSchemeType taxScheme = new TaxSchemeType();
//            CodeType code = new CodeType();
//            code.setValue(taxTypeCode);
//            taxScheme.setTaxTypeCode(code);
//            taxCategory.setTaxScheme(taxScheme);
//            documentLevelTaxSubtotal.setTaxCategory(taxCategory);
//
//            // calculate the amount for this new tax subtotal
//            AmountType taxAmount = zeroAmount();
//            List<TaxSubtotalType> lineTaxSubtotals = entry.getValue();
//            for (TaxSubtotalType lineTaxSubtotal : lineTaxSubtotals) {
//                BigDecimal lineTaxSubtotalAmount = lineTaxSubtotal.getTaxAmount().getValue();
//                BigDecimal newTaxAmountValue = new BigDecimal(taxAmount.getValue().intValue() + lineTaxSubtotalAmount.intValue());
//                taxAmount.setValue(newTaxAmountValue);
//            }
//            documentLevelTaxSubtotal.setTaxAmount(taxAmount);
//
//            documentLevelTaxSubtotals.add(documentLevelTaxSubtotal);
//        }
//
//        // now calculate the document level total tax amount by summing document level tax subtotals
//        AmountType taxTotalAmount = zeroAmount();
//        for (TaxSubtotalType documentLevelTaxSubtotal : documentLevelTaxSubtotals) {
//            BigDecimal taxSubtotalAmount = documentLevelTaxSubtotal.getTaxAmount().getValue();
//            BigDecimal newTaxAmountValue = new BigDecimal(taxTotalAmount.getValue().intValue() + taxSubtotalAmount.intValue());
//            taxTotalAmount.setValue(newTaxAmountValue);
//        }
//
//        // finally create the TaxTotal
//        TaxTotalType taxTotal = new TaxTotalType();
//        taxTotal.setTaxAmount(taxTotalAmount);
//        taxTotal.setTaxSubtotal(documentLevelTaxSubtotals);
//        return taxTotal;
//    }
//
//    private static MonetaryTotalType createMonetaryTotal(List<OrderLineType> orderLines) {
//        // get empty zero monetary total
//        MonetaryTotalType total = createEmptyMonetaryTotal();
//
//        for (OrderLineType orderLine : orderLines) {
//            // sum line extension amounts
//            AmountType lineAmount = orderLine.getLineItem().getLineExtensionAmount();
//
//            BigDecimal totalLineAmount = total.getLineExtensionAmount().getValue();
//            totalLineAmount = new BigDecimal(totalLineAmount.intValue() + lineAmount.getValue().intValue());
//            total.getLineExtensionAmount().setValue(totalLineAmount);
//            // sum allowance total amount
//            List<AllowanceChargeType> allowanceCharges = orderLine.getLineItem().getAllowanceCharge();
//            BigDecimal allowances = total.getAllowanceTotalAmount().getValue();
//            BigDecimal charges = total.getChargeTotalAmount().getValue();
//
//            for (AllowanceChargeType allowanceCharge : allowanceCharges) {
//                BigDecimal allowanceChargeAmount = allowanceCharge.getAmount().getValue();
//                if (allowanceCharge.isChargeIndicator())
//                    total.getChargeTotalAmount().setValue(new BigDecimal(charges.intValue() + allowanceChargeAmount.intValue()));
//                else
//                    total.getAllowanceTotalAmount().setValue(new BigDecimal(allowances.intValue() + allowanceChargeAmount.intValue()));
//            }
//            // sum tax total amount in the tax inclusive amount
//            BigDecimal taxTotalAmount = total.getTaxInclusiveAmount().getValue();
//            BigDecimal taxTotalLineAmount = orderLine.getLineItem().getTotalTaxAmount().getValue();
//            total.getTaxInclusiveAmount().setValue(new BigDecimal(taxTotalAmount.intValue() + taxTotalLineAmount.intValue()));
//        }
//
//        // adjust the final values
//        BigDecimal lineExtensionAmount = total.getLineExtensionAmount().getValue();
//        BigDecimal allowanceAmount = total.getAllowanceTotalAmount().getValue();
//        BigDecimal chargeAmount = total.getChargeTotalAmount().getValue();
//        BigDecimal taxTotalAmount = total.getTaxInclusiveAmount().getValue();
//        BigDecimal prepaidAmount = total.getPrepaidAmount().getValue();
//
//        BigDecimal taxExclusiveAmount = new BigDecimal(lineExtensionAmount.intValue() + chargeAmount.intValue() - allowanceAmount.intValue());
//        BigDecimal taxInclusiveAmount = new BigDecimal(taxExclusiveAmount.intValue() + taxTotalAmount.intValue());
//        BigDecimal payableAmount = new BigDecimal(taxInclusiveAmount.intValue() - prepaidAmount.intValue());
//
//        total.getTaxInclusiveAmount().setValue(taxInclusiveAmount);
//        total.getTaxExclusiveAmount().setValue(taxExclusiveAmount);
//        total.getPayableAmount().setValue(payableAmount);
//
//        return total;
//    }
//
//    private static OrderType createEmptyOrder() {
//        OrderType order = new OrderType();
//        order.setID(UUID.randomUUID().toString());
//        Date now = new Date();
//        order.setIssueDateItem(now);
//
//        return order;
//    }
//
//    private static OrderResponseSimpleType createEmptyOrderResponse() {
//        OrderResponseSimpleType orderResponse = new OrderResponseSimpleType();
//        orderResponse.setID(UUID.randomUUID().toString());
//        Date now = new Date();
//        orderResponse.setIssueDateItem(now);
//        return orderResponse;
//    }
//
//    private static AmountType zeroAmount() {
//        AmountType amount = new AmountType();
//        amount.setCurrencyID("EUR");
//        amount.setValue(new BigDecimal(0));
//        return amount;
//    }
//
//    private static MonetaryTotalType createEmptyMonetaryTotal() {
//        MonetaryTotalType total = new MonetaryTotalType();
//        // initialize the inner amounts
//        total.setAllowanceTotalAmount(zeroAmount());
//        total.setChargeTotalAmount(zeroAmount());
//        total.setLineExtensionAmount(zeroAmount());
//        total.setPayableAlternativeAmount(zeroAmount());
//        total.setPayableRoundingAmount(zeroAmount());
//        total.setPayableAmount(zeroAmount());
//        total.setPrepaidAmount(zeroAmount());
//        total.setTaxExclusiveAmount(zeroAmount());
//        total.setTaxInclusiveAmount(zeroAmount());
//
//        return total;
//    }
}
