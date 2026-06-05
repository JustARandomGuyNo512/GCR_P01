package com.sheridan.gcr.modularSys.util.io;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.modularSys.ISlotProvider;
import com.sheridan.gcr.modularSys.ISlotProviderModular;
import com.sheridan.gcr.modularSys.util.Pivot;
import com.sheridan.gcr.modularSys.util.PivotMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads bedrock format model as PivotMap both in the client side and server side.
 * */
public class PivotMapLoader extends AbstractDeferredLoader<PivotMap> implements ResourceManagerReloadListener {

    private static PivotMapLoader SERVER;
    private static PivotMapLoader CLIENT;

    public static PivotMapLoader getServer() {
        if (SERVER == null) {
            SERVER = new PivotMapLoader();
        }
        return SERVER;
    }

    public static PivotMapLoader getClient() {
        if (CLIENT == null) {
            CLIENT = new PivotMapLoader();
        }
        return CLIENT;
    }

    public void book(ISlotProviderModular modular) {
        ISlotProvider slotProvider = modular.getSlotProvider();
        if (slotProvider != null) {
            ResourceLocation assetsPath = slotProvider.getAssetsPath();
            super.putTask(assetsPath, res -> res.ifPresent(slotProvider::setPivotMap));
        }
    }

    @Override
    protected PivotMap readJsonStr(String jsonStr) {
        JsonObject json = this.gson.fromJson(jsonStr, JsonObject.class);
        JsonObject geometry = json.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonElement bones = geometry.get("bones");
        if (bones.isJsonArray()) {
            JsonArray asJsonArray = bones.getAsJsonArray();
            return loadFromJsonArray(asJsonArray);
        }
        return null;
    }

    private PivotMap loadFromJsonArray(JsonArray array) {
        Pivot root = null;
        Map<String, Pivot> pivots = new HashMap<>();
        for (int i = 0; i < array.size(); i ++) {
            JsonObject bone = array.get(i).getAsJsonObject();
            String name = bone.get("name").getAsString();
            JsonArray posArray = bone.get("pivot").getAsJsonArray();
            Vector3f offset = new Vector3f(
                    posArray.get(0).getAsFloat() / 16f,
                    posArray.get(1).getAsFloat() / 16f,
                    posArray.get(2).getAsFloat() / 16f
            );
            Vector3f rotation = new Vector3f(0, 0, 0);
            if (bone.has("rotation")) {
                JsonArray rotationArray = bone.get("rotation").getAsJsonArray();
                rotation = new Vector3f(
                        (float) Math.toRadians(rotationArray.get(0).getAsFloat()),
                        (float) Math.toRadians(rotationArray.get(1).getAsFloat()),
                        (float) Math.toRadians(rotationArray.get(2).getAsFloat())
                );
            }

            if (bone.has("parent")) {
                Pivot parent = pivots.get(bone.get("parent").getAsString());
                if (parent != null) {
                    float x = - offset.x - parent.x;
                    float y = offset.y - parent.y;
                    float z = offset.z - parent.z;
                    Pivot pivot = new Pivot(x, y, z, rotation.x, rotation.y, rotation.z, name, parent);
                    parent.addChild(name, pivot);
                    pivots.put(name, pivot);
                }
            } else {
                if ("root".equals(name)) {
                    root = new Pivot(offset, rotation, name, null);
                    pivots.put(name, root);
                }
            }

        }
        return root == null ? null : new PivotMap(root);
    }


    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        GCR.LOGGER.info("load pivot map triggered on server");
        trigger(resourceManager, false);
    }
}
