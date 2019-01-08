package eu.nimble.service.bp.impl.util.persistence.catalogue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.QualifyingPartyType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CataloguePersistenceUtility {
    private static final Logger logger = LoggerFactory.getLogger(CataloguePersistenceUtility.class);

    private static final String QUERY_SELECT_BY_ID_AND_PARTY_ID = "SELECT cl FROM CatalogueLineType cl WHERE cl.ID = :lineId AND cl.goodsItem.item.manufacturerParty.ID = :partyId";

    public static CatalogueLineType getCatalogueLine(OrderType order) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_SELECT_BY_ID_AND_PARTY_ID,
                new String[]{"lineId", "partyId"},
                new Object[]{order.getOrderLine().get(0).getLineItem().getLineReference().get(0).getLineID(), order.getOrderLine().get(0).getLineItem().getItem().getManufacturerParty().getID()});
    }

    public static PartyType getParty(PartyType party) {
        if (party == null) {
            return null;
        }
        PartyType catalogueParty = PartyPersistenceUtility.getPartyByID(party.getID());
        if (catalogueParty != null) {
            return catalogueParty;
        } else {
            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();;
            objectMapper = objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            try {
                JSONObject object = new JSONObject(objectMapper.writeValueAsString(party));
                JsonSerializationUtility.removeHjidFields(object);
                party = objectMapper.readValue(object.toString(), PartyType.class);
            } catch (Exception e) {
                String msg = String.format("Failed to remove hjid fields from the party: %s", party.getID());
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }

            new JPARepositoryFactory().forCatalogueRepository().persistEntity(party);
            return party;
        }
    }

    public static PartyType getParty(String partyId) {
        PartyType party = PartyPersistenceUtility.getPartyByID(partyId);
        return party;
    }

    public static QualifyingPartyType getQualifyingPartyType(String partyID, String bearerToken) {
        QualifyingPartyType qualifyingParty = PartyPersistenceUtility.getQualifyingParty(partyID);
        if (qualifyingParty == null) {
            qualifyingParty = new QualifyingPartyType();
            // get party using identity service
            PartyType partyType = null;
            try {
                partyType = SpringBridge.getInstance().getIdentityClientTyped().getParty(bearerToken, partyID);
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
