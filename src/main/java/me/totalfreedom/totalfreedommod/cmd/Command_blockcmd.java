package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import me.totalfreedom.api.cmd.FCommand;
import me.totalfreedom.api.cmd.annotation.*;
import me.totalfreedom.totalfreedommod.player.PlayerData;

@Command(name = "blockcmd", description = "Block all commands for a specific player.", usage = "/<command> <-a | purge | <player>>", aliases = {"blockcommands", "blockcommand", "bc", "bcmd"})
@Permission(permission = "tfm.admin.blockcmd")
public class Command_blockcmd extends FCommand
{
    @Callback
    public void blockPlayer(final CommandSender sender, final Player player)
    {
        if (isAdmin(player))
        {
            msg(sender, "<gray><player> is an admin, and cannot have their commands blocked.",
                    Placeholder.unparsed("player", player.getName()));
            return;
        }

        final PlayerData playerdata = plugin().players().getData(player);

        adminAction(sender, "<red><blocked:Unb:B>locking all commands for <player>",
                Formatter.booleanChoice("blocked", playerdata.isCommandsBlocked()),
                Placeholder.unparsed("player", player.getName()));

        playerdata.setCommandsBlocked(!playerdata.isCommandsBlocked());

        // Only blocking counts as a punishment; unblocking doesn't add points.
        if (playerdata.isCommandsBlocked())
        {
            msg(player, "<red>Your commands have been blocked by an admin.");
            plugin().database().recordPunishment(player, sender, "BLOCKCMD", null, null, "manual-blockcmd");
        }

        msg(sender, "<gray><blocked:B:Unb>locked all commands for <player>.",
                Formatter.booleanChoice("blocked", playerdata.isCommandsBlocked()),
                Placeholder.unparsed("player", player.getName()));
    }

    @Callback
    @Subcommand("-a") // This one is also not fit for a switch, but this case is due to the way handlers are implemented
    public void blockAll(final CommandSender sender)
    {
        adminAction(sender, "<red>Blocking commands for all non-admins");

        final long count = server().getOnlinePlayers()
                .stream()
                .filter(p -> !isAdmin(p))
                .peek(p -> {
                    plugin().players().getData(p).setCommandsBlocked(true);
                    msg(p, "<red>Your commands have been blocked by an admin.");
                })
                .count();

        msg(sender, "<gray>Blocked commands for <count> player<plural>.",
                Formatter.number("count", count),
                Placeholder.unparsed("plural", count == 1 ? "" : "s"));
    }

    @Callback
    @Subcommand("purge")
    public void purge(final CommandSender sender)
    {
        adminAction(sender, "<red>Unblocking commands for all players");

        final long count = server().getOnlinePlayers()
              .stream()
              .map(plugin().players()::getData)
              .filter(PlayerData::isCommandsBlocked)
              .reduce(0L, (total, data) ->
                    {
                        data.setCommandsBlocked(false);
                        return total + 1;
                    }, Long::sum);

        msg(sender, "<gray>Unblocked commands for <count> player<plural>.",
                Formatter.number("count", count),
                Placeholder.unparsed("plural", count == 1 ? "" : "s"));
    }
}
