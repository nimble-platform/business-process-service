package eu.nimble.service.bp.util.persistence.catalogue;

import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.utility.persistence.JPARepositoryFactory;

public class CataloguePersistenceUtility {

    private static final String QUERY_SELECT_CAT_LINE_PARTY_ID_MANUFACTURER_ITEM_ID = "SELECT partyIdentification.ID,cl.goodsItem.item.manufacturersItemIdentification.ID FROM CatalogueLineType cl join cl.goodsItem.item.manufacturerParty.partyIdentification partyIdentification where cl.hjid = :hjid";
    private static final String QUERY_SELECT_LINE_BY_CATALOG_AND_LINE_IDS = "SELECT catalogueLine FROM CatalogueType catalogue join catalogue.catalogueLine catalogueLine WHERE catalogue.UUID = :uuid AND catalogueLine.ID = :id";
    private static final String QUERY_GET_LINE_HJID_BY_CATALOG_AND_LINE_IDS = "SELECT catalogueLine.hjid FROM CatalogueType catalogue join catalogue.catalogueLine catalogueLine WHERE catalogue.UUID = :uuid AND catalogueLine.ID = :id";
    private static final String QUERY_SELECT_LINE_BY_HJID = "SELECT catalogueLine FROM CatalogueLineType catalogueLine WHERE catalogueLine.hjid = :hjid";
    private static final String QUERY_GET_SHOPPING_CART_WITH_PERSON_ID =
            "SELECT cat" +
            " FROM CatalogueType cat join cat.providerParty party join party.person person" +
            " WHERE person.ID = :personId" +
                " AND cat.ID = 'SHOPPING_CART'";

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

    public static Long getCatalogueLineHjid(String catalogueUuid, String lineId){
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_GET_LINE_HJID_BY_CATALOG_AND_LINE_IDS,
                new String[]{"uuid","id"},
                new Object[]{catalogueUuid,lineId});
    }

    public static CatalogueLineType getCatalogueLine(String hjid){
        return new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntity(QUERY_SELECT_LINE_BY_HJID,
                new String[]{"hjid"},
                new Object[]{Long.parseLong(hjid)});
    }

    public static CatalogueType getShoppingCartWithPersonId(String personId) {
        return new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntity(QUERY_GET_SHOPPING_CART_WITH_PERSON_ID,
                new String[]{"personId"},
                new Object[]{personId});
    }
}
