package me.buhlmann.engine4.api.ui;

import java.util.List;

public interface IUIRenderable
{
    List<IUIRenderable> getChildren();
}
