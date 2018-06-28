package eu.nimble.service.bp.impl.util.persistence;

import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.utility.Configuration;

import java.util.List;

public class CatalogueDAOUtility {

    public static CatalogueLineType getCatalogueLine(OrderType order){
        String query = "SELECT cl FROM CatalogueLineType cl WHERE cl.ID = '"+order.getOrderLine().get(0).getLineItem().getLineReference().get(0).getLineID()+"' AND cl.goodsItem.item.manufacturerParty.ID = '"+order.getOrderLine().get(0).getLineItem().getItem().getManufacturerParty().getID()+"'";
        List<CatalogueLineType> catalogueLineTypes = (List<CatalogueLineType>) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadAll(query);
        if(catalogueLineTypes.size() == 0){
            return null;
        }
        return catalogueLineTypes.get(0);
    }

    public static List<String> getOrderIds(CatalogueLineType catalogueLine){
        String query = "SELECT order_.ID from OrderType order_ join order_.orderLine line where line.lineItem.item.manufacturerParty.ID = '"+catalogueLine.getGoodsItem().getItem().getManufacturerParty().getID()+"' AND line.lineItem.item.manufacturersItemIdentification.ID = '"+catalogueLine.getGoodsItem().getItem().getManufacturersItemIdentification().getID()+"'";
        return (List<String>) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadAll(query);
    }
}
