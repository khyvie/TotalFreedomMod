package me.totalfreedom.totalfreedommod.cmd;

import org.bukkit.entity.Player;

import me.totalfreedom.api.cmd.FCommand;
import me.totalfreedom.api.cmd.SourceType;
import me.totalfreedom.api.cmd.annotation.*;
import me.totalfreedom.totalfreedommod.player.PlayerData;

@Command(name = "joinmessages", description = "Toggle visibility of other players' join/leave messages.", usage = "/joinmessages", aliases = {"jlm", "togglejoinmessages"})
@Permission(permission = "tfm.player.joinmessages", source = SourceType.ONLY_IN_GAME)
public class Command_joinmessages extends FCommand
{
    @Callback
    public void toggle(final Player player)
    {
        final PlayerData data = plugin().players().getData(player);
        final boolean enabled = !data.isJoinLeaveMessagesEnabled();

        data.setJoinLeaveMessagesEnabled(enabled);
        plugin().players().saveData(data);

        msg(player, enabled
            ? "<gray>You will now see other players' join/leave messages."
            : "<gray>You will no longer see other players' join/leave messages. Admin joins/leaves will still show.");
    }
}
