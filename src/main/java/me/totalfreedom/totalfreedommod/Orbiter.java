package me.totalfreedom.totalfreedommod;

import me.totalfreedom.api.FreedomAPI;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import me.totalfreedom.totalfreedommod.player.PlayerData;

public class Orbiter extends FreedomService
{

    public Orbiter(FreedomAPI plugin)
    {
        super(plugin);
    }

    @Override
    public void onStart()
    {
    }

    @Override
    public void onStop()
    {
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event)
    {
        if (!event.hasChangedPosition())
        {
            return;
        }

        final Player player = event.getPlayer();
        final PlayerData playerData = plugin.players().getData(player);

        if (!playerData.isOrbiting())
        {
            return;
        }

        if (player.getVelocity().length() < playerData.orbitStrength() * (2.0 / 3.0))
        {
            player.setVelocity(new Vector(0, playerData.orbitStrength(), 0));
        }
    }

}
