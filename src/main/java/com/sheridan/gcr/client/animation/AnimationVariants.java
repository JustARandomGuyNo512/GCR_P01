package com.sheridan.gcr.client.animation;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.Utils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Extra clips live in the same gun JSON. Name them {@code <category>} or
 * {@code <category>_<suffix>} ({@code shoot_one}, {@code mag_reload_empty_two}).
 * Longest category wins so {@code mag_reload_empty_*} is never treated as {@code mag_reload}.
 */
@OnlyIn(Dist.CLIENT)
public final class AnimationVariants {

    public static final String MAG_RELOAD = "mag_reload";
    public static final String MAG_RELOAD_EMPTY = "mag_reload_empty";
    public static final String MAG_RELOAD_CHARGE = "mag_reload_charge";

    private static final Map<String, List<Variant>> POOL = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, String> LOCATION_TO_POOL = new ConcurrentHashMap<>();

    private AnimationVariants() {
    }

    public record Variant(String alias, ResourceLocation location) {
    }

    public record Pick(String category, String animationName, @Nullable AnimationDef animation) {
        public int sendPacketDelayTicks(String gunPrefix, Map<String, Float> taskTimers) {
            float configured = taskTimers.getOrDefault(category + "_length", 1.0f);
            float animLength = animation != null ? animation.lengthInSeconds() : 0f;
            if (animLength <= 0f) {
                return Math.max(1, Utils.secondToTick(configured));
            }
            AnimationDef canonical = AnimationRegister.get(GCR.RL(gunPrefix + "_" + category));
            float canonicalLength = canonical != null ? canonical.lengthInSeconds() : 0f;
            float ratio = canonicalLength > 0f ? configured / canonicalLength : 0.6f;
            ratio = Math.min(0.95f, Math.max(0.1f, ratio));
            int delay = Math.round(animLength * ratio * 20f);
            int total = lengthTicks();
            return Math.min(Math.max(1, delay), total);
        }

        public int lengthTicks() {
            if (animation == null) {
                return 1;
            }
            return Math.max(1, Math.round(animation.lengthInSeconds() * 20f));
        }
    }

    @Nullable
    public static String categoryOf(String clipName, Collection<String> categories) {
        if (clipName == null) {
            return null;
        }
        String best = null;
        for (String category : categories) {
            if (matches(clipName, category) && (best == null || category.length() > best.length())) {
                best = category;
            }
        }
        return best;
    }

    public static void collectFromClips(String gunPrefix, Map<String, AnimationDef> clips, Map<String, String> mapping) {
        Set<String> categories = mapping.keySet();
        for (Map.Entry<String, AnimationDef> entry : clips.entrySet()) {
            String clipName = entry.getKey();
            String category = categoryOf(clipName, categories);
            if (category == null) {
                continue;
            }
            ResourceLocation registered = mappedLocation(gunPrefix, clipName, mapping);
            if (AnimationRegister.get(registered) == null) {
                AnimationRegister.register(registered, entry.getValue());
            }
            addVariant(gunPrefix, category, clipName, registered);
        }
    }

    public static Pick pick(String gunPrefix, String category) {
        List<Variant> variants = POOL.get(key(gunPrefix, category));
        Variant chosen;
        if (variants == null || variants.isEmpty()) {
            chosen = new Variant(category, GCR.RL(gunPrefix + "_" + category));
        } else if (variants.size() == 1) {
            chosen = variants.get(0);
        } else {
            chosen = variants.get(ThreadLocalRandom.current().nextInt(variants.size()));
        }
        return new Pick(category, chosen.alias(), AnimationRegister.get(chosen.location()));
    }

    public static List<Variant> variants(String gunPrefix, String category) {
        List<Variant> variants = POOL.get(key(gunPrefix, category));
        return variants == null ? List.of() : List.copyOf(variants);
    }

    public static List<Variant> variantsForRegistered(ResourceLocation registered) {
        String poolKey = LOCATION_TO_POOL.get(registered);
        if (poolKey == null) {
            return List.of();
        }
        List<Variant> variants = POOL.get(poolKey);
        return variants == null ? List.of() : List.copyOf(variants);
    }

    @Nullable
    public static String gunPrefixOf(ResourceLocation registeredPath, String category) {
        String path = registeredPath.getPath();
        String suffix = "_" + category;
        if (!path.endsWith(suffix)) {
            return null;
        }
        return path.substring(0, path.length() - suffix.length());
    }

    private static boolean matches(String clipName, String category) {
        return clipName.equals(category) || clipName.startsWith(category + "_");
    }

    private static ResourceLocation mappedLocation(String gunPrefix, String clipName, Map<String, String> mapping) {
        String mapped = mapping.get(clipName);
        if (mapped != null) {
            return GCR.RL(mapped);
        }
        return GCR.RL(gunPrefix + "_" + clipName);
    }

    private static void addVariant(String gunPrefix, String category, String alias, ResourceLocation registered) {
        String poolKey = key(gunPrefix, category);
        List<Variant> existing = POOL.computeIfAbsent(poolKey, unused -> new ArrayList<>());
        for (Variant variant : existing) {
            if (variant.location().equals(registered) || variant.alias().equals(alias)) {
                LOCATION_TO_POOL.putIfAbsent(registered, poolKey);
                return;
            }
        }
        existing.add(new Variant(alias, registered));
        LOCATION_TO_POOL.put(registered, poolKey);
        GCR.LOGGER.info("Animation variant {} / {} <- {}", gunPrefix, category, alias);
    }

    private static String key(String gunPrefix, String category) {
        return gunPrefix + "/" + category;
    }
}
