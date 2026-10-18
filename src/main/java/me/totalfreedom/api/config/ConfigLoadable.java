package me.totalfreedom.api.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * An object that can be loaded from a configuration section.
 */
public interface ConfigLoadable
{
    /**
     * Loads data from a configuration section.
     *
     * @param cs The configuration section to load from
     */
    void loadFrom(ConfigurationSection cs);
}
