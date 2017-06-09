package eu.nimble.service.bp.application;

import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.AmountType;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by yildiray on 6/8/2017.
 */
public class UBLUtility {
    private static Logger logger = LoggerFactory.getLogger(UBLUtility.class);

    private static List<OrderLineType> createOrderLines(String sellerID, String content) {
        List<OrderLineType> orderLines = new ArrayList<>();
        // TODO: receive the order lines from the content

        OrderLineType orderLine = new OrderLineType();
        LineItemType lineItem = new LineItemType();
        orderLine.setLineItem(lineItem);
        lineItem.setID("1");
        QuantityType quantity = new QuantityType();
        quantity.setUnitCode("KGM");
        quantity.setValue(new BigDecimal(5));
        lineItem.setQuantity(quantity);
        AmountType lineExtensionAmount = zeroAmount();
        lineExtensionAmount.setValue(new BigDecimal(100));
        lineItem.setLineExtensionAmount(lineExtensionAmount);
        AmountType totalTaxAmount = zeroAmount();
        totalTaxAmount.setValue(new BigDecimal(18));
        lineItem.setTotalTaxAmount(totalTaxAmount);
        PriceType price = new PriceType();
        AmountType priceAmount = zeroAmount();
        priceAmount.setValue(new BigDecimal(20));
        price.setPriceAmount(priceAmount);
        lineItem.setPrice(price);
        ItemType item = new ItemType();
        item.setName("Apple");
        lineItem.setItem(item);
        TaxTotalType taxTotal = new TaxTotalType();
        taxTotal.setTaxAmount(totalTaxAmount);
        TaxSubtotalType taxSubTotal = new TaxSubtotalType();
        taxSubTotal.setTaxAmount(totalTaxAmount);
        taxSubTotal.setPercent(new BigDecimal(18));
        TaxCategoryType taxCategory = new TaxCategoryType();
        TaxSchemeType taxScheme = new TaxSchemeType();
        CodeType taxTypeCode = new CodeType();
        taxTypeCode.setValue("VAT");
        taxScheme.setTaxTypeCode(taxTypeCode);
        taxCategory.setTaxScheme(taxScheme);
        taxSubTotal.setTaxCategory(taxCategory);
        taxTotal.getTaxSubtotal().add(taxSubTotal);

        lineItem.setTaxTotal(taxTotal);

        orderLines.add(orderLine);
        ///////
        return orderLines;
    }

    private static PartyType getPartyType(String partyID) {
        // TODO: receive this from IdentityService
        PartyType party = new PartyType();
        party.setID(partyID);

        return party;
    }

    private static String getRejectionNote(String content) {
        // TODO: parse the content and extract the rejection note
        return "It is rejected";
    }

    private static boolean getAcceptedIndicator(String content) {
        // TODO: parse the content and extract the accepted indicator
        return true;
    }

    public static OrderResponseSimpleType createOrderResponse(String initiatorID, String responderID, String content, String orderID) {
        OrderResponseSimpleType orderResponse = createEmptyOrderResponse();
        // set parties
        PartyType seller = getPartyType(initiatorID);
        PartyType buyer = getPartyType(responderID);
        CustomerPartyType customer = new CustomerPartyType();
        SupplierPartyType supplier = new SupplierPartyType();
        customer.setParty(buyer);
        supplier.setParty(seller);
        orderResponse.setBuyerCustomerParty(customer);
        orderResponse.setSellerSupplierParty(supplier);
        // set order reference
        OrderReferenceType orderReference = new OrderReferenceType();
        orderReference.setID(orderID);
        orderResponse.setOrderReference(orderReference);
        // get accepted indicator
        boolean acceptedIndicator = getAcceptedIndicator(content);
        orderResponse.setAcceptedIndicator(acceptedIndicator);
        if(acceptedIndicator == false) {
            String rejectionNote = getRejectionNote(content);
            orderResponse.setRejectionNote(rejectionNote);
        }

        return orderResponse;
    }

    public static OrderType createOrderDocument(String initiatorID, String responderID, String content) {
        OrderType order = createEmptyOrder();

        // set parties
        PartyType buyer = getPartyType(initiatorID);
        PartyType seller = getPartyType(responderID);
        CustomerPartyType customer = new CustomerPartyType();
        SupplierPartyType supplier = new SupplierPartyType();
        customer.setParty(buyer);
        supplier.setParty(seller);
        order.setBuyerCustomerParty(customer);
        order.setSellerSupplierParty(supplier);

        // set the items
        List<OrderLineType> orderLines = createOrderLines(responderID, content);
        order.setOrderLine(orderLines);

        // calculate the taxes
        TaxTotalType taxTotal = createTaxTotal(orderLines);
        order.setTaxTotal(taxTotal);

        // calculate the monetary total
        MonetaryTotalType monetaryTotal = createMonetaryTotal(orderLines);
        order.setAnticipatedMonetaryTotal(monetaryTotal);

        return  order;
    }

    private static TaxTotalType createTaxTotal(List<OrderLineType> orderLines) {
        // first collect the TaxSubTotals in the line items
        List<TaxSubtotalType> taxSubtotals = new ArrayList<>();
        for(OrderLineType orderLine : orderLines) {
            List<TaxSubtotalType> lineTaxSubTotals = orderLine.getLineItem().getTaxTotal().getTaxSubtotal();
            for(TaxSubtotalType subtotal: lineTaxSubTotals)
                taxSubtotals.add(subtotal);
        }

        // examine all the tax totals based on taxtypecode and percent. The same ones should be collected into the same taxsubtotal
        Map<String, List<TaxSubtotalType>> subtotalMap = new HashMap<>();
        for(TaxSubtotalType subtotal: taxSubtotals) {
            BigDecimal percent = subtotal.getPercent();
            String taxTypeCode = subtotal.getTaxCategory().getTaxScheme() != null ? subtotal.getTaxCategory().getTaxScheme().getTaxTypeCode().getValue() : "S";
            logger.debug(" $$$ tax type code {} and percent {}: ", taxTypeCode, percent);

            List<TaxSubtotalType> subtotalList = subtotalMap.get(taxTypeCode + "-" + percent.intValue());
            if(subtotalList == null) {
                subtotalList = new ArrayList<>();
            }
            subtotalList.add(subtotal);
            subtotalMap.put(taxTypeCode + "-" + percent.intValue(), subtotalList);
        }
        // sum the taxsubtotals and obtain the document level tax subtotals
        List<TaxSubtotalType> documentLevelTaxSubtotals = new ArrayList<>();
        for(Map.Entry<String, List<TaxSubtotalType>> entry: subtotalMap.entrySet()) {
            String taxTypeCode = entry.getKey().split("-")[0];
            BigDecimal percent = new BigDecimal(entry.getKey().split("-")[1]);
            // create new document level tax subtotal
            TaxSubtotalType documentLevelTaxSubtotal = new TaxSubtotalType();
            documentLevelTaxSubtotal.setPercent(percent);
            TaxCategoryType taxCategory = new TaxCategoryType();
            TaxSchemeType taxScheme = new TaxSchemeType();
            CodeType code = new CodeType();
            code.setValue(taxTypeCode);
            taxScheme.setTaxTypeCode(code);
            taxCategory.setTaxScheme(taxScheme);
            documentLevelTaxSubtotal.setTaxCategory(taxCategory);

            // calculate the amount for this new tax subtotal
            AmountType taxAmount = zeroAmount();
            List<TaxSubtotalType> lineTaxSubtotals = entry.getValue();
            for(TaxSubtotalType lineTaxSubtotal: lineTaxSubtotals) {
                BigDecimal lineTaxSubtotalAmount = lineTaxSubtotal.getTaxAmount().getValue();
                BigDecimal newTaxAmountValue = new BigDecimal(taxAmount.getValue().intValue() + lineTaxSubtotalAmount.intValue());
                taxAmount.setValue(newTaxAmountValue);
            }
            documentLevelTaxSubtotal.setTaxAmount(taxAmount);

            documentLevelTaxSubtotals.add(documentLevelTaxSubtotal);
        }

        // now calculate the document level total tax amount by summing document level tax subtotals
        AmountType taxTotalAmount = zeroAmount();
        for(TaxSubtotalType documentLevelTaxSubtotal: documentLevelTaxSubtotals) {
            BigDecimal taxSubtotalAmount = documentLevelTaxSubtotal.getTaxAmount().getValue();
            BigDecimal newTaxAmountValue = new BigDecimal(taxTotalAmount.getValue().intValue() + taxSubtotalAmount.intValue());
            taxTotalAmount.setValue(newTaxAmountValue);
        }

        // finally create the TaxTotal
        TaxTotalType taxTotal = new TaxTotalType();
        taxTotal.setTaxAmount(taxTotalAmount);
        taxTotal.setTaxSubtotal(documentLevelTaxSubtotals);
        return taxTotal;
    }

    private static MonetaryTotalType createMonetaryTotal(List<OrderLineType> orderLines) {
        // get empty zero monetary total
        MonetaryTotalType total = createEmptyMonetaryTotal();

        for(OrderLineType orderLine : orderLines) {
            // sum line extension amounts
            AmountType lineAmount = orderLine.getLineItem().getLineExtensionAmount();

            BigDecimal totalLineAmount = total.getLineExtensionAmount().getValue();
            totalLineAmount = new BigDecimal(totalLineAmount.intValue() + lineAmount.getValue().intValue());
            total.getLineExtensionAmount().setValue(totalLineAmount);
            // sum allowance total amount
            List<AllowanceChargeType> allowanceCharges = orderLine.getLineItem().getAllowanceCharge();
            BigDecimal allowances = total.getAllowanceTotalAmount().getValue();
            BigDecimal charges = total.getChargeTotalAmount().getValue();

            for(AllowanceChargeType allowanceCharge: allowanceCharges) {
                BigDecimal allowanceChargeAmount = allowanceCharge.getAmount().getValue();
                if(allowanceCharge.isChargeIndicator())
                    total.getChargeTotalAmount().setValue(new BigDecimal(charges.intValue() + allowanceChargeAmount.intValue()));
                else
                    total.getAllowanceTotalAmount().setValue(new BigDecimal(allowances.intValue() + allowanceChargeAmount.intValue()));
            }
            // sum tax total amount in the tax inclusive amount
            BigDecimal taxTotalAmount = total.getTaxInclusiveAmount().getValue();
            BigDecimal taxTotalLineAmount = orderLine.getLineItem().getTotalTaxAmount().getValue();
            total.getTaxInclusiveAmount().setValue(new BigDecimal(taxTotalAmount.intValue() + taxTotalLineAmount.intValue()));
        }

        // adjust the final values
        BigDecimal lineExtensionAmount = total.getLineExtensionAmount().getValue();
        BigDecimal allowanceAmount = total.getAllowanceTotalAmount().getValue();
        BigDecimal chargeAmount = total.getChargeTotalAmount().getValue();
        BigDecimal taxTotalAmount = total.getTaxInclusiveAmount().getValue();
        BigDecimal prepaidAmount = total.getPrepaidAmount().getValue();

        BigDecimal taxExclusiveAmount = new BigDecimal(lineExtensionAmount.intValue() + chargeAmount.intValue() - allowanceAmount.intValue());
        BigDecimal taxInclusiveAmount = new BigDecimal(taxExclusiveAmount.intValue() + taxTotalAmount.intValue());
        BigDecimal payableAmount = new BigDecimal(taxInclusiveAmount.intValue() - prepaidAmount.intValue());

        total.getTaxInclusiveAmount().setValue(taxInclusiveAmount);
        total.getTaxExclusiveAmount().setValue(taxExclusiveAmount);
        total.getPayableAmount().setValue(payableAmount);

        return total;
    }

    private static OrderType createEmptyOrder() {
        OrderType order = new OrderType();
        order.setID(UUID.randomUUID().toString());
        Date now = new Date();
        order.setIssueDateItem(now);
        order.setIssueTimeItem(now);

        return order;
    }

    private static OrderResponseSimpleType createEmptyOrderResponse() {
        OrderResponseSimpleType orderResponse = new OrderResponseSimpleType();
        orderResponse.setID(UUID.randomUUID().toString());
        Date now = new Date();
        orderResponse.setIssueDateItem(now);
        orderResponse.setIssueTimeItem(now);
        return orderResponse;
    }

    public static AmountType zeroAmount() {
        AmountType amount = new AmountType();
        amount.setCurrencyID("EUR");
        amount.setValue(new BigDecimal(0));
        return amount;
    }

    private static MonetaryTotalType createEmptyMonetaryTotal() {
        MonetaryTotalType total = new MonetaryTotalType();
        // initialize the inner amounts
        total.setAllowanceTotalAmount(zeroAmount());
        total.setChargeTotalAmount(zeroAmount());
        total.setLineExtensionAmount(zeroAmount());
        total.setPayableAlternativeAmount(zeroAmount());
        total.setPayableRoundingAmount(zeroAmount());
        total.setPayableAmount(zeroAmount());
        total.setPrepaidAmount(zeroAmount());
        total.setTaxExclusiveAmount(zeroAmount());
        total.setTaxInclusiveAmount(zeroAmount());

        return total;
    }
}
