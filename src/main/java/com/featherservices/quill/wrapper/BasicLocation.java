package com.featherservices.quill.wrapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Objects;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BasicLocation {
    private String world;
    private double x;
    private double y;
    private double z;

    public static BasicLocation fromLocation(Location location) {
        return new BasicLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }

    public static Location toLocation(BasicLocation location) {
        return new Location(Bukkit.getWorld(location.getWorld()), location.getX(), location.getY(), location.getZ());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicLocation that = (BasicLocation) o;
        return Double.compare(x, that.x) == 0 && Double.compare(y, that.y) == 0 && Double.compare(z, that.z) == 0 && Objects.equals(world, that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z);
    }
}
