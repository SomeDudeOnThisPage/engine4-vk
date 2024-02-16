package me.buhlmann.engine4.api;

public interface ILogger
{
    enum LEVEL
    {
        TRACE(3),
        INFO(2),
        WARNING(1),
        ERROR(0);

        private final int value;

        public int getValue()
        {
            return this.value;
        }

        LEVEL(int value)
        {
            this.value = value;
        }
    }

    void trace(String message, Object... parameters);
    void info(String message, Object... parameters);
    void warning(String message, Object... parameters);
    void error(String message, Object... parameters);

    void trace(Throwable... messages);
    void info(Throwable... messages);
    void warning(Throwable... messages);
    void error(Throwable... messages);
}
