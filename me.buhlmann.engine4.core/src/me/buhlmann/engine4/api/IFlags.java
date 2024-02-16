package me.buhlmann.engine4.api;

public interface IFlags<T, R>
{
    boolean isFlagged(T flag);
    R setFlag(T flag);
    R unsetFlag(T flag);
}
