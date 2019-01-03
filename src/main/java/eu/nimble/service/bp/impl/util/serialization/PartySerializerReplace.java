package eu.nimble.service.bp.impl.util.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import eu.nimble.service.bp.impl.util.persistence.catalogue.CataloguePersistenceUtility;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import util.DataModelUtility;

import java.io.IOException;

/**
 * Replaces the incoming party with one obtained from the ubldb. The aim is not to duplicate the party instances representing
 * the same company in the ubldb.
 *
 * Created by suat on 27-Dec-18.
 */
public class PartySerializerReplace extends JsonSerializer<PartyType> {

    @Override
    public void serialize(PartyType partyType, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

        PartyType catalogueParty = CataloguePersistenceUtility.getParty(partyType);
        DataModelUtility.nullifyPartyFields(partyType);
        DataModelUtility.copyParty(partyType, catalogueParty);

        jsonGenerator.writeNull();
    }
}
