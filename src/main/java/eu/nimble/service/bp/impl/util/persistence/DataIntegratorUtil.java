package eu.nimble.service.bp.impl.util.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.impl.util.serialization.Serializer;
import eu.nimble.utility.JsonSerializationUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DataIntegratorUtil {
    private static final Logger logger = LoggerFactory.getLogger(DataIntegratorUtil.class);

    public static void checkExistingParties(Object object){
        ObjectMapper mapper = Serializer.getObjectMapperForSerializingParties();
        try {
            mapper.writeValue(new ByteArrayOutputStream(), object);
        } catch (IOException e) {
            String serializedObject = JsonSerializationUtility.serializeEntitySilently(object);
            String msg = String.format("Failed to replace parties in the entity: {}", serializedObject);
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }
}
