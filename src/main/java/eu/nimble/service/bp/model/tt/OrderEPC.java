package eu.nimble.service.bp.model.tt;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by suat on 26-Jul-19.
 */
public class OrderEPC {
    private String orderId;
    private Set<String> codes = new HashSet<>();

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Set<String> getCodes() {
        return codes;
    }

    public void setCodes(Set<String> codes) {
        this.codes = codes;
    }
}
