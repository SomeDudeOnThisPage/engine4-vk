package me.buhlmann.engine4.core.gfx.vulkan.utils;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.gfx.ShaderAttribute;
import me.buhlmann.engine4.core.gfx.vulkan.context.VulkanDeviceContext;
import me.buhlmann.engine4.core.gfx.vulkan.descriptor.VulkanBufferedUniform;
import me.buhlmann.engine4.utils.Paths;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.*;
import org.lwjgl.vulkan.VK11;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class VulkanShaderCompiler
{
    private static final Map<String, String> MACRO_DEFINITIONS = new HashMap<>()
    {{
        put("_E4C_PIPELINE", "layout(set = 0, binding = 0) uniform _EC4_u_pipeline_buffer");
        put("_E4_MATERIAL", "layout(set = 1, binding = 0) uniform _EC4_u_material_buffer");
        put("_E4_MATERIAL_SAMPLER_1", "layout(set = 1, binding = 1) uniform sampler2D");
        put("_E4_MATERIAL_SAMPLER_2", "layout(set = 1, binding = 2) uniform sampler2D");
        put("_E4_MATERIAL_SAMPLER_3", "layout(set = 1, binding = 3) uniform sampler2D");
        put("_E4_MATERIAL_SAMPLER_4", "layout(set = 1, binding = 4) uniform sampler2D");
        put("_E4_MATERIAL_SAMPLER_5", "layout(set = 1, binding = 5) uniform sampler2D");
    }};

    private static final Map<Integer, Integer> VK_SHADER_TYPES = new HashMap<>()
    {{
        put(VK11.VK_SHADER_STAGE_VERTEX_BIT, Shaderc.shaderc_vertex_shader);
        put(VK11.VK_SHADER_STAGE_GEOMETRY_BIT, Shaderc.shaderc_geometry_shader);
        put(VK11.VK_SHADER_STAGE_FRAGMENT_BIT, Shaderc.shaderc_fragment_shader);
    }};

    private static final String VK_SHADER_INCLUDE_PATH = "shaders;shaders/core-deferred";
    private static final String VK_SPIRV_SHADER_CACHE = "shaders/.shadercache/";

    @SuppressWarnings("all")
    private static final List<ShadercIncludeResult> INCLUDE_RESULTS = new ArrayList<>();

    private static void setShaderInputMacros(final long options)
    {
        for (final ShaderAttribute attribute : ShaderAttribute.values())
        {
            final String definition = String.format(
                "layout(location = %d) in %s",
                attribute.getLocation(),
                attribute.getDataType().getGLSLName()
            );
            Shaderc.shaderc_compile_options_add_macro_definition(options, attribute + "_ATTRIBUTE", definition);
        }
    }

    private static void setShaderTypeMacros(final long options, final int vkShaderType)
    {
        switch (vkShaderType)
        {
            case VK11.VK_SHADER_STAGE_VERTEX_BIT -> Shaderc.shaderc_compile_options_add_macro_definition(options, "VERTEX_SHADER", "");
            case VK11.VK_SHADER_STAGE_FRAGMENT_BIT -> Shaderc.shaderc_compile_options_add_macro_definition(options, "FRAGMENT_SHADER", "");
            case VK11.VK_SHADER_STAGE_GEOMETRY_BIT -> Shaderc.shaderc_compile_options_add_macro_definition(options, "GEOMETRY_SHADER", "");
        }
    }

    private static Path resolveShaderPath(final String requested, final String requesting, final int type)
    {
        if (type == Shaderc.shaderc_include_type_standard)
        {
            for (String part : VulkanShaderCompiler.VK_SHADER_INCLUDE_PATH.split(File.pathSeparator))
            {
                if (Files.notExists(Path.of(Paths.resolve(part + File.separator + requested))))
                {
                    continue;
                }

                Engine4.getLogger().trace("found included shader (standard) " + part + File.separator + requested);
                return Path.of(Paths.resolve(part + File.separator + requested));
            }
        }
        else if (type == Shaderc.shaderc_include_type_relative)
        {
            final Path parent = Path.of(requesting).getParent();
            final Path relative = parent.resolve(requested);

            if (Files.exists(relative))
            {
                Engine4.getLogger().trace("found included shader (relative) " + relative + File.separator + requested);
                return relative;
            }
        }

        Engine4.getLogger().error("could not find shader include " + requested + " on include paths");
        return null;
    }

    public static byte[] compile(final VulkanDeviceContext vkContext, final String name, final String code, final int vkShaderType)
    {
        final long compiler = Shaderc.shaderc_compiler_initialize();
        final long options = Shaderc.shaderc_compile_options_initialize();

        // Setup include callbacks.
        Shaderc.shaderc_compile_options_set_include_callbacks(
            options,
            /* org.lwjgl.util.shaderc.ShadercIncludeResolveI */ (userdata, requested_source, type, requesting_source, depth) ->
            {
                // Don't auto-close result (try-with-resources) here!
                final ShadercIncludeResult result = ShadercIncludeResult.create();
                final String requested = MemoryUtil.memASCII(requested_source);
                final String requesting = MemoryUtil.memASCII(requesting_source);
                Engine4.getLogger().trace("[SHADERC] processing include_resolve", "requested = " + requested, "requesting = " + requesting);

                final Path path = VulkanShaderCompiler.resolveShaderPath(requested, requesting, type);
                if (path == null)
                {
                    return Shaderc.shaderc_compilation_status_compilation_error;
                }

                try
                {
                    final String data = Files.readString(path);
                    // Don't null-terminate these string buffers!
                    result.source_name(MemoryUtil.memASCIISafe(requested, false));
                    result.content(MemoryUtil.memASCIISafe(data, false));

                    Engine4.getLogger().trace("[SHADERC] shader code for included '" + requested + "':", data, "string length = " + data.length(), "size = " + result.content_length());
                }
                catch (IOException e)
                {
                    Engine4.getLogger().error(e);
                    return Shaderc.shaderc_compilation_status_compilation_error;
                }

                // Keep the result active in the JVM until we can free it on the native side.
                VulkanShaderCompiler.INCLUDE_RESULTS.add(result);
                return result.address();
            },
            /* org.lwjgl.util.shaderc.ShadercIncludeResultReleaseI */ (userdata, address) ->
            {
                final ShadercIncludeResult result = ShadercIncludeResult.create(address);
                Engine4.getLogger().trace("[SHADERC] include_result_release " + result.source_name());

                // Free the memory in native code.
                // result.free();

                // Let the result be collected by the GC.
                VulkanShaderCompiler.INCLUDE_RESULTS.remove(result);
            },
            0L
        );

        // Add custom macro definitions.
        for (Map.Entry<String, String> entry : VulkanShaderCompiler.MACRO_DEFINITIONS.entrySet())
        {
            Shaderc.shaderc_compile_options_add_macro_definition(options, entry.getKey(), entry.getValue());
        }

        VulkanShaderCompiler.setShaderTypeMacros(options, vkShaderType);
        VulkanShaderCompiler.setShaderInputMacros(options);

        Shaderc.shaderc_compile_options_set_forced_version_profile(options, 450, Shaderc.shaderc_profile_core);
        long result = Shaderc.shaderc_compile_into_spv(
            compiler,
            code,
            VulkanShaderCompiler.VK_SHADER_TYPES.get(vkShaderType),
            name,
            "main",
            options
        );

        if (Shaderc.shaderc_result_get_compilation_status(result) != Shaderc.shaderc_compilation_status_success)
        {
            throw new RuntimeException("failed to compile shader: " + Shaderc.shaderc_result_get_error_message(result));
        }

        final ByteBuffer buffer = Shaderc.shaderc_result_get_bytes(result);
        if (buffer == null)
        {
            throw new RuntimeException("failed to compile shader: " + Shaderc.shaderc_result_get_error_message(result));
        }

        final byte[] compiled = new byte[buffer.remaining()];
        buffer.get(compiled);
        Shaderc.shaderc_compile_options_release(options);
        Shaderc.shaderc_compiler_release(compiler);
        return compiled;
    }

    public record ShaderCompilationResult(String file, VulkanBufferedUniform<?>[] vkMaterialUniforms) {}

    public static ShaderCompilationResult compile(final VulkanDeviceContext vkContext, final String path, final int vkShaderType)
    {
        try
        {
            final File glsl = new File(Paths.resolve(path));
            final String cached = Base64.getEncoder().encodeToString((Paths.resolve(path) + "." + vkShaderType + ".spv").getBytes()).replace("=", "");
            final File spv = new File(Paths.resolve(VulkanShaderCompiler.VK_SPIRV_SHADER_CACHE) + cached);

            Engine4.getLogger().trace("compiling " + glsl.getPath() + " to " + spv.getPath());

            final String code = new String(Files.readAllBytes(glsl.toPath()));
            // TODO: Extract uniform buffers and write them to a separate file to parse later...
            // TODO: Can we incorporate this into the spv file?
            final VulkanBufferedUniform<?>[] vkMaterialUniforms = ShaderUtils.getBufferedUniformsFromShader("_MATERIAL", code);
            final byte[] bytecode = VulkanShaderCompiler.compile(vkContext, spv.getAbsolutePath(), code, vkShaderType);
            Files.write(spv.toPath(), bytecode);

            return new ShaderCompilationResult(spv.getPath(), vkMaterialUniforms);
        }
        catch (IOException exception)
        {
            throw new RuntimeException(exception);
        }
    }
}
