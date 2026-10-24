package me.totalfreedom.api.rank;

import java.util.Optional;

import org.bukkit.command.CommandSender;

import me.totalfreedom.totalfreedommod.rank.CustomRank;

/**
 * The single place that answers "what rank is this?" and "what rank does this permission need?".
 */
public interface IRankRegistry
{
    /**
     * Rebuilds the permission index from the current rank map. Call after ranks are loaded or after
     * inheritance is re-resolved, on the main thread.
     */
    void reindex();

    /**
     * The rank registered under {@code id}, if any.
     */
    Optional<CustomRank> byId(String id);

    /**
     * The rank filling {@code role}, preferring one that declares it in {@code ranks.json} and
     * otherwise deriving it from the shape of the registry.
     */
    Optional<CustomRank> byRole(RankRole role);

    /**
     * The rank every unplaceable sender falls back to.
     */
    Optional<CustomRank> floor();

    /**
     * Whether {@code holder} grants every node {@code other} grants, which is what it means for one
     * rank to be able to do everything another can.
     */
    boolean covers(CustomRank holder, CustomRank other);

    /**
     * The least privileged rank that grants {@code permission}. Empty when no rank grants it,
     * meaning the node is unreachable. This picks a rank to assign; it is not an access check.
     */
    Optional<CustomRank> requiredFor(String permission);

    /**
     * The rank {@code sender} acts at, falling back to {@link #floor()} when it cannot be placed.
     */
    Optional<CustomRank> forSender(CommandSender sender);

    /**
     * Whether {@code sender} may exercise {@code permission}, by rank or by title.
     */
    boolean satisfies(CommandSender sender, String permission);

    /**
     * Whether {@code holder} grants {@code permission}.
     */
    boolean satisfies(CustomRank holder, String permission);

    /**
     * Whether {@code rank} carries the marker {@code tag}, which labels a rank without gating
     * anything.
     */
    boolean hasMarker(CustomRank rank, String tag);
}
