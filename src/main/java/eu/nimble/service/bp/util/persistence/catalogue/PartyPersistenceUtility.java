package eu.nimble.service.bp.util.persistence.catalogue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CompletedTaskType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.QualifyingPartyType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.GenericJPARepository;
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

    private static final String QUERY_SELECT_BY_ID = "SELECT party FROM PartyType party JOIN party.partyIdentification partyIdentification WHERE partyIdentification.ID = :partyId AND party.federationInstanceID =:federationId";
    private static final String QUERY_SELECT_BY_CONDITIONS = "SELECT party FROM PartyType party JOIN party.partyIdentification partyIdentification WHERE %s";
    private static final String QUERY_SELECT_PERSON_BY_ID = "SELECT person FROM PersonType person WHERE person.ID = :id";
    private static final String QUERY_GET_QUALIFIYING_PARTY = "SELECT qpt FROM QualifyingPartyType qpt JOIN qpt.party.partyIdentification partyIdentification WHERE partyIdentification.ID = :partyId AND qpt.party.federationInstanceID = :federationId";
    private static final String QUERY_GET_ALL_QUALIFIYING_PARTIES = "SELECT qpt.completedTask FROM QualifyingPartyType qpt where qpt.completedTask.size > 0 and qpt.party is not null";


    private static PersonType getPersonByID(String personId){
        List<PersonType> personTypes = new JPARepositoryFactory().forCatalogueRepository(true).getEntities(QUERY_SELECT_PERSON_BY_ID, new String[]{"id"}, new Object[]{personId});
        if(personTypes.size() > 0){
            return personTypes.get(0);
        }
        return null;
    }
    public static PartyType getPartyByID(String partyId,String federationId,boolean lazyDisabled) {
        return new JPARepositoryFactory().forCatalogueRepository(lazyDisabled).getSingleEntity(QUERY_SELECT_BY_ID, new String[]{"partyId","federationId"}, new Object[]{partyId,federationId});
    }

    public static PartyType getPartyByID(String partyId, String federationId) {
        return getPartyByID(partyId,federationId,true);
    }

    public static PartyType getPartyByID(String partyId, String federationId, GenericJPARepository repository) {
        return repository.getSingleEntity(QUERY_SELECT_BY_ID, new String[]{"partyId","federationId"}, new Object[]{partyId,federationId});
    }

    public static List<PartyType> getPartyByIDs(List<String> partyIds, List<String> federationIds) {
        StringBuilder conditions = new StringBuilder();
        int size = partyIds.size();
        for (int i = 0; i < size; i++) {
            conditions.append(String.format("(partyIdentification.ID = %s AND party.federationInstanceID = %s)",partyIds.get(i),federationIds.get(i)));
            if(i != size-1){
                conditions.append(" OR ");
            }
        }
        String query = String.format(QUERY_SELECT_BY_CONDITIONS,conditions.toString());
        return new JPARepositoryFactory().forCatalogueRepository(true).getEntities(query);
    }

    public static QualifyingPartyType getQualifyingParty(String partyId,String federationId) {
        return new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntity(QUERY_GET_QUALIFIYING_PARTY, new String[]{"partyId","federationId"}, new Object[]{partyId,federationId});
    }

    public static QualifyingPartyType getQualifyingParty(String partyId,String federationId, GenericJPARepository repository) {
        return repository.getSingleEntity(QUERY_GET_QUALIFIYING_PARTY, new String[]{"partyId","federationId"}, new Object[]{partyId,federationId});
    }

    public static List<CompletedTaskType> getCompletedTasks() {
        return getCompletedTasks(new JPARepositoryFactory().forCatalogueRepository(true));
    }


    public static List<CompletedTaskType> getCompletedTasks(GenericJPARepository repository) {
        return repository.getEntities(QUERY_GET_ALL_QUALIFIYING_PARTIES);
    }

    public static PartyType getParty(PartyType party,String businessProcessContextId) {
        if (party == null) {
            return null;
        }

        GenericJPARepository repository;
        if(businessProcessContextId != null){
            repository = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(businessProcessContextId).getCatalogRepository();
        }
        else{
            repository = new JPARepositoryFactory().forCatalogueRepository(true);
        }

        PartyType catalogueParty;
        // For some cases (for example, carrier party in despatch advice), we have parties which do not have any identifiers
        // In such cases, we simply save the given party.
        if(party.getPartyIdentification() == null || party.getPartyIdentification().size() == 0 || party.getFederationInstanceID() == null){
            catalogueParty = null;
        }
        else {
            catalogueParty = PartyPersistenceUtility.getPartyByID(party.getPartyIdentification().get(0).getID(),party.getFederationInstanceID(),repository);
        }
        if (catalogueParty != null) {
            return catalogueParty;
        } else {
            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();

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

            party = repository.updateEntity(party);
            return party;
        }
    }

    public static PartyType getParty(PartyType party) {
        return getParty(party,null);
    }

    public static PartyType getParty(String partyId, String federationId) {
        return getParty(partyId, federationId,false);
    }

    public static PartyType getParty(String partyId, String federationId, boolean lazyDisabled) {
        PartyType party = PartyPersistenceUtility.getPartyByID(partyId,federationId,lazyDisabled);
        return party;
    }

    public static QualifyingPartyType getQualifyingPartyType(String partyID, String federationId, String bearerToken, String businessContextId) {
        QualifyingPartyType qualifyingParty;
        if(businessContextId != null){
            qualifyingParty = PartyPersistenceUtility.getQualifyingParty(partyID,federationId,BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(businessContextId).getCatalogRepository());
        }
        else {
            qualifyingParty = PartyPersistenceUtility.getQualifyingParty(partyID,federationId);
        }
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
            qualifyingParty.setParty(getParty(partyType,businessContextId));
        }
        return qualifyingParty;
    }

    public static QualifyingPartyType getQualifyingPartyType(String partyID, String federationId, String bearerToken) {
        return getQualifyingPartyType(partyID, federationId,bearerToken,null);
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
    public static List<PartyType> getParties(String bearerToken, List<String> partyIds, List<String> federationIds) throws IOException {
        List<PartyType> parties = new ArrayList<>();

        List<PartyType> identityParties = SpringBridge.getInstance().getiIdentityClientTyped().getParties(bearerToken,partyIds);
        // check whether there are parties which are not available in the identity-service
        if(identityParties != null){
            // update the party id list
            for (PartyType party : identityParties) {
                partyIds.remove(party.getPartyIdentification().get(0).getID());
            }

            parties.addAll(identityParties);
        }
        // if there are parties which are not in the identity-service, retrieve them from the ubldb database
        if(partyIds.size() > 0){
            parties.addAll(getPartyByIDs(partyIds,federationIds));
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
