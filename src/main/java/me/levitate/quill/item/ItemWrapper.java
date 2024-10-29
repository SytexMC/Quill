package me.levitate.quill.item;

import de.exlll.configlib.Configuration;
import lombok.Getter;
import me.levitate.quill.chat.Chat;
import me.levitate.quill.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Damageable;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Configuration
public class ItemWrapper {
    private Plugin plugin;
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

    // Recipe-related fields
    private String[] shapedPattern;
    private Map<Character, Object> shapedIngredients;
    private List<Object> shapelessIngredients;
    private Object furnaceInput;
    private float furnaceExperience;
    private int furnaceCookingTime;
    private String recipeId;

    public ItemWrapper() {
    }

    public ItemWrapper plugin(Plugin plugin) {
        this.plugin = plugin;
        return this;
    }

    public ItemWrapper material(Material material) {
        this.material = material;
        return this;
    }

    public ItemWrapper name(String name) {
        this.name = name;
        return this;
    }

    public ItemWrapper headId(String headId) {
        this.headId = headId;
        return this;
    }

    public ItemWrapper lore(List<String> lore) {
        this.lore = lore;
        return this;
    }

    public ItemWrapper amount(int amount) {
        this.amount = amount;
        return this;
    }

    public ItemWrapper modelData(int modelData) {
        this.modelData = modelData;
        return this;
    }

    public ItemWrapper slots(List<Integer> slots) {
        this.slots = slots;
        return this;
    }

    public ItemWrapper enchantments(Map<Enchantment, Integer> enchantments) {
        this.enchantments = enchantments;
        return this;
    }

    public ItemWrapper flags(List<ItemFlag> flags) {
        this.flags = flags;
        return this;
    }

    public ItemWrapper unbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
        return this;
    }

    public ItemWrapper customDurability(Integer customDurability) {
        this.customDurability = customDurability;
        return this;
    }

    private NamespacedKey getKey(String key) {
        if (plugin == null) {
            throw new IllegalStateException("Plugin must be set to use persistent data");
        }
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

        if (meta != null && plugin != null) {
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

    /**
     * Sets a specific ID for the recipe. If not set, a random UUID will be used.
     * @param recipeId The ID to use for the recipe
     * @return The ItemWrapper instance
     */
    public ItemWrapper setRecipeId(String recipeId) {
        this.recipeId = recipeId;
        return this;
    }

    /**
     * Sets a shaped crafting recipe for the item
     * @param pattern Array of 3 strings representing the crafting pattern
     * @param ingredients Map of characters to Materials or ItemStacks
     * @return The ItemWrapper instance
     */
    public ItemWrapper setShapedRecipe(String[] pattern, Map<Character, Object> ingredients) {
        if (pattern.length != 3) {
            throw new IllegalArgumentException("Pattern must be exactly 3 rows");
        }
        this.shapedPattern = pattern;
        this.shapedIngredients = ingredients;
        return this;
    }

    /**
     * Sets a shapeless crafting recipe for the item
     * @param ingredients List of Materials or ItemStacks needed to craft the item
     * @return The ItemWrapper instance
     */
    public ItemWrapper setShapelessRecipe(List<Object> ingredients) {
        this.shapelessIngredients = ingredients;
        return this;
    }

    /**
     * Sets a furnace recipe for the item
     * @param input Material or ItemStack to be smelted
     * @param experience Experience given when smelting
     * @param cookingTime Time in ticks to cook
     * @return The ItemWrapper instance
     */
    public ItemWrapper setFurnaceRecipe(Object input, float experience, int cookingTime) {
        this.furnaceInput = input;
        this.furnaceExperience = experience;
        this.furnaceCookingTime = cookingTime;
        return this;
    }

    private RecipeChoice getRecipeChoice(Object ingredient) {
        if (ingredient instanceof Material) {
            return new RecipeChoice.MaterialChoice((Material) ingredient);
        } else if (ingredient instanceof ItemStack) {
            return new RecipeChoice.ExactChoice((ItemStack) ingredient);
        } else {
            throw new IllegalArgumentException("Recipe ingredient must be either Material or ItemStack");
        }
    }

    /**
     * Registers all defined recipes for this item
     */
    public void registerRecipes() {
        if (plugin == null) {
            throw new IllegalStateException("Plugin must be set to register recipes");
        }

        String baseId = recipeId != null ? recipeId : UUID.randomUUID().toString();
        ItemStack result = this.build();

        // Register shaped recipe if defined
        if (shapedPattern != null && shapedIngredients != null) {
            ShapedRecipe shapedRecipe = new ShapedRecipe(new NamespacedKey(plugin, baseId + "_shaped"), result);
            shapedRecipe.shape(shapedPattern);
            shapedIngredients.forEach((key, ingredient) ->
                    shapedRecipe.setIngredient(key, getRecipeChoice(ingredient)));
            plugin.getServer().addRecipe(shapedRecipe);
        }

        // Register shapeless recipe if defined
        if (shapelessIngredients != null && !shapelessIngredients.isEmpty()) {
            ShapelessRecipe shapelessRecipe = new ShapelessRecipe(new NamespacedKey(plugin, baseId + "_shapeless"), result);
            shapelessIngredients.forEach(ingredient ->
                    shapelessRecipe.addIngredient(getRecipeChoice(ingredient)));
            plugin.getServer().addRecipe(shapelessRecipe);
        }

        // Register furnace recipe if defined
        if (furnaceInput != null) {
            FurnaceRecipe furnaceRecipe = new FurnaceRecipe(
                    new NamespacedKey(plugin, baseId + "_furnace"),
                    result,
                    getRecipeChoice(furnaceInput),
                    furnaceExperience,
                    furnaceCookingTime
            );
            plugin.getServer().addRecipe(furnaceRecipe);
        }
    }

    /**
     * Unregisters all recipes for this item
     */
    public void unregisterRecipes() {
        if (plugin == null || recipeId == null) {
            return;
        }

        plugin.getServer().removeRecipe(new NamespacedKey(plugin, recipeId + "_shaped"));
        plugin.getServer().removeRecipe(new NamespacedKey(plugin, recipeId + "_shapeless"));
        plugin.getServer().removeRecipe(new NamespacedKey(plugin, recipeId + "_furnace"));
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
                meta.displayName(MiniMessage.miniMessage().deserialize(Chat.replacePlaceholders(name, placeholders))
                        .decoration(TextDecoration.ITALIC, false));
            }

            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream()
                        .map(line -> Chat.replacePlaceholders(line, placeholders))
                        .map(line -> MiniMessage.miniMessage().deserialize(line))
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

            if (persistentData != null && !persistentData.isEmpty() && plugin != null) {
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
        return meta != null && plugin != null && meta.getPersistentDataContainer().has(getKey(key), PersistentDataType.STRING);
    }

    public String getString(ItemStack item, String key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || plugin == null) return null;
        return meta.getPersistentDataContainer().get(getKey(key), PersistentDataType.STRING);
    }

    public Integer getInt(ItemStack item, String key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || plugin == null) return null;
        return meta.getPersistentDataContainer().get(getKey(key), PersistentDataType.INTEGER);
    }

    public Double getDouble(ItemStack item, String key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || plugin == null) return null;
        return meta.getPersistentDataContainer().get(getKey(key), PersistentDataType.DOUBLE);
    }

    public Boolean getBoolean(ItemStack item, String key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || plugin == null) return null;
        Byte value = meta.getPersistentDataContainer().get(getKey(key), PersistentDataType.BYTE);
        return value != null ? value == 1 : null;
    }

    public <T, Z> void setPersistentData(ItemStack item, String key, PersistentDataType<T, Z> type, Z value) {
        if (plugin == null) {
            throw new IllegalStateException("Plugin must be set to use persistent data");
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(getKey(key), type, value);
            item.setItemMeta(meta);
        }
    }

    public Set<String> getPersistentDataKeys(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || plugin == null) return Collections.emptySet();

        return meta.getPersistentDataContainer().getKeys().stream()
                .filter(key -> key.getNamespace().equals(plugin.getName()))
                .map(NamespacedKey::getKey)
                .collect(Collectors.toSet());
    }
}