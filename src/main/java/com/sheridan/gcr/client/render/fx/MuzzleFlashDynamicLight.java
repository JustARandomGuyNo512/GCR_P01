package com.sheridan.gcr.client.render.fx;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * TacZ Additions-compatible, short-lived gunfire illumination. Uses the
 * Atomic Dynamic Lights API reflectively, so publication builds do not depend
 * on other mods. All shots (local and remote) are illuminated at the shooter's
 * entity position, and every light expires after two client ticks.
 */
@OnlyIn(Dist.CLIENT)
public final class MuzzleFlashDynamicLight {
    private static final Map<Entity, LightState> SOURCES = new IdentityHashMap<>();
    private static final int LIFETIME_TICKS = 2;
    private static final int MAX_SOURCES = 192;

    private static boolean initialized;
    private static boolean available;
    private static Method addLightSource;
    private static Method removeLightSource;
    private static Class<?> sourceInterface;
    private static net.minecraft.world.level.Level lastLevel;

    private MuzzleFlashDynamicLight() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        MinecraftForge.EVENT_BUS.register(MuzzleFlashDynamicLight.class);
        try {
            ClassLoader loader = MuzzleFlashDynamicLight.class.getClassLoader();
            Class<?> dynamicLights = Class.forName(
                    "atomicstryker.dynamiclights.server.DynamicLights", false, loader);
            sourceInterface = Class.forName(
                    "atomicstryker.dynamiclights.server.IDynamicLightSource", false, loader);
            addLightSource = dynamicLights.getMethod("addLightSource", sourceInterface);
            try {
                removeLightSource = dynamicLights.getMethod("removeLightSource", sourceInterface);
            } catch (NoSuchMethodException ignored) {
                // Older Dynamic Lights versions keep zero-brightness sources.
            }
            available = true;
            GCR.LOGGER.info("GCR gunfire: Atomic Dynamic Lights integration enabled");
        } catch (ReflectiveOperationException | LinkageError ignored) {
            available = false;
            GCR.LOGGER.info("GCR gunfire: Atomic Dynamic Lights unavailable; using GCR first-person glow only");
        }
    }

    /**
     * Safe to call from the dedicated GCR firing loop: all world/Java reflection
     * mutations happen on Minecraft's client thread.
     */
    public static void fire(Entity shooter, IGun gun, ItemStack weapon) {
        MuzzleFlashLightProfile profile = MuzzleFlashLightProfile.forGun(gun, weapon);
        if (profile.blockLight() <= 0 || shooter == null) return;
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (!available || mc.level == null || !shooter.isAlive()
                    || shooter.level() != mc.level) return;
            if (lastLevel != mc.level) {
                clearSources();
                lastLevel = mc.level;
            }
            LightState existing = SOURCES.get(shooter);
            if (existing != null) {
                existing.refresh(profile.blockLight());
                return;
            }
            if (SOURCES.size() >= MAX_SOURCES) return;

            LightState state = new LightState(shooter, profile.blockLight());
            InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
                case "getAttachmentEntity" -> state.shooter;
                case "getLightLevel" -> state.level();
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> args != null && args.length == 1 && proxy == args[0];
                case "toString" -> "GCRMuzzleLight(" + state.shooter.getId() + ")";
                default -> null;
            };
            try {
                Object proxy = Proxy.newProxyInstance(
                        sourceInterface.getClassLoader(), new Class<?>[]{sourceInterface}, handler);
                addLightSource.invoke(null, proxy);
                state.source = proxy;
                SOURCES.put(shooter, state);
            } catch (ReflectiveOperationException | LinkageError e) {
                available = false;
                GCR.LOGGER.warn("GCR gunfire: could not register Atomic Dynamic Lights muzzle source", e);
            }
        });
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !available) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != lastLevel) {
            clearSources();
            lastLevel = mc.level;
            return;
        }
        Iterator<LightState> it = SOURCES.values().iterator();
        while (it.hasNext()) {
            LightState light = it.next();
            light.tick();
            if (!light.shooter.isAlive() || light.shooter.isRemoved()) {
                unregister(light);
                it.remove();
            } else if (light.ticksAlive > LIFETIME_TICKS && removeLightSource != null) {
                unregister(light);
                it.remove();
            }
        }
    }

    private static void clearSources() {
        for (LightState state : SOURCES.values()) unregister(state);
        SOURCES.clear();
    }

    private static void unregister(LightState state) {
        state.brightness = 0;
        if (removeLightSource != null && state.source != null) {
            try {
                removeLightSource.invoke(null, state.source);
            } catch (ReflectiveOperationException | LinkageError e) {
                GCR.LOGGER.debug("GCR gunfire: dynamic light source cleanup failed", e);
            }
        }
    }

    private static final class LightState {
        final Entity shooter;
        Object source;
        volatile int brightness;
        volatile int ticksAlive;

        LightState(Entity shooter, int brightness) {
            this.shooter = shooter;
            refresh(brightness);
        }

        void refresh(int newBrightness) {
            brightness = newBrightness;
            ticksAlive = 0;
        }

        void tick() {
            ticksAlive++;
        }

        int level() {
            return shooter.isAlive() && ticksAlive < LIFETIME_TICKS ? brightness : 0;
        }
    }
}
