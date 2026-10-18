package me.totalfreedom.totalfreedommod.cmd;

import java.util.List;

import org.bukkit.command.CommandSender;

import net.kyori.adventure.text.Component;

import me.totalfreedom.api.cmd.FCommand;
import me.totalfreedom.api.cmd.annotation.Callback;
import me.totalfreedom.api.cmd.annotation.Command;
import me.totalfreedom.api.cmd.annotation.Completer;
import me.totalfreedom.api.cmd.annotation.Greedy;
import me.totalfreedom.api.cmd.annotation.Permission;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.util.AdventureUtil;

@Command(name = "realname", description = "Finds the real name of a nicknamed player", usage = "/<command> <nickname..>")
@Permission(permission = "tfm.player.realname")
public class Command_realname extends FCommand
{
    @Completer(value = "", position = 0, scope = Completer.Scope.ARGUMENT)
    public List<String> completeNickname(CommandSender sender, String partial)
    {
        return NameCandidates.onlineNicknames(plugin(), server(), partial);
    }

    @Callback
    public void realname(CommandSender sender, @Greedy String nickname)
    {
        final boolean foundOne = server().getOnlinePlayers()
                .stream()
                .reduce(false, (found, player) ->
                    {
                        final PlayerData data = plugin().players().getData(player);
                        final Component playerNickname = data.getNickname();
                        if (playerNickname != null)
                        {
                            final String plainNick = AdventureUtil.componentToPlainText(playerNickname);
                            if (plainNick.contains(nickname))
                            {
                                msg(sender, "<nickname><gray> is <name>.", MessageUtils.component("nickname", data.getDisplayedNickname()),
                                        MessageUtils.unparsed("name", player.getName()));
                                return true;
                            }
                        }
                        return found;
                    }, (a, b) -> a || b);

        if (!foundOne)
        {
            msg(sender, "<gray>Could not find a player with such a nickname.");
        }
    }
}
