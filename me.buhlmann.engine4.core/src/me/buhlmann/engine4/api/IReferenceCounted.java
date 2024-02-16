package me.buhlmann.engine4.api;

public interface IReferenceCounted
{
    void addReference();
    void removeReference();
    int getReferenceCount();
}
