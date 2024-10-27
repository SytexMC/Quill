package me.levitate.quill.item;

import de.exlll.configlib.Configuration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.levitate.quill.chat.Chat;
import me.levitate.quill.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Damageable;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Configuration
@AllArgsConstructor
public class ItemWrapper {
    private final Plugin plugin;

    private Material material;
    private String name;
    private String headId;
    private List<String> lore;
    private int amount = 1;
    private int modelData;
    private List<Integer> slots;
    private Map<Enchantment, Integer> enchantments;
    private List<ItemFlag> flags;
    private boolean unbreakable;
    private Integer customDurability;
    private Map<String, Object> persistentData;

    public ItemWrapper(Plugin plugin) {
        this.plugin = plugin;
    }

    private NamespacedKey getKey(String key) {
        return new NamespacedKey(plugin, key);
    }

    public ItemWrapper setString(String key, String value) {
        if (persistentData == null) {
            persistentData = new HashMap<>();
        }
        persistentData.put(key, value);
        return this;
    }

    public ItemWrapper setInt(String key, int value) {
        if (persistentData == null) {
            persistentData = new HashMap<>();
        }
        persistentData.put(key, value);
        return this;
    }

    public ItemWrapper setDouble(String key, double value) {
        if (persistentData == null) {
            persistentData = new HashMap<>();
        }
        persistentData.put(key, value);
        return this;
    }

    public ItemWrapper setBoolean(String key, boolean value) {
        if (persistentData == null) {
            persistentData = new HashMap<>();
        }
        persistentData.put(key, value);
        return this;
    }

    public Map<String, Object> getPersistentData(ItemStack item) {
        Objects.requireNonNull(item, "Item cannot be null");
        Map<String, Object> data = new HashMap<>();
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();

            for (NamespacedKey key : container.getKeys()) {
                if (key.getNamespace().equals(plugin.getName())) {
                    String keyString = key.getKey();
                    if (container.has(key, PersistentDataType.STRING)) {
                        data.put(keyString, container.get(key, PersistentDataType.STRING));
                    } else if (container.has(key, PersistentDataType.INTEGER)) {
                        data.put(keyString, container.get(key, PersistentDataType.INTEGER));
                    } else if (container.has(key, PersistentDataType.DOUBLE)) {
                        data.put(keyString, container.get(key, PersistentDataType.DOUBLE));
                    } else if (container.has(key, PersistentDataType.BYTE)) {
                        data.put(keyString, container.get(key, PersistentDataType.BYTE) == 1);
                    }
                }
            }
        }

        return data;
    }

    public ItemStack build(Object... placeholders) {
        ItemStack item;

        if (headId != null && material == Material.PLAYER_HEAD) {
            item = ItemUtils.createCustomHead(headId);
        } else {
            item = new ItemStack(material);
        }

        item.setAmount(amount);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.displayName(Component.text(Chat.replacePlaceholders(name, placeholders))
                        .decoration(TextDecoration.ITALIC, false));
            }

            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream()
                        .map(line -> Chat.replacePlaceholders(line, placeholders))
                        .map(Component::text)
                        .map(component -> component.decoration(TextDecoration.ITALIC, false))
                        .collect(Collectors.toList()));
            }

            if (modelData != 0) {
                meta.setCustomModelData(modelData);
            }

            if (enchantments != null) {
                enchantments.forEach((ench, level) -> meta.addEnchant(ench, level, true));
            }

            if (flags != null) {
                flags.forEach(meta::addItemFlags);
            }

            meta.setUnbreakable(unbreakable);

            if (customDurability != null && meta instanceof Damageable damageable) {
                damageable.damage(customDurability);
            }

            if (persistentData != null && !persistentData.isEmpty()) {
                PersistentDataContainer container = meta.getPersistentDataContainer();

                persistentData.forEach((key, value) -> {
                    NamespacedKey namespacedKey = getKey(key);
                    if (value instanceof String stringValue) {
                        container.set(namespacedKey, PersistentDataType.STRING, stringValue);
                    } else if (value instanceof Integer intValue) {
                        container.set(namespacedKey, PersistentDataType.INTEGER, intValue);
                    } else if (value instanceof Double doubleValue) {
                        container.set(namespacedKey, PersistentDataType.DOUBLE, doubleValue);
                    } else if (value instanceof Boolean boolValue) {
                        container.set(namespacedKey, PersistentDataType.BYTE, (byte) (boolValue ? 1 : 0));
                    }
                });
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean hasString(ItemStack item, String key) {
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(getKey(key), PersistentDataType.STRING);
    }

    public String getString(ItemStack item, String key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(getKey(key), PersistentDataType.STRING);
    }

    public Integer getInt(ItemStack item, String key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(getKey(key), PersistentDataType.INTEGER);
    }

    public Double getDouble(ItemStack item, String key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(getKey(key), PersistentDataType.DOUBLE);
    }

    public Boolean getBoolean(ItemStack item, String key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        Byte value = meta.getPersistentDataContainer().get(getKey(key), PersistentDataType.BYTE);
        return value != null ? value == 1 : null;
    }

    public <T, Z> void setPersistentData(ItemStack item, String key, PersistentDataType<T, Z> type, Z value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(getKey(key), type, value);
            item.setItemMeta(meta);
        }
    }

    public Set<String> getPersistentDataKeys(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Collections.emptySet();

        return meta.getPersistentDataContainer().getKeys().stream()
                .filter(key -> key.getNamespace().equals(plugin.getName()))
                .map(NamespacedKey::getKey)
                .collect(Collectors.toSet());
    }
}