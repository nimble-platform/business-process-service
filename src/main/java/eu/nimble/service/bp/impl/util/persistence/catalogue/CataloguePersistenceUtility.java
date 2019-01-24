package eu.nimble.service.bp.impl.util.persistence.catalogue;

import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.utility.persistence.JPARepositoryFactory;

public class CataloguePersistenceUtility {

    private static final String QUERY_SELECT_BY_ID_AND_PARTY_ID = "SELECT cl FROM CatalogueLineType cl WHERE cl.ID = :lineId AND cl.goodsItem.item.manufacturerParty.ID = :partyId";

    public static CatalogueLineType getCatalogueLine(OrderType order) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_SELECT_BY_ID_AND_PARTY_ID,
                new String[]{"lineId", "partyId"},
                new Object[]{order.getOrderLine().get(0).getLineItem().getLineReference().get(0).getLineID(), order.getOrderLine().get(0).getLineItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID()});
    }
}
