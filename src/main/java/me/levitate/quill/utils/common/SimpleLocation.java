package me.levitate.quill.utils.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Objects;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SimpleLocation {
    private String world;
    private double x;
    private double y;
    private double z;

    public static SimpleLocation fromLocation(Location location) {
        return new SimpleLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }

    public static Location toLocation(SimpleLocation location) {
        return new Location(Bukkit.getWorld(location.getWorld()), location.getX(), location.getY(), location.getZ());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleLocation that = (SimpleLocation) o;
        return Double.compare(x, that.x) == 0 && Double.compare(y, that.y) == 0 && Double.compare(z, that.z) == 0 && Objects.equals(world, that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z);
    }
}
