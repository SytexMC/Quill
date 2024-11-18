package me.levitate.quill.menu;

import lombok.Getter;
import me.levitate.quill.chat.Chat;
import me.levitate.quill.menu.filler.MenuFiller;
import me.levitate.quill.menu.item.MenuItem;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;

public abstract class QuillMenu {
    protected final Map<Integer, MenuItem> items;
    protected final Map<Integer, List<MenuItem>> pages;
    protected final List<Consumer<InventoryClickEvent>> globalClickHandlers;
    protected final List<Consumer<InventoryCloseEvent>> closeHandlers;
    protected final List<Consumer<InventoryOpenEvent>> openHandlers;
    protected final List<Consumer<InventoryDragEvent>> dragHandlers;

    @Getter
    protected String title;
    @Getter
    protected int size;
    @Getter
    protected int currentPage = 1;
    @Getter
    protected int totalPages = 1;
    @Getter
    protected boolean cancelAllClicks = true;
    @Getter
    protected Sound clickSound;
    @Getter
    protected float clickVolume = 1.0f;
    @Getter
    protected float clickPitch = 1.0f;
    @Getter
    protected ItemStack pageFiller;
    @Getter
    protected int[] pageSlots;

    protected QuillMenu() {
        this.items = new HashMap<>();
        this.pages = new HashMap<>();
        this.globalClickHandlers = new ArrayList<>();
        this.closeHandlers = new ArrayList<>();
        this.openHandlers = new ArrayList<>();
        this.dragHandlers = new ArrayList<>();
    }

    protected void setMenu(String title, int rows) {
        this.title = title;
        this.size = rows * 9;
        this.pageSlots = generateDefaultPageSlots();
    }

    protected abstract void onCreate();

    protected void onOpen(Player player) {
    }

    protected void onClose(Player player) {
    }

    protected boolean onClick(Player player, InventoryClickEvent event) {
        return true;
    }

    public void addItem(MenuItem item) {
        if (item.isPaginatable()) {
            addPaginatedItem(item);
        } else {
            items.put(item.getSlot(), item);
        }
    }

    protected void addItems(Collection<MenuItem> menuItems) {
        menuItems.forEach(this::addItem);
    }

    protected void setClickSound(Sound sound, float volume, float pitch) {
        this.clickSound = sound;
        this.clickVolume = volume;
        this.clickPitch = pitch;
    }

    protected void setPageFiller(ItemStack filler) {
        this.pageFiller = filler;
    }

    protected void setCancelAllClicks(boolean cancel) {
        this.cancelAllClicks = cancel;
    }

    protected void setPageSlots(int[] slots) {
        this.pageSlots = slots;
        recalculatePagination();
    }

    private void addPaginatedItem(MenuItem item) {
        int page = (pages.isEmpty()) ? 1 : pages.size();
        List<MenuItem> pageItems = pages.computeIfAbsent(page, k -> new ArrayList<>());

        if (pageItems.size() >= pageSlots.length) {
            page++;
            pageItems = new ArrayList<>();
            pages.put(page, pageItems);
        }

        MenuItem pageItem = item.clone();
        pageItem.setSlot(pageSlots[pageItems.size()]);
        pageItems.add(pageItem);
        totalPages = page;
    }

    private void recalculatePagination() {
        if (pages.isEmpty()) return;

        Map<Integer, List<MenuItem>> oldPages = new HashMap<>(pages);
        pages.clear();

        oldPages.values().stream()
                .flatMap(List::stream)
                .forEach(this::addPaginatedItem);
    }

    private int[] generateDefaultPageSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row < size / 9 - 1; row++) {
            for (int col = 1; col < 8; col++) {
                slots.add(row * 9 + col);
            }
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    public void open(Player player) {
        if (title == null || size == 0) {
            throw new IllegalStateException("Menu not properly initialized. Call setMenu() in onCreate()");
        }

        Inventory inventory = Bukkit.createInventory(null, size, Chat.translate(title));
        updateInventory(inventory, currentPage);
        player.openInventory(inventory);
        onOpen(player);
    }

    public void updateInventory(Inventory inventory, int page) {
        inventory.clear();

        // Add static items
        items.forEach((slot, item) -> inventory.setItem(slot, item.getItemStack()));

        // Add page items
        List<MenuItem> pageItems = pages.get(page);
        if (pageItems != null) {
            pageItems.forEach(item -> inventory.setItem(item.getSlot(), item.getItemStack()));

            // Fill empty page slots
            if (pageFiller != null) {
                for (int slot : pageSlots) {
                    if (inventory.getItem(slot) == null) {
                        inventory.setItem(slot, pageFiller);
                    }
                }
            }
        }
    }

    public void nextPage(Player player) {
        if (currentPage < totalPages) {
            currentPage++;
            updateInventory(player.getOpenInventory().getTopInventory(), currentPage);
        }
    }

    public void previousPage(Player player) {
        if (currentPage > 1) {
            currentPage--;
            updateInventory(player.getOpenInventory().getTopInventory(), currentPage);
        }
    }

    public void setPage(Player player, int page) {
        if (page >= 1 && page <= totalPages) {
            currentPage = page;
            updateInventory(player.getOpenInventory().getTopInventory(), currentPage);
        }
    }

    public final void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        if (!onClick(player, event)) {
            return;
        }

        globalClickHandlers.forEach(handler -> handler.accept(event));

        if (cancelAllClicks) {
            event.setCancelled(true);
        }

        if (clickSound != null) {
            player.playSound(player.getLocation(), clickSound, clickVolume, clickPitch);
        }

        int slot = event.getSlot();
        MenuItem item = items.get(slot);

        if (item == null && pages.containsKey(currentPage)) {
            item = pages.get(currentPage).stream()
                    .filter(pageItem -> pageItem.getSlot() == slot)
                    .findFirst()
                    .orElse(null);
        }

        if (item != null && item.getClickHandler() != null) {
            item.getClickHandler().accept(event);
        }
    }

    public final void handleClose(InventoryCloseEvent event) {
        onClose((Player) event.getPlayer());
        closeHandlers.forEach(handler -> handler.accept(event));
    }

    public final void handleDrag(InventoryDragEvent event) {
        dragHandlers.forEach(handler -> handler.accept(event));
        if (cancelAllClicks) {
            event.setCancelled(true);
        }
    }

    /**
     * Creates a new filler instance for this menu
     * @return A new MenuFiller instance
     */
    protected MenuFiller filler() {
        return new MenuFiller(this);
    }

    /**
     * Quick fill method for simple fills
     * @param item The item to fill with
     */
    protected void fill(ItemStack item) {
        filler().item(item).fill();
    }

    /**
     * Quick border method for simple borders
     * @param item The item to create border with
     */
    protected void fillBorder(ItemStack item) {
        filler().item(item).fillBorder();
    }

    /**
     * Check if a slot has an item
     * @param slot The slot to check
     * @return true if the slot has an item
     */
    public boolean hasItem(int slot) {
        return items.containsKey(slot);
    }
}