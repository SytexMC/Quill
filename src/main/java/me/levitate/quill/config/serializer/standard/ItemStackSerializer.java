package me.levitate.quill.config.serializer.standard;

import me.levitate.quill.config.exception.SerializationException;
import me.levitate.quill.config.serializer.ConfigurationSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ItemStackSerializer implements ConfigurationSerializer<ItemStack> {
    @Override
    public Object serialize(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize ItemStack", e);
        }
    }

    @Override
    public ItemStack deserialize(Object value) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(value.toString()));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize ItemStack", e);
        }
    }

    @Override
    public Class<ItemStack> getType() {
        return ItemStack.class;
    }
}