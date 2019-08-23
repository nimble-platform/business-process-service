package eu.nimble.service.bp.util.persistence.catalogue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.QualifyingPartyType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 01-Jan-19.
 */
public class PartyPersistenceUtility {
    private static final Logger logger = LoggerFactory.getLogger(PartyPersistenceUtility.class);

    private static final String QUERY_SELECT_BY_ID = "SELECT party FROM PartyType party JOIN party.partyIdentification partyIdentification WHERE partyIdentification.ID = :partyId";
    private static final String QUERY_SELECT_PERSON_BY_ID = "SELECT person FROM PersonType person WHERE person.ID = :id";
    private static final String QUERY_GET_QUALIFIYING_PARTY = "SELECT qpt FROM QualifyingPartyType qpt JOIN qpt.party.partyIdentification partyIdentification WHERE partyIdentification.ID = :partyId";

    private static PersonType getPersonByID(String personId){
        List<PersonType> personTypes = new JPARepositoryFactory().forCatalogueRepository(true).getEntities(QUERY_SELECT_PERSON_BY_ID, new String[]{"id"}, new Object[]{personId});
        if(personTypes.size() > 0){
            return personTypes.get(0);
        }
        return null;
    }

    public static PartyType getPartyByID(String partyId,boolean lazyDisabled) {
        return new JPARepositoryFactory().forCatalogueRepository(lazyDisabled).getSingleEntity(QUERY_SELECT_BY_ID, new String[]{"partyId"}, new Object[]{partyId});
    }

    public static PartyType getPartyByID(String partyId) {
        return getPartyByID(partyId,true);
    }

    public static QualifyingPartyType getQualifyingParty(String partyId) {
        return new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntity(QUERY_GET_QUALIFIYING_PARTY, new String[]{"partyId"}, new Object[]{partyId});
    }

    public static PartyType getParty(PartyType party) {
        if (party == null) {
            return null;
        }
        PartyType catalogueParty;
        // For some cases (for example, carrier party in despatch advice), we have parties which do not have any identifiers
        // In such cases, we simply save the given party.
        if(party.getPartyIdentification() == null || party.getPartyIdentification().size() == 0){
            catalogueParty = null;
        }
        else {
            catalogueParty = PartyPersistenceUtility.getPartyByID(party.getPartyIdentification().get(0).getID());
        }
        if (catalogueParty != null) {
            return catalogueParty;
        } else {
            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
            ;
            objectMapper = objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            try {
                JSONObject object = new JSONObject(objectMapper.writeValueAsString(party));
                JsonSerializationUtility.removeHjidFields(object);
                party = objectMapper.readValue(object.toString(), PartyType.class);
            } catch (Exception e) {
                String msg = String.format("Failed to remove hjid fields from the party: %s", party.getPartyIdentification().get(0).getID());
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }

            new JPARepositoryFactory().forCatalogueRepository().persistEntity(party);
            return party;
        }
    }

    public static PartyType getParty(String partyId) {
        return getParty(partyId, false);
    }

    public static PartyType getParty(String partyId, boolean lazyDisabled) {
        PartyType party = PartyPersistenceUtility.getPartyByID(partyId,lazyDisabled);
        return party;
    }

    public static QualifyingPartyType getQualifyingPartyType(String partyID, String bearerToken) {
        QualifyingPartyType qualifyingParty = PartyPersistenceUtility.getQualifyingParty(partyID);
        if (qualifyingParty == null) {
            qualifyingParty = new QualifyingPartyType();
            // get party using identity service
            PartyType partyType = null;
            try {
                partyType = SpringBridge.getInstance().getiIdentityClientTyped().getParty(bearerToken, partyID);
            } catch (IOException e) {
                String msg = String.format("Failed to get qualifying party: %s", partyID);
                logger.error(msg);
                throw new RuntimeException(msg, e);
            }

            qualifyingParty.setParty(getParty(partyType));
        }
        return qualifyingParty;
    }

    /**
     * Retrieve the user with given id. The user info is taken from the identity-service if it exists or
     * database directly.
     * */
    public static PersonType getPerson(String bearerToken, String userId) throws IOException {
        // get Person info from the identity-service
        PersonType person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken,userId);
        // if there is no such Person in the identity-service, take it from the database
        if(person == null){
            person = getPersonByID(userId);
        }
        return person;
    }

    /**
     * Retrieve the parties with the given ids. The party info is taken from the identity-service if it exists
     * or database directly.
     * */
    public static List<PartyType> getParties(String bearerToken, List<String> partyIds) throws IOException {
        List<PartyType> parties = new ArrayList<>();

        for(String partyId:partyIds){
            PartyType party = SpringBridge.getInstance().getiIdentityClientTyped().getParty(bearerToken,partyId);
            if(party == null){
                party = getParty(partyId, true);
            }
            parties.add(party);
        }

        return parties;
    }

    /**
     * Retrieve the party info for the given party. The party info is taken from the identity-service if it exists
     * ,otherwise the given party is returned.
     * */
    public static PartyType getParty(String bearerToken, PartyType partyType) throws IOException {
        PartyType party = SpringBridge.getInstance().getiIdentityClientTyped().getParty(bearerToken,partyType.getPartyIdentification().get(0).getID());
        return party == null ? partyType : party;
    }
}
