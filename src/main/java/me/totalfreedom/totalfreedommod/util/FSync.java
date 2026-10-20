package me.totalfreedom.totalfreedommod.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import me.totalfreedom.api.FreedomAPI;
import me.totalfreedom.totalfreedommod.ChatManager;
import me.totalfreedom.api.PluginProvider;

public class FSync
{

    public static void playerMsg(final Player player, final String message)
    {
        final FreedomAPI plugin = PluginProvider.get();
        plugin.getServer().getScheduler().runTask(plugin, FTask.guard("FSync/playerMsg", () -> FUtil.playerMsg(player, message)));
    }

    public static void playerKick(final Player player, final Component reason)
    {
        final FreedomAPI plugin = PluginProvider.get();
        plugin.getServer().getScheduler().runTask(plugin, FTask.guard("FSync/playerKick", () -> player.kick(reason)));
    }

    public static void playerKick(final Player player, final String reason)
    {
        playerKick(player, FUtil.colorizeWithLinks(reason));
    }

    public static void adminChatMessage(final CommandSender sender, final String message)
    {
        final FreedomAPI plugin = PluginProvider.get();
        plugin.getServer().getScheduler().runTask(plugin, FTask.guard("FSync/adminChatMessage", () -> plugin.services().require(ChatManager.class).adminChat(sender, message)));
    }

    public static void autoEject(final Player player, final String kickMessage)
    {
        final FreedomAPI plugin = PluginProvider.get();
        plugin.getServer().getScheduler().runTask(plugin, FTask.guard("FSync/autoEject", () -> plugin.autoEject().autoEject(player, kickMessage)));
    }

    public static void bcastMsg(final String message, final NamedTextColor color)
    {
        final FreedomAPI plugin = PluginProvider.get();
        plugin.getServer().getScheduler().runTask(plugin, FTask.guard("FSync/bcastMsg", () -> FUtil.bcastMsg(message, color)));
    }
}
