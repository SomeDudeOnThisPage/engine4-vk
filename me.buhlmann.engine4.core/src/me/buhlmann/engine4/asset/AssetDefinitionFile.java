package me.buhlmann.engine4.asset;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.IAsset;
import me.buhlmann.engine4.api.IAssetFactory;
import me.buhlmann.engine4.api.IAssetManager;
import me.buhlmann.engine4.api.IAssetReference;
import me.buhlmann.engine4.utils.Paths;
import me.buhlmann.engine4.utils.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssetDefinitionFile
{
    final List<IAssetReference<? extends IAsset>> assets;
    final Map<String, Element> meta;

    private <T extends IAsset> void loadSynchronous(final AssetManager am, final AssetLoadTask<T> task)
    {
        try
        {
            IAssetReference<T> reference = task.reference();
            IAssetFactory<T, ? extends IAssetFactory.MetaData> factory = am.getAssetFactory(reference.getType());
            synchronized (factory)
            {
                final JAXBContext context = JAXBContext.newInstance(factory.getMetaDataClass());
                final Unmarshaller unmarshaller = context.createUnmarshaller();
                final Object data = unmarshaller.unmarshal(task.data());
                final T loaded = factory.load(data);
                reference.set(loaded);
                reference.setLoadingStage(IAssetReference.LoadingStage.LOADED);
            }
        }
        catch (final Exception e)
        {
            Engine4.getLogger().error(e);
            throw new UnsupportedOperationException(e);
        }
    }

    private <T extends IAsset> void add(final IAssetReference<T> reference, final IAssetManager manager)
    {
        if (manager instanceof AssetManager am)
        {
            final Element element = this.meta.get(reference.getKey());
            final boolean async = StringUtils.isNullOrBlank(element.getAttribute("initialize")) || element.getAttribute("initialize").equals("async");
            final AssetLoadTask<T> task = new AssetLoadTask<>(this.meta.get(reference.getKey()), reference);

            am.put(reference.getType(), reference.getKey(), reference);

            if (async)
            {
                am.getAssetLoadingQueue().add(task);
                am.addLoadingTimePerformanceTracker(reference.getKey());
                Engine4.getLogger().trace("[ASSET MANAGER] loading asset '" + reference.getKey() + "' asynchronously");
            }
            else
            {
                Engine4.getLogger().trace("[ASSET MANAGER] loading asset '" + reference.getKey() + "' synchronously");
                this.loadSynchronous(am, task);
            }
        }
    }

    public boolean isLoaded()
    {
        return this.assets.parallelStream().allMatch((asset) -> asset.getLoadingStage() == IAssetReference.LoadingStage.LOADED);
    }

    public void addAll(final IAssetManager manager)
    {
        for (IAssetReference<? extends IAsset> reference : this.assets)
        {
            this.add(reference, manager);
        }
    }

    public AssetDefinitionFile(final String file)
    {
        this.assets = new ArrayList<>();
        this.meta = new HashMap<>();

        final String path = Paths.resolve(file);
        try
        {
            final byte[] data = Files.readAllBytes(Path.of(path + ".xml"));
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = f.newDocumentBuilder();
            StringBuilder xmlStringBuilder = new StringBuilder(new String(data));
            xmlStringBuilder.insert(0, "<?xml version=\"1.0\"?>\n");

            ByteArrayInputStream input = new ByteArrayInputStream(xmlStringBuilder.toString().getBytes(StandardCharsets.UTF_8));
            Document document = builder.parse(input);

            Element root = (Element) document.getElementsByTagName("manifest").item(0);
            NodeList children = root.getChildNodes();

            for (int i = 0; i < children.getLength(); i++)
            {
                final Node child = children.item(i);
                if (child.getNodeType() != Node.ELEMENT_NODE)
                {
                    continue;
                }

                final Element element = (Element) child;
                final String tag = element.getTagName();
                final String id = element.getAttribute("id");
                // final String counted = element.getAttribute("refcount");

                if (id == null)
                {
                    Engine4.getLogger().error("skipped asset definition due to missing id");
                    continue;
                }

                IAssetFactory<? extends IAsset, ?> factory = Engine4.getAssetManager().getAssetFactory(tag);
                if (factory != null)
                {
                    final IAssetReference<? extends IAsset> reference = factory.createAssetReference(id);
                    this.assets.add(reference);
                    this.meta.put(reference.getKey(), element);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
