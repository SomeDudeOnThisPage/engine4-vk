package me.buhlmann.engine4.utils;

import me.buhlmann.engine4.api.utils.ICollectionMap;

import java.util.*;

@SuppressWarnings("unchecked")
public class ListMap<K, V> implements ICollectionMap<K, V>
{
    private Map<K, List<V>> map;
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
    public boolean containsValue(Object value)
    {
        for (List<V> set : this.map.values())
        {
            if (set.contains((V) value))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<K> keySet()
    {
        return this.map.keySet();
    }

    @Override
    public Collection<V> getAll(Object key)
    {
        return this.map.get((K) key);
    }

    @Override
    public V remove(Object value, boolean fromAllCollections)
    {
        for (Map.Entry<K, List<V>> entry : this.map.entrySet()) {
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
    public V put(K key, V value)
    {
        if (!this.map.containsKey(key))
        {
            this.map.put(key, new ArrayList<>());
        }

        this.map.get(key).add(value);

        return value;
    }

    public void instantiate(K... keys)
    {
        for (K key : keys)
        {
            if (!this.map.containsKey(key))
            {
                this.map.put(key, new ArrayList<>());
            }
        }
    }

    public ListMap()
    {
        this.map = new HashMap<>();
        this.remove = new LinkedList<>();
    }
}
