package eu.nimble.service.bp.impl.util.persistence;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;
import eu.nimble.utility.JsonSerializationUtility;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CatalogueDAOUtility {
    private static final Logger logger = LoggerFactory.getLogger(CatalogueDAOUtility.class);

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

    public static PartyType getParty(PartyType party){
        if(party == null){
            return null;
        }
        String query = "SELECT party FROM PartyType party WHERE party.ID = ?";
        PartyType partyType = HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).load(query,party.getID());
        if(partyType == null){
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper = objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            try {
                JSONObject object = new JSONObject(objectMapper.writeValueAsString(party));
                JsonSerializationUtility.removeHjidFields(object);
                party = objectMapper.readValue(object.toString(), PartyType.class);
            }
            catch (Exception e){
                logger.error("Failed to remove hjid fields from the party",e);
            }

            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(party);
            return party;
        }
        return partyType;
    }
}
