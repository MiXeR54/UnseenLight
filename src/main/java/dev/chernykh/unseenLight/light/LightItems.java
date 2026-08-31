package dev.chernykh.unseenLight.light;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.Light;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockDataMeta;

import java.util.concurrent.atomic.AtomicBoolean;

/** Builds the vanilla {@code minecraft:light} item with a fixed light level baked in. */
public final class LightItems {

    private static final AtomicBoolean FALLBACK_WARNED = new AtomicBoolean();

    private LightItems() {
    }

    public static ItemStack create(int level, int amount) {
        int clamped = Math.clamp(level, 0, 15);
        ItemStack stack;
        try {
            // Same shape as the vanilla creative item: only the level property. Going through
            // BlockDataMeta would also bake in waterlogged=false, which then overrides the
            // waterlogging the block gets when it is placed into water.
            stack = Bukkit.getItemFactory()
                    .createItemStack("minecraft:light[block_state={level:\"" + clamped + "\"}]");
        } catch (IllegalArgumentException exception) {
            // Only reachable if a future server build changes the component string grammar.
            if (FALLBACK_WARNED.compareAndSet(false, true)) {
                Bukkit.getLogger().warning("[UnseenLight] Falling back to BlockDataMeta for the light item ("
                        + exception.getMessage() + "); such items do not stack with previously created ones.");
            }
            stack = new ItemStack(Material.LIGHT);
            stack.editMeta(BlockDataMeta.class, meta -> {
                Light data = (Light) Material.LIGHT.createBlockData();
                data.setLevel(clamped);
                meta.setBlockData(data);
            });
        }
        stack.setAmount(amount);
        return stack;
    }

    /** The item that a placed light block turns back into, keeping its light level. */
    public static ItemStack fromPlaced(BlockData placed) {
        return create(placed instanceof Levelled levelled ? levelled.getLevel() : 15, 1);
    }
}
