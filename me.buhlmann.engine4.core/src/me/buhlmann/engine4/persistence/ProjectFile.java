package me.buhlmann.engine4.persistence;

import java.util.ArrayList;
import java.util.List;

public class ProjectFile
{
    final List<SceneFile> scenes;

    public ProjectFile()
    {
        this.scenes = new ArrayList<>();
    }
}
