package me.buhlmann.engine4.api.exception;

public class GLFWException extends RuntimeException
{
    private final int code;
    private final long description;

    public GLFWException(final int code, final long description)
    {
        this.code = code;
        this.description = description;
    }

    @Override
    public String toString()
    {
        return String.format("GLFW error #%d: %dl", this.code, this.description);
    }
}
