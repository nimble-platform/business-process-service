package eu.nimble.service.bp.impl.util.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;

public class XMLGregorianCalendarSerializer extends StdSerializer<XMLGregorianCalendar>{

    public XMLGregorianCalendarSerializer() {
        this(null);
    }

    public XMLGregorianCalendarSerializer(Class<XMLGregorianCalendar> t) {
        super(t);
    }

    @Override
    public void serialize(XMLGregorianCalendar value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.toString());
    }
}
