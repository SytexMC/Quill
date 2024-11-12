package me.levitate.quill.item;

import me.levitate.quill.injection.annotation.Module;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.stream.Collectors;

@Module
public class RecipeBuilder {
    private final Plugin plugin;
    private final ItemStack result;
    private String recipeId;
    
    // Shaped Recipe
    private String[] shapedPattern;
    private Map<Character, RecipeChoice> shapedIngredients;
    
    // Shapeless Recipe
    private List<RecipeChoice> shapelessIngredients;
    
    // Furnace Recipe
    private RecipeChoice furnaceInput;
    private float furnaceExperience;
    private int furnaceCookingTime;

    private RecipeBuilder(Plugin plugin, ItemStack result) {
        this.plugin = plugin;
        this.result = result;
    }

    /**
     * Create a new recipe builder
     * @param plugin Your plugin instance
     * @param result The resulting item from the recipe
     * @return A new RecipeBuilder instance
     */
    public static RecipeBuilder create(Plugin plugin, ItemStack result) {
        return new RecipeBuilder(plugin, result);
    }

    /**
     * Set a custom ID for the recipe
     * @param id The recipe ID
     * @return The builder instance
     */
    public RecipeBuilder id(String id) {
        this.recipeId = id;
        return this;
    }

    /**
     * Create a shaped recipe
     * @param pattern Array of 3 strings representing the crafting pattern
     * @param ingredients Map of characters to ingredients (supports ItemStack, Material, or RecipeChoice)
     * @return The builder instance
     */
    public RecipeBuilder shaped(String[] pattern, Map<Character, Object> ingredients) {
        if (pattern.length != 3) {
            throw new IllegalArgumentException("Pattern must be exactly 3 rows");
        }
        
        this.shapedPattern = pattern;
        this.shapedIngredients = new HashMap<>();
        
        ingredients.forEach((key, ingredient) -> 
            this.shapedIngredients.put(key, createRecipeChoice(ingredient)));
        
        return this;
    }

    /**
     * Create a shapeless recipe
     * @param ingredients List of ingredients (supports ItemStack, Material, or RecipeChoice)
     * @return The builder instance
     */
    public RecipeBuilder shapeless(List<Object> ingredients) {
        this.shapelessIngredients = ingredients.stream()
                .map(this::createRecipeChoice)
                .collect(Collectors.toList());
        return this;
    }

    /**
     * Create a furnace recipe
     * @param input The input ingredient (supports ItemStack, Material, or RecipeChoice)
     * @param experience Experience given when smelting
     * @param cookingTime Time in ticks to cook
     * @return The builder instance
     */
    public RecipeBuilder furnace(Object input, float experience, int cookingTime) {
        this.furnaceInput = createRecipeChoice(input);
        this.furnaceExperience = experience;
        this.furnaceCookingTime = cookingTime;
        return this;
    }

    /**
     * Register all defined recipes
     * @return List of created recipe keys
     */
    public List<NamespacedKey> register() {
        List<NamespacedKey> registeredKeys = new ArrayList<>();
        String baseId = recipeId != null ? recipeId : UUID.randomUUID().toString();

        // Register shaped recipe
        if (shapedPattern != null && shapedIngredients != null) {
            NamespacedKey key = new NamespacedKey(plugin, baseId + "_shaped");
            ShapedRecipe recipe = new ShapedRecipe(key, result);
            recipe.shape(shapedPattern);
            shapedIngredients.forEach(recipe::setIngredient);
            plugin.getServer().addRecipe(recipe, true);
            registeredKeys.add(key);
        }

        // Register shapeless recipe
        if (shapelessIngredients != null && !shapelessIngredients.isEmpty()) {
            NamespacedKey key = new NamespacedKey(plugin, baseId + "_shapeless");
            ShapelessRecipe recipe = new ShapelessRecipe(key, result);
            shapelessIngredients.forEach(recipe::addIngredient);
            plugin.getServer().addRecipe(recipe, true);
            registeredKeys.add(key);
        }

        // Register furnace recipe
        if (furnaceInput != null) {
            NamespacedKey key = new NamespacedKey(plugin, baseId + "_furnace");
            FurnaceRecipe recipe = new FurnaceRecipe(
                key,
                result,
                furnaceInput,
                furnaceExperience,
                furnaceCookingTime
            );
            plugin.getServer().addRecipe(recipe, true);
            registeredKeys.add(key);
        }

        return registeredKeys;
    }

    /**
     * Unregister all recipes created with the current ID
     */
    public void unregister() {
        if (recipeId == null) return;
        
        plugin.getServer().removeRecipe(new NamespacedKey(plugin, recipeId + "_shaped"));
        plugin.getServer().removeRecipe(new NamespacedKey(plugin, recipeId + "_shapeless"));
        plugin.getServer().removeRecipe(new NamespacedKey(plugin, recipeId + "_furnace"));
    }

    private RecipeChoice createRecipeChoice(Object ingredient) {
        if (ingredient instanceof RecipeChoice) {
            return (RecipeChoice) ingredient;
        } else if (ingredient instanceof ItemStack) {
            return new RecipeChoice.ExactChoice((ItemStack) ingredient);
        } else if (ingredient instanceof Material) {
            return new RecipeChoice.MaterialChoice((Material) ingredient);
        } else if (ingredient instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                throw new IllegalArgumentException("Ingredient collection cannot be empty");
            }
            
            Object first = collection.iterator().next();
            if (first instanceof ItemStack) {
                return new RecipeChoice.ExactChoice(
                    collection.stream()
                        .map(item -> (ItemStack) item)
                        .collect(Collectors.toList())
                );
            } else if (first instanceof Material) {
                return new RecipeChoice.MaterialChoice(
                    collection.stream()
                        .map(mat -> (Material) mat)
                        .collect(Collectors.toList())
                );
            }
        }
        
        throw new IllegalArgumentException("Unsupported ingredient type: " + 
            (ingredient != null ? ingredient.getClass().getName() : "null"));
    }
}