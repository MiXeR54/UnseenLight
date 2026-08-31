package dev.chernykh.unseenLight.config;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/** Player facing messages, written in MiniMessage and overridable from config.yml. */
public final class Messages {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String DEFAULT_PREFIX = "<gray>[</gray><gold>UnseenLight</gold><gray>]</gray> ";

    private static final Map<String, String> DEFAULTS = Map.of(
            "no-permission", "<red>You are not allowed to do that.",
            "light-removed", "<gray>Invisible light removed.",
            "light-given", "<gray>Gave <white><amount></white> light of level <white><level></white> to <white><player></white>",
            "no-targets", "<red>No players matched the selector.",
            "config-reloaded", "<green>Configuration reloaded.",
            "config-load-failed", "<red>config.yml is empty or invalid, built-in defaults are in effect. Check the console.",
            "highlight-found", "<gray>Highlighted <white><count></white> light sources within <white><radius></white> blocks.",
            "highlight-empty", "<gray>No invisible light nearby.",
            "highlight-disabled", "<red>Highlighting is disabled in the configuration.",
            "players-only", "<red>This command can only be used by a player.");

    private final String prefix;
    private final Map<String, String> values;

    private Messages(String prefix, Map<String, String> values) {
        this.prefix = prefix;
        this.values = values;
    }

    public static Messages load(@Nullable ConfigurationSection section) {
        Map<String, String> values = new HashMap<>(DEFAULTS);
        if (section == null) {
            return new Messages(DEFAULT_PREFIX, values);
        }
        for (String key : section.getKeys(false)) {
            if (!key.equals("prefix")) {
                values.put(key, section.getString(key, ""));
            }
        }
        return new Messages(section.getString("prefix", DEFAULT_PREFIX), values);
    }

    /** Sends the message, unless it was blanked out in the config. */
    public void send(CommandSender receiver, String key, TagResolver... placeholders) {
        String raw = values.getOrDefault(key, "");
        if (raw.isBlank()) {
            return;
        }
        receiver.sendMessage(MINI_MESSAGE.deserialize(prefix + raw, placeholders));
    }
}
