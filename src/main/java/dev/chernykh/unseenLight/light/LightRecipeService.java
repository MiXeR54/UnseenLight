package dev.chernykh.unseenLight.light;

import dev.chernykh.unseenLight.Permissions;
import dev.chernykh.unseenLight.UnseenLight;
import dev.chernykh.unseenLight.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.jetbrains.annotations.Nullable;

/** Owns the single crafting recipe this plugin adds. */
public final class LightRecipeService {

    private final UnseenLight plugin;
    private final NamespacedKey key;

    public LightRecipeService(UnseenLight plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "unseen_light");
    }

    /** Asks the live registry, so it stays truthful across datapack reloads. */
    public boolean isRegistered() {
        return Bukkit.getRecipe(key) != null;
    }

    /** Re-registers the recipe from the current config. Safe to call again on reload. */
    public void register() {
        unregister();

        PluginConfig config = plugin.config();
        PluginConfig.Recipe settings = config.recipe();
        if (!settings.enabled()) {
            return;
        }

        try {
            ShapedRecipe recipe = new ShapedRecipe(key, LightItems.create(config.lightLevel(), settings.amount()));
            recipe.shape(settings.shape().toArray(String[]::new));
            recipe.setCategory(CraftingBookCategory.BUILDING);
            recipe.setGroup("unseen_light");

            String usedSymbols = String.join("", settings.shape());
            settings.ingredients().forEach((symbol, material) -> {
                // Bukkit rejects ingredients for symbols the shape never uses.
                if (usedSymbols.indexOf(symbol) >= 0) {
                    recipe.setIngredient(symbol, material);
                }
            });

            Bukkit.addRecipe(recipe, true);
        } catch (RuntimeException exception) {
            // A config-driven registration must never abort onEnable or /unseenlight reload.
            plugin.getLogger().warning("Could not register the crafting recipe: " + exception.getMessage());
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            syncDiscovery(player);
        }
    }

    public void unregister() {
        Bukkit.removeRecipe(key, true);
    }

    /** Discovery is additive: the plugin unlocks the recipe but never revokes what others granted. */
    public void syncDiscovery(Player player) {
        if (isRegistered() && plugin.config().recipe().autoDiscover() && player.hasPermission(Permissions.CRAFT)) {
            player.discoverRecipe(key);
        }
    }

    public boolean isOurs(@Nullable Recipe recipe) {
        return recipe instanceof Keyed keyed && key.equals(keyed.getKey());
    }
}
