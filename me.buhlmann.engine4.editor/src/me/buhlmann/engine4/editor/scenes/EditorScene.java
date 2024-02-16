package me.buhlmann.engine4.editor.scenes;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.gfx.material.IMaterial;
import me.buhlmann.engine4.api.renderer.ICamera;
import me.buhlmann.engine4.api.scene.AbstractScene;
import me.buhlmann.engine4.api.scene.IScene;
import me.buhlmann.engine4.api.gfx.ModelComponent;
import me.buhlmann.engine4.editor.entity.CameraEntity;
import me.buhlmann.engine4.editor.entity.CameraMovementScript;
import me.buhlmann.engine4.editor.entity.ModelSwapperScript;
import me.buhlmann.engine4.editor.entity.RotationSystem;
import me.buhlmann.engine4.entity.component.TransformComponent;
import me.buhlmann.engine4.factory.EntityFactory;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class EditorScene extends AbstractScene implements IScene
{
    private ICamera camera;

    @Override
    public ICamera getMainCamera()
    {
        return this.camera;
    }

    @Override
    public void onEnter()
    {
        this.camera = EntityFactory.createInScene(new CameraEntity("editor-camera-main"), this, new CameraMovementScript());
        this.camera.setProjection(0.67f, Engine4.getWindow().getSize(), new Vector2f(0.01f, 100.0f));

        final ModelComponent model = new ModelComponent("model.org.khronos.sponza");

        EntityFactory.createInScene(this, "org.khronos.sponza",
            new TransformComponent(
                new Vector3f(0.0f, 0.0f, 0.0f),
                new Quaternionf(),
                new Vector3f(0.03f)
            ),
            model
        );

    }
}
