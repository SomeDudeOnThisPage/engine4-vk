package me.buhlmann.engine4.platform.window;

public enum VSyncState
{
    ENABLED(1),
    DISABLED(0);

    private int value;

    public int getValue()
    {
        return this.value;
    }

    VSyncState(int value)
    {
        this.value = value;
    }
}
