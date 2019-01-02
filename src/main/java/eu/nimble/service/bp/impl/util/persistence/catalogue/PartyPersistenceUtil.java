package eu.nimble.service.bp.impl.util.persistence.catalogue;

import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.QualifyingPartyType;
import eu.nimble.utility.persistence.JPARepositoryFactory;

/**
 * Created by suat on 01-Jan-19.
 */
public class PartyPersistenceUtil {
    private static final String QUERY_SELECT_BY_ID = "SELECT party FROM PartyType party WHERE party.ID = :partyId";
    private static final String QUERY_GET_QUALIFIYING_PARTY = "SELECT qpt FROM QualifyingPartyType qpt WHERE qpt.party.ID = :partyId";

    public static PartyType getPartyByID(String partyId) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_SELECT_BY_ID, new String[]{"partyId"}, new Object[]{partyId});
    }

    public static QualifyingPartyType getQualifyingParty(String partyId) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_GET_QUALIFIYING_PARTY, new String[]{"partyId"}, new Object[]{partyId});
    }


}
