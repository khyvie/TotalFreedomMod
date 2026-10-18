package me.totalfreedom.totalfreedommod;

import me.totalfreedom.api.FreedomAPI;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import net.kyori.adventure.text.format.NamedTextColor;

import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.util.FUtil;

public class AntiNuke extends FreedomService
{

    public AntiNuke(FreedomAPI plugin)
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event)
    {
        if (!ConfigEntry.NUKE_MONITOR_ENABLED.getBoolean())
        {
            return;
        }

        final Player player = event.getPlayer();
        final Location location = event.getBlock().getLocation();
        final PlayerData playerData = plugin.players().getData(player);

        final Location playerLocation = player.getLocation();

        final double nukeMonitorRange = ConfigEntry.NUKE_MONITOR_RANGE.getDouble();

        boolean outOfRange = false;
        if (!playerLocation.getWorld().equals(location.getWorld()))
        {
            outOfRange = true;
        }
        else if (playerLocation.distanceSquared(location) > (nukeMonitorRange * nukeMonitorRange))
        {
            outOfRange = true;
        }

        if (outOfRange)
        {
            if (playerData.incrementAndGetFreecamDestroyCount() > ConfigEntry.FREECAM_TRIGGER_COUNT.getInteger())
            {
                FUtil.bcastMsg(player.getName() + " has been flagged for possible freecam nuking.", NamedTextColor.RED);
                plugin.autoEject().autoEject(player, "Freecam (extended range) block breaking is not permitted on this server.");

                playerData.resetFreecamDestroyCount();

                event.setCancelled(true);
                return;
            }
        }

        if (playerData.incrementAndGetBlockDestroyCount() > ConfigEntry.NUKE_MONITOR_COUNT_BREAK.getInteger())
        {
            FUtil.bcastMsg(player.getName() + " is breaking blocks too fast!", NamedTextColor.RED);
            plugin.autoEject().autoEject(player, "You are breaking blocks too fast. Nukers are not permitted on this server.");

            playerData.resetBlockDestroyCount();

            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        if (!ConfigEntry.NUKE_MONITOR_ENABLED.getBoolean())
        {
            return;
        }

        Player player = event.getPlayer();
        Location blockLocation = event.getBlock().getLocation();
        PlayerData playerData = plugin.players().getData(player);

        Location playerLocation = player.getLocation();

        double nukeMonitorRange = ConfigEntry.NUKE_MONITOR_RANGE.getDouble();

        boolean outOfRange = false;
        if (!playerLocation.getWorld().equals(blockLocation.getWorld()))
        {
            outOfRange = true;
        }
        else if (playerLocation.distanceSquared(blockLocation) > (nukeMonitorRange * nukeMonitorRange))
        {
            outOfRange = true;
        }

        if (outOfRange)
        {
            if (playerData.incrementAndGetFreecamPlaceCount() > ConfigEntry.FREECAM_TRIGGER_COUNT.getInteger())
            {
                FUtil.bcastMsg(player.getName() + " has been flagged for possible freecam building.", NamedTextColor.RED);
                plugin.autoEject().autoEject(player, "Freecam (extended range) block building is not permitted on this server.");

                playerData.resetFreecamPlaceCount();

                event.setCancelled(true);
                return;
            }
        }

        if (playerData.incrementAndGetBlockPlaceCount() > ConfigEntry.NUKE_MONITOR_COUNT_PLACE.getInteger())
        {
            FUtil.bcastMsg(player.getName() + " is placing blocks too fast!", NamedTextColor.RED);
            plugin.autoEject().autoEject(player, "You are placing blocks too fast.");

            playerData.resetBlockPlaceCount();

            event.setCancelled(true);
        }
    }
}
