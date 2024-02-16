package me.buhlmann.engine4.persistence;

import org.w3c.dom.Element;

public interface IPersistedSceneElement
{
    Element serialize();
    void deserialize(final Element element);
}
