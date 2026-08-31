package dev.chernykh.unseenLight.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.chernykh.unseenLight.Permissions;
import dev.chernykh.unseenLight.UnseenLight;
import dev.chernykh.unseenLight.config.Messages;
import dev.chernykh.unseenLight.light.LightHighlighter;
import dev.chernykh.unseenLight.light.LightItems;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /unseenlight reload | give <targets> [level] [amount] | show} */
public final class UnseenLightCommand {

    private UnseenLightCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> build(UnseenLight plugin) {
        return Commands.literal("unseenlight")
                .requires(source -> Permissions.ALL_COMMANDS.stream().anyMatch(source.getSender()::hasPermission))
                .then(Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission(Permissions.COMMAND_RELOAD))
                        .executes(context -> {
                            boolean parsed = plugin.reload();
                            plugin.config().messages().send(context.getSource().getSender(),
                                    parsed ? "config-reloaded" : "config-load-failed");
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("give")
                        .requires(source -> source.getSender().hasPermission(Permissions.COMMAND_GIVE))
                        .then(Commands.argument("targets", ArgumentTypes.players())
                                .executes(context -> give(plugin, context, plugin.config().lightLevel(), 1))
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, 15))
                                        .executes(context -> give(plugin, context,
                                                IntegerArgumentType.getInteger(context, "level"), 1))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                                .executes(context -> give(plugin, context,
                                                        IntegerArgumentType.getInteger(context, "level"),
                                                        IntegerArgumentType.getInteger(context, "amount")))))))
                .then(Commands.literal("show")
                        .requires(source -> source.getSender().hasPermission(Permissions.COMMAND_SHOW))
                        .executes(context -> show(plugin, context)))
                .build();
    }

    private static int give(UnseenLight plugin, CommandContext<CommandSourceStack> context, int level, int amount)
            throws CommandSyntaxException {
        List<Player> targets = context.getArgument("targets", PlayerSelectorArgumentResolver.class)
                .resolve(context.getSource());
        Messages messages = plugin.config().messages();
        CommandSender sender = context.getSource().getSender();

        if (targets.isEmpty()) {
            messages.send(sender, "no-targets");
            return 0;
        }
        for (Player target : targets) {
            target.give(LightItems.create(level, amount));
            messages.send(sender, "light-given",
                    Placeholder.unparsed("amount", String.valueOf(amount)),
                    Placeholder.unparsed("level", String.valueOf(level)),
                    Placeholder.unparsed("player", target.getName()));
        }
        return targets.size();
    }

    private static int show(UnseenLight plugin, CommandContext<CommandSourceStack> context) {
        Messages messages = plugin.config().messages();
        CommandSender sender = context.getSource().getSender();

        if (!(context.getSource().getExecutor() instanceof Player player)) {
            messages.send(sender, "players-only");
            return 0;
        }
        if (!plugin.config().highlight().enabled()) {
            messages.send(sender, "highlight-disabled");
            return 0;
        }

        int found = LightHighlighter.show(plugin, player);
        if (found == 0) {
            messages.send(sender, "highlight-empty");
        } else {
            messages.send(sender, "highlight-found",
                    Placeholder.unparsed("count", String.valueOf(found)),
                    Placeholder.unparsed("radius", String.valueOf(plugin.config().highlight().radius())));
        }
        return found;
    }
}
