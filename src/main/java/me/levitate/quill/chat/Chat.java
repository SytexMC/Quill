package me.levitate.quill.chat;

import me.levitate.quill.hook.PlaceholderFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Chat {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();
    private static final Pattern soundPattern = Pattern.compile("<sound:([A-Z_]+):([0-9.]+):([0-9.]+)>");
    private static final Pattern clickPattern = Pattern.compile("<click:(\\w+):([^>]+)>(.+?)</click>");
    private static final Pattern hoverPattern = Pattern.compile("<hover:([^>]+)>(.+?)</hover>");

    public static Component translate(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return miniMessage.deserialize(text);
    }

    public static String translateLegacy(Component text) {
        return text != null ? legacySerializer.serialize(text) : "";
    }

    public static List<Component> translate(List<String> text) {
        if (text == null) {
            return Collections.emptyList();
        }
        return text.stream()
                .filter(Objects::nonNull)
                .map(Chat::translate)
                .collect(Collectors.toList());
    }

    public static void sendMessage(CommandSender sender, String text, Object... placeholders) {
        Objects.requireNonNull(sender, "Sender cannot be null");

        if (text == null || text.isEmpty()) {
            return;
        }

        String processed = replacePlaceholders(text, placeholders);
        if (sender instanceof Player player) {
            processed = PlaceholderFactory.setPlaceholders(player, processed);
            sendWithSoundAndInteraction(player, processed);
        } else {
            sender.sendMessage(removeSoundTags(processed));
        }
    }

    private static void sendWithSoundAndInteraction(Player player, String text) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(text, "Text cannot be null");

        // Handle sounds
        Matcher soundMatcher = soundPattern.matcher(text);
        while (soundMatcher.find()) {
            try {
                Sound sound = Sound.valueOf(soundMatcher.group(1));
                float volume = Float.parseFloat(soundMatcher.group(2));
                float pitch = Float.parseFloat(soundMatcher.group(3));
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Process message
        String messageText = removeSoundTags(text);
        Component component = processInteractiveElements(messageText);
        player.sendMessage(component);
    }

    private static Component processInteractiveElements(String text) {
        // Process click events
        Matcher clickMatcher = clickPattern.matcher(text);
        StringBuffer clickBuffer = new StringBuffer();

        while (clickMatcher.find()) {
            String action = clickMatcher.group(1);
            String value = clickMatcher.group(2);
            String content = clickMatcher.group(3);

            ClickEvent.Action clickAction = switch (action.toLowerCase()) {
                case "command" -> ClickEvent.Action.RUN_COMMAND;
                case "suggest" -> ClickEvent.Action.SUGGEST_COMMAND;
                case "copy" -> ClickEvent.Action.COPY_TO_CLIPBOARD;
                case "url" -> ClickEvent.Action.OPEN_URL;
                default -> null;
            };

            if (clickAction != null) {
                String replacement = "<click_event:" + clickAction.name() + ":" + value + ">" + content + "</click_event>";
                clickMatcher.appendReplacement(clickBuffer, Matcher.quoteReplacement(replacement));
            }
        }
        clickMatcher.appendTail(clickBuffer);
        text = clickBuffer.toString();

        // Process hover events
        Matcher hoverMatcher = hoverPattern.matcher(text);
        StringBuffer hoverBuffer = new StringBuffer();

        while (hoverMatcher.find()) {
            String hoverText = hoverMatcher.group(1);
            String content = hoverMatcher.group(2);
            String replacement = "<hover_event:show_text:'" + hoverText + "'>" + content + "</hover_event>";
            hoverMatcher.appendReplacement(hoverBuffer, Matcher.quoteReplacement(replacement));
        }
        hoverMatcher.appendTail(hoverBuffer);
        text = hoverBuffer.toString();

        return miniMessage.deserialize(text);
    }

    private static String removeSoundTags(String text) {
        return soundPattern.matcher(text).replaceAll("");
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Objects.requireNonNull(player, "Player cannot be null");

        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)
        );

        Component titleComponent = title != null ?
                translate(PlaceholderFactory.setPlaceholders(player, title)) :
                Component.empty();
        Component subtitleComponent = subtitle != null ?
                translate(PlaceholderFactory.setPlaceholders(player, subtitle)) :
                Component.empty();

        Title titleObj = Title.title(titleComponent, subtitleComponent, times);
        player.showTitle(titleObj);
    }

    public static void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, 10, 70, 20);
    }

    public static void sendActionBar(Player player, String text, Object... placeholders) {
        Objects.requireNonNull(player, "Player cannot be null");
        if (text == null || text.isEmpty()) {
            return;
        }

        String processed = replacePlaceholders(text, placeholders);
        processed = PlaceholderFactory.setPlaceholders(player, processed);
        player.sendActionBar(translate(processed));
    }

    public static void broadcast(String text, Object... placeholders) {
        if (text == null || text.isEmpty()) {
            return;
        }

        String processed = replacePlaceholders(text, placeholders);
        Component message = translate(processed);
        Bukkit.getServer().sendMessage(message);
    }

    public static void broadcastToPermission(String text, String permission, Object... placeholders) {
        Objects.requireNonNull(permission, "Permission cannot be null");
        if (text == null || text.isEmpty()) {
            return;
        }

        String processed = replacePlaceholders(text, placeholders);
        Component message = translate(processed);
        Bukkit.getServer().broadcast(message, permission);
    }

    public static void broadcastActionBar(String text, Object... placeholders) {
        if (text == null || text.isEmpty()) {
            return;
        }

        String processed = replacePlaceholders(text, placeholders);
        Component message = translate(processed);
        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerProcessed = PlaceholderFactory.setPlaceholders(player, processed);
            player.sendActionBar(translate(playerProcessed));
        }
    }

    public static void broadcastTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    public static void broadcastTitle(String title, String subtitle) {
        broadcastTitle(title, subtitle, 10, 70, 20);
    }

    public static String replacePlaceholders(String text, Object... placeholders) {
        if (text == null) return null;
        if (placeholders == null || placeholders.length == 0) return text;

        String result = text;
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String key = String.valueOf(placeholders[i]);
            String value = String.valueOf(placeholders[i + 1]);
            result = result.replace(key, value);
        }
        return result;
    }

    public static void sendMessages(CommandSender sender, List<String> messages, Object... placeholders) {
        if (messages == null || messages.isEmpty()) return;
        messages.forEach(message -> sendMessage(sender, message, placeholders));
    }

    public static String colorize(String text) {
        if (text == null || text.isEmpty()) return text;
        return translateLegacy(translate(text));
    }

    public static List<String> colorize(List<String> texts) {
        if (texts == null) return Collections.emptyList();
        return texts.stream()
                .filter(Objects::nonNull)
                .map(Chat::colorize)
                .collect(Collectors.toList());
    }

    public static void clearTitle(Player player) {
        Objects.requireNonNull(player, "Player cannot be null");
        player.clearTitle();
    }

    public static void clearTitles() {
        Bukkit.getOnlinePlayers().forEach(Player::clearTitle);
    }

    /**
     * Sends a message to a list of players
     */
    public static void sendMessage(List<Player> players, String text, Object... placeholders) {
        Objects.requireNonNull(players, "Players collection cannot be null");
        players.forEach(player -> sendMessage(player, text, placeholders));
    }

    /**
     * Sends a message to a list of UUIDs
     */
    public static void sendMessage(Collection<UUID> uuids, String text, Object... placeholders) {
        Objects.requireNonNull(uuids, "UUIDs collection cannot be null");
        uuids.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(player -> sendMessage(player, text, placeholders));
    }

    /**
     * Sends a message to a player by UUID
     */
    public static void sendMessage(UUID uuid, String text, Object... placeholders) {
        Objects.requireNonNull(uuid, "UUID cannot be null");
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            sendMessage(player, text, placeholders);
        }
    }

    /**
     * Sends multiple messages to a list of players
     */
    public static void sendMessages(List<Player> players, List<String> messages, Object... placeholders) {
        Objects.requireNonNull(players, "Players list cannot be null");
        players.forEach(player -> sendMessages(player, messages, placeholders));
    }

    /**
     * Sends multiple messages to a list of UUIDs
     */
    public static void sendMessages(Collection<UUID> uuids, List<String> messages, Object... placeholders) {
        Objects.requireNonNull(uuids, "UUIDs collection cannot be null");
        uuids.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(player -> sendMessages(player, messages, placeholders));
    }

    /**
     * Sends multiple messages to a player by UUID
     */
    public static void sendMessages(UUID uuid, List<String> messages, Object... placeholders) {
        Objects.requireNonNull(uuid, "UUID cannot be null");
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            sendMessages(player, messages, placeholders);
        }
    }
}