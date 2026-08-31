package dev.chernykh.unseenLight.listener;

import dev.chernykh.unseenLight.Permissions;
import dev.chernykh.unseenLight.UnseenLight;
import dev.chernykh.unseenLight.config.PluginConfig;
import dev.chernykh.unseenLight.light.Effects;
import dev.chernykh.unseenLight.light.LightItems;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Every way a placed light block can be taken down or lost.
 * <p>
 * The primary gesture matches the reference server: a light block has no collision or outline
 * shape, so a right click aimed at its spot hits the block behind it, and the spot itself is where
 * the held block would go. On top of that sit safety nets for each vanilla mechanic that would
 * otherwise overwrite the (replaceable) light silently: block placement, lava and powder snow
 * buckets, falling blocks, and the vanilla break available while holding a light item.
 */
public final class LightRemovalListener implements Listener {

    /** The one right click both hands report: remember what it already did this tick. */
    private record HandledClick(int tick, Block target) { }

    private final UnseenLight plugin;
    private final Map<UUID, HandledClick> handledClicks = new HashMap<>();
    private boolean firingSyntheticBreak;

    public LightRemovalListener(UnseenLight plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }
        // Vanilla places INTO a replaceable clicked block (grass, snow) and next to anything else.
        Block target = clicked.isReplaceable() ? clicked : clicked.getRelative(event.getBlockFace());

        Player player = event.getPlayer();
        // One right click fires once per hand. Without this the off hand would happily drop its
        // block into the spot the main hand has just cleared.
        HandledClick handled = handledClicks.get(player.getUniqueId());
        if (handled != null && handled.tick() == Bukkit.getCurrentTick() && target.equals(handled.target())) {
            event.setCancelled(true);
            return;
        }

        if (target.getType() != Material.LIGHT) {
            return;
        }

        PluginConfig.Removal settings = plugin.config().removal();
        if (!settings.enabled()) {
            return;   // onPlaceIntoLight still keeps the light from being overwritten
        }
        GameMode gameMode = player.getGameMode();
        if (gameMode == GameMode.ADVENTURE || gameMode == GameMode.SPECTATOR) {
            return;
        }

        ItemStack inHand = event.getItem();
        // With a light item in hand vanilla already renders light blocks and lets you break them.
        if (inHand != null && inHand.getType() == Material.LIGHT) {
            return;
        }
        if (settings.requireBlockInHand() && (inHand == null || !inHand.getType().isBlock())) {
            return;
        }
        if (settings.requireSneak() && !player.isSneaking()) {
            return;
        }
        // Chests, doors and the like open on a plain right click in vanilla too -- only a sneaking
        // player is aiming at the block space in front of them. Pseudo-interactive blocks (fences)
        // let the placement through instead, and onPlaceIntoLight converts it into a removal.
        if (!player.isSneaking() && isInteractable(clicked.getType())) {
            return;
        }

        event.setCancelled(true);
        handledClicks.put(player.getUniqueId(), new HandledClick(Bukkit.getCurrentTick(), target));
        attemptRemoval(player, target);
    }

    /**
     * The net under the interact heuristic: no placement may overwrite a light silently, whatever
     * path it took (fence faces, replaceable clicks, an off hand straddling a tick, creative).
     * The held block never goes through -- the light either pops properly or stays protected.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlaceIntoLight(BlockPlaceEvent event) {
        if (event.getBlockReplacedState().getType() != Material.LIGHT) {
            return;
        }
        if (!event.canBuild()) {
            return;   // vanilla reverts this placement anyway
        }
        event.setCancelled(true);

        Player player = event.getPlayer();
        PluginConfig.Removal settings = plugin.config().removal();
        if (!settings.enabled()
                || player.getGameMode() == GameMode.ADVENTURE
                || (settings.requireSneak() && !player.isSneaking())) {
            return;   // cancelled placement restores the light: protected
        }

        Block target = event.getBlock();
        handledClicks.put(player.getUniqueId(), new HandledClick(Bukkit.getCurrentTick(), target));
        // The cancelled event restores the replaced light only after all handlers ran, so the
        // actual removal has to wait until the next tick.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (target.getType() == Material.LIGHT) {
                attemptRemoval(player, target);
            }
        });
    }

    /** Lava and powder snow buckets replace the light block; water merely waterlogs it. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketIntoLight(PlayerBucketEmptyEvent event) {
        if (event.getBlock().getType() != Material.LIGHT) {
            return;
        }
        Material bucket = event.getBucket();
        if (bucket != Material.LAVA_BUCKET && bucket != Material.POWDER_SNOW_BUCKET) {
            return;
        }
        event.setCancelled(true);

        PluginConfig.Removal settings = plugin.config().removal();
        Player player = event.getPlayer();
        if (!settings.enabled()
                || player.getGameMode() == GameMode.ADVENTURE
                || (settings.requireSneak() && !player.isSneaking())) {
            return;
        }
        attemptRemoval(player, event.getBlock());
    }

    /** A falling block lands in the light's spot: vanilla replaces it, we compensate the item. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFallingBlockIntoLight(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock) || event.getBlock().getType() != Material.LIGHT) {
            return;
        }
        if (plugin.config().removal().dropMode() == PluginConfig.DropMode.DESTROY) {
            return;
        }
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().toCenterLocation(),
                LightItems.fromPlaced(event.getBlock().getBlockData()));
    }

    /**
     * The vanilla break path: while holding a light item, light blocks are visible and breakable,
     * but they have no loot table -- honour drop-mode there too. The drop waits a tick so that a
     * foreign synthetic BlockBreakEvent (a protection plugin probing) cannot conjure items.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVanillaBreak(BlockBreakEvent event) {
        if (firingSyntheticBreak || event.getBlock().getType() != Material.LIGHT) {
            return;
        }
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        Block block = event.getBlock();
        ItemStack light = LightItems.fromPlaced(block.getBlockData());
        PluginConfig.Removal settings = plugin.config().removal();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (block.getType() == Material.LIGHT) {
                return;   // the break did not actually happen
            }
            giveBack(player, block, light, settings.dropMode());
            Effects.play(block.getLocation(), settings.sound(), settings.particles() ? Particle.SMOKE : null);
            plugin.config().messages().send(player, "light-removed");
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        handledClicks.remove(event.getPlayer().getUniqueId());
    }

    /** Permission gate, protection check via a synthetic break event, then clear and give back. */
    private void attemptRemoval(Player player, Block target) {
        if (!player.hasPermission(Permissions.REMOVE)) {
            plugin.config().messages().send(player, "no-permission");
            return;
        }

        BlockData placed = target.getBlockData();
        BlockBreakEvent breakEvent = new BlockBreakEvent(target, player);
        breakEvent.setDropItems(false);
        firingSyntheticBreak = true;
        try {
            if (!breakEvent.callEvent()) {
                return;   // land protection said no; the light stays
            }
        } finally {
            firingSyntheticBreak = false;
        }

        boolean waterlogged = placed instanceof Waterlogged waterloggedData && waterloggedData.isWaterlogged();
        target.setType(waterlogged ? Material.WATER : Material.AIR, true);

        PluginConfig.Removal settings = plugin.config().removal();
        if (player.getGameMode() != GameMode.CREATIVE) {
            giveBack(player, target, LightItems.fromPlaced(placed), settings.dropMode());
        }
        Effects.play(target.getLocation(), settings.sound(), settings.particles() ? Particle.SMOKE : null);
        plugin.config().messages().send(player, "light-removed");
    }

    /** Deprecated in Bukkit for being approximate, but there is no replacement for it yet. */
    @SuppressWarnings("deprecation")
    private static boolean isInteractable(Material material) {
        return material.isInteractable();
    }

    private void giveBack(Player player, Block target, ItemStack light, PluginConfig.DropMode mode) {
        switch (mode) {
            case GROUND -> target.getWorld().dropItemNaturally(target.getLocation().toCenterLocation(), light);
            case INVENTORY -> player.give(light);   // drops leftovers at the feet, vanilla style
            case DESTROY -> {
            }
        }
    }
}
