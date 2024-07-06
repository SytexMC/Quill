package com.featherservices.quill.item;

import com.featherservices.quill.utils.Chat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Function;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ItemWrapper {

    private Material material;
    private String name;
    private List<String> lore;
    private int amount;
    private int modelData;

    private List<Integer> slots;

    public ItemWrapper(Material material) {
        this.material = material;
        this.amount = 1;
    }

    public ItemWrapper(Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    public ItemWrapper name(String name) {
        this.name = name;

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

    public int getSlot() {
        return slots.get(0);
    }

    public ItemWrapper slot(int slot) {
        this.slots = List.of(slot);

        return this;
    }

    public ItemWrapper slots(List<Integer> slots) {
        this.slots = slots;

        return this;
    }

    /**
     * Builds a final item with translated name, lore and other information.
     *
     * @return The final ItemStack.
     */
    public ItemStack build() {
        final ItemStack item = new ItemStack(material, amount);

        item.editMeta(m -> {
            if (name != null)
                m.displayName(Chat.translate(name).decoration(TextDecoration.ITALIC, false));

            if (lore != null)
                m.lore(Chat.translate(lore).stream().map(l -> l.decoration(TextDecoration.ITALIC, false)).toList());

            if (modelData != 0)
                m.setCustomModelData(modelData);
        });

        return item;
    }

    /**
     * Builds a final item with translated name, lore and other information.
     *
     * @param replace A replace function used to replace values in name and lore.
     * @return The final ItemStack
     */
    public ItemStack build(Function<String, String> replace) {
        final ItemStack item = new ItemStack(material, amount);

        item.editMeta(m -> {
            if (name != null)
                m.displayName(Chat.translate(replace.apply(name)).decoration(TextDecoration.ITALIC, false));

            if (lore != null)
                m.lore(Chat.translate(lore.stream().map(replace).toList()).stream().map(l -> l.decoration(TextDecoration.ITALIC, false)).toList());

            if (modelData != 0)
                m.setCustomModelData(modelData);
        });

        return item;
    }

}
