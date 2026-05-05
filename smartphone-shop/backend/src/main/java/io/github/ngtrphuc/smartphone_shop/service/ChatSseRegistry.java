package io.github.ngtrphuc.smartphone_shop.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class ChatSseRegistry {

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<SseEmitter> adminEmitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribeUser(String email, long timeoutMs) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        userEmitters.computeIfAbsent(email, key -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeUserEmitter(email, emitter));
        emitter.onTimeout(() -> removeUserEmitter(email, emitter));
        emitter.onError(exception -> removeUserEmitter(email, emitter));
        return emitter;
    }

    public SseEmitter subscribeAdmin(long timeoutMs) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        adminEmitters.add(emitter);
        emitter.onCompletion(() -> adminEmitters.remove(emitter));
        emitter.onTimeout(() -> adminEmitters.remove(emitter));
        emitter.onError(exception -> adminEmitters.remove(emitter));
        return emitter;
    }

    public void withEachAdminEmitter(EmitterSender sender) {
        pruneEmitters(adminEmitters, sender);
    }

    public void withEachUserEmitter(String email, EmitterSender sender) {
        CopyOnWriteArrayList<SseEmitter> emitters = userEmitters.get(email);
        if (emitters == null) {
            return;
        }
        pruneEmitters(emitters, sender);
        if (emitters.isEmpty()) {
            userEmitters.remove(email, emitters);
        }
    }

    public void heartbeatAll(EmitterSender sender) {
        pruneEmitters(adminEmitters, sender);
        for (Map.Entry<String, CopyOnWriteArrayList<SseEmitter>> entry : userEmitters.entrySet()) {
            CopyOnWriteArrayList<SseEmitter> emitters = entry.getValue();
            pruneEmitters(emitters, sender);
            if (emitters.isEmpty()) {
                userEmitters.remove(entry.getKey(), emitters);
            }
        }
    }

    private void pruneEmitters(CopyOnWriteArrayList<SseEmitter> emitters, EmitterSender sender) {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                sender.send(emitter);
            } catch (IOException | IllegalStateException exception) {
                emitter.complete();
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }

    private void removeUserEmitter(String email, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = userEmitters.get(email);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(email, emitters);
            }
        }
    }

    @FunctionalInterface
    public interface EmitterSender {
        void send(SseEmitter emitter) throws IOException;
    }
}
