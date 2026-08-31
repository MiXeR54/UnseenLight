package dev.chernykh.unseenLight;

import dev.chernykh.unseenLight.command.UnseenLightCommand;
import dev.chernykh.unseenLight.config.PluginConfig;
import dev.chernykh.unseenLight.light.LightRecipeService;
import dev.chernykh.unseenLight.listener.LightPlacementListener;
import dev.chernykh.unseenLight.listener.LightRemovalListener;
import dev.chernykh.unseenLight.listener.RecipeListener;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class UnseenLight extends JavaPlugin {

    /** bstats.org plugin id; 0 keeps metrics off. https://bstats.org/plugin/bukkit/UnseenLight/33769 */
    private static final int BSTATS_PLUGIN_ID = 33769;

    private PluginConfig config;
    private LightRecipeService recipes;
    private Metrics metrics;
    private boolean commandsRegistered;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.config = PluginConfig.load(this);
        this.recipes = new LightRecipeService(this);
        this.recipes.register();

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new LightPlacementListener(this), this);
        pluginManager.registerEvents(new LightRemovalListener(this), this);
        pluginManager.registerEvents(new RecipeListener(this), this);

        registerCommands();
        startMetrics();
    }

    @Override
    public void onDisable() {
        if (metrics != null) {
            metrics.shutdown();
            metrics = null;
        }
        if (recipes != null) {
            recipes.unregister();
        }
    }

    public PluginConfig config() {
        return config;
    }

    public LightRecipeService recipes() {
        return recipes;
    }

    /** @return false when config.yml could not be parsed and built-in defaults are in effect. */
    public boolean reload() {
        PluginConfig previous = this.config;
        reloadConfig();
        boolean parsed = !getConfig().getKeys(false).isEmpty();
        if (!parsed) {
            getLogger().warning("config.yml is empty or invalid, using built-in defaults (see the error above).");
        }
        this.config = PluginConfig.load(this);

        // Re-registering broadcasts the recipe set to everyone online, so skip it when unchanged.
        if (previous == null || previous.lightLevel() != config.lightLevel()
                || !previous.recipe().equals(config.recipe()) || !recipes.isRegistered()) {
            recipes.register();
        }
        return parsed;
    }

    @SuppressWarnings("ConstantValue")   // BSTATS_PLUGIN_ID stays 0 until the bstats.org registration
    private void startMetrics() {
        if (BSTATS_PLUGIN_ID == 0) {
            getLogger().info("bStats is off: set BSTATS_PLUGIN_ID after registering the plugin at bstats.org.");
            return;
        }
        metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        metrics.addCustomChart(new SimplePie("drop_mode", () -> config.removal().dropMode().name()));
        metrics.addCustomChart(new SimplePie("light_level", () -> String.valueOf(config.lightLevel())));
    }

    private void registerCommands() {
        if (commandsRegistered) {
            return;
        }
        try {
            getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                    event.registrar().register(UnseenLightCommand.build(this),
                            "Manage the UnseenLight plugin", List.of("ul")));
            commandsRegistered = true;
        } catch (IllegalStateException exception) {
            // A plugin-manager disable/enable cycle re-runs onEnable after lifecycle registration
            // has closed; the commands registered by the first enable are still in place.
            getLogger().warning("Skipped command registration: " + exception.getMessage());
        }
    }
}
