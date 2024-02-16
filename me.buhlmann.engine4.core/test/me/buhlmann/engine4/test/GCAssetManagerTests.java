package me.buhlmann.engine4.test;

import me.buhlmann.engine4.api.asset2.IAsset2;
import me.buhlmann.engine4.api.asset2.IAssetDefinition;
import me.buhlmann.engine4.api.gfx.primitive.IModel;
import me.buhlmann.engine4.asset.FilesystemAssetDefinition;
import me.buhlmann.engine4.asset.GCAssetManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GCAssetManagerTests
{
    private GCAssetManager gcAssetManager;

    @BeforeEach
    protected void setup()
    {
        this.gcAssetManager = new GCAssetManager();
    }

    @Test
    protected void testModelFactory()
    {
        final IAssetDefinition<IModel> definition = new FilesystemAssetDefinition<>("sponza", "test/models/sponza/sponza.obj", IModel.class);

    }

    @Test
    protected void testAsynchronousAssetLoading()
    {
        class TestAsset implements IAsset2
        {
            @Override
            public void dispose()
            {

            }
        }

        final IAssetDefinition<TestAsset> definition = new FilesystemAssetDefinition<>("sponza", "test/models/sponza/sponza.obj", TestAsset.class);
        final TestAsset model = this.gcAssetManager.load(definition);

        while (this.gcAssetManager.isLoading())
        {
            // Engine4.getLogger().trace("loading...");
        }

        Assertions.assertNotNull(this.gcAssetManager.get(TestAsset.class, "sponza"));
    }
}
