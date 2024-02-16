package me.buhlmann.engine4.api.utils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Map containing a list of values for each key in the form of {@code Map<K, Collection<V>>}.
 * Initialization and destruction of the lists is handled internally.
 *
 * The following {@code Map} operations are <strong>by default not</strong> supported:
 * <ul>
 *     <li>{@code get} - use getAll instead to get a reference to the internal collection</li>
 *     <li>{@code values}</li>
 *     <li>{@code entrySet}</li>
 *     <li>{@code putAll}</li>
 * </ul>
 *
 * @param <K> Key type parameter.
 * @param <V> Value type parameter.
 */
public interface ICollectionMap<K, V> extends Map<K, V>
{
    Collection<V> getAll(K key);

    V remove(Object value, boolean fromAllCollections);
    void removeFrom(K key, V value);
    void removeAll(K key);

    default V remove(Object value)
    {
        return this.remove(value, false);
    }

    default V get(Object key)
    {
        throw new UnsupportedOperationException("Use 'ICollectionMap#getAll(K key)' instead.");
    }

    default Collection<V> values()
    {
        throw new UnsupportedOperationException();
    }

    default Set<Entry<K, V>> entrySet()
    {
        throw new UnsupportedOperationException();
    }

    default void putAll(Map<? extends K, ? extends V> m)
    {
        throw new UnsupportedOperationException();
    }
}
