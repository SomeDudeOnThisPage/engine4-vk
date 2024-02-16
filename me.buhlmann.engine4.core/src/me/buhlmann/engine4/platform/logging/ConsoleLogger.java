package me.buhlmann.engine4.platform.logging;

import me.buhlmann.engine4.api.ILogger;

import java.io.PrintStream;
import java.util.Arrays;

@SuppressWarnings("unused")
public class ConsoleLogger implements ILogger
{
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";

    private final ILogger.LEVEL level;

    private static String serialize(final Throwable e)
    {
        if (e == null) {
            return "Failed to serialize Throwable to String - null";
        }

        final StackTraceElement[] trace = e.getStackTrace();
        final StringBuilder string = new StringBuilder();

        string.append(e);
        for (StackTraceElement element : trace)
        {
            string.append("\n\tat ").append(element.toString());
        }

        return string.toString();
    }

    private String formatStackTrace(final StackTraceElement trace)
    {
        // Intellij link formatting
        // TODO: other platforms?
        return String.format("(%s:%d)", trace.getFileName(), trace.getLineNumber());
    }

    @Override
    public void trace(final String message, final Object... parameters)
    {
        if (this.level.getValue() < ILogger.LEVEL.TRACE.getValue())
        {
            return;
        }

        System.out.println(ANSI_BLUE + "[TRACE] " + String.format(message, parameters) + " @" + this.formatStackTrace(Thread.currentThread().getStackTrace()[2]) + ANSI_RESET);
    }

    @Override
    public void info(final String message, final Object... parameters)
    {
        if (this.level.getValue() < ILogger.LEVEL.INFO.getValue())
        {
            return;
        }

        System.out.println("[INFO] " + String.format(message, parameters) + " @" + this.formatStackTrace(Thread.currentThread().getStackTrace()[2]));
    }

    @Override
    public void warning(final String message, final Object... parameters)
    {
        if (this.level.getValue() < ILogger.LEVEL.WARNING.getValue())
        {
            return;
        }

        System.out.println(ANSI_YELLOW + "[INFO] " + String.format(message, parameters) + " @" + this.formatStackTrace(Thread.currentThread().getStackTrace()[2]) + ANSI_RESET);
    }

    @Override
    public void error(final String message, final Object... parameters)
    {
        if (this.level.getValue() < ILogger.LEVEL.ERROR.getValue())
        {
            return;
        }

        System.out.println(ANSI_RED + "[INFO] " + String.format(message, parameters) + " @" + this.formatStackTrace(Thread.currentThread().getStackTrace()[2]) + ANSI_RESET);
    }

    @Override
    public void trace(final Throwable... messages)
    {
        Arrays.stream(messages).map(ConsoleLogger::serialize).forEach(this::trace);
    }

    @Override
    public void info(final Throwable... messages)
    {
        Arrays.stream(messages).map(ConsoleLogger::serialize).forEach(this::info);
    }

    @Override
    public void warning(final Throwable... messages)
    {
        Arrays.stream(messages).map(ConsoleLogger::serialize).forEach(this::warning);
    }

    @Override
    public void error(final Throwable... messages)
    {
        Arrays.stream(messages).map(ConsoleLogger::serialize).forEach(this::error);
    }

    public ConsoleLogger(final PrintStream out, final ILogger.LEVEL level)
    {
        this.level = level;
    }
}
