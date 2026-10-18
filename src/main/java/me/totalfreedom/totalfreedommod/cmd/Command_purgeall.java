package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.freeze.Freezer;

import me.totalfreedom.totalfreedommod.EntityWiper;

import org.bukkit.command.CommandSender;
import org.bukkit.potion.PotionEffect;

import me.totalfreedom.api.cmd.FCommand;
import me.totalfreedom.api.cmd.annotation.Callback;
import me.totalfreedom.api.cmd.annotation.Command;
import me.totalfreedom.api.cmd.annotation.Permission;
import me.totalfreedom.totalfreedommod.lockup.LockupManager;
import me.totalfreedom.totalfreedommod.player.PlayerData;

@Command(name = "purgeall", description = "Superadmin command - Purge everything! (except for bans).", usage = "/<command>")
@Permission(permission = "tfm.admin.purgeall")
public class Command_purgeall extends FCommand
{
    @Callback
    public void purgeAll(CommandSender sender)
    {
        adminAction(sender, "<red>Purging all player data");

        plugin().services().require(EntityWiper.class).wipeEntities(true);
        plugin().services().require(LockupManager.class).purge();

        server().getOnlinePlayers().forEach(player -> 
        {
            PlayerData playerData = getData(player);

            if (playerData.isMuted())
            {
                playerData.setMuted(false);
            }

            if (playerData.isCommandsBlocked())
            {
                playerData.setCommandsBlocked(false);
            }

            if (playerData.isOrbiting())
            {
                playerData.stopOrbiting();
            }

            if (playerData.getFreezeData().isFrozen())
            {
                playerData.getFreezeData().setFrozen(false);
            }

            for (PotionEffect potionEffect : player.getActivePotionEffects())
            {
                player.removePotionEffect(potionEffect.getType());
            }

            if (playerData.getCageData().isCaged())
            {
                playerData.getCageData().setCaged(false);
            }
        });

        plugin().services().require(Freezer.class).setGlobalFreeze(false);

        Command_mobpurge.purgeMobs();
    }
}
