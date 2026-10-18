package me.totalfreedom.totalfreedommod;

import me.totalfreedom.api.FreedomAPI;

import java.util.List;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.util.FSync;

public class Muter extends FreedomService
{

    public static final List<String> MUTE_COMMANDS = List.of("say", "me", "msg", "tell", "reply", "mail", ",");

    public Muter(FreedomAPI plugin)
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

    @EventHandler(priority = EventPriority.LOW)
    public void onAsyncPlayerChatEvent(AsyncChatEvent event)
    {
        if (shouldCancelChat(event.getPlayer()))
        {
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW)
    public void onLegacyAsyncPlayerChat(AsyncPlayerChatEvent event)
    {
        if (shouldCancelChat(event.getPlayer()))
        {
            event.setCancelled(true);
        }
    }

    private boolean shouldCancelChat(Player player)
    {
        PlayerData playerData = plugin.players().getData(player);

        if (!playerData.isMuted())
        {
            return false;
        }

        if (plugin.ranks().isAdmin(player))
        {
            playerData.setMuted(false);
            return false;
        }

        FSync.playerMsg(player, "You are muted, STFU! - You will be unmuted in 5 minutes.");
        return true;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {

        Player player = event.getPlayer();
        PlayerData playerData = plugin.players().getData(event.getPlayer());

        // Block commands if player is muted
        if (!playerData.isMuted())
        {
            return;
        }

        String message = event.getMessage();
        if (plugin.ranks().isAdmin(player))
        {
            playerData.setMuted(false);
            return;
        }

        String cmdName = message.split(" ")[0].toLowerCase();
        if (cmdName.startsWith("/"))
        {
            cmdName = cmdName.substring(1);
        }

        Command command = server.getCommandMap().getCommand(cmdName);
        if (command != null)
        {
            cmdName = command.getName().toLowerCase();
        }

        if (MUTE_COMMANDS.contains(cmdName))
        {
            player.sendMessage(Component.text("That command is blocked while you are muted.", NamedTextColor.RED));
            event.setCancelled(true);
        }
    }
}
