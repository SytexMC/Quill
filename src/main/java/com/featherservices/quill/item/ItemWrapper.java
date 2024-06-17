package com.featherservices.quill.item;

import com.featherservices.quill.utils.Chat;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.List;

@Getter
public class ItemWrapper {
    private final Material material;
    private String name;
    private List<String> lore;
    private int amount;
    private int modelData;

    private int slot;

    public ItemWrapper(Material material) {
        this.material = material;
        this.amount = 1;
    }

    public ItemWrapper(Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    public void name(String name) {
        this.name = name;
    }

    public void lore(List<String> lore) {
        this.lore = lore;
    }

    public void amount(int amount) {
        this.amount = amount;
    }

    public void modelData(int modelData) {
        this.modelData = modelData;
    }

    public void slot(int slot) {
        this.slot = slot;
    }

    /**
     * Builds a final item with translated name, lore and other information.
     * @return The final item.
     */
    public ItemStack build() {
        final ItemStack item = new ItemStack(material, amount);

        item.editMeta(m -> {
            if (name != null)
                m.displayName(Chat.translate(name));

            if (lore != null)
                m.lore(Chat.translate(lore));

            if (modelData != 0)
                m.setCustomModelData(modelData);
        });

        return item;
    }
}
