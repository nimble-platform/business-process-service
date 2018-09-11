package eu.nimble.service.bp.impl.util.serialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ClauseType;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Created by suat on 16-May-18.
 */
public class Serializer {
    public static ObjectMapper getDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper = mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper = mapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);
        mapper = mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
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

    public static ObjectMapper getDefaultObjectMapperForFilledFields() {
        ObjectMapper mapper = getDefaultObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        return mapper;
    }
}
