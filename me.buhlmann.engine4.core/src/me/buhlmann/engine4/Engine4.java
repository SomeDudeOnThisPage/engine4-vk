package me.buhlmann.engine4;

import me.buhlmann.engine4.api.*;
import me.buhlmann.engine4.api.asset2.IAssetManager2;
import me.buhlmann.engine4.api.renderer.IRenderer;
import me.buhlmann.engine4.asset.AssetManager;
import me.buhlmann.engine4.asset.GCAssetManager;
import me.buhlmann.engine4.configuration.Arguments;
import me.buhlmann.engine4.event.EventBus;
import me.buhlmann.engine4.platform.Keyboard;
import me.buhlmann.engine4.platform.logging.ConsoleLogger;
import me.buhlmann.engine4.platform.window.GLFWWindow;

public class Engine4 implements IEngine
{
    private static Engine4 instance;
    private Arguments arguments;

    private SceneManager sceneManager;
    private GLFWWindow glfwWindow;
    private EventBus eventBus;
    private IRenderer renderer;
    private Keyboard keyboard;

    private ILogger logger;
    private IAssetManager2 assetManager;

    public static Engine4 getInstance()
    {
        return Engine4.instance;
    }

    public static IAssetManager2 getAssetManager()
    {
        return Engine4.getInstance().assetManager;
    }

    public static IRenderer getRenderer()
    {
        return Engine4.getInstance().renderer;
    }

    public static void setRenderer(final IRenderer renderer)
    {
        Engine4.getInstance().renderer = renderer;
    }

    public static ILogger getLogger()
    {
        return Engine4.getInstance().logger;
    }

    public static EventBus getEventBus()
    {
        return Engine4.getInstance().eventBus;
    }

    public static Keyboard getKeyboard()
    {
        return Engine4.getInstance().keyboard;
    }

    public static GLFWWindow getWindow()
    {
        return Engine4.getInstance().glfwWindow;
    }

    public boolean shouldClose()
    {
        return this.glfwWindow.shouldClose();
    }

    public Arguments getArguments()
    {
        return this.arguments;
    }

    private double last;
    private long fps;
    private long lastFPS;

    /**
     * Tick the entire engine logic once.
     * This updates all engine systems/logic, then hands control to client application via the currently set scene.
     * Afterward, engine systems/logic required to run after client application are run.
     */
    public void tick()
    {
        double time = System.currentTimeMillis();
        double ft = time - last;
        this.fps++;

        if (ft <= 0) {
            ft = 0.000001;
        }

        // Update pre-tick systems.
        this.eventBus.update();
        this.assetManager.update();

        // Poll events for current tick.
        this.glfwWindow.poll();

        // Update scene with game logic (hand control to client application).
        this.sceneManager.update((float) ft / 1000.0f);

        this.renderer.render(new IRenderer.Input(
            Engine4.getWindow(),
            Engine4.getInstance().sceneManager.getActiveScene(),
            this.sceneManager.getActiveScene().getMainCamera())
        );

        // Finish tick with swapping old to new buffer.
        this.glfwWindow.swapBuffers();

        this.last = time;

        if (time - this.lastFPS >= 1000.0) {
            Engine4.getLogger().info("FPS: " + this.fps);
            this.fps = 0;
            this.lastFPS = System.currentTimeMillis();
        }
    }

    @Override
    public void start()
    {
        try {
            while (!this.shouldClose())
            {
                this.tick();
            }
        }
        catch (Exception engine)
        {
            this.logger.error(engine);
            if (this.renderer != null)
            {
                this.renderer.terminate();
            }
        }
        finally
        {
            if (this.renderer != null)
            {
                this.renderer.finish();
            }

            this.assetManager.shutdown();

            if (this.renderer != null)
            {
                this.renderer.terminate();
            }

            this.glfwWindow.terminate();
        }
    }

    public Engine4(final EngineCreationInfo info)
    {
        if (Engine4.instance != null) {
            Engine4.getLogger().error("Engine singleton already defined.");
            return;
        }
        Engine4.instance = this;
        this.arguments = new Arguments();
        this.arguments.parse(info.args());

        // Setup initial logger for startup phase.
        this.logger = new ConsoleLogger(System.out, ILogger.LEVEL.INFO);

        try
        {
            this.eventBus = new EventBus();
            this.assetManager = new GCAssetManager();

            this.sceneManager = new SceneManager();
            this.glfwWindow = info.window();
            this.keyboard = new Keyboard(this.glfwWindow);

            this.renderer = info.renderer().getConstructor().newInstance();
            this.renderer.initialize(info.window());
            this.sceneManager.setActiveScene(info.scene().getConstructor().newInstance());
        }
        catch (Exception engine)
        {
            this.logger.error(engine);
        }
    }
}
