package me.totalfreedom.api.player;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.player.PlayerData;

public interface IPlayerList
{
    /**
     * Data for every online player, keyed by UUID.
     */
    Map<UUID, PlayerData> getDataMap();

    File getConfigFolder();

    /**
     * Queue a write of every loaded player record.
     */
    void save();

    void saveAsync();

    void saveData(PlayerData data);

    void clearSavedTag(Player player);

    boolean saveCurrentTag(Player player);

    String getIp(OfflinePlayer player);

    /**
     * The data for an online player, loading or creating it on first use. Safe off the main
     * thread. May not return null.
     */
    PlayerData getData(Player player);

    /**
     * The live instance when this player is online, otherwise a fresh load. May return null.
     */
    PlayerData getData(UUID uuid);

    /**
     * The live instance when {@code username} is online, otherwise the stored record that last
     * joined under that name. Names can change hands, so prefer {@link #getData(UUID)}. May
     * return null.
     */
    PlayerData getData(String username);

    Collection<PlayerData> getAllData();

    Collection<PlayerData> getLoadedData();

    /**
     * Every record holding a staff rank, active or not, online or not.
     */
    Collection<PlayerData> getStaff();

    /**
     * When records share the name, an active one wins, then whoever joined last.
     */
    Optional<PlayerData> getStaffByName(String username);

    /**
     * Moves {@code record} onto {@code target}'s UUID, replacing what they had and releasing the
     * old UUID. Console only: running it is what vouches for {@code target}.
     */
    void rekeyStaff(PlayerData record, Player target);

    /**
     * Rebuilds the staff index from storage.
     */
    void reloadStaff();

    /**
     * The active staff member who trusts {@code ip}.
     */
    Optional<PlayerData> getStaffByIp(String ip);

    /**
     * As {@link #getStaffByIp(String)}, also matching on the first three octets.
     */
    Optional<PlayerData> getStaffByIpFuzzy(String ip);

    Optional<PlayerData> getStaffByDiscordId(String discordId);

    /**
     * {@code player}'s record when they hold an active staff rank and this session passes the
     * identity check; empty otherwise.
     */
    Optional<PlayerData> getVerifiedStaff(Player player);

    /**
     * The same test made at login, before there is a {@link Player} to ask.
     */
    boolean isVerifiedStaffLogin(UUID uuid, String ip);

    /**
     * Whether {@code player} is on an active staff record but this session fails the identity check.
     */
    boolean isImpostor(Player player);

    boolean isIdentityMatched(Player player);

    Set<Player> getOnlineStaff();

    void deactivateOldStaff(boolean verbose);

    /**
     * Links {@code discordId} to {@code data}, taking it off any other record first.
     *
     * @return the record's Discord identifier, assigned if it had none.
     */
    String linkDiscord(PlayerData data, String discordId);

    void unlinkDiscord(PlayerData data);
}
