package com.sheridan.gcr.client.animation.io;

import com.google.gson.*;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.Utils;
import com.sheridan.gcr.client.animation.AnimationChannel;
import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.Keyframe;
import com.sheridan.gcr.client.animation.KeyframeAnimator;
import com.sheridan.gcr.client.animation.command.Command;
import com.sheridan.gcr.client.animation.command.CommandFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@OnlyIn(Dist.CLIENT)
public class BedrockAnimationLoader {
    private static final Gson GSON_INSTANCE = new Gson();

    public static Map<String, AnimationDef> loadAnimationCollection(ResourceLocation location) {
        return loadAnimationCollection(location, false);
    }

    /**
     * This method is not guaranteed to be thread-safe.
     * <p>
     * Reads bedrock format animation json file which exported by blockbench.
     * <p>
     * STEP interpretation is not supported and will be read as LINEAR instead.
     * */
    public static Map<String, AnimationDef> loadAnimationCollection(ResourceLocation location, boolean readSounds) {
        AtomicReference<Map<String, AnimationDef>> resultRef = new AtomicReference<>(new HashMap<>());
        try {
            ResourceManager manager = Minecraft.getInstance().getResourceManager();
            manager.getResource(location).ifPresent(res -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(res.open(), StandardCharsets.UTF_8))) {
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                    reader.close();
                    String json = stringBuilder.toString();
                    JsonObject jsonObject = GSON_INSTANCE.fromJson(json, JsonObject.class);
                    JsonObject animations = jsonObject.getAsJsonObject("animations");
                    String modid = readSounds ? location.getNamespace() : "";
                    Map<String, AnimationDef> animationsMap = new HashMap<>();
                    for (String key : animations.keySet()) {
                        Map<String, AnimationDef> animation = readAnimation(animations.getAsJsonObject(key), readSounds, modid);
                        if (animation == null) {
                            continue;
                        }
                        for (Map.Entry<String, AnimationDef> entry : animation.entrySet()) {
                            String localName = entry.getKey();
                            AnimationDef value = entry.getValue();
                            String globalName = "main".equals(localName) ? key : key + "." + localName;
                            animationsMap.put(globalName, value);
                        }
                    }
                    resultRef.set(animationsMap);
                } catch (Exception e) {
                    GCR.LOGGER.error("Error parsing {}: {}", location, e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            GCR.LOGGER.error("Error loading resource {}: {}", location, e.getMessage());
            e.printStackTrace();
        }
        return resultRef.get();
    }

    private static Map<String, AnimationDef> readAnimation(JsonObject jsonObject, boolean readSounds, String modid) {
        if (!jsonObject.has("animation_length")) {
            return null;
        }
        Map<String, AnimationDef> animations = new HashMap<>();
        Map<String, AnimationDef.Builder> builders = new HashMap<>();
        float length = jsonObject.get("animation_length").getAsFloat();
        boolean loop = false;
        boolean stopAtLastFrame = false;
        if (jsonObject.has("loop")) {
            JsonPrimitive loopPrim = jsonObject.getAsJsonPrimitive("loop");
            if (loopPrim.isBoolean()) {
                loop = loopPrim.getAsBoolean();
            } else if (loopPrim.isString()) {
                stopAtLastFrame = "hold_on_last_frame".equals(loopPrim.getAsString());
            }
        }
        builders.put("main", AnimationDef.Builder
                .withLength(length)
                .setLooping(loop)
                .setStopAtLastFrame(stopAtLastFrame));
        if (jsonObject.has("bones")) {
            JsonObject animation = jsonObject.getAsJsonObject("bones");
            for (String bone : animation.keySet()) {
                String name = bone;
                AnimationDef.Builder currentBuilder;
                if (bone.contains("@")) {
                    String[] split = bone.split("@");
                    String subAnimationName = split[1];
                    name = split[0];
                    if (!builders.containsKey(subAnimationName)) {
                        builders.put(subAnimationName, AnimationDef.Builder.withLength(length).setLooping(loop));
                    }
                    currentBuilder = builders.get(subAnimationName);
                } else {
                    currentBuilder = builders.get("main");
                }
                JsonObject animationBone = animation.getAsJsonObject(bone);
                AnimationChannel rotate = readRotation(animationBone);
                AnimationChannel position = readPosition(animationBone);
                AnimationChannel scale = readScale(animationBone);
                if (rotate != null) {
                    currentBuilder.addAnimation(name, rotate);
                }
                if (position != null) {
                    currentBuilder.addAnimation(name, position);
                }
                if (scale != null) {
                    currentBuilder.addAnimation(name, scale);
                }
            }
        }
        AnimationDef.Builder main = builders.get("main");
        if (readSounds && jsonObject.has("sound_effects")) {
            JsonObject sounds = jsonObject.getAsJsonObject("sound_effects");
            for (String key : sounds.keySet()) {
                JsonObject soundPointObject = sounds.getAsJsonObject(key);
                String effect = soundPointObject.get("effect").getAsString();
                ResourceLocation soundName;
                if (effect.indexOf(':') != -1) {
                    soundName = ResourceLocation.parse(effect);
                } else {
                    soundName = GCR.RL(modid, effect);
                }
                int tick = Utils.secondToTick(Float.parseFloat(key));
                main.addSoundPoint(new AnimationDef.SoundPoint(tick, soundName));
            }
        }
        //加载命令队列
        if (jsonObject.has("timeline")) {
            JsonObject commands = jsonObject.getAsJsonObject("timeline");
            for (String key : commands.keySet()) {
                try {
                    String command = commands.get(key).getAsString();
                    float time = Float.parseFloat(key);
                    Command res = CommandFactory.INSTANCE.createCommand(command, time);
                    if (res != null) {
                        main.addCommandPoint(res);
                    }
                } catch (Exception ignored) {}
            }
        }
        for (Map.Entry<String, AnimationDef.Builder> entry : builders.entrySet()) {
            String key = entry.getKey();
            AnimationDef.Builder value = entry.getValue();
            animations.put(key, value.build());
        }
        return animations;
    }

    private static AnimationChannel readRotation(JsonObject jsonObject) {
        return readChannel(jsonObject, "rotation", AnimationChannel.Targets.ROTATION);
    }

    private static AnimationChannel readPosition(JsonObject jsonObject) {
        return readChannel(jsonObject, "position", AnimationChannel.Targets.POSITION);
    }

    private static AnimationChannel readScale(JsonObject jsonObject) {
        return readChannel(jsonObject, "scale", AnimationChannel.Targets.SCALE);
    }

    private static AnimationChannel readChannel(JsonObject jsonObject, String type, AnimationChannel.Target target) {
        if (!jsonObject.has(type)) {
            return null;
        } else {
            List<Keyframe> keyframes = new ArrayList<>();
            JsonElement content = jsonObject.get(type);
            if (content.isJsonArray()) {
                Vector3f vec = getAsVector3f(content.getAsJsonArray());
                keyframes.add(new Keyframe(0, getVec(vec, type), AnimationChannel.Interpolations.LINEAR));
            } else if (content.isJsonObject()) {
                keyframes = readKeyframes(content.getAsJsonObject(), type);
            } else if (content.isJsonPrimitive()) {
                float asFloat = content.getAsFloat();
                Vector3f vec = new Vector3f(asFloat, asFloat, asFloat);
                keyframes.add(new Keyframe(0, getVec(vec, type), AnimationChannel.Interpolations.LINEAR));
            }

            return new AnimationChannel(target, keyframes.toArray(new Keyframe[0]));
        }
    }

    private static List<Keyframe> readKeyframes(JsonObject jsonObject, String type) {
        List<Keyframe> keyframes = new ArrayList<>();
        for (String timeStamp : jsonObject.keySet()) {
            float time = Float.parseFloat(timeStamp);
            if (jsonObject.get(timeStamp).isJsonObject()) {
                JsonObject frameObject = jsonObject.get(timeStamp).getAsJsonObject();
                JsonArray pos = frameObject.get("post").getAsJsonArray();
                Vector3f post = getAsVector3f(pos);
                if (frameObject.has("lerp_mode") && "catmullrom".equals(frameObject.get("lerp_mode").getAsString())) {
                    Keyframe keyframe = new Keyframe(time, getVec(post, type), AnimationChannel.Interpolations.CATMULL_ROM);
                    keyframes.add(keyframe);
                } else {
                    Keyframe keyframe = new Keyframe(time, getVec(post, type), AnimationChannel.Interpolations.LINEAR);
                    keyframes.add(keyframe);
                }
            } else if (jsonObject.get(timeStamp).isJsonArray()) {
                Vector3f pos = getAsVector3f(jsonObject.get(timeStamp).getAsJsonArray());
                Keyframe keyframe = new Keyframe(time, getVec(pos, type), AnimationChannel.Interpolations.LINEAR);
                keyframes.add(keyframe);
            }
        }
        return keyframes;
    }

    private static Vector3f getAsVector3f(JsonArray array) {
        return new Vector3f(
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat(),
                array.get(2).getAsFloat()
        );
    }

    private static Vector3f getVec(Vector3f pos, String type) {
        return switch (type) {
            case "rotation" -> KeyframeAnimator.degreeVec(pos.x, pos.y, pos.z);
            case "position" -> KeyframeAnimator.posVec(pos.x, pos.y, pos.z);
            case "scale" -> KeyframeAnimator.scaleVec(pos.x, pos.y, pos.z);
            default -> null;
        };
    }
}
