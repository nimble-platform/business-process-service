package eu.nimble.service.bp.impl.model.statistics;

import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemIdentificationType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by suat on 22-Jun-18.
 */
public class NonOrderedProducts {
    private Map<String, PartyItems> companies = new HashMap<>();

    public Map getCompanies() {
        return companies;
    }

    public void addProduct(String partyId, String partyName, String itemManId, String itemName) {
        // check company map
        PartyItems companyProducts = companies.get(partyId);
        if(companyProducts == null) {
            companyProducts = new PartyItems();
            companyProducts.setPartyName(partyName);
            companies.put(partyId, companyProducts);
        }

        ItemType item = new ItemType();
        item.setManufacturersItemIdentification(new ItemIdentificationType());
        item.getManufacturersItemIdentification().setID(itemManId);
        item.setName(itemName);
        companyProducts.getProducts().add(item);
    }

    private static class PartyItems {
        private String partyName;
        private List<ItemType> products = new ArrayList<>();

        public String getPartyName() {
            return partyName;
        }

        public void setPartyName(String partyName) {
            this.partyName = partyName;
        }

        public List<ItemType> getProducts() {
            return products;
        }

        public void setProducts(List<ItemType> products) {
            this.products = products;
        }
    }
}
