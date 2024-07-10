package com.featherservices.quill.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Objects;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SerializableLocation {
    private String world;
    private double x;
    private double y;
    private double z;

    public static SerializableLocation fromLocation(Location location) {
        return new SerializableLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }

    public static Location toLocation(SerializableLocation location) {
        return new Location(Bukkit.getWorld(location.getWorld()), location.getX(), location.getY(), location.getZ());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SerializableLocation that = (SerializableLocation) o;
        return Double.compare(x, that.x) == 0 && Double.compare(y, that.y) == 0 && Double.compare(z, that.z) == 0 && Objects.equals(world, that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z);
    }
}
