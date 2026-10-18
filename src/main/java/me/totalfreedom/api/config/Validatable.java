package me.totalfreedom.api.config;

/**
 * An object that can validate its own state.
 */
public interface Validatable
{
    /**
     * Checks if this object is in a valid state.
     *
     * @return true if the object is valid, false otherwise
     */
    boolean isValid();
}
