package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.entity.Player;

import me.totalfreedom.api.cmd.FCommand;
import me.totalfreedom.api.cmd.SourceType;
import me.totalfreedom.api.cmd.annotation.*;
import me.totalfreedom.totalfreedommod.player.PlayerData;

/**
 * Admins only ever have the one stored address, which moves on its own when a verified session
 * joins from a new one, so there's nothing here to manage it with.
 */
@Permission(source = SourceType.ONLY_IN_GAME, permission = "tfm.admin.myadmin")
@Command(name = "myadmin", description = "Manage my admin entry", usage = "/myadmin <setlogin <message> | clearlogin>")
public class Command_myadmin extends FCommand
{
    @Callback
    @Subcommand("setlogin")
    public void setLoginMessage(Player player, @Greedy String message)
    {
        final PlayerData entry = getData(player);
        //removed conversion method. we want to enforce <tags> because tags are peak.

        entry.setLoginMessage(message);
        plugin().players().saveData(entry);

        msg(
            player, // removing the tabs in the string block because they are also processed by MessageUtils.
            """
            Your login message is now:
            > <login>\
            """,
            MessageUtils.component("login", plugin().ranks().formatLoginMessage(player))
        );
    }

    @Callback
    @Subcommand("clearlogin")
    public void clearLogin(Player player)
    {
        final PlayerData entry = getData(player);

        entry.setLoginMessage(null);
        plugin().players().saveData(entry);
        msg(player, "<gray>Your login message has been removed.");
    }
}
