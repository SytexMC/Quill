package me.levitate.quill.serializers.location;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import me.levitate.quill.wrapper.BasicLocation;

import java.io.IOException;

public class BasicLocationDeserializer extends JsonDeserializer<BasicLocation> {
    @Override
    public BasicLocation deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        return new BasicLocation(
            node.get("world").asText(),
            node.get("x").asDouble(),
            node.get("y").asDouble(),
            node.get("z").asDouble()
        );
    }
}