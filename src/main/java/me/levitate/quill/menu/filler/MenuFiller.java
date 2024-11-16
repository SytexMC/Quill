package me.levitate.quill.menu.filler;

import me.levitate.quill.menu.item.MenuItem;
import me.levitate.quill.menu.QuillMenu;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class MenuFiller {
    private final QuillMenu menu;
    private ItemStack fillItem;
    private final Set<Integer> excludedSlots = new HashSet<>();
    private boolean skipExisting = true;
    private final Map<Character, ItemStack> patternItems = new HashMap<>();

    public MenuFiller(QuillMenu menu) {
        this.menu = menu;
    }

    /**
     * Set the default fill item
     */
    public MenuFiller item(ItemStack item) {
        this.fillItem = item;
        this.patternItems.put('X', item);
        return this;
    }

    /**
     * Add a pattern item mapping
     */
    public MenuFiller patternItem(char character, ItemStack item) {
        this.patternItems.put(character, item);
        return this;
    }

    /**
     * Exclude specific slots from filling
     */
    public MenuFiller exclude(int... slots) {
        for (int slot : slots) {
            excludedSlots.add(slot);
        }
        return this;
    }

    /**
     * Exclude an entire column
     */
    public MenuFiller excludeColumn(int column) {
        if (column < 0 || column > 8) {
            throw new IllegalArgumentException("Column must be between 0 and 8");
        }
        for (int row = 0; row < menu.getSize() / 9; row++) {
            excludedSlots.add(row * 9 + column);
        }
        return this;
    }

    /**
     * Exclude an entire row
     */
    public MenuFiller excludeRow(int row) {
        if (row < 0 || row > menu.getSize() / 9 - 1) {
            throw new IllegalArgumentException("Row must be between 0 and " + (menu.getSize() / 9 - 1));
        }
        for (int col = 0; col < 9; col++) {
            excludedSlots.add(row * 9 + col);
        }
        return this;
    }

    /**
     * Set whether to skip slots that already have items
     */
    public MenuFiller skipExisting(boolean skip) {
        this.skipExisting = skip;
        return this;
    }

    /**
     * Fill all empty slots
     */
    public void fill() {
        validateFillItem();
        for (int i = 0; i < menu.getSize(); i++) {
            if (canFillSlot(i)) {
                fillSlot(i, fillItem);
            }
        }
    }

    /**
     * Fill the menu border
     */
    public void fillBorder() {
        validateFillItem();
        int rows = menu.getSize() / 9;

        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            fillSlot(i, fillItem);
            fillSlot((rows - 1) * 9 + i, fillItem);
        }

        // Left and right columns
        for (int i = 1; i < rows - 1; i++) {
            fillSlot(i * 9, fillItem);
            fillSlot(i * 9 + 8, fillItem);
        }
    }

    /**
     * Fill using a custom pattern
     */
    public void fillPattern(String[] pattern) {
        if (pattern == null || pattern.length == 0) {
            throw new IllegalArgumentException("Pattern cannot be null or empty");
        }

        int maxRows = menu.getSize() / 9;
        if (pattern.length > maxRows) {
            throw new IllegalArgumentException("Pattern has too many rows");
        }

        for (int row = 0; row < pattern.length; row++) {
            String rowPattern = pattern[row];
            if (rowPattern.length() > 9) {
                throw new IllegalArgumentException("Pattern row " + row + " is too long");
            }

            for (int col = 0; col < rowPattern.length(); col++) {
                char c = rowPattern.charAt(col);
                ItemStack item = patternItems.get(c);
                
                if (item != null) {
                    int slot = row * 9 + col;
                    if (canFillSlot(slot)) {
                        fillSlot(slot, item);
                    }
                }
            }
        }
    }

    /**
     * Fill using a preset pattern
     */
    public void fillPreset(FillPattern preset) {
        validateFillItem();
        fillPattern(preset.getPattern());
    }

    private boolean canFillSlot(int slot) {
        return !excludedSlots.contains(slot) && 
               (!skipExisting || !menu.hasItem(slot));
    }

    private void fillSlot(int slot, ItemStack item) {
        if (canFillSlot(slot)) {
            menu.addItem(MenuItem.builder()
                .item(item)
                .slot(slot)
                .build());
        }
    }

    private void validateFillItem() {
        if (fillItem == null) {
            throw new IllegalStateException("Fill item not set");
        }
    }
}