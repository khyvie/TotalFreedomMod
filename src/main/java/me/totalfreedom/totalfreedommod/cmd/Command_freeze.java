package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.freeze.Freezer;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import me.totalfreedom.api.cmd.FCommand;
import me.totalfreedom.api.cmd.annotation.Callback;
import me.totalfreedom.api.cmd.annotation.Command;
import me.totalfreedom.api.cmd.annotation.Permission;
import me.totalfreedom.api.cmd.annotation.Resolve;
import me.totalfreedom.api.cmd.annotation.Subcommand;

@Command(name = "freeze", description = "Freeze players. Append \"on\" or \"off\" at the end to set a specific state.",
        usage = "/freeze <[on | off] | <player> [on | off]>", aliases = {"fr"})
@Permission(permission = "tfm.admin.freeze")
public class Command_freeze extends FCommand
{

    @Callback
    public void toggleGlobalFreeze(CommandSender sender)
    {
        setGlobalFreeze(sender, !plugin().services().require(Freezer.class).isGlobalFreeze());
    }

    @Callback
    @Subcommand("on")
    public void enableGlobalFreeze(CommandSender sender)
    {
        setGlobalFreeze(sender, true);
    }

    @Callback
    @Subcommand("off")
    public void disableGlobalFreeze(CommandSender sender)
    {
        setGlobalFreeze(sender, false);
    }

    private void setGlobalFreeze(CommandSender sender, boolean value)
    {
        adminAction(sender, "<red><value:Enabling:Disabling> global player freeze",
                Formatter.booleanChoice("value", value));

        plugin().services().require(Freezer.class).setGlobalFreeze(value);

        if (value)
        {
            server().getOnlinePlayers().stream()
                    .filter(player -> !plugin().ranks().isAdmin(player))
                    .forEach(player -> msg(player, "<red>You have been temporarily frozen due to rulebreakers. You will be unfrozen soon."));
        }

        msg(sender, "<gray>Players are now <state>.",
                Placeholder.unparsed("state", value ? "frozen" : "free to move"));
    }

    @Callback
    @Subcommand("purge")
    public void unfreezeAll(CommandSender sender)
    {
        adminAction(sender, "<red>Unfreezing all players");
        plugin().services().require(Freezer.class).purge();
    }

    @Callback
    public void togglePlayerFreeze(CommandSender sender, Player player)
    {
        setFreezeForPlayer(sender, player, !plugin().players().getData(player).getFreezeData().isFrozen());
    }

    @Callback
    public void setFreezeForPlayer(CommandSender sender, Player player, @Resolve("Boolean") boolean value)
    {
        if (value && isProtectedAdmin(sender, player))
            return;

        plugin().players().getData(player).getFreezeData().setFrozen(value);

        msg(sender, "<gray><player> has been <state>.",
                Placeholder.unparsed("player", player.getName()),
                Placeholder.unparsed("state", value ? "frozen" : "unfrozen"));
        msg(player, value ? "<aqua>You have been frozen." : "<aqua>You have been unfrozen.");
    }
}
