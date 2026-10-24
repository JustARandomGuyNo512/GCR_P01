package com.sheridan.gcr.common;

import com.sheridan.gcr.client.render.fx.MuzzleFlashLightProfile;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Server-side muzzle flash light fallback based on TacZ Additions'
 * ServerMuzzleFlashManager. Works without Sodium/Atomic Dynamic Lights.
 *
 * Only replaces AIR with minecraft:light and always restores AIR after a
 * short lifetime. Never touches constructed blocks, liquid, existing light
 * blocks or other mods' non-air states. Does not spawn light entities.
 */
public final class MuzzleFlashLightBlockManager {
    private static final int FLASH_TICKS = 3;
    private static final int MAX_ACTIVE_PER_LEVEL = 128;
    private static final Map<ServerLevel, Map<BlockPos, Flash>> ACTIVE = new HashMap<>();

    // Prefer the shooter's head-level air block, then nearest unobstructed air.
    private static final int[][] SEARCH_OFFSETS = {
            {0, 0, 0}, {0, 1, 0}, {0, -1, 0},
            {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
            {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1},
            {1, -1, 0}, {-1, -1, 0}, {0, -1, 1}, {0, -1, -1}
    };

    private MuzzleFlashLightBlockManager() {}

    /** Only invoked for a successfully fired server-authoritative GCR bullet. */
    public static void onShot(ServerLevel level, LivingEntity shooter, IGun gun, ItemStack weapon) {
        int brightness = MuzzleFlashLightProfile.forGun(gun, weapon).blockLight();
        if (brightness == 0 || shooter == null) return;

        Map<BlockPos, Flash> levelFlashes = ACTIVE.computeIfAbsent(level, ignored -> new HashMap<>());
        BlockPos center = BlockPos.containing(shooter.getX(), shooter.getEyeY() - 0.25, shooter.getZ());
        BlockPos selected = null;
        boolean ownLight = false;

        for (int[] offset : SEARCH_OFFSETS) {
            BlockPos candidate = center.offset(offset[0], offset[1], offset[2]);
            if (!level.hasChunkAt(candidate)) continue;
            BlockState current = level.getBlockState(candidate);
            Flash existing = levelFlashes.get(candidate);
            if (existing != null && current.is(Blocks.LIGHT)) {
                selected = candidate;
                ownLight = true;
                break;
            }
            if (current.isAir()) {
                selected = candidate;
                break;
            }
        }
        if (selected == null) return;

        long deadline = level.getGameTime() + FLASH_TICKS;
        Flash previous = levelFlashes.get(selected);
        if (ownLight && previous != null) {
            // Repeat shots within the same light interval refresh the flash.
            previous.expiresAt = deadline;
            if (brightness > previous.level) {
                previous.level = brightness;
                level.setBlock(selected,
                        Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, brightness), 3);
            }
            return;
        }

        if (levelFlashes.size() >= MAX_ACTIVE_PER_LEVEL) return;
        // Double check immediately before modifying the world: air only.
        if (!level.getBlockState(selected).isAir()) return;
        if (level.setBlock(selected,
                Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, brightness), 3)) {
            levelFlashes.put(selected, new Flash(deadline, brightness));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) return;
        Iterator<Map.Entry<ServerLevel, Map<BlockPos, Flash>>> levels = ACTIVE.entrySet().iterator();
        while (levels.hasNext()) {
            Map.Entry<ServerLevel, Map<BlockPos, Flash>> entry = levels.next();
            ServerLevel level = entry.getKey();
            long time = level.getGameTime();
            Iterator<Map.Entry<BlockPos, Flash>> lights = entry.getValue().entrySet().iterator();
            while (lights.hasNext()) {
                Map.Entry<BlockPos, Flash> light = lights.next();
                if (time < light.getValue().expiresAt) continue;
                removeOwnLight(level, light.getKey());
                lights.remove();
            }
            if (entry.getValue().isEmpty()) levels.remove();
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ACTIVE.forEach((level, lights) -> lights.keySet().forEach(pos -> removeOwnLight(level, pos)));
        ACTIVE.clear();
    }

    private static void removeOwnLight(ServerLevel level, BlockPos pos) {
        if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.LIGHT)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static final class Flash {
        long expiresAt;
        int level;
        Flash(long expiresAt, int level) {
            this.expiresAt = expiresAt;
            this.level = level;
        }
    }
}
