package dev.chernykh.unseenLight.listener;

import dev.chernykh.unseenLight.Permissions;
import dev.chernykh.unseenLight.UnseenLight;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/** Recipe book bookkeeping and the crafting permission gates. */
public final class RecipeListener implements Listener {

    private final UnseenLight plugin;

    public RecipeListener(UnseenLight plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.recipes().syncDiscovery(event.getPlayer());
    }

    /** A datapack reload (/minecraft:reload) rebuilds the recipe manager and drops plugin recipes. */
    @EventHandler
    public void onServerResourcesReloaded(ServerResourcesReloadedEvent event) {
        plugin.recipes().register();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!plugin.recipes().isOurs(event.getRecipe())) {
            return;
        }
        HumanEntity crafter = event.getView().getPlayer();
        if (!crafter.hasPermission(Permissions.CRAFT)) {
            event.getInventory().setResult(null);
        }
    }

    /** Crafter blocks craft without a player, so no permission can be checked there. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrafterCraft(CrafterCraftEvent event) {
        if (!plugin.config().recipe().allowCrafter() && plugin.recipes().isOurs(event.getRecipe())) {
            event.setCancelled(true);
        }
    }
}
