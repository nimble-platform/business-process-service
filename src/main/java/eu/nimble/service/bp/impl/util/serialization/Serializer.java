package eu.nimble.service.bp.impl.util.serialization;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ClauseType;

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
        return mapper;
    }
}
