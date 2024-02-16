package me.buhlmann.engine4.event;

import me.buhlmann.engine4.Engine4;
import me.buhlmann.engine4.api.IEvent;
import me.buhlmann.engine4.api.annotation.EventListener;
import me.buhlmann.engine4.api.annotation.EventSubscription;
import me.buhlmann.engine4.api.utils.ICollectionMap;
import me.buhlmann.engine4.utils.SetMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@SuppressWarnings({"unchecked"})
public final class EventBus
{
    private static final float EVENT_CYCLE_SPIKE_MODIFIER = 10.0f;

    private record CompositeEventSubscription(Class<? extends IEvent> clazz, EventSubscription<? extends IEvent> subscription) {}

    private final Queue<IEvent> bus;
    private final ICollectionMap<Class<? extends IEvent>, EventSubscription<? extends IEvent>> subscriptions;
    private final ICollectionMap<Object, CompositeEventSubscription> bindings;

    private int lastEventCycleBusSize;

    /**
     * Immediately fires all event subscribers until the event is cancelled
     * @param event event to be fired
     * @param <T> type
     */
    @SuppressWarnings("unchecked")
    private <T extends IEvent> void fire(final T event)
    {
        if (event.isCancelled())
        {
            return;
        }

        for (EventSubscription<? extends IEvent> subscription : this.subscriptions.getAll(event.getClass()))
        {
            if (!event.isCancelled())
            {
                ((EventSubscription<T>) subscription).onEvent(event);
            }
            else
            {
                break;
            }
        }
    }

    public <T extends IEvent> EventSubscription<T> subscribe(final Class<T> event, final EventSubscription<T> subscription)
    {
        // if (this.subscriptions.get(event) == null)
        // {
        //     throw new UnsupportedOperationException(
        //         String.format("failed to subscribe to event of type '%s' - event type is not registered", event));
        // }

        this.subscriptions.put(event, subscription);

        return subscription;
    }

    public <T extends IEvent> void subscribe(final Object object)
    {
        final Class<?> clazz = object.getClass();

        // Iterate all methods of the class, and check for event listeners.
        for (Method method : clazz.getMethods())
        {
            if (method.isAnnotationPresent(EventListener.class))
            {
                EventListener listener = method.getAnnotation(EventListener.class);
                Class<T> event = (Class<T>) listener.value();

                // Create event subscription that calls the annotated method.
                final EventSubscription<T> subscription = this.subscribe(event, (e) -> {
                    try
                    {
                        method.invoke(object, e);
                    }
                    catch (IllegalAccessException exception)
                    {
                        Engine4.getLogger().error("Attempted to access event listener with non-public modifier - make sure both" +
                            "the class and the event callback are public.");
                    }
                    catch (InvocationTargetException exception)
                    {
                        Engine4.getLogger().error(exception);
                    }
                });

                Engine4.getLogger().trace("Registered");
                this.bindings.put(object, new CompositeEventSubscription(event, subscription));
            }
        }
    }

    public void unsubscribe(final Object object)
    {
        for (CompositeEventSubscription composite : this.bindings.getAll(object))
        {
            this.unsubscribe(composite.clazz, composite.subscription);
        }
        this.bindings.removeAll(object);
    }

    public void unsubscribe(final Class<? extends IEvent> event, final EventSubscription<? extends IEvent> subscription)
    {
        this.subscriptions.removeFrom(event, subscription);
    }

    public void publish(final IEvent event)
    {
        if (!event.isCancelled())
        {
            if (event.isDeferred())
            {
                this.bus.add(event);
            }
            else
            {
                this.fire(event);
            }
        }
    }

    public void update()
    {
        if (this.bus.size() / EventBus.EVENT_CYCLE_SPIKE_MODIFIER > this.lastEventCycleBusSize + 1)
        {
            Engine4.getLogger().warning("Event spike - current cycle: " + this.bus.size() + ", last cycle: " + this.lastEventCycleBusSize);
        }

        this.lastEventCycleBusSize = this.bus.size();

        while (!this.bus.isEmpty())
        {
            this.fire(this.bus.poll());
        }
    }

    public EventBus()
    {
        this.bus = new LinkedList<>();
        this.subscriptions = new SetMap<>();
        this.bindings = new SetMap<>();
        this.lastEventCycleBusSize = 0;
    }
}
