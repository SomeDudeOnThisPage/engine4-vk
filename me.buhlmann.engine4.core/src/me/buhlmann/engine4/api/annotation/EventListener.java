package me.buhlmann.engine4.api.annotation;

import me.buhlmann.engine4.api.IEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventListener
{
    Class<? extends IEvent> value();
}
