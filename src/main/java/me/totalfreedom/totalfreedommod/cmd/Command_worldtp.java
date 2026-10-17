package me.totalfreedom.totalfreedommod.cmd;

import java.util.List;

import org.bukkit.entity.Player;

import me.totalfreedom.api.cmd.FCommand;
import me.totalfreedom.api.cmd.SourceType;
import me.totalfreedom.api.cmd.annotation.*;
import me.totalfreedom.totalfreedommod.cmd.internal.FuzzyMatch;

@Command(name = "worldtp", description = "Teleport to a world.", usage = "/worldtp <world>", aliases = {"wtp", "tpw", "tpworld"})
@Permission(permission = "tfm.world.tp", source = SourceType.ONLY_IN_GAME)
public class Command_worldtp extends FCommand
{
    @Completer(value = "", position = 0)
    public List<String> completeWorld(Player sender, String partial)
    {
        return FuzzyMatch.filter(plugin().worlds().worldNames(), partial);
    }

    @Callback
    public void worldtp(Player player, String world)
    {
        plugin().worlds().gotoWorld(player, world);
    }
}
