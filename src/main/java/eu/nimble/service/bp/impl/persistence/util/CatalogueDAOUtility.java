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
//        String query = "SELECT cl FROM CatalogueLineType cl WHERE cl.ID = '"+order.getOrderLine().get(0).getLineItem().getLineReference().get(0).getLineID()+"' AND cl.goodsItem.item.manufacturerParty.ID = '"+order.getOrderLine().get(0).getLineItem().getItem().getManufacturerParty().getID()+"'";
        List<CatalogueLineType> catalogueLineTypes = SpringBridge.getInstance().getCatalogueRepository().getCatalogueLine(order.getOrderLine().get(0).getLineItem().getLineReference().get(0).getLineID(), order.getOrderLine().get(0).getLineItem().getItem().getManufacturerParty().getID());
//        List<CatalogueLineType> catalogueLineTypes = (List<CatalogueLineType>) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadAll(query);
        if(catalogueLineTypes.size() == 0){
            return null;
        }
        return catalogueLineTypes.get(0);
    }

    public static List<String> getOrderIds(CatalogueLineType catalogueLine){
//        String query = "SELECT order_.ID from OrderType order_ join order_.orderLine line where line.lineItem.item.manufacturerParty.ID = '"+catalogueLine.getGoodsItem().getItem().getManufacturerParty().getID()+"' AND line.lineItem.item.manufacturersItemIdentification.ID = '"+catalogueLine.getGoodsItem().getItem().getManufacturersItemIdentification().getID()+"'";
//        return (List<String>) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadAll(query);
        return SpringBridge.getInstance().getCatalogueRepository().getOrderIds(catalogueLine.getGoodsItem().getItem().getManufacturerParty().getID(), catalogueLine.getGoodsItem().getItem().getManufacturersItemIdentification().getID());
    }

    public static PartyType getParty(PartyType party){
        if(party == null){
            return null;
        }
//        String query = "SELECT party FROM PartyType party WHERE party.ID = ? ORDER BY party.hjid ASC";
//        List<PartyType> partyTypes = (List<PartyType>) GenericJPARepositoryImpl.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadAll(query,party.getID());
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

//            GenericJPARepositoryImpl.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(party);
            SpringBridge.getInstance().getCatalogueRepository().persistEntity(party);
            return party;
        }
    }

    public static PartyType getParty(String partyId) {
//        List<PartyType>  partyTypes = (List<PartyType>) GenericJPARepositoryImpl.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadAll(GET_PARTY_QUERY, partyId);
//        List<PartyType> partyTypes = SpringBridge.getInstance().getCatalogueRepository().getPartyByID(partyId);
//        if(partyTypes.size() == 0){
//            return null;
//        }
//        return partyTypes.get(0);
        PartyType party = SpringBridge.getInstance().getCatalogueRepository().getPartyByID(partyId);
        return party;
    }

    public static QualifyingPartyType getQualifyingPartyType(String partyID,String bearerToken){
//        String query = "SELECT qpt FROM QualifyingPartyType qpt WHERE qpt.party.ID = ?";
//        QualifyingPartyType qualifyingParty = HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).load(query,partyID);
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
//            qualifyingParty = (QualifyingPartyType) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(qualifyingParty);
            qualifyingParty = SpringBridge.getInstance().getCatalogueRepository().updateEntity(qualifyingParty);
        }
        return qualifyingParty;
    }


}
