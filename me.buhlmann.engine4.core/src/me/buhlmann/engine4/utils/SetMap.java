package me.buhlmann.engine4.utils;

import me.buhlmann.engine4.api.utils.ICollectionMap;

import java.util.*;

@SuppressWarnings("unchecked")
public class SetMap<K, V> implements ICollectionMap<K, V> {
    private final Map<K, Set<V>> map;
    private final List<K> remove;

    @Override
    public int size()
    {
        return map.size();
    }

    @Override
    public void clear()
    {
        this.map.clear();
    }

    @Override
    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key)
    {
        return map.containsKey(key);
    }

    @Override
    public Set<K> keySet()
    {
        return this.map.keySet();
    }

    @Override
    public boolean containsValue(Object value)
    {
        for (Set<V> set : this.map.values())
        {
            if (set.contains((V) value))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public V put(K key, V value)
    {
        if (!this.map.containsKey(key))
        {
            this.map.put(key, new HashSet<>());
        }

        this.map.get(key).add(value);

        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SetMap) {
            return this.map.equals(((SetMap<?, ?>) o).map);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.map.hashCode();
    }

    @Override
    public Collection<V> getAll(K key)
    {
        return this.map.get(key);
    }

    @Override
    public void removeFrom(K key, V value)
    {
        this.map.get(key).remove(value);
        if (this.map.get(key).isEmpty())
        {
            this.map.remove(key);
        }
    }

    @Override
    public void removeAll(K key)
    {
        this.map.remove(key);
    }

    @Override
    public V remove(Object value, boolean fromAllCollections)
    {
        for (Map.Entry<K, Set<V>> entry : this.map.entrySet()) {
            if (entry.getValue().contains((V) value))
            {
                entry.getValue().remove((V) value);

                if (entry.getValue().isEmpty())
                {
                    this.remove.add(entry.getKey());
                }

                if (!fromAllCollections)
                {
                    break;
                }
            }
        }

        for (K key : this.remove)
        {
            this.map.remove(key);
        }
        this.remove.clear();

        return (V) value;
    }

    public void instantiate(K... keys)
    {
        for (K key : keys)
        {
            if (!this.map.containsKey(key))
            {
                this.map.put(key, new HashSet<>());
            }
        }
    }

    public SetMap()
    {
        this.map = new HashMap<>();
        this.remove = new LinkedList<>();
    }
}
