package eu.nimble.service.bp.util.persistence.catalogue;

import eu.nimble.service.model.ubl.commonaggregatecomponents.DocumentReferenceType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.OrderLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.OrderReferenceType;
import eu.nimble.service.model.ubl.invoice.InvoiceType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class PaymentPersistenceUtility {

    private static String QUERY_CHECK_EXISTENCE_OF_INVOICE = "SELECT count(*) FROM InvoiceType invoice WHERE invoice.orderReference.documentReference.ID = :orderId";
    private static String QUERY_GET_INVOICE = "SELECT invoice FROM InvoiceType invoice WHERE invoice.orderReference.documentReference.ID = :orderId";

    public static boolean isPaymentDoneForOrder(String orderId){
        Long size = new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_CHECK_EXISTENCE_OF_INVOICE,new String[]{"orderId"},new Object[]{orderId});
        return size > 0;
    }

    public static InvoiceType getInvoiceForOrder(String orderId){
        return new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntity(QUERY_GET_INVOICE,new String[]{"orderId"},new Object[]{orderId});
    }

    public static InvoiceType createInvoiceForOrder(String orderId, String invoiceId, String invoiceUrl){
        DocumentReferenceType orderDocumentReference = new DocumentReferenceType();
        orderDocumentReference.setID(orderId);

        OrderReferenceType orderReference = new OrderReferenceType();
        orderReference.setDocumentReference(orderDocumentReference);

        InvoiceType invoice = new InvoiceType();
        invoice.setOrderReference(orderReference);
        if(invoiceId != null){
            invoice.setID(invoiceId);
        }
        if(invoiceUrl != null){
            DocumentReferenceType invoiceUrlDocumentReference = new DocumentReferenceType();
            invoiceUrlDocumentReference.setID(invoiceUrl);

            invoice.setOriginatorDocumentReference(Arrays.asList(invoiceUrlDocumentReference));
        }

        return invoice;
    }

    public static String createJSONBodyForPayment(OrderType order){
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date dateTime = new Date();

        String timestamp = dateFormat.format(dateTime);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderLineType orderLine : order.getOrderLine()) {
            BigDecimal quantity = orderLine.getLineItem().getQuantity().getValue();
            BigDecimal unitPrice = orderLine.getLineItem().getPrice().getPriceAmount().getValue();
            totalAmount = totalAmount.add(quantity.multiply(unitPrice));
        }

        JSONObject json = new JSONObject();
        json.put("transactionId",order.getID());
        json.put("buyerId",order.getBuyerCustomerParty().getParty().getPartyIdentification().get(0).getID());
        json.put("sellerId",order.getSellerSupplierParty().getParty().getPartyIdentification().get(0).getID());
        json.put("action","PAYMENT");
        json.put("platform","NIMBLE");
        json.put("originPlatform","EFACTORY");
        json.put("totalAmount",totalAmount);
        json.put("status","completed");
        json.put("@timestamp",timestamp);
        return json.toString();
    }
}
