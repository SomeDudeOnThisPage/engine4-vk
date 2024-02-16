package me.buhlmann.engine4;

import me.buhlmann.engine4.api.scene.IScene;
import me.buhlmann.engine4.api.annotation.EventListener;
import me.buhlmann.engine4.event.OnSceneLoadedEvent;

import java.util.HashMap;
import java.util.Map;

public class SceneManager
{
    private IScene active;
    private boolean onEnterFired;

    private Map<String, IScene> loaded;
    private Map<String, IScene> loading;

    @EventListener(OnSceneLoadedEvent.class)
    public void onSceneLoaded(OnSceneLoadedEvent event)
    {
        Engine4.getLogger().trace("[SCENE MANAGER] onSceneLoaded w/ sceneID '" + event.getSceneID() + "'");
        if (this.loading.containsKey(event.getSceneID()))
        {
            this.loaded.put(event.getSceneID(), this.loading.get(event.getSceneID()));
            this.loading.remove(event.getSceneID());
        }
    }

    public void update(float dt)
    {
        if (!this.onEnterFired)
        {
            this.active.onEnter();
            this.onEnterFired = true;
        }
        this.active.getECS().update(dt);
        final IScene scene = this.active.onUpdate();
        if (scene != this.active)
        {
            this.setActiveScene(scene);
        }
    }

    public void setActiveScene(final IScene scene)
    {
        if (this.active != null)
        {
            this.active.onExit();
        }

        this.active = scene;
        this.active.onEnter();
        if (Engine4.getRenderer() != null)
        {
            Engine4.getRenderer().bind(this.active);
            this.active.getECS().update(Float.NaN);
        }
    }

    public IScene getActiveScene()
    {
        return this.active;
    }

    public SceneManager()
    {
        this.loaded = new HashMap<>();
        this.loading = new HashMap<>();
        this.onEnterFired = false;

        Engine4.getEventBus().subscribe(this);
    }
}
