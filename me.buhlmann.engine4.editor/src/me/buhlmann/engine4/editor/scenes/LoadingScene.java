package me.buhlmann.engine4.editor.scenes;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.gfx.ModelComponent;
import me.buhlmann.engine4.api.gfx.material.IMaterial;
import me.buhlmann.engine4.api.gfx.primitive.IModel;
import me.buhlmann.engine4.api.renderer.ICamera;
import me.buhlmann.engine4.api.scene.AbstractScene;
import me.buhlmann.engine4.api.scene.IScene;
import me.buhlmann.engine4.asset.AssetDefinitionFile;
import me.buhlmann.engine4.asset.AssetManager;
import me.buhlmann.engine4.core.gfx.vulkan.factory.VulkanMaterialFactory;
import me.buhlmann.engine4.core.gfx.vulkan.factory.VulkanModelFactory;
import me.buhlmann.engine4.editor.entity.CameraEntity;
import me.buhlmann.engine4.editor.entity.CameraMovementScript;
import me.buhlmann.engine4.editor.entity.RotationSystem;
import me.buhlmann.engine4.entity.component.TransformComponent;
import me.buhlmann.engine4.factory.EntityFactory;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class LoadingScene extends AbstractScene
{
    private ICamera camera;
    private AssetDefinitionFile assets;
    private AssetDefinitionFile definition;

    @Override
    public ICamera getMainCamera()
    {
        return this.camera;
    }

    private void getVulkanModelData()
    {
        this.definition = new AssetDefinitionFile("manifest");
        this.definition.addAll(Engine4.getAssetManager());
    }

    @Override
    public void onEnter()
    {
        ((AssetManager) Engine4.getAssetManager()).addAssetFactory(IModel.class, new VulkanModelFactory());
        ((AssetManager) Engine4.getAssetManager()).addAssetFactory(IMaterial.class, new VulkanMaterialFactory());

        this.assets = new AssetDefinitionFile("loading");
        this.assets.addAll(Engine4.getAssetManager());

        this.getVulkanModelData();

        this.camera = EntityFactory.createInScene(new CameraEntity("loading-camera-main"), this, new CameraMovementScript());
        this.camera.setProjection(0.67f, Engine4.getWindow().getSize(), new Vector2f(0.01f, 100.0f));
        this.camera.getComponent(TransformComponent.class).setPosition(new Vector3f(-4.0f, 0.0f, -10.0f));

        // EntityFactory.createInScene(this, new RotationSystem());
        EntityFactory.createInScene(this, "my cube",
            new TransformComponent(
                new Vector3f(0.0f, 0.0f, 0.0f),
                new Quaternionf(),
                new Vector3f(1.0f)
            ),
            new ModelComponent("model.loading").setMaterial(0, Engine4.getAssetManager().request(IMaterial.class, "mtl.loading"))
        );
    }

    private long delay = 0;

    @Override
    public IScene onUpdate()
    {
        if (!this.definition.isLoaded())
        {
           return this;
        }

        return new EditorScene();
    }
}
