package me.levitate.quill.config.comment;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import me.levitate.quill.config.annotation.Comment;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class CommentedConfigurationSerializer extends SimpleBeanPropertyFilter {
    private final Map<String, String> comments = new HashMap<>();
    private JsonGenerator currentGenerator;

    public CommentedConfigurationSerializer(Class<?> configClass) {
        loadComments(configClass);
    }

    private void loadComments(Class<?> configClass) {
        for (Field field : configClass.getDeclaredFields()) {
            Comment comment = field.getAnnotation(Comment.class);
            if (comment != null) {
                comments.put(field.getName(), comment.value());
            }
        }
    }

    @Override
    public void serializeAsField(Object pojo, JsonGenerator generator, SerializerProvider provider, PropertyWriter writer) throws Exception {
        this.currentGenerator = generator;

        if (generator instanceof YAMLGenerator yamlGenerator) {
            String propertyName = writer.getName();
            String comment = comments.get(propertyName);

            // Write comment if it exists
            if (comment != null) {
                // Ensure we're at the start of a new line
                yamlGenerator.writeRaw("\n");
                // Write each line of the comment
                for (String line : comment.split("\n")) {
                    yamlGenerator.writeRaw("# " + line + "\n");
                }
            }
        }

        // Write the actual field
        if (writer instanceof BeanPropertyWriter beanWriter) {
            if (include(beanWriter)) {
                writer.serializeAsField(pojo, generator, provider);
                return;
            }
        }
        if (!generator.canOmitFields()) {
            writer.serializeAsOmittedField(pojo, generator, provider);
        }
    }

    public boolean include(BeanPropertyWriter writer) {
        return true;
    }
}