package me.buhlmann.engine4.platform;

import me.buhlmann.engine4.platform.window.GLFWWindow;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class Keyboard
{
    public enum Key
    {
        SPACE(GLFW.GLFW_KEY_SPACE),
        LCONTROL(GLFW.GLFW_KEY_LEFT_CONTROL),
        APOSTROPHE(GLFW.GLFW_KEY_APOSTROPHE),
        COMMA(GLFW.GLFW_KEY_COMMA),
        MINUS(GLFW.GLFW_KEY_MINUS),
        PERIOD(GLFW.GLFW_KEY_PERIOD),
        SLASH(GLFW.GLFW_KEY_SLASH),
        K0(GLFW.GLFW_KEY_0),
        K1(GLFW.GLFW_KEY_1),
        K2(GLFW.GLFW_KEY_2),
        K3(GLFW.GLFW_KEY_3),
        K4(GLFW.GLFW_KEY_4),
        K5(GLFW.GLFW_KEY_5),
        K6(GLFW.GLFW_KEY_6),
        K7(GLFW.GLFW_KEY_7),
        K8(GLFW.GLFW_KEY_8),
        K9(GLFW.GLFW_KEY_9),
        SEMICOLON(GLFW.GLFW_KEY_SEMICOLON),
        EQUAL(GLFW.GLFW_KEY_EQUAL),
        A(GLFW.GLFW_KEY_A),
        B(GLFW.GLFW_KEY_B),
        C(GLFW.GLFW_KEY_C),
        D(GLFW.GLFW_KEY_D),
        E(GLFW.GLFW_KEY_E),
        F(GLFW.GLFW_KEY_F),
        G(GLFW.GLFW_KEY_G),
        H(GLFW.GLFW_KEY_H),
        I(GLFW.GLFW_KEY_I),
        J(GLFW.GLFW_KEY_J),
        K(GLFW.GLFW_KEY_K),
        L(GLFW.GLFW_KEY_L),
        M(GLFW.GLFW_KEY_M),
        N(GLFW.GLFW_KEY_N),
        O(GLFW.GLFW_KEY_O),
        P(GLFW.GLFW_KEY_P),
        Q(GLFW.GLFW_KEY_Q),
        R(GLFW.GLFW_KEY_R),
        S(GLFW.GLFW_KEY_S),
        T(GLFW.GLFW_KEY_T),
        U(GLFW.GLFW_KEY_U),
        V(GLFW.GLFW_KEY_V),
        W(GLFW.GLFW_KEY_W),
        X(GLFW.GLFW_KEY_X),
        Y(GLFW.GLFW_KEY_Y),
        Z(GLFW.GLFW_KEY_Z),
        ;

        private final int glfw;

        public int getGLFWMapping()
        {
            return this.glfw;
        }

        Key(final int mapping)
        {
            this.glfw = mapping;
        }
    }

    private final Map<Keyboard.Key, Boolean> pressed;

    public boolean isDown(final Keyboard.Key... keys)
    {
        for (final Keyboard.Key key : keys)
        {
            if (!this.pressed.get(key))
            {
                return false;
            }
        }
        return true;
    }

    public Keyboard(final GLFWWindow window)
    {
        this.pressed = new HashMap<>();
        final Map<Integer, Keyboard.Key> km = new HashMap<>();

        for (final Keyboard.Key key : Keyboard.Key.values())
        {
            this.pressed.put(key, false);
            km.put(key.getGLFWMapping(), key);
        }

        GLFW.glfwSetKeyCallback(window.getHandle(), (long handle, int keycode, int scancode, int action, int mods) -> {
            final Keyboard.Key key = km.get(keycode);
            if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)
            {
                this.pressed.put(key, true);
            }
            else if (action == GLFW.GLFW_RELEASE)
            {
                this.pressed.put(key, false);
            }
        });
    }
}
