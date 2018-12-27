package eu.nimble.service.bp.impl.persistence.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.QualifyingPartyType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.utility.JsonSerializationUtility;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class CatalogueDAOUtility {
    private static final Logger logger = LoggerFactory.getLogger(CatalogueDAOUtility.class);

    public static CatalogueLineType getCatalogueLine(OrderType order){
        List<CatalogueLineType> catalogueLineTypes = SpringBridge.getInstance().getCatalogueRepository().getCatalogueLine(order.getOrderLine().get(0).getLineItem().getLineReference().get(0).getLineID(), order.getOrderLine().get(0).getLineItem().getItem().getManufacturerParty().getID());
        if(catalogueLineTypes.size() == 0){
            return null;
        }
        return catalogueLineTypes.get(0);
    }

    public static List<String> getOrderIds(CatalogueLineType catalogueLine){
        return SpringBridge.getInstance().getCatalogueRepository().getOrderIds(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getID(), catalogueLine.getGoodsItem().getItem().getManufacturersItemIdentification().getID());
    }

    public static PartyType getParty(PartyType party){
        if(party == null){
            return null;
        }
        PartyType catalogueParty = SpringBridge.getInstance().getCatalogueRepository().getPartyByID(party.getID());
        if(catalogueParty != null) {
            return catalogueParty;
        } else {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper = objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            try {
                JSONObject object = new JSONObject(objectMapper.writeValueAsString(party));
                JsonSerializationUtility.removeHjidFields(object);
                party = objectMapper.readValue(object.toString(), PartyType.class);
            }
            catch (Exception e){
                String msg = String.format("Failed to remove hjid fields from the party: %s", party.getID());
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }

            SpringBridge.getInstance().getCatalogueRepository().persistEntity(party);
            return party;
        }
    }

    public static PartyType getParty(String partyId) {
        PartyType party = SpringBridge.getInstance().getCatalogueRepository().getPartyByID(partyId);
        return party;
    }

    public static QualifyingPartyType getQualifyingPartyType(String partyID,String bearerToken){
        QualifyingPartyType qualifyingParty = SpringBridge.getInstance().getCatalogueRepository().getQualifyingParty(partyID);
        if(qualifyingParty == null){
            qualifyingParty = new QualifyingPartyType();
            // get party using identity service
            PartyType partyType = null;
            try {
                partyType = SpringBridge.getInstance().getIdentityClientTyped().getParty(bearerToken,partyID);
            } catch (IOException e) {
                String msg = String.format("Failed to get qualifying party: %s", partyID);
                logger.error(msg);
                throw new RuntimeException(msg, e);
            }

            qualifyingParty.setParty(getParty(partyType));
        }
        return qualifyingParty;
    }
}
