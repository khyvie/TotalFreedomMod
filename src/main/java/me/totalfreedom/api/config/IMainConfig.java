package me.totalfreedom.api.config;

import java.util.List;

import me.totalfreedom.totalfreedommod.config.ConfigEntry;

public interface IMainConfig
{
    void load();

    String getString(ConfigEntry entry);

    void setString(ConfigEntry entry, String value);

    Double getDouble(ConfigEntry entry);

    void setDouble(ConfigEntry entry, Double value);

    Boolean getBoolean(ConfigEntry entry);

    void setBoolean(ConfigEntry entry, Boolean value);

    Integer getInteger(ConfigEntry entry);

    void setInteger(ConfigEntry entry, Integer value);

    List getList(ConfigEntry entry);

    void setList(ConfigEntry entry, List<?> value);

    <T> T get(ConfigEntry entry, Class<T> type) throws IllegalArgumentException;

    <T> void set(ConfigEntry entry, T value, Class<T> type) throws IllegalArgumentException;

    void save();
}
