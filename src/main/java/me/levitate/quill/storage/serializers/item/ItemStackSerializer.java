package me.levitate.quill.storage.serializers.item;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ItemStackSerializer extends JsonSerializer<ItemStack> {
    @Override
    public void serialize(ItemStack item, JsonGenerator gen, SerializerProvider provider) throws IOException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            gen.writeString(Base64Coder.encodeLines(outputStream.toByteArray()));
        } catch (Exception e) {
            throw new IOException("Failed to serialize ItemStack", e);
        }
    }
}