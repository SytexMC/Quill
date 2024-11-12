package me.levitate.quill.storage.serializers.location;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import me.levitate.quill.utils.common.SimpleLocation;

import java.io.IOException;

public class BasicLocationDeserializer extends JsonDeserializer<SimpleLocation> {
    @Override
    public SimpleLocation deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        return new SimpleLocation(
            node.get("world").asText(),
            node.get("x").asDouble(),
            node.get("y").asDouble(),
            node.get("z").asDouble()
        );
    }
}