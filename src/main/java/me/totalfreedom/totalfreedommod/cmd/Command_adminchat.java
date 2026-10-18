package me.totalfreedom.totalfreedommod.cmd;

import me.totalfreedom.totalfreedommod.ChatManager;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;

import me.totalfreedom.api.cmd.FCommand;
import me.totalfreedom.api.cmd.SourceType;
import me.totalfreedom.api.cmd.annotation.*;
import me.totalfreedom.totalfreedommod.player.PlayerData;

@Command(name = "adminchat",
        description = "AdminChat - Talk privately with other admins. Using the command by itself will toggle AdminChat on and off for all messages.",
        usage = "/<command> [message...]",
        aliases = {"o", "ac"})
@Permission(source = SourceType.BOTH, permission = "tfm.admin.adminchat")
public class Command_adminchat extends FCommand
{

    @Callback
    @Permission(permission = "tfm.admin.adminchat", source = SourceType.ONLY_IN_GAME)
    public void toggle(Player sender)
    {
        final PlayerData fplayer = plugin().players().getData(sender);
        boolean mode = !fplayer.inAdminChat();

        fplayer.setAdminChat(mode);
        msg(sender, "<gray>Toggled Admin Chat <mode:on:off>.", Formatter.booleanChoice("mode", mode));
    }

    @Completer(value = "", position = 0)
    public List<String> completeMessage(CommandSender sender, String partial)
    {
        return NameCandidates.onlineTyped(server(), partial);
    }
    
    @Callback
    public void sendMessage(CommandSender sender, @Greedy String message)
    {
        plugin().services().require(ChatManager.class).adminChat(sender, message);
    }
}
