package me.totalfreedom.totalfreedommod.player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

import me.totalfreedom.totalfreedommod.util.FUtil;

/**
 * One admin as the retired admin list stored them, in whichever of its formats (SQL tables,
 * {@code admins.json}, {@code admins.yml}). Every importer turns its rows into these and lets this
 * decide where they land, so the rules below exist once.
 *
 * @param uuid          the stored UUID, if any. Often wrong: see {@link #targetUuid(boolean)}.
 * @param ips           every stored address, oldest first. May be empty.
 * @param lastLoginUnix seconds since the epoch, or {@code 0} when never recorded.
 */
public record LegacyAdmin(String uuid, String username, String rankId, boolean active, List<String> ips,
                          long lastLoginUnix, String loginMessage)
{
    public LegacyAdmin
    {
        ips = ips == null ? List.of() : List.copyOf(ips);
    }

    /**
     * The UUID this admin's {@link PlayerData} should be keyed by.
     * <p>
     * When Mojang vouches for session UUIDs (an online-mode server, or a backend behind an
     * online-mode proxy), a stored Mojang (version 4) UUID is the one the server will see, so it's
     * used and the admin is never matched by name. Anywhere else the stored UUID can't be trusted:
     * records were often given the premium UUID from a Mojang lookup, or an offline UUID hashed
     * from the lowercased name, and an offline-mode server issues neither. There it's the name's
     * offline UUID hashed the way vanilla does, which is the session UUID on an offline-mode server.
     *
     * @param authenticated whether session UUIDs are authenticated, see
     *                      {@link io.papermc.paper.configuration.ServerConfiguration#isProxyOnlineMode()}.
     */
    public UUID targetUuid(boolean authenticated)
    {
        return Optional.ofNullable(uuid)
                       .map(FUtil::parseUuid)
                       .filter(parsed -> authenticated && parsed.version() == 4)
                       .orElseGet(() -> PlayerData.offlineUuid(username));
    }

    /**
     * The name as the player actually spells it, from the server's own name cache. The retired
     * admin list keyed entries by the lowercased name, and the offline UUID hashes the exact name,
     * so the wrong capitalisation lands the admin on a UUID nobody joins with.
     */
    public static Optional<String> cachedName(Server server, String name)
    {
        return Optional.ofNullable(server.getOfflinePlayerIfCached(name))
                       .map(OfflinePlayer::getName);
    }

    /**
     * Copies the admin's standing onto {@code data}, which must already be keyed by
     * {@link #targetUuid(boolean)}. Join times only ever move to cover the recorded login.
     */
    public void applyTo(PlayerData data)
    {
        data.setRankId(rankId);
        data.setAdminActive(active);
        data.setLoginMessage(loginMessage);
        if (!ips.isEmpty())
        {
            data.setIp(ips.getLast());
            data.setTrustedIps(ips);
        }

        final long lastLogin = lastLoginUnix > 0 ? lastLoginUnix : FUtil.getUnixTime();
        if (data.getLastJoinUnix() < lastLogin)
            data.setLastJoinUnix(lastLogin);
        if (data.getFirstJoinUnix() == 0L)
            data.setFirstJoinUnix(data.getLastJoinUnix());
    }

    /**
     * Whether a record found only by name may take this admin's standing: only while it is still
     * an unclaimed placeholder. One under a real UUID belongs to whoever holds the name now.
     */
    public static boolean isUnclaimedPlaceholder(PlayerData data)
    {
        return data.getUuid().equals(PlayerData.offlineUuid(data.getUsername()));
    }

    /**
     * {@code custom_rank} held the assigned rank and took precedence over {@code rank}, which held
     * a fixed tier name. A tier name lowercases into an id, the convention {@code ranks.json} uses.
     */
    public static String resolveRankId(String customRank, String legacyRank)
    {
        if (customRank != null && !customRank.isBlank())
            return customRank.toLowerCase();

        return legacyRank == null || legacyRank.isBlank() ? null : legacyRank.toLowerCase();
    }
}
