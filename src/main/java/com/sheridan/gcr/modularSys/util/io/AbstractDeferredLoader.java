package com.sheridan.gcr.modularSys.util.io;

import com.google.gson.Gson;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.aiStuff.DevAssetResolver;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

public abstract class AbstractDeferredLoader<T> {
    protected final Gson gson = new Gson();

    private final Map<ResourceLocation, List<Consumer<Optional<T>>>> loadingTasks = new HashMap<>();

    public void putTask(ResourceLocation path, Consumer<Optional<T>> onResult) {
        loadingTasks.computeIfAbsent(path, k -> new ArrayList<>()).add(onResult);
    }

    @Nullable
    public T load(ResourceLocation location, ResourceManager manager) {
        try {
            // 开发环境下优先读取 src/main/resources，热重载后才能看到刚修改的 pivot/voxel 数据
            try (InputStream in = DevAssetResolver.openOrNull(location, manager)) {
                if (in == null) {
                    return null;
                }
                String jsonStr = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                return readJsonStr(jsonStr);
            } catch (Exception e) {
                GCR.LOGGER.error("Error parsing {}: {}", location, e.getMessage());
                return null;
            }
        } catch (Exception e) {
            GCR.LOGGER.error("Error loading resource {}: {}", location, e.getMessage());
            return null;
        }
    }

    public void trigger(ResourceManager manager, boolean clearAfter) {
        if (manager == null) {
            return;
        }
        loadingTasks.forEach((path, tasks) -> {
            Optional<T> result = Optional.ofNullable(load(path, manager));
            tasks.forEach(task -> task.accept(result));
        });
        if (clearAfter) {
            loadingTasks.clear();
        }
    }

    protected abstract T readJsonStr(String jsonStr);
}
