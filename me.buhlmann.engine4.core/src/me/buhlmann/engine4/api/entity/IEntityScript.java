package me.buhlmann.engine4.api.entity;

public interface IEntityScript extends IEntityComponent
{
    void update(float dt, IEntity entity);
}
