package me.totalfreedom.api.world;

import java.util.List;

import org.bukkit.entity.Player;

import me.totalfreedom.totalfreedommod.world.CustomWorld;

public interface IWorldManager
{
    /** The {@link CustomWorld} wrapper for a world name, creating and caching one on first use. */
    CustomWorld get(String worldName);

    /** Every world name worth suggesting for a world-targeting command. */
    List<String> worldNames();

    /**
     * Drops the cached access check on every managed world. An admin or title grant/revoke can
     * change who passes a world's access check, so this sweeps every managed world rather than
     * naming one.
     */
    void invalidateAccessCaches();

    void gotoWorld(Player player, String targetWorld);
}
