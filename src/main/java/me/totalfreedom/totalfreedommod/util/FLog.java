package me.totalfreedom.totalfreedommod.util;

import me.totalfreedom.api.PluginProvider;
import org.bukkit.Bukkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FLog
{

    private static final Logger FALLBACK_LOGGER = LoggerFactory.getLogger("Minecraft-Server");
    private static Logger serverLogger = null;
    private static Logger pluginLogger = null;

    private FLog()
    {
    }

    public static void debug(String message)
    {
        log(Level.DEBUG, message, false);
    }

    public static void debug(String format, Object... args)
    {
        log(Level.DEBUG, String.format(format, args), false);
    }

    public static void info(String message)
    {
        log(Level.INFO, message, false);
    }

    public static void info(String message, boolean raw)
    {
        log(Level.INFO, message, raw);
    }

    public static void info(String format, Object... args)
    {
        log(Level.INFO, String.format(format, args), false);
    }

    public static void info(Throwable ex)
    {
        log(Level.INFO, ex);
    }

    public static void warn(String message)
    {
        log(Level.WARN, message, false);
    }

    public static void warn(String message, boolean raw)
    {
        log(Level.WARN, message, raw);
    }

    public static void warn(String format, Object... args)
    {
        log(Level.WARN, String.format(format, args), false);
    }

    public static void warn(Throwable ex)
    {
        log(Level.WARN, ex);
    }

    public static void error(String message)
    {
        log(Level.ERROR, message, false);
    }

    public static void error(String message, boolean raw)
    {
        log(Level.ERROR, message, raw);
    }

    public static void error(String format, Object... args)
    {
        log(Level.ERROR, String.format(format, args), false);
    }

    public static void error(Throwable ex)
    {
        log(Level.ERROR, ex);
    }
    
    public static void fatal(String message)
    {
        log(Level.ERROR, message, true);
        Bukkit.getPluginManager().disablePlugin(PluginProvider.get());
    }
    
    public static void fatal(String format, Object... args)
    {
        log(Level.ERROR, String.format(format, args), true);
        Bukkit.getPluginManager().disablePlugin(PluginProvider.get());
    }
    
    public static void fatal(Throwable ex)
    {
        log(Level.ERROR, ex);
        Bukkit.getPluginManager().disablePlugin(PluginProvider.get());
    }

    public static void setServerLogger(Logger logger)
    {
        serverLogger = logger;
    }

    public static void setPluginLogger(Logger logger)
    {
        pluginLogger = logger;
    }

    public static Logger getPluginLogger()
    {
        return (pluginLogger != null ? pluginLogger : FALLBACK_LOGGER);
    }

    public static Logger getServerLogger()
    {
        return (serverLogger != null ? serverLogger : FALLBACK_LOGGER);
    }

    private static void log(Level level, String message, boolean raw)
    {
        if (message != null && !raw && message.contains("&") && !message.contains("§"))
        {
            message = message.replace('&', '§');
        }
        emit(getLogger(raw), level, message);
    }

    private static void log(Level level, Throwable throwable)
    {
        emit(getLogger(false), level, throwable);
    }

    private static void emit(Logger logger, Level level, String message)
    {
        switch (level)
        {
            case DEBUG -> logger.debug(message);
            case INFO -> logger.info(message);
            case WARN -> logger.warn(message);
            case ERROR -> logger.error(message);
        }
    }

    private static void emit(Logger logger, Level level, Throwable throwable)
    {
        switch (level)
        {
            case DEBUG -> logger.debug("", throwable);
            case INFO -> logger.info("", throwable);
            case WARN -> logger.warn("", throwable);
            case ERROR -> logger.error("", throwable);
        }
    }

    private static Logger getLogger(boolean raw)
    {
        if (raw || pluginLogger == null)
        {
            return (serverLogger != null ? serverLogger : FALLBACK_LOGGER);
        }
        else
        {
            return pluginLogger;
        }
    }

    private enum Level
    {
        DEBUG, INFO, WARN, ERROR
    }
}
