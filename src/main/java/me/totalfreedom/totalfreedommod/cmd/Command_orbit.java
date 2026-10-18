package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import me.totalfreedom.api.cmd.FCommand;
import me.totalfreedom.api.cmd.annotation.Callback;
import me.totalfreedom.api.cmd.annotation.Command;
import me.totalfreedom.api.cmd.annotation.Permission;
import me.totalfreedom.totalfreedommod.player.PlayerData;

@Command(name = "orbit", description = "POW!!! Right in the kisser! One of these days Alice, straight to the Moon!", usage = "/orbit <target>")
@Permission(permission = "tfm.fun.orbit")
public class Command_orbit extends FCommand
{
    @Callback
    public void setOrbit(CommandSender sender, Player player)
    {
        final PlayerData target = plugin().players().getData(player);
        if (!target.isOrbiting())
        {
            if (isProtectedAdmin(sender, player))
                return;

            player.setGameMode(GameMode.SURVIVAL);
            final double strength = 10.0;
            target.startOrbiting(strength);

            player.setVelocity(new Vector(0, strength, 0));
            adminAction(sender, "<aqua>Orbiting <target>",
                    Placeholder.unparsed("target", target.getUsername()));
        }
        else
        {
            msg(sender, "<gray>Stopped orbiting <player>", Placeholder.unparsed("player", target.getUsername()));
            target.stopOrbiting();
        }
    }
}
