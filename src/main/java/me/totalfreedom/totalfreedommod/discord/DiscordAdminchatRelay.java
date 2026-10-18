package me.totalfreedom.totalfreedommod.discord;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.totalfreedom.api.FreedomAPI;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;

/**
 * Bidirectional chat relay for the adminchat channel.
 */
public class DiscordAdminchatRelay extends AbstractDiscordChatRelay
{
    public DiscordAdminchatRelay(FreedomAPI plugin, DiscordBridge bridge)
    {
        super(bridge::currentAdminchatChannel,
            ConfigEntry.DISCORD_ADMINCHAT_CHANNEL_FORMAT.getString(),
            ConfigEntry.DISCORD_ADMINCHAT_FORMAT.getString(),
            component -> {
                Bukkit.getConsoleSender().sendMessage(component);
                for (Player player : plugin.players().getOnlineStaff())
                    player.sendMessage(component);
            },
            plugin,
            bridge);
    }
}
