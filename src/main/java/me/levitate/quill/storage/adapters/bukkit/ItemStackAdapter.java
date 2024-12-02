package me.levitate.quill.storage.adapters.bukkit;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ItemStackAdapter extends JsonAdapter<ItemStack> {
    @Override
    public ItemStack fromJson(JsonReader reader) throws IOException {
        String base64 = reader.nextString();
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(base64));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to deserialize ItemStack", e);
        }
    }

    @Override
    public void toJson(JsonWriter writer, ItemStack item) throws IOException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            writer.value(Base64Coder.encodeLines(outputStream.toByteArray()));
        } catch (Exception e) {
            throw new IOException("Failed to serialize ItemStack", e);
        }
    }
}