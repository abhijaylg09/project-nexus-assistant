package com.nexus.core;

import javafx.application.Platform;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * High-performance, thread-safe multimodal event broker.
 * Enables seamless communication between Java Core, Vision, Speech, LLM, and JavaFX UI.
 */
public class MultimodalEventBus {

    private static final MultimodalEventBus INSTANCE = new MultimodalEventBus();
    private final Map<Class<?>, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    private MultimodalEventBus() {}

    public static MultimodalEventBus getInstance() {
        return INSTANCE;
    }

    /**
     * Subscribe a consumer to events of type T.
     */
    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, Consumer<T> consumer) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                 .add((Consumer<Object>) consumer);
    }

    /**
     * Publish an event to all subscribers.
     * If JavaFX is running and listener wants UI thread execution, can be dispatched accordingly.
     */
    public void publish(Object event) {
        if (event == null) return;
        List<Consumer<Object>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (Consumer<Object> listener : eventListeners) {
                try {
                    listener.accept(event);
                } catch (Exception e) {
                    System.err.println("[EventBus] Error handling event " + event.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Publish an event guaranteed to run on the JavaFX UI thread.
     */
    public void publishOnFxThread(Object event) {
        if (Platform.isFxApplicationThread()) {
            publish(event);
        } else {
            try {
                Platform.runLater(() -> publish(event));
            } catch (IllegalStateException e) {
                // JavaFX toolkit not initialized yet (e.g. during CLI or headless tests)
                publish(event);
            }
        }
    }
}
