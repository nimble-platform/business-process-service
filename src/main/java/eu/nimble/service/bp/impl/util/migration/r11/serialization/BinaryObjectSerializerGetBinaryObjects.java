package eu.nimble.service.bp.impl.util.migration.r11.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates binary objects stored in the ubl database. When this serializer is attached to
 * an {@link com.fasterxml.jackson.databind.ObjectMapper} and an object including several {@link BinaryObjectType}s residing
 * in various locations in the object hierarchy is serialized with the serializer, all {@link BinaryObjectType}s which have a uri are
 * accumulated inside the {@code objects}.
 */
public class BinaryObjectSerializerGetBinaryObjects extends JsonSerializer<BinaryObjectType> {

    private List<BinaryObjectType> objects = new ArrayList<>();
    private List<String> listOfUris = new ArrayList<>();

    @Override
    public void serialize(BinaryObjectType binaryObjectType, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (binaryObjectType.getUri() != null && !binaryObjectType.getUri().equals("")) {
            objects.add(binaryObjectType);
        }

        jsonGenerator.writeObject(null);
    }

    public List<BinaryObjectType> getObjects() {
        return objects;
    }
}