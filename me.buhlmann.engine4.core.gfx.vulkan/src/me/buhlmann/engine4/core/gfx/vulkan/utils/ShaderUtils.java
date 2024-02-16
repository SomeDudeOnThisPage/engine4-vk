package me.buhlmann.engine4.core.gfx.vulkan.utils;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.core.gfx.vulkan.descriptor.VulkanBufferedUniform;
import me.buhlmann.engine4.utils.StringUtils;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShaderUtils
{
    private static final Pattern ELEMENTS_IN_BLOCK_PATTERN = Pattern.compile("(?<=\\{)[^}]*(?=\\})");
    private static final Pattern UNIFORM_TYPE_IN_CODE = Pattern.compile("(?<=\\s|;)(int|float|mat.|vec.)(?=\\s)(.*)");

    private ShaderUtils()
    {

    }

    public static VulkanBufferedUniform<?>[] getBufferedUniformsFromShader(final String definition, final String code)
    {
        final Pattern pattern = Pattern.compile("\\s*"+ definition + "\\s*\\{[^}]*\\}.*;");
        Matcher matcher = pattern.matcher(code);

        if (!matcher.find())
        {
            return new VulkanBufferedUniform[0];
            // throw new RuntimeException("could not find " + definition + " pattern in shadercode.");
        }

        final String block = matcher.group(0);
        matcher = ShaderUtils.ELEMENTS_IN_BLOCK_PATTERN.matcher(block);
        if (!matcher.find())
        {
            throw new RuntimeException("could not find elements pattern inside _MATERIAL pattern in shadercode.");
        }

        final List<String> elements = Arrays.stream(matcher.group(0).split(";")).filter((s) -> !StringUtils.isNullOrBlank(s)).toList();
        final VulkanBufferedUniform<?>[] uniforms = new VulkanBufferedUniform<?>[elements.size()];

        for (int i = 0; i < elements.size(); i++)
        {
            uniforms[i] = ShaderUtils.getVulkanUniform(elements.get(i));
        }

        return uniforms;
    }

    private static VulkanBufferedUniform<?> getVulkanUniform(String code)
    {
        Matcher matcher = ShaderUtils.UNIFORM_TYPE_IN_CODE.matcher(code);
        if (!matcher.find())
        {
            throw new RuntimeException("could not find uniform pattern in shadercode '" + code + "'");
        }

        final String type = matcher.group(1);
        final String name = matcher.group(2);
        Engine4.getLogger().trace("found uniform buffer element: " + type + name + " from '" + code + "'");

        return switch (type)
        {
            case "mat4" -> new VulkanBufferedUniform<>(name.trim(), new Matrix4f());
            case "vec4" -> new VulkanBufferedUniform<>(name.trim(), new Vector4f());
            case "float" -> new VulkanBufferedUniform<>(name.trim(), 0.0f);
            case "int" -> new VulkanBufferedUniform<>(name.trim(), 0);
            default -> throw new RuntimeException("unsupported data type in uniform buffer - " + type);
        };
    }
}
