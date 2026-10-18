package me.totalfreedom.totalfreedommod.cmd;

import java.util.List;
import java.util.Objects;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import me.totalfreedom.api.cmd.FCommand;
import me.totalfreedom.api.cmd.annotation.*;
import me.totalfreedom.totalfreedommod.player.PlayerData;

@Permission(permission = "tfm.admin.mute")
@Command(name = "stfu", aliases = "mute", description = "Mutes a player with brute force.", usage = "/<command> <<player> [reason] | list | purge | all>")
public class Command_stfu extends FCommand
{
    @Callback
    @Subcommand("list")
    public void list(CommandSender sender)
    {
        final List<PlayerData> players = server().getOnlinePlayers()
                                              .stream()
                                              .map(player -> plugin().players().getData(player))
                                              .filter(p -> p.isMuted())
                                              .toList();

        if (players.isEmpty())
        {
            msg(sender, "<gray>No one on the server is currently muted.");
            return;
        }

        final boolean senderIsConsole = !(sender instanceof Player);
        final List<Component> entries = players.stream()
                                               .map(fplayer -> senderIsConsole
                                                        ? MessageUtils.parse("<white><name>", Placeholder.unparsed("name", fplayer.getUsername()))
                                                        : MessageUtils.parse("<white><hover:show_text:'<name>'><display></hover>",
                                                    Placeholder.unparsed("name", fplayer.getUsername()),
                                                    MessageUtils.component("display", Objects.requireNonNull(server().getPlayer(fplayer.getUsername())).displayName())))
                                               .toList();

        msg(sender, "<gray>Muted players: <players>", MessageUtils.joinedComponents("players", entries));
    }

    @Callback
    @Subcommand("purge")
    public void purge(CommandSender sender)
    {
        adminAction(sender, "<red>Unmuting all players");

        final List<PlayerData> players = server().getOnlinePlayers()
                                              .stream()
                                              .map(player -> plugin().players().getData(player))
                                              .filter(p -> p.isMuted())
                                              .peek(player -> player.setMuted(false))
                                              .toList();

        msg(sender, "<gray>Unmuted <count> players.", Formatter.number("count", players.size()));
    }

    @Callback
    @Subcommand("all")
    public void muteAll(CommandSender sender)
    {
        adminAction(sender, "<red>Muting all non-admins");

        final List<PlayerData> players = server().getOnlinePlayers()
                                              .stream()
                                              .filter(player -> !isAdmin(player))
                                              .map(player -> plugin().players().getData(player))
                                              .peek(player -> player.setMuted(true))
                                              .toList();

        msg(sender, "<gray>Muted <count> players.", Formatter.number("count", players.size()));
    }

    @Callback
    public void mutePlayer(CommandSender sender, Player player)
    {
        mutePlayerWithReason(sender, player, null);
    }

    @Completer(value = "", position = 1)
    public List<String> completeReason(CommandSender sender, String partial)
    {
        return NameCandidates.onlineTyped(server(), partial);
    }

    @Callback
    public void mutePlayerWithReason(CommandSender sender, Player player, @Greedy String reason)
    {
        final PlayerData fplayer = getData(player);

        if (isProtectedAdmin(sender, player))
            return;

        if (fplayer.isMuted())
        {
            adminAction(sender, "<red>Unmuting <player>", Placeholder.unparsed("player", player.getName()));
            fplayer.setMuted(false);
            msg(player, "<green>You have been unmuted.");
        }
        else
        {
            if (reason != null)
                adminAction(sender, "<red>Muting <player><newline>  Reason: <yellow><reason>",
                            Placeholder.unparsed("player", player.getName()),
                            MessageUtils.parsed("reason", reason));
            else
                adminAction(sender, "<red>Muting <player>", 
                            Placeholder.unparsed("player", player.getName()));

            fplayer.setMuted(true);
            plugin().database().recordPunishment(player, sender, "MUTE", reason, null, "manual-mute");

            if (reason != null)
                msg(player, "<red>You have been muted. Reason: <yellow><reason>", 
                    MessageUtils.parsed("reason", reason));
            else
                msg(player, "<red>You have been muted.");
        }
    }
}
