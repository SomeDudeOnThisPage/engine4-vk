package me.buhlmann.engine4.core.gfx.vulkan.factory;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.gfx.material.IMaterialFactory;
import me.buhlmann.engine4.api.gfx.primitive.IMesh;
import me.buhlmann.engine4.utils.Paths;
import me.buhlmann.engine4.utils.StringUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class AssimpMeshLoader
{
    private static synchronized int[] iUnbox(List<Integer> list)
    {
        final int[] array = new int[list.size()];

        for (int i = 0; i < list.size(); i++)
        {
            array[i] = list.get(i);
        }

        return array;
    }

    private static synchronized float[] fUnbox(List<Float> list)
    {
        final float[] array = new float[list.size()];

        for (int i = 0; i < list.size(); i++)
        {
            array[i] = list.get(i);
        }

        return array;
    }

    private static synchronized List<Float> parseAIVector3(final AIVector3D.Buffer buffer)
    {
        final List<Float> data = new ArrayList<>();

        if (buffer == null) return data;

        while (buffer.remaining() > 0)
        {
            final AIVector3D vertex = buffer.get();
            data.add(vertex.x());
            data.add(vertex.y());
            data.add(vertex.z());
        }

        return data;
    }

    private static List<Float> parseUVCoordinates(final AIMesh aimesh)
    {
        final AIVector3D.Buffer aiTextures = aimesh.mTextureCoords(0);
        final List<Float> data = new ArrayList<>();

        if (aiTextures == null) return data;

        while (aiTextures.remaining() > 0)
        {
            final AIVector3D aiTexture = aiTextures.get();
            data.add(aiTexture.x());
            data.add(1 - aiTexture.y()); // invert texture y coordinate
        }

        return data;
    }

    private static List<Integer> parseIndices(final AIMesh aimesh)
    {
        final List<Integer> data = new ArrayList<>();
        final AIFace.Buffer aiFaces = aimesh.mFaces();

        for (int i = 0; i < aimesh.mNumFaces(); i++)
        {
            final AIFace aiFace = aiFaces.get();
            final IntBuffer buffer = aiFace.mIndices();
            while (buffer.remaining() > 0)
            {
                data.add(buffer.get());
            }
        }

        return data;
    }

    public record MaterialMapping(String mapping, String data) {}
    public record MeshResult(IMesh.MeshData data, int material) {}
    public record MaterialResult(String name, List<MaterialMapping> mapping) {}
    public record Result(List<MeshResult> data, List<MaterialResult> mapping) {}

    private static IMesh.MeshData processMesh(final AIMesh aiMesh)
    {
        final List<Float> vertices = AssimpMeshLoader.parseAIVector3(aiMesh.mVertices());
        final List<Float> normals = AssimpMeshLoader.parseAIVector3(aiMesh.mNormals());
        final List<Float> uv = AssimpMeshLoader.parseUVCoordinates(aiMesh);
        final List<Float> tangents = AssimpMeshLoader.parseAIVector3(aiMesh.mTangents());
        final List<Float> bitangents = AssimpMeshLoader.parseAIVector3(aiMesh.mBitangents());
        final List<Integer> indices = AssimpMeshLoader.parseIndices(aiMesh);

        return new IMesh.MeshData(
            aiMesh.mName().dataString(),
            AssimpMeshLoader.fUnbox(vertices),
            AssimpMeshLoader.fUnbox(normals),
            AssimpMeshLoader.fUnbox(uv),
            AssimpMeshLoader.iUnbox(indices)
        );
    }

    private static MaterialResult processMaterial(final AIMaterial aiMaterial)
    {
        // TODO: more mat-keys..
        final MaterialResult result = new MaterialResult("shitfuckass", new ArrayList<>());
        final PointerBuffer pProperties = aiMaterial.mProperties();
        for (int i = 0; i < aiMaterial.mNumProperties(); i++)
        {
            final AIMaterialProperty aiProperty = AIMaterialProperty.create(pProperties.get(i));
            final String name = aiProperty.mKey().dataString();
            final String data = MemoryUtil.memUTF16(aiProperty.mData());
            if (!StringUtils.isNullOrBlank(data))
            {
                switch (name)
                {
                    case Assimp.AI_MATKEY_NAME ->
                    {
                        AIString aiName = AIString.create();
                        Assimp.aiGetMaterialString(aiMaterial, Assimp.AI_MATKEY_NAME, 0, 0, aiName);
                        result.mapping().add(new MaterialMapping("NAME", aiName.dataString()));
                    }
                    case Assimp.AI_MATKEY_COLOR_AMBIENT ->
                    {
                        AIColor4D colour = AIColor4D.create();
                        int err = Assimp.aiGetMaterialColor(aiMaterial, Assimp.AI_MATKEY_COLOR_AMBIENT, Assimp.aiTextureType_NONE, 0, colour);
                        if (err != Assimp.aiReturn_SUCCESS)
                        {
                            result.mapping().add(new MaterialMapping("AMBIENT", "0.0 0.0 0.0"));
                        }
                        else
                        {
                            result.mapping().add(new MaterialMapping("AMBIENT", (double) colour.r() + " " + (double) colour.g() + " " + (double) colour.b()));
                        }
                    }
                }
            }
        }

        AIString aiDiffuseTexturePath = AIString.calloc();
        Assimp.aiGetMaterialTexture(aiMaterial, Assimp.aiTextureType_DIFFUSE, 0, aiDiffuseTexturePath, (IntBuffer) null,
            null, null, null, null, null);
        final String diffuseTexturePath = aiDiffuseTexturePath.dataString();

        if (!StringUtils.isNullOrBlank(diffuseTexturePath))
        {
            result.mapping().add(new MaterialMapping("DIFFUSE", diffuseTexturePath));
        }
        return result;
    }

    public static synchronized AssimpMeshLoader.Result load(String path, IMaterialFactory.MetaData materials)
    {
        try
        {
            AIScene scene = Assimp.aiImportFile(Paths.resolve(path), Assimp.aiProcess_ImproveCacheLocality
                | Assimp.aiProcess_JoinIdenticalVertices
                | Assimp.aiProcess_Triangulate
                | Assimp.aiProcess_CalcTangentSpace
                | Assimp.aiProcess_GenBoundingBoxes
            );
            if (scene == null)
            {
                throw new UnsupportedOperationException("scene " + Paths.resolve(path) + " is null");
            }

            int numMeshes = scene.mNumMeshes();
            int numMaterials = scene.mNumMaterials();
            int numTextures = scene.mNumTextures();

            final PointerBuffer aiMeshes = scene.mMeshes();
            final PointerBuffer aiMaterials = scene.mMaterials();
            final AssimpMeshLoader.Result result = new Result(new ArrayList<>(), new ArrayList<>());

            assert aiMaterials != null; // TODO
            assert aiMeshes != null; // TODO

            for (int j = 0; j < numMaterials; j++)
            {
                AIMaterial aiMaterial = AIMaterial.create(aiMaterials.get(j));
                result.mapping().add(processMaterial(aiMaterial));
                //final IMesh.MeshData md = AssimpMeshLoader.processMaterial(aiMaterial);
                //result.data().add(md);
            }

            for (int i = 0; i < numMeshes; i++)
            {
                AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
                final IMesh.MeshData md = AssimpMeshLoader.processMesh(aiMesh);
                result.data().add(new MeshResult(md, aiMesh.mMaterialIndex()));
            }

            // Engine4.getLogger().trace("model %s contains %d submeshes with a total of %d vertices", path, numMeshes,
            //     data.stream().mapToInt((md) -> md.vertices().length).sum());
            return result;
        }
        catch (Exception e)
        {
            e.printStackTrace(); // TODO
            return null;
        }
    }
}
