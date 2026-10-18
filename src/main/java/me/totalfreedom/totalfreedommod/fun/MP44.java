package me.totalfreedom.totalfreedommod.fun;

import me.totalfreedom.api.FreedomAPI;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;

import me.totalfreedom.totalfreedommod.FreedomService;

public class MP44 extends FreedomService
{

    public MP44(FreedomAPI plugin)
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        plugin.players().getData(event.getPlayer()).disarmMP44();
    }

}
