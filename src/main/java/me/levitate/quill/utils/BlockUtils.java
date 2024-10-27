package me.levitate.quill.utils;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class BlockUtils {
    /**
     * Get all blocks within a radius
     */
    public static List<Block> getBlocksInRadius(Location center, double radius) {
        List<Block> blocks = new ArrayList<>();
        int radiusInt = (int) radius;
        
        for (int x = -radiusInt; x <= radiusInt; x++) {
            for (int y = -radiusInt; y <= radiusInt; y++) {
                for (int z = -radiusInt; z <= radiusInt; z++) {
                    Location loc = center.clone().add(x, y, z);
                    if (loc.distance(center) <= radius) {
                        blocks.add(loc.getBlock());
                    }
                }
            }
        }
        
        return blocks;
    }

    /**
     * Get blocks between two points
     */
    public static List<Block> getBlocksBetween(Location loc1, Location loc2) {
        List<Block> blocks = new ArrayList<>();
        
        int maxDistance = (int) loc1.distance(loc2);
        Vector direction = loc2.toVector().subtract(loc1.toVector()).normalize();
        
        for (int i = 0; i <= maxDistance; i++) {
            Location loc = loc1.clone().add(direction.clone().multiply(i));
            blocks.add(loc.getBlock());
        }
        
        return blocks;
    }

    /**
     * Check if a block is solid (not air, liquid, or passthrough)
     */
    public static boolean isSolid(Block block) {
        return block.getType().isSolid() && !block.isLiquid();
    }

    /**
     * Get the highest block at a location
     */
    public static Block getHighestBlock(Location location) {
        return location.getWorld().getHighestBlockAt(location);
    }

    /**
     * Check if a block is safe to teleport to
     */
    public static boolean isSafeLocation(Location location) {
        Block feet = location.getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        Block ground = feet.getRelative(BlockFace.DOWN);
        
        return !isSolid(feet) && !isSolid(head) && isSolid(ground);
    }
}