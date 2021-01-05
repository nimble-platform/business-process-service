package eu.nimble.service.bp.util.stripe;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import eu.nimble.service.model.ubl.commonaggregatecomponents.OrderLineType;
import eu.nimble.service.model.ubl.order.OrderType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
public class StripeClient {

    @Value("${nimble.stripe.secretKey}")
    private String stripeSecretKey;

    @Value("${nimble.stripe.applicationFeePercentage}")
    private BigDecimal stripeApplicationFeePercentage;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public String createPaymentIntent(String accountId, OrderType order) throws Exception {
        // calculate the total price
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (OrderLineType orderLine : order.getOrderLine()) {
            BigDecimal quantity = orderLine.getLineItem().getQuantity().getValue();
            BigDecimal unitPrice = orderLine.getLineItem().getPrice().getPriceAmount().getValue();
            BigDecimal productPrice = quantity.multiply(unitPrice);
            totalPrice = totalPrice.add(productPrice);
        }
        // total price should be multiplied by 100
        // it is not needed for the zero-decimal currencies, but we do not have such currencies now
        // for more information, refer to https://stripe.com/docs/currencies#zero-decimal
        totalPrice = totalPrice.multiply(BigDecimal.valueOf(100));
        // calculate the application fee
        BigDecimal applicationFee = totalPrice.multiply(stripeApplicationFeePercentage).divide(BigDecimal.valueOf(100));
        // create the payment intent
        ArrayList paymentMethodTypes = new ArrayList();
        paymentMethodTypes.add("card");

        Map<String, Object> params = new HashMap<>();
        params.put("payment_method_types", paymentMethodTypes);
        params.put("amount", totalPrice.intValue());
        params.put("currency", order.getOrderLine().get(0).getLineItem().getPrice().getPriceAmount().getCurrencyID().toLowerCase());
        params.put("application_fee_amount", applicationFee.intValue());
        Map<String, Object> transferDataParams = new HashMap<>();
        transferDataParams.put("destination", accountId);
        params.put("transfer_data", transferDataParams);
        PaymentIntent paymentIntent = PaymentIntent.create(params);
        return paymentIntent.getClientSecret();
    }
}