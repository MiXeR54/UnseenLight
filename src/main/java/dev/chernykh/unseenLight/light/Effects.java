package dev.chernykh.unseenLight.light;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

/** Feedback for actions on a block nobody can see. */
public final class Effects {

    private Effects() {
    }

    public static void play(Location blockLocation, @Nullable Sound sound, @Nullable Particle particle) {
        Location center = blockLocation.toCenterLocation();
        World world = center.getWorld();
        if (sound != null) {
            world.playSound(center, sound, 0.7f, 1.4f);
        }
        if (particle != null) {
            world.spawnParticle(particle, center, 8, 0.2, 0.2, 0.2, 0.01);
        }
    }
}
