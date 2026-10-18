package me.totalfreedom.api.player;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

import me.totalfreedom.api.config.ConfigLoadable;
import me.totalfreedom.api.config.Validatable;

/**
 * What TotalFreedomMod stores about a player, as seen by dependent plugins.
 * <p>
 * Records are keyed by {@link #getUuid()}. The username is kept for display and name lookups
 * only, since names can change hands.
 */
public interface IPlayerData extends ConfigLoadable, Validatable
{
    UUID getUuid();

    String getUsername();

    /**
     * @return the online player this data belongs to, or {@code null} when they are offline.
     */
    Player getPlayer();

    /**
     * @return the address of the current session when online, otherwise the most recently stored
     * one.
     */
    String getIp();

    /**
     * @return the staff rank id this player holds on record, active or not; empty for a regular
     * player.
     */
    Optional<String> getRankId();

    /**
     * @return the linked Discord user id, if any.
     */
    Optional<String> getDiscordId();

    long getFirstJoinUnix();

    long getLastJoinUnix();

    boolean isMuted();

    boolean isFrozen();

    boolean isCommandsBlocked();

    int getStrikes();

    /**
     * @return the ids of every title this player holds.
     */
    Set<String> getTitles();

    boolean hasTitle(String titleId);

    boolean hasCustomNickname();

    /**
     * @return the nickname as shown in chat, or {@code null} when the player has none.
     */
    Component getDisplayedNickname();
}
