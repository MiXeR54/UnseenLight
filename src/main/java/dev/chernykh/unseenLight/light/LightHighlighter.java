package dev.chernykh.unseenLight.light;

import dev.chernykh.unseenLight.UnseenLight;
import dev.chernykh.unseenLight.config.PluginConfig;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Marks nearby invisible light blocks with particles, for the player who asked only. */
public final class LightHighlighter {

    private static final int MAX_LIGHTS = 1000;

    /** One highlight per player: a repeated /ul show restarts instead of stacking tasks. */
    private static final Map<UUID, ScheduledTask> ACTIVE = new HashMap<>();

    private LightHighlighter() {
    }

    /** @return how many light blocks were found around the player. */
    public static int show(UnseenLight plugin, Player player) {
        PluginConfig.Highlight settings = plugin.config().highlight();
        World world = player.getWorld();
        List<Location> lights = scan(player, settings.radius());
        if (lights.isEmpty()) {
            return 0;
        }

        UUID id = player.getUniqueId();
        ScheduledTask previous = ACTIVE.remove(id);
        if (previous != null) {
            previous.cancel();
        }

        int period = settings.periodTicks();
        AtomicInteger repeatsLeft = new AtomicInteger(Math.max(1, settings.durationSeconds() * 20 / period));
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, current -> {
            if (!world.equals(player.getWorld()) || repeatsLeft.decrementAndGet() < 0) {
                current.cancel();
                ACTIVE.remove(id, current);
                return;
            }
            for (Location light : lights) {
                player.spawnParticle(Particle.END_ROD, light, 4, 0.15, 0.15, 0.15, 0.0);
            }
        }, () -> ACTIVE.remove(id), 1L, period);

        if (task != null) {
            ACTIVE.put(id, task);
        }
        return lights.size();
    }

    private static List<Location> scan(Player player, int radius) {
        World world = player.getWorld();
        Block center = player.getLocation().getBlock();
        int minY = Math.max(world.getMinHeight(), center.getY() - radius);
        int maxY = Math.min(world.getMaxHeight() - 1, center.getY() + radius);

        List<Location> found = new ArrayList<>();
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;   // never sync-load terrain just to draw particles
                }
                for (int y = minY; y <= maxY; y++) {
                    if (world.getBlockAt(x, y, z).getType() == Material.LIGHT) {
                        found.add(new Location(world, x + 0.5, y + 0.5, z + 0.5));
                        if (found.size() >= MAX_LIGHTS) {
                            return found;
                        }
                    }
                }
            }
        }
        return found;
    }
}
