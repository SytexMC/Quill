package me.levitate.quill.event;

import me.levitate.quill.injection.annotation.Inject;
import me.levitate.quill.injection.annotation.Module;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;
import java.util.function.Predicate;

@Module
public class EventManager {
    @Inject
    private Plugin hostPlugin;

    /**
     * Utility method to filter cancelled events
     */
    public static <T extends Cancellable> Predicate<T> notCancelled() {
        return event -> !event.isCancelled();
    }

    /**
     * Start building an event listener
     */
    public <T extends Event> EventBuilder<T> listen(Class<T> eventClass) {
        return new EventBuilder<>(hostPlugin, eventClass);
    }

    /**
     * Quick register with default priority
     */
    public <T extends Event> void listen(Class<T> eventClass, Consumer<T> handler) {
        new EventBuilder<>(hostPlugin, eventClass).handle(handler);
    }

    /**
     * Utility method to run handler async
     */
    public Consumer<Event> async(Consumer<Event> handler) {
        return event -> hostPlugin.getServer().getScheduler().runTaskAsynchronously(
                hostPlugin,
                () -> handler.accept(event)
        );
    }

    /**
     * Utility method to run handler on next tick
     */
    public Consumer<Event> nextTick(Consumer<Event> handler) {
        return event -> hostPlugin.getServer().getScheduler().runTask(
                hostPlugin,
                () -> handler.accept(event)
        );
    }

    public static class EventBuilder<T extends Event> {
        private final Plugin plugin;
        private final Class<T> eventClass;
        private EventPriority priority = EventPriority.NORMAL;
        private boolean ignoreCancelled = false;
        private Predicate<T> filter;

        private EventBuilder(Plugin plugin, Class<T> eventClass) {
            this.plugin = plugin;
            this.eventClass = eventClass;
        }

        /**
         * Set the event priority
         */
        public EventBuilder<T> priority(EventPriority priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Set to monitor priority
         */
        public EventBuilder<T> monitor() {
            return priority(EventPriority.MONITOR);
        }

        /**
         * Set to the highest priority
         */
        public EventBuilder<T> highest() {
            return priority(EventPriority.HIGHEST);
        }

        /**
         * Set to the lowest priority
         */
        public EventBuilder<T> lowest() {
            return priority(EventPriority.LOWEST);
        }

        /**
         * Set whether to ignore cancelled events
         */
        public EventBuilder<T> ignoreCancelled(boolean ignore) {
            this.ignoreCancelled = ignore;
            return this;
        }

        /**
         * Add a filter condition
         */
        public EventBuilder<T> filter(Predicate<T> filter) {
            if (this.filter == null) {
                this.filter = filter;
            } else {
                this.filter = this.filter.and(filter);
            }
            return this;
        }

        /**
         * Register the event handler
         */
        public void handle(Consumer<T> handler) {
            Listener listener = new Listener() {
            };

            Consumer<T> wrappedHandler = filter != null
                    ? event -> {
                if (filter.test(event)) handler.accept(event);
            }
                    : handler;

            plugin.getServer().getPluginManager().registerEvent(
                    eventClass,
                    listener,
                    priority,
                    (l, event) -> {
                        if (eventClass.isInstance(event)) {
                            wrappedHandler.accept(eventClass.cast(event));
                        }
                    },
                    plugin,
                    ignoreCancelled
            );
        }
    }
}