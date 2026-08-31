package dev.chernykh.unseenLight.config;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/** Snapshot of config.yml, rebuilt from scratch on every reload. */
public record PluginConfig(
        int lightLevel,
        Recipe recipe,
        Placement placement,
        Removal removal,
        Highlight highlight,
        Messages messages
) {

    public record Recipe(boolean enabled, List<String> shape, Map<Character, Material> ingredients,
                         int amount, boolean autoDiscover, boolean allowCrafter) {

        static Recipe defaults(boolean enabled, int amount, boolean autoDiscover, boolean allowCrafter) {
            return new Recipe(enabled, List.of("PPP", "PTP", "PPP"),
                    Map.of('P', Material.GLASS_PANE, 'T', Material.TORCH), amount, autoDiscover, allowCrafter);
        }

        /** @return the reason this recipe is unusable, or {@code null} when it is fine. */
        @Nullable String problem() {
            if (shape.isEmpty() || shape.size() > 3) {
                return "shape must have 1 to 3 rows";
            }
            int width = shape.getFirst().length();
            if (width == 0 || width > 3) {
                return "every shape row must have 1 to 3 characters";
            }
            boolean empty = true;
            for (String row : shape) {
                if (row.length() != width) {
                    return "all shape rows must be the same length";
                }
                for (char symbol : row.toCharArray()) {
                    if (symbol != ' ' && !ingredients.containsKey(symbol)) {
                        return "symbol " + symbol + " is missing from the ingredients section";
                    }
                    empty &= symbol == ' ';
                }
            }
            return empty ? "shape needs at least one ingredient" : null;
        }
    }

    public record Placement(boolean checkPermission, @Nullable Sound sound, boolean particles) { }

    public record Removal(boolean enabled, DropMode dropMode, boolean requireBlockInHand, boolean requireSneak,
                          @Nullable Sound sound, boolean particles) { }

    public record Highlight(boolean enabled, int radius, int durationSeconds, int periodTicks) { }

    /** What happens to a light block that a player takes down. */
    public enum DropMode { GROUND, INVENTORY, DESTROY }

    public static PluginConfig load(Plugin plugin) {
        FileConfiguration config = plugin.getConfig();
        Logger log = plugin.getLogger();
        return new PluginConfig(
                Math.clamp(config.getInt("light-level", 15), 0, 15),
                readRecipe(section(config, "recipe"), log),
                readPlacement(section(config, "placement"), log),
                readRemoval(section(config, "removal"), log),
                readHighlight(section(config, "highlight")),
                Messages.load(config.getConfigurationSection("messages")));
    }

    /** The two-arg getters below carry the defaults; a missing section just means "all defaults". */
    private static ConfigurationSection section(FileConfiguration config, String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        return section != null ? section : new MemoryConfiguration();
    }

    private static Recipe readRecipe(ConfigurationSection section, Logger log) {
        boolean enabled = section.getBoolean("enabled", true);
        int amount = Math.clamp(section.getInt("result-amount", 1), 1, 64);
        boolean autoDiscover = section.getBoolean("auto-discover", true);
        boolean allowCrafter = section.getBoolean("allow-crafter", true);

        Map<Character, Material> ingredients = new LinkedHashMap<>();
        ConfigurationSection ingredientSection = section.getConfigurationSection("ingredients");
        if (ingredientSection != null) {
            for (String key : ingredientSection.getKeys(false)) {
                String rawMaterial = ingredientSection.getString(key, "");
                Material material = Material.matchMaterial(rawMaterial);
                if (key.isBlank() || key.length() != 1) {
                    log.warning("recipe.ingredients: key '" + key + "' must be a single non-space character, skipped.");
                } else if (material == null || !material.isItem() || material.isAir()) {
                    // AIR passes isItem() but RecipeChoice rejects it with an exception.
                    log.warning("recipe.ingredients." + key + ": '" + rawMaterial + "' is not a craftable item, skipped.");
                } else {
                    ingredients.put(key.charAt(0), material);
                }
            }
        }

        Recipe recipe = new Recipe(enabled, List.copyOf(section.getStringList("shape")),
                Map.copyOf(ingredients), amount, autoDiscover, allowCrafter);
        String problem = recipe.problem();
        if (problem == null) {
            return recipe;
        }
        log.warning("Invalid recipe in config.yml (" + problem + "), falling back to the default one.");
        return Recipe.defaults(enabled, amount, autoDiscover, allowCrafter);
    }

    private static Placement readPlacement(ConfigurationSection section, Logger log) {
        return new Placement(section.getBoolean("check-permission", true),
                readSound(section, "sound", Sound.BLOCK_AMETHYST_BLOCK_CHIME, log),
                section.getBoolean("particles", true));
    }

    private static Removal readRemoval(ConfigurationSection section, Logger log) {
        return new Removal(section.getBoolean("enabled", true),
                readDropMode(section.getString("drop-mode", "GROUND"), log),
                section.getBoolean("require-block-in-hand", true),
                section.getBoolean("require-sneak", false),
                readSound(section, "sound", Sound.BLOCK_GLASS_BREAK, log),
                section.getBoolean("particles", true));
    }

    private static Highlight readHighlight(ConfigurationSection section) {
        return new Highlight(section.getBoolean("enabled", true),
                Math.clamp(section.getInt("radius", 16), 1, 32),
                Math.clamp(section.getInt("duration-seconds", 10), 1, 120),
                Math.clamp(section.getInt("period-ticks", 10), 1, 100));
    }

    private static DropMode readDropMode(String raw, Logger log) {
        try {
            return DropMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            log.warning("removal.drop-mode: unknown value '" + raw + "', using GROUND.");
            return DropMode.GROUND;
        }
    }

    /**
     * Accepts both the vanilla key (block.glass.break) and the constant name (BLOCK_GLASS_BREAK).
     * A missing key keeps the built-in default; an explicitly blank value mutes the sound.
     */
    private static @Nullable Sound readSound(ConfigurationSection section, String path,
                                             @Nullable Sound fallback, Logger log) {
        if (!section.isSet(path)) {
            return fallback;
        }
        String value = section.getString(path, "").trim();
        if (value.isEmpty()) {
            return null;
        }

        Registry<Sound> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT);
        NamespacedKey key = NamespacedKey.fromString(value.toLowerCase(Locale.ROOT));
        Sound sound = key == null ? null : registry.get(key);
        if (sound != null) {
            return sound;
        }
        // BLOCK_GLASS_BREAK is how the key block.glass.break reads as a Sound constant.
        String wanted = value.toUpperCase(Locale.ROOT);
        for (Sound candidate : registry) {
            NamespacedKey candidateKey = registry.getKey(candidate);
            if (candidateKey != null
                    && wanted.equals(candidateKey.getKey().replace('.', '_').toUpperCase(Locale.ROOT))) {
                return candidate;
            }
        }

        log.warning(section.getName() + "." + path + ": unknown sound '" + value + "', muted.");
        return null;
    }
}
