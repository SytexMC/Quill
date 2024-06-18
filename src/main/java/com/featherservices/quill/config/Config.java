package com.featherservices.quill.config;

import com.featherservices.quill.item.ItemWrapper;
import com.featherservices.quill.utils.Chat;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unused")
public abstract class Config {
    public final Plugin plugin;
    public FileConfiguration fileConfiguration;
    private final boolean loadMessages;

    @Getter
    private final Map<String, String> messages = new LinkedHashMap<>();

    /**
     * The constructor of Config.
     * @param plugin The plugin
     * @param fileConfiguration The file configuration
     * @param loadMessages Whether to load messages under the messages section.
     */
    public Config(Plugin plugin, FileConfiguration fileConfiguration, boolean loadMessages) {
        this.plugin = plugin;
        this.fileConfiguration = fileConfiguration;
        this.loadMessages = loadMessages;

        this.reloadConfig();
    }

    /**
     * Sends a formatted message to the player.
     * @param player Player to send message to.
     * @param messageName Message to send to the Player.
     */
    public void sendMessage(Player player, String messageName) {
        final String message = messages.get(messageName);
        if (message == null)
            throw new NullPointerException("Could not find message " + messageName + " in the config.");

        player.sendMessage(Chat.translate(message));
    }

    /**
     * Sends a formatted message to the CommandSender.
     * @param sender Player/Console to send message to.
     * @param messageName Message to send to the sender.
     */
    public void sendMessage(CommandSender sender, String messageName) {
        final String message = messages.get(messageName);
        if (message == null)
            throw new NullPointerException("Could not find message " + messageName + " in the config.");

        sender.sendMessage(Chat.translate(message));
    }

    /**
     * Sends a formatted message to the CommandSender.
     * @param sender Player/Console to send message to.
     * @param message Message to send to the sender.
     * @param replace A replace function used to replace values in the message.
     */
    public void sendMessage(CommandSender sender, String message, Function<String, String> replace) {
        sendMessage(sender, replace.apply(message));
    }

    /**
     * Sends a formatted message to the player.
     * @param player Player to send message to.
     * @param message Message to send to the Player.
     * @param replace A replace function used to replace values in the message.
     */
    public void sendMessage(Player player, String message, Function<String, String> replace) {
        sendMessage(player, replace.apply(message));
    }

    /**
     * Formats a message from the config using MiniMessage
     * @param messageName The name of the message to format.
     * @return The formatted message in the form of a component.
     */
    public Component formatMessage(String messageName) {
        final String message = messages.get(messageName);
        if (message == null)
            throw new NullPointerException("Could not find message " + messageName + " in the config.");

        return Chat.translate(message);
    }

    /**
     * Formats a message from the config using MiniMessage
     * @param messageName The name of the message to format.
     * @param replace A replace function used to replace values in the message.
     * @return The formatted message in the form of a component.
     */
    public Component formatMessage(String messageName, Function<String, String> replace) {
        final String message = messages.get(messageName);
        if (message == null)
            throw new NullPointerException("Could not find message " + messageName + " in the config.");

        return Chat.translate(replace.apply(message));
    }

    /**
     * Gets the message with the message name.
     * @param messageName The name of the message to get.
     * @return The message as a string.
     */
    public String getMessage(String messageName) {
        return messages.get(messageName);
    }

    /**
     * Reloads the configuration.
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        plugin.saveDefaultConfig();

        this.fileConfiguration = plugin.getConfig();
        fileConfiguration.options().copyDefaults(true);
        plugin.saveConfig();

        if (loadMessages)
            this.loadMessages();

        this.loadSettings();
    }

    /**
     * Loads all the messages from the config.
     */
    private void loadMessages() {
        messages.clear();

        final ConfigurationSection messagesSection = fileConfiguration.getConfigurationSection("messages");
        if (messagesSection == null)
            throw new RuntimeException("Messages section is null.");

        for (String key : messagesSection.getKeys(false)) {
            messages.put(key, fileConfiguration.getString("messages." + key));
        }
    }

    public abstract void loadSettings();

    /**
     * Turns a configuration section into an Item object.
     * @param path The path of the section
     * @return The newly created Item.
     */
    public ItemWrapper sectionToItem(String path) {
        final ConfigurationSection section = fileConfiguration.getConfigurationSection(path);
        if (section == null)
            throw new NullPointerException("The configuration section for path: " + path + " is invalid.");

        final String name = section.getString("name");
        final List<String> lore = section.getStringList("lore");

        final String materialString = section.getString("material");
        if (materialString == null)
            throw new NullPointerException("The material string for path: " + path + " is invalid.");

        final Material material = Material.matchMaterial(materialString);
        if (material == null)
            throw new NullPointerException("The material for path: " + path + " is invalid.");

        final int modelData = section.getInt("modelData");

        int amount = section.getInt("amount") == 0 ? 1 : section.getInt("amount");

        final ItemWrapper item = new ItemWrapper(material, amount)
                .name(name)
                .lore(lore)
                .modelData(modelData);

        if (section.contains("slot"))
            item.slot(section.getInt("slot"));

        else if (section.contains("slots"))
            item.slots(section.getIntegerList("slots"));

        return item;
    }
}
