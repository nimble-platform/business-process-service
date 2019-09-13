package eu.nimble.service.bp.util.persistence.catalogue;

import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.utility.persistence.JPARepositoryFactory;

public class CataloguePersistenceUtility {

    private static final String QUERY_SELECT_TT_DETAILS_BY_ID_AND_PARTY_ID = "SELECT cl.hjid,cl.goodsItem.item.catalogueDocumentReference.ID FROM CatalogueLineType cl JOIN cl.goodsItem.item.manufacturerParty.partyIdentification partyIdentification WHERE cl.ID = :lineId AND partyIdentification.ID = :partyId";
    private static final String QUERY_SELECT_CAT_LINE_PARTY_ID_MANUFACTURER_ITEM_ID = "SELECT partyIdentification.ID,cl.goodsItem.item.manufacturersItemIdentification.ID FROM CatalogueLineType cl join cl.goodsItem.item.manufacturerParty.partyIdentification partyIdentification where cl.hjid = :hjid";
    private static final String QUERY_SELECT_LINE_BY_CATALOG_AND_LINE_IDS = "SELECT catalogueLine FROM CatalogueType catalogue join catalogue.catalogueLine catalogueLine WHERE catalogue.UUID = :uuid AND catalogueLine.ID = :id";
    private static final String QUERY_SELECT_LINE_BY_HJID = "SELECT catalogueLine FROM CatalogueLineType catalogueLine WHERE catalogueLine.hjid = :hjid";

    public static Object[] getCatalogueIdAndLineId(OrderType order) {
        return new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntity(QUERY_SELECT_TT_DETAILS_BY_ID_AND_PARTY_ID,
                new String[]{"lineId", "partyId"},
                new Object[]{order.getOrderLine().get(0).getLineItem().getLineReference().get(0).getLineID(), order.getOrderLine().get(0).getLineItem().getItem().getManufacturerParty().getPartyIdentification().get(0).getID()});
    }

    public static Object[] getCatalogueLinePartyIdAndManufacturersItemIdentification(Long hjid) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_SELECT_CAT_LINE_PARTY_ID_MANUFACTURER_ITEM_ID,
                new String[]{"hjid"},
                new Object[]{hjid});
    }

    public static CatalogueLineType getCatalogueLine(String catalogueUuid, String lineId){
        return getCatalogueLine(catalogueUuid, lineId, true);
    }

    public static CatalogueLineType getCatalogueLine(String catalogueUuid, String lineId, Boolean lazyLoadingDisabled){
        return new JPARepositoryFactory().forCatalogueRepository(lazyLoadingDisabled).getSingleEntity(QUERY_SELECT_LINE_BY_CATALOG_AND_LINE_IDS,
                new String[]{"uuid","id"},
                new Object[]{catalogueUuid,lineId});
    }

    public static CatalogueLineType getCatalogueLine(String hjid){
        return new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntity(QUERY_SELECT_LINE_BY_HJID,
                new String[]{"hjid"},
                new Object[]{Long.parseLong(hjid)});
    }
}
