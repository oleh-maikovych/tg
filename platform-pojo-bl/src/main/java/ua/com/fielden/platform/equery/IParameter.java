package ua.com.fielden.platform.equery;

import ua.com.fielden.platform.utils.Pair;

/**
 * Represents a parameter concept, that could be range (use ) or single.
 *
 * @author oleh
 *
 */
public interface IParameter {
    /**
     * Indicates whether parameter is range or single.
     *
     * @return
     */
    boolean isRange();

    /**
     * Returns single value for single property. Throws {@link UnsupportedOperationException} if the property is range.
     *
     * @return
     */
    Object getValue() throws UnsupportedOperationException;

    /**
     * Returns {@link Pair} of two values for range property. Throws {@link UnsupportedOperationException} if the property is single.
     *
     * For date properties both values represent an interval [from; to]. In case when interval is represented by date prefix with mnemonic (e.g. CURR MONTH) both values
     * will be represented as [from; to), where the right boundary is exclusive.
     *
     * @return
     */
    Pair<Object, Object> getRange() throws UnsupportedOperationException;
}
