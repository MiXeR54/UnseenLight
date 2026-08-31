package dev.chernykh.unseenLight;

import java.util.List;

/** Permission nodes. Must stay in sync with the permissions section of plugin.yml. */
public final class Permissions {

    public static final String CRAFT = "unseenlight.craft";
    public static final String PLACE = "unseenlight.place";
    public static final String REMOVE = "unseenlight.remove";
    public static final String COMMAND_RELOAD = "unseenlight.command.reload";
    public static final String COMMAND_GIVE = "unseenlight.command.give";
    public static final String COMMAND_SHOW = "unseenlight.command.show";

    /** Holding any of these makes the /unseenlight root visible. */
    public static final List<String> ALL_COMMANDS = List.of(COMMAND_RELOAD, COMMAND_GIVE, COMMAND_SHOW);

    private Permissions() {
    }
}
