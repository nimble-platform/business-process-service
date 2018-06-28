package eu.nimble.service.bp.impl.util.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ClauseType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DataMonitoringClauseType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DocumentClauseType;

import java.io.IOException;

/**
 * Created by suat on 15-May-18.
 */
public class  ClauseDeserializer extends JsonDeserializer<ClauseType> {

    @Override
    public ClauseType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);

        if(node == null) {
            return null;
        }

        String type = node.get("type").asText();
        String serializedClause = node.toString();

        ObjectMapper mapper = new ObjectMapper();
        if (!type.contentEquals(eu.nimble.service.bp.impl.model.ClauseType.DATA_MONITORING.toString())) {
            return mapper.readValue(serializedClause, DocumentClauseType.class);
        } else {
            return mapper.readValue(serializedClause, DataMonitoringClauseType.class);
        }
    }
}
