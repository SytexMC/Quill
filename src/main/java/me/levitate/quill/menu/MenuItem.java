package me.levitate.quill.menu;

import lombok.Getter;
import lombok.Setter;
import me.levitate.quill.item.ItemBuilder;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

@Getter
@Setter
public class MenuItem implements Cloneable {
    private ItemStack itemStack;
    private int slot;
    private Consumer<InventoryClickEvent> clickHandler;
    private boolean paginatable;

    private MenuItem(Builder builder) {
        this.itemStack = builder.itemStack;
        this.slot = builder.slot;
        this.clickHandler = builder.clickHandler;
        this.paginatable = builder.paginatable;
    }

    @Override
    public MenuItem clone() {
        try {
            MenuItem clone = (MenuItem) super.clone();
            clone.itemStack = itemStack.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public static class Builder {
        private ItemStack itemStack;
        private int slot;
        private Consumer<InventoryClickEvent> clickHandler;
        private boolean paginatable;

        public Builder item(ItemStack itemStack) {
            this.itemStack = itemStack;
            return this;
        }

        public Builder item(ItemBuilder itemBuilder) {
            this.itemStack = itemBuilder.build();
            return this;
        }

        public Builder slot(int slot) {
            this.slot = slot;
            return this;
        }

        public Builder onClick(Consumer<InventoryClickEvent> handler) {
            this.clickHandler = handler;
            return this;
        }

        public Builder paginatable(boolean paginatable) {
            this.paginatable = paginatable;
            return this;
        }

        public MenuItem build() {
            return new MenuItem(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}