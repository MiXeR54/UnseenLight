package dev.chernykh.unseenLight.listener;

import dev.chernykh.unseenLight.Permissions;
import dev.chernykh.unseenLight.UnseenLight;
import dev.chernykh.unseenLight.config.PluginConfig;
import dev.chernykh.unseenLight.light.Effects;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

/** Placing a light block is vanilla behaviour; this only gates it and adds feedback. */
public final class LightPlacementListener implements Listener {

    private final UnseenLight plugin;

    public LightPlacementListener(UnseenLight plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.LIGHT || !event.canBuild()) {
            return;   // canBuild=false placements (spawn protection) are reverted by vanilla
        }

        PluginConfig.Placement settings = plugin.config().placement();
        Player player = event.getPlayer();
        if (settings.checkPermission() && !player.hasPermission(Permissions.PLACE)) {
            event.setCancelled(true);
            plugin.config().messages().send(player, "no-permission");
        }
    }

    /** Runs last, so nothing is heard or seen unless the placement actually goes through. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaced(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.LIGHT || !event.canBuild()) {
            return;
        }

        PluginConfig.Placement settings = plugin.config().placement();
        Effects.play(event.getBlockPlaced().getLocation(), settings.sound(),
                settings.particles() ? Particle.END_ROD : null);
    }
}
