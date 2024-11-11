package me.levitate.quill.hook.hooks;

import me.levitate.quill.hook.PluginHook;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Hook for LuckPerms plugin functionality
 */
public class LuckPermsHook implements PluginHook {
    private static LuckPerms luckPerms;
    private boolean enabled = false;

    @Override
    public boolean init() {
        try {
            luckPerms = LuckPermsProvider.get();
            enabled = true;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getPluginName() {
        return "LuckPerms";
    }

    /**
     * Gets a player's prefix
     * @param uuid Player's UUID
     * @return The player's prefix or empty string if not found
     */
    public String getPrefix(UUID uuid) {
        if (!enabled) return "";
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) return "";
        return user.getCachedData().getMetaData().getPrefix() != null ?
                user.getCachedData().getMetaData().getPrefix() : "";
    }

    /**
     * Gets a player's suffix
     * @param uuid Player's UUID
     * @return The player's suffix or empty string if not found
     */
    public String getSuffix(UUID uuid) {
        if (!enabled) return "";
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) return "";
        return user.getCachedData().getMetaData().getSuffix() != null ?
                user.getCachedData().getMetaData().getSuffix() : "";
    }

    /**
     * Gets a player's primary group
     * @param uuid Player's UUID
     * @return The primary group name or empty string if not found
     */
    public String getPrimaryGroup(UUID uuid) {
        if (!enabled) return "";
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) return "";
        return user.getPrimaryGroup();
    }

    /**
     * Gets all groups a player is in
     * @param uuid Player's UUID
     * @return Set of group names
     */
    public Set<String> getAllGroups(UUID uuid) {
        if (!enabled) return Collections.emptySet();
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) return Collections.emptySet();

        return user.getNodes().stream()
                .filter(NodeType.INHERITANCE::matches)
                .map(NodeType.INHERITANCE::cast)
                .map(InheritanceNode::getGroupName)
                .collect(Collectors.toSet());
    }

    /**
     * Checks if a player is in a specific group
     * @param uuid Player's UUID
     * @param group Group name to check
     * @return true if the player is in the group
     */
    public boolean isInGroup(UUID uuid, String group) {
        if (!enabled) return false;
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) return false;

        return user.getNodes().stream()
                .filter(NodeType.INHERITANCE::matches)
                .map(NodeType.INHERITANCE::cast)
                .anyMatch(node -> node.getGroupName().equalsIgnoreCase(group));
    }

    /**
     * Gets list of groups that the specified group inherits from
     * @param group Group name
     * @return List of inherited group names
     */
    public List<String> getInheritedGroups(String group) {
        if (!enabled) return Collections.emptyList();
        Group g = luckPerms.getGroupManager().getGroup(group);
        if (g == null) return Collections.emptyList();

        return g.getNodes().stream()
                .filter(NodeType.INHERITANCE::matches)
                .map(NodeType.INHERITANCE::cast)
                .map(InheritanceNode::getGroupName)
                .collect(Collectors.toList());
    }

    /**
     * Checks if a permission has an expiry time for a player
     * @param uuid Player's UUID
     * @param permission Permission to check
     * @return true if the permission has an expiry time
     */
    public boolean hasPermissionExpiry(UUID uuid, String permission) {
        if (!enabled) return false;
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) return false;

        return user.getNodes().stream()
                .filter(node -> node.getKey().equals(permission))
                .anyMatch(Node::hasExpiry);
    }

    /**
     * Gets all permissions a player has
     * @param uuid Player's UUID
     * @return Set of permission nodes
     */
    public Set<String> getAllPermissions(UUID uuid) {
        if (!enabled) return Collections.emptySet();
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) return Collections.emptySet();

        return user.getNodes().stream()
                .filter(NodeType.PERMISSION::matches)
                .map(Node::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Gets all permissions that start with a specific prefix
     * @param uuid Player's UUID
     * @param prefix Permission prefix to filter by
     * @return Set of matching permission nodes
     */
    public Set<String> getPermissionsWithPrefix(UUID uuid, String prefix) {
        if (!enabled) return Collections.emptySet();

        final User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) return Collections.emptySet();

        return user.getNodes().stream()
                .filter(NodeType.PERMISSION::matches)
                .map(Node::getKey)
                .filter(perm -> perm.startsWith(prefix))
                .collect(Collectors.toSet());
    }
}