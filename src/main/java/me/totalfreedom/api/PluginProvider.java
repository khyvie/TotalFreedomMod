package me.totalfreedom.api;

import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Supplier for TotalFreedomMod that replaces the static singleton.
 * Preferably, use a constructor-injected plugin reference over calling this directly when possible.
 * <p>
 * The instance is resolved from Bukkit's services manager on the first call and cached, so every
 * later call is a single atomic read. {@link #release()} must be called when the plugin disables,
 * otherwise a reload would keep handing out the previous instance.
 */
public final class PluginProvider
{
    private static final AtomicReference<FreedomAPI> CACHED = new AtomicReference<>();

    private PluginProvider() {}

    /**
     * @return the live plugin instance.
     * @throws IllegalStateException if the plugin is not currently registered as a service.
     */
    public static FreedomAPI get()
    {
        final FreedomAPI cached = CACHED.get();

        if (cached != null)
            return cached;

        final RegisteredServiceProvider<FreedomAPI> provider = Bukkit.getServicesManager().getRegistration(FreedomAPI.class);

        if (provider == null)
            throw new IllegalStateException("TotalFreedomMod is not currently initialized.");

        CACHED.compareAndSet(null, provider.getProvider());
        return CACHED.get();
    }

    /**
     * Drops the cached instance. Call from the plugin's {@code onDisable}.
     */
    public static void release()
    {
        CACHED.set(null);
    }
}
