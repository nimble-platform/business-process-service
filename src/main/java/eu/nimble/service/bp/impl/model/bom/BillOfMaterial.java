package eu.nimble.service.bp.impl.model.bom;

import eu.nimble.service.model.ubl.commonaggregatecomponents.LineItemType;

import java.util.List;

/**
 * Created by suat on 18-Apr-19.
 */
public class BillOfMaterial {
    private List<LineItemType> lineItems;

    public BillOfMaterial() {
    }

    public List<LineItemType> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<LineItemType> lineItems) {
        this.lineItems = lineItems;
    }

}
