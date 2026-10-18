package me.totalfreedom.totalfreedommod.freeze;

import me.totalfreedom.api.FreedomAPI;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.util.FUtil;

public class Freezer extends FreedomService
{

    private boolean globalFreeze = false;

    public Freezer(FreedomAPI plugin)
    {
        super(plugin);
    }

    @Override
    public void onStart()
    {
        globalFreeze = false;
    }

    @Override
    public void onStop()
    {
        // unused
    }

    public boolean isGlobalFreeze()
    {
        return globalFreeze;
    }

    public void setGlobalFreeze(boolean frozen)
    {
        this.globalFreeze = frozen;
    }

    public void purge()
    {
        this.globalFreeze = false;

        for (Player player : server.getOnlinePlayers())
            plugin.players().getData(player).getFreezeData().setFrozen(false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event)
    {
        if (!event.hasChangedPosition())
            return;

        final Player player = event.getPlayer();

        if (plugin.ranks().isAdmin(player))
            return;

        final FreezeData fd = plugin.players().getData(player).getFreezeData();
        if (!fd.isFrozen() && !globalFreeze)
            return;

        FUtil.setFlying(player, true);

        Location loc = fd.getLocation();
        if (loc == null)
            loc = event.getFrom();

        event.setTo(loc);
    }

}
