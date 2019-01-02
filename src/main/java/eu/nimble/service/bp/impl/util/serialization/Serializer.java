package eu.nimble.service.bp.impl.util.serialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ClauseType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.serialization.ClauseDeserializer;
import eu.nimble.utility.serialization.XMLGregorianCalendarSerializer;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Created by suat on 16-May-18.
 */
public class Serializer {
    public static ObjectMapper getDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper = mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper = mapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ClauseType.class, new ClauseDeserializer());
        mapper.registerModule(module);
        SimpleModule dateModule = new SimpleModule();
        dateModule.addSerializer(XMLGregorianCalendar.class,new XMLGregorianCalendarSerializer());
        mapper.registerModule(dateModule);
        return mapper;
    }

    public static ObjectMapper getObjectMapperForContracts(){
        ObjectMapper mapper = new ObjectMapper();
        mapper = mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper = mapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);
        return mapper;
    }

    public static ObjectMapper getObjectMapperForSerializingParties() {
        ObjectMapper mapper = getDefaultObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(PartyType.class, new PartySerializerReplace());
        mapper.registerModule(simpleModule);
        return mapper;
    }
}
