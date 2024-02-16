package me.buhlmann.engine4.api.scene;

import com.sun.istack.NotNull;
import me.buhlmann.engine4.api.entity.IEntityComponentSystem;
import me.buhlmann.engine4.api.renderer.ICamera;

public interface IScene
{
    IEntityComponentSystem getECS();

    default void onEnter() {}
    default IScene onUpdate()
    {
        return this;
    }
    default void onExit() {}
    default void onDestroy() {}
    default boolean isLoaded()
    {
        return false;
    }
    @NotNull ICamera getMainCamera();
}
