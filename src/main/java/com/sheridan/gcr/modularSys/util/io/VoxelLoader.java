package com.sheridan.gcr.modularSys.util.io;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.modularSys.MultiVoxel;
import com.sheridan.gcr.modularSys.Voxel;
import com.sheridan.gcr.modularSys.modules.IVoxelHandler;
import com.sheridan.gcr.modularSys.modules.IVoxelHandlerModule;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


/**
 * Loads bedrock format model as minecraft AABB list both in the client side and server side.
 */

public class VoxelLoader extends AbstractDeferredLoader<MultiVoxel> implements ResourceManagerReloadListener {
    private static VoxelLoader SERVER;
    private static VoxelLoader CLIENT;

    public static VoxelLoader getServer() {
        if (SERVER == null) {
            SERVER = new VoxelLoader();
        }
        return SERVER;
    }

    public static VoxelLoader getClient() {
        if (CLIENT == null) {
            CLIENT = new VoxelLoader();
        }
        return CLIENT;
    }

    public void book(IVoxelHandlerModule modular) {
        System.out.println("VOXEL >> " + modular);
        IVoxelHandler handler = modular.getHandler();
        if (handler != null) {
            super.putTask(handler.getAssetPath(), res -> res.ifPresent(handler::setVoxelIfNull));
        }
    }

    @Override
    public MultiVoxel readJsonStr(String jsonStr) {
        JsonObject root = gson.fromJson(jsonStr, JsonObject.class);
        if (!root.has("minecraft:geometry")) {
            return null;
        }

        JsonElement geometryElement = root.get("minecraft:geometry");
        if (!geometryElement.isJsonArray()) {
            return null;
        }

        JsonArray geometryArray = geometryElement.getAsJsonArray();
        if (geometryArray.isEmpty()) {
            return null;
        }

        JsonObject geometry = geometryArray.get(0).getAsJsonObject();
        if (!geometry.has("bones")) {
            return null;
        }

        JsonArray bones = geometry.getAsJsonArray("bones");
        MultiVoxel multiVoxel = new MultiVoxel();

        for (JsonElement e : bones) {
            JsonObject bone = e.getAsJsonObject();
            String name = getString(bone, "name");
            String parent = getString(bone, "parent");

            if (name == null) {
                continue;
            }
            if ("root".equals(name) || "root".equals(parent)) {
                List<AABB> aabbs = readFromRootBone(bone);
                if (aabbs != null) {
                    multiVoxel.setVoxelIfNull(name, new Voxel(aabbs));
                }
            }
        }

        return multiVoxel;
    }

    private static String getString(JsonObject obj, String key) {
        return obj.has(key) ? obj.get(key).getAsString() : null;
    }

    private List<AABB> readFromRootBone(JsonObject root) {
        JsonArray cubes = root.getAsJsonArray("cubes");
        if (cubes == null || cubes.isEmpty()) {
            return null;
        }
        List<AABB> shape = new ArrayList<>();
        for (int i = 0; i < cubes.size(); i++) {
            JsonObject cube = cubes.get(i).getAsJsonObject();
            if (cube.has("rotation")) {
                continue;
            }
            JsonArray origin = cube.getAsJsonArray("origin");
            JsonArray size = cube.getAsJsonArray("size");
            float x = origin.get(0).getAsFloat() / 16f;
            float y = origin.get(1).getAsFloat() / 16f;
            float z = origin.get(2).getAsFloat() / 16f;
            float sx = size.get(0).getAsFloat() / 16f;
            float sy = size.get(1).getAsFloat() / 16f;
            float sz = size.get(2).getAsFloat() / 16f;
            AABB aabb = new AABB(-x - sx, y, z, -x, y + sy, z + sz);
            shape.add(aabb);
        }
        return shape;
    }

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        GCR.LOGGER.info("load voxels triggered on server");
        trigger(resourceManager, false);

    }
}
