package me.levitate.quill.utils.bukkit;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

public class ParticleHelper {
    public static void spawnParticle(Location location, Particle particle, int count, double offsetX, double offsetY, double offsetZ) {
        location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ);
    }

    public static void spawnParticleCircle(Location center, Particle particle, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location point = new Location(center.getWorld(), x, center.getY(), z);
            spawnParticle(point, particle, 1, 0, 0, 0);
        }
    }

    public static void spawnParticleLine(Location start, Location end, Particle particle, double spacing) {
        double distance = start.distance(end);
        Vector direction = end.toVector().subtract(start.toVector()).normalize();

        for (double d = 0; d <= distance; d += spacing) {
            Vector vec = direction.clone().multiply(d);
            Location point = start.clone().add(vec);
            spawnParticle(point, particle, 1, 0, 0, 0);
        }
    }
}