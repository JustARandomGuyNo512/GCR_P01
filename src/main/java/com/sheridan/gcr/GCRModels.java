package com.sheridan.gcr;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.sheridan.gcr.client.GunEffectManager;
import com.sheridan.gcr.client.KeyBinds;
import com.sheridan.gcr.client.events.ClientEvents;
import com.sheridan.gcr.client.events.ControllerEvents;
import com.sheridan.gcr.client.events.RenderEvents;
import com.sheridan.gcr.client.events.TestEvents;
import com.sheridan.gcr.client.recoil.RecoilController;
import com.sheridan.gcr.client.recoil.RecoilData;
import com.sheridan.gcr.client.recoil.RecoilImpulse;
import com.sheridan.gcr.client.recoil.VisualRecoilMix;
import com.sheridan.gcr.client.render.delayed.DelayedRenderTaskHandler;
import com.sheridan.gcr.client.render.entity.BulletRenderer;
import com.sheridan.gcr.client.render.entity.M433Renderer;
import com.sheridan.gcr.client.render.events.GuiEvents;
import com.sheridan.gcr.client.render.fx.*;
import com.sheridan.gcr.client.render.fx.particles.ModParticles;
import com.sheridan.gcr.client.render.fx.particles.ember.EmberParticle;
import com.sheridan.gcr.client.render.fx.particles.explosion.FlashParticle;
import com.sheridan.gcr.client.render.fx.particles.explosion.FragmentParticle;
import com.sheridan.gcr.client.render.fx.particles.explosion.SparkParticle;
import com.sheridan.gcr.client.screen.containers.ModContainers;
import com.sheridan.gcr.common.CommonEvents;
import com.sheridan.gcr.common.Commons;
import com.sheridan.gcr.common.GunHeatHandler;
import com.sheridan.gcr.components.ModComponents;
import com.sheridan.gcr.data.ModData;
import com.sheridan.gcr.data.PlayerStatusEvents;
import com.sheridan.gcr.entity.ModEntities;
import com.sheridan.gcr.items.DisplayData;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.items.ModuleItem;
import com.sheridan.gcr.modularSys.*;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.fire.closedBolt.AKFullAuto;
import com.sheridan.gcr.modularSys.fire.closedBolt.AKSemi;
import com.sheridan.gcr.modularSys.fire.closedBolt.ARFullAuto;
import com.sheridan.gcr.modularSys.fire.closedBolt.ARSemi;
import com.sheridan.gcr.modularSys.modules.*;
import com.sheridan.gcr.modularSys.modules.gunProperties.impl.BaseProperties;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.modules.guns.ak.AK;
import com.sheridan.gcr.modularSys.modules.guns.ar.AR;
import com.sheridan.gcr.modularSys.modules.impl.*;
import com.sheridan.gcr.modularSys.slot.*;
import com.sheridan.gcr.modularSys.util.io.PivotMapLoader;
import com.sheridan.gcr.modularSys.util.io.VoxelLoader;
import com.sheridan.gcr.network.c2s.*;
import com.sheridan.gcr.network.s2c.*;
import com.sheridan.gcr.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;


/** Gun and attachment module definitions. */
public final class GCRModels {

    private GCRModels() {
    }

    public static final IModular M4_PROFILE_FSB_BARREL = new ARBarrel(RL( "m4_profile_fsb_barrel"), 1.0f, 0.1f, 1.0f,
            new SlotProvider(RL( "common/pivot_maps/m4_profile_fsb_barrel.pivot.geo.json"))
                    .addSlot(new SingleFixedSlot("UNDER_BARREL")
                            .setFilter(SlotFilters.hasAllTags("under_barrel", "ar")))
                    .addSlot(new SingleFixedSlot("MUZZLE")
                            .setFilter(SlotFilters.hasAllTags("muzzle", "ar", "5.56x45"))),
            new VoxelHandler(RL("common/voxel_shapes/m4_profile_fsb_barrel_voxel.geo.json"))
    ).addTags("has_ar_front_sight", "barrel", "5.56x45");

    public static final IModular A2_PISTOL_GRIP = new RiflePistolGrip(
            RL( "a2_pistol_grip"), 0.08f, 0.07f, 0.12f, 0.05f)
            .addTags("rear_grip", "ar");

    public static final IModular MOE_GRIP = new RiflePistolGrip(
            RL( "moe_grip"), 0.1f, 0.1f, 0.1f, 0.06f)
            .addTags("rear_grip", "ar");

    public static final IModular AK_POLYMER_GRIP = new RiflePistolGrip(
            RL( "ak_polymer_grip"), 0.08f, 0.08f, 0.13f, 0.05f)
            .addTags("rear_grip", "ak");

    public static final IModular URGI_BARREL = new ARBarrel(RL( "urgi_barrel"), 0.9f, 0.12f, 1.0f,
            new SlotProvider(RL( "common/pivot_maps/urgi_barrel.pivot.geo.json"))
                    .addSlot(new SingleFixedSlot("MUZZLE").setFilter(SlotFilters.hasAllTags("muzzle", "ar"))),
            new VoxelHandler(RL("common/voxel_shapes/urgi_barrel_voxel.geo.json"))
    ).addTags("barrel", "5.56x45", "5.56x45");

    public static final IModular STANAG_MAG_30R = new Mag(RL( "stanag_mag_30r"), 0.11f, 30).addTags("mag", "ar", "5.56x45");
    public static final IModular PMAG_40R = new Mag(RL( "pmag_40r"), 0.2f, 40).addTags("mag", "ar", "5.56x45");
    public static final IModular SUREFIRE_MAG_60R = new Mag(RL( "surefire_mag_60r"), 0.33f, 60).addTags("mag", "ar", "5.56x45");
    public static final IModular USGI_MAG_20R = new Mag(RL( "usgi_mag_20r"), 0.07f, 20).addTags("mag", "ar", "5.56x45");

    public static final IModular MAG_6L18 = new Mag(RL( "6l18"), 0.28f, 45).addTags("mag", "ak", "5.45x39");
    public static final IModular MAG_6L23 = new Mag(RL( "6l23"), 0.2f, 30).addTags("mag", "ak", "5.45x39");
    public static final IModular MAG_6L31 = new Mag(RL( "6l31"), 0.3f, 60).addTags("mag", "ak", "5.45x39");


    public static final IModular CAR_15_HANDGUARD = new SplitARHandguard(
            RL("car_15_handguard"),
            0.26f, 0.03f,
            new SplitARHandguardVoxelHandler(GCR.RL("common/voxel_shapes/car_15_handguard_voxel.geo.json")),
            new IArmHandlerModular.AdditionalPropModifier(0.1f,0.1f,0.1f, 0.05f)
    ).addTags("handguard", "ar");

    public static final IModular M203 = new M203(
            RL( "m203"), 1.36f,
            new VoxelHandler(RL("common/voxel_shapes/m203_voxel.geo.json")),
            new IArmHandlerModular.AdditionalPropModifier(0.12f, 0.12f, -0.05f, -0.07f),
            3.3f, 2.45f, 60f, 20f, 40f, 140f,
            0.4f, 4f, 4)
            .addTags("under_barrel", "sub_weapon", "ar");

    public static final IModular A2_CARRY_HANDLE = new IronSight(
            RL( "a2_carry_handle"),
            new VoxelHandler(RL("common/voxel_shapes/a2_carry_handle.geo.json"), true, true),
            0.1f, true).addTags("sight", "iron_sight", "upper", "on_rail");


    public static final IModular KAC_FOLDING_SIGHT_FAR = new FoldingFarIronSight(
            RL( "kac_folding_sight_far"),
            false,
            0.01f,
            new FoldingIronSightVoxelHandler(RL("common/voxel_shapes/kac_folding_sight_far_voxel.geo.json"), false, false))
            .addTags("sight", "iron_sight", "upper", "on_rail");

    public static final IModular KAC_FOLDING_SIGHT_REAR = new FoldingRearIronSight(
            RL( "kac_folding_sight_rear"),
            new FoldingIronSightVoxelHandler(RL("common/voxel_shapes/kac_folding_sight_rear_voxel.geo.json"), true, false),
            0.01f,
            false,
            1.0f
            ).addTags("sight", "iron_sight", "upper", "on_rail");


    public static final IModular ACOG = new Scope(
            RL( "acog"),
            new VoxelHandler(RL("common/voxel_shapes/acog_voxel.geo.json")),
            0.3f, 1.0f, 1.5f, 4f, 0.15f
            ).addTags("sight", "scope", "upper", "on_rail");

    public static final IModular VORTEX_RAZOR_HD = new Scope(
            RL( "vortex_razor_hd"),
            new VoxelHandler(RL("common/voxel_shapes/vortex_razor_hd_voxel.geo.json")),
            0.61f, 1.0f, 1.0f, 6f, 0.1f
    ).addTags("sight", "scope", "upper", "on_rail");

    public static final IModular CANTED_RAIL = new CantedRail(
            RL( "canted_rail"),
            new VoxelHandler(RL("common/voxel_shapes/canted_rail.voxel.geo.json")),
            new SlotProvider(RL("common/pivot_maps/canted_rail.pivot.geo.json"))
                    .addSlot(new SingleFixedSlot("SIGHT")
                            .setFilter(SlotFilters.hasAllTags("sight", "on_rail", "upper", "canted_sight").and(SlotFilters.notModular(RL( "canted_rail"))))),
            1).addTags("canted_sight", "sight", "upper", "on_rail");

    public static final IModular M4_CARBINE_STOCK = new Stock(RL( "m4_carbine_stock"), 0.22f, 0.14f, 0.15f).addTags("stock", "ar");
    public static final IModular MOE_CARBINE_STOCK = new Stock(RL( "moe_carbine_stock"), 0.2f, 0.15f, 0.18f).addTags("stock", "ar");
    public static final IModular STOCK_6P34 = new Stock(RL( "6p34_stock"), 0.3f, 0.18f, 0.18f).addTags("stock", "ak");


    public static final IModular A2_FLASH_HINDER = new Muzzle(RL( "a2_flash_hinder"), 0.035f, 0.1f, 0.05f, IGun.FIRE_SOUND_NORMAL, 0, 1.0f).addTags("muzzle", "ar", "5.56x45");
    public static final IModular SOCOM_RC2 = new Muzzle(RL( "socom_rc2"), 0.48f, 0.15f, 0.075f, IGun.FIRE_SOUND_SUPPRESSED, -0.35f, 1.8f).addTags("muzzle", "ar", "5.56x45");
    public static final IModular AR15_MUZZLE_BRAKE = new Muzzle(RL( "ar15_muzzle_brake"), 0.045f, 0.2f, 0.1f, IGun.FIRE_SOUND_NORMAL, 0.1f, 1.0f).addTags("muzzle", "ar", "5.56x45");

    public static final IModular AK74_MUZZLE_BRAKE = new Muzzle(RL( "ak74_muzzle_brake"), 0.05f, 0.2f, 0.13f, IGun.FIRE_SOUND_NORMAL, 0.1f, 1.0f).addTags("muzzle", "ak", "5.45x39");
    public static final IModular PBS_4 = new Muzzle(RL( "pbs_4"), 0.65f, 0.18f, 0.07f, IGun.FIRE_SOUND_SUPPRESSED, -0.38f, 1.8f).addTags("muzzle", "ak", "5.45x39");
    public static final IModular DTK1_COMPENSATOR = new Muzzle(RL( "dtk1_compensator"), 0.06f, 0.22f, 0.1f, IGun.FIRE_SOUND_NORMAL, 0.1f, 1.0f).addTags("muzzle", "ak", "5.45x39");


    public static final IModular KAC_RAS_HANDGUARD = new SplitSlottedARHandguard(
            RL( "kac_ras_handguard"),
            new SlotProvider(RL( "common/pivot_maps/kac_ras.pivot.geo.json"))
                    .addSlot(new Rail("RAIL_LOWER", Direction.LOWER, 13.9162f, 0.9207f, -12.0747f)
                            .setFilter(SlotFilters.hasTag("on_rail").and(
                                    SlotFilters.hasTag("lower").or(SlotFilters.hasTag("all_rail_direction"))
                            )))
                    .addSlot(new Rail("RAIL_LEFT", Direction.LOWER, 13.9162f, 0.9207f, -12.0747f)
                            .setFilter(SlotFilters.hasAllTags("on_rail", "all_rail_direction")))
                    .addSlot(new Rail("RAIL_RIGHT", Direction.LOWER, 13.9162f, 0.9207f, -12.0747f)
                            .setFilter(SlotFilters.hasAllTags("on_rail", "all_rail_direction")))
                    .addSlot(new Rail("RAIL_UPPER", Direction.UPPER, 13.9162f, 0.9207f, -12.0747f)
                            .setFilter(SlotFilters.hasTag("on_rail").and(
                                    SlotFilters.hasTag("upper").or(SlotFilters.hasTag("all_rail_direction"))
                            ))),
            0.25f, 0.02f,
            new SplitARHandguardVoxelHandler(GCR.RL("common/voxel_shapes/kac_ras_voxel.geo.json")),
            new IArmHandlerModular.AdditionalPropModifier(0.085f,0.07f,0.09f, 0.05f),
            "RAIL_LOWER"
    ).addTags("handguard", "ar");

    public static final IModular DANIEL_DEFENSE_RIS_II_HANDGUARD = new SplitSlottedARHandguard(
            RL( "daniel_defense_ris_ii_handguard"),
            new SlotProvider(RL( "common/pivot_maps/daniel_defense_ris_ii.pivot.geo.json"))
                    .addSlot(new Rail("RAIL_LOWER", Direction.LOWER, 13.35f, 0.9207f, -33.1603f)
                            .setFilter(SlotFilters.hasTag("on_rail").and(
                                    SlotFilters.hasTag("lower").or(SlotFilters.hasTag("all_rail_direction"))
                            )))
                    .addSlot(new Rail("RAIL_LEFT", Direction.LOWER, 17.498f, -8.3778f, -34.2535f)
                            .setFilter(SlotFilters.hasAllTags("on_rail", "all_rail_direction")))
                    .addSlot(new Rail("RAIL_RIGHT", Direction.LOWER, 17.498f, -8.3778f, -34.2535f)
                            .setFilter(SlotFilters.hasAllTags("on_rail", "all_rail_direction")))
                    .addSlot(new Rail("RAIL_UPPER", Direction.UPPER, 17.498f, 2.698f, -12.102f)
                            .setFilter(SlotFilters.hasTag("on_rail").and(
                                    SlotFilters.hasTag("upper").or(SlotFilters.hasTag("all_rail_direction"))
                            )))
                    .addSlot(new Rail("RAIL_UPPER_FRONT", Direction.UPPER, -20.925f, -27.7092f, -34.2535f)
                            .setFilter(SlotFilters.hasTag("on_rail").and(
                                    SlotFilters.hasTag("upper").or(SlotFilters.hasTag("all_rail_direction"))
                            ))),
            0.5f, 0.03f,
            new SplitARHandguardVoxelHandler(GCR.RL("common/voxel_shapes/daniel_defense_ris_ii_voxel.geo.json")),
            new IArmHandlerModular.AdditionalPropModifier(0.12f,0.11f,0.05f, 0.05f),
            "RAIL_LOWER"
    ).addTags("handguard", "ar");

    public static final IModular URGI_HANDGUARD = new Handguard
            (RL( "urgi_handguard"),
            0.4f, 0.025f,
            new SlotProvider(RL( "common/pivot_maps/urgi_handguard.pivot.geo.json"))
                    .addSlot(MLokRail.of("RAIL_GRIP", Direction.LOWER, 6.3311f, -6.2771f, -38.6771f, 0.3311f, 6.6082f, 6)
                            .setFilter(SlotFilters.hasAllTags("on_rail", "m_lok_rail_fit").and(
                                    SlotFilters.hasTag("lower").or(SlotFilters.hasTag("all_rail_direction"))
                            )))
                    .addSlot(new Rail("RAIL_UPPER", Direction.UPPER, 17.5174f, -10.9463f, -38.0826f)
                            .setFilter(SlotFilters.hasTag("on_rail").and(
                                    SlotFilters.hasTag("upper").or(SlotFilters.hasTag("all_rail_direction"))
                            )))
                    .addSlot(MLokRail.of("RAIL_LEFT", Direction.LOWER, 6.3311f, -12.8852f, -38.6771f, 0.3311f, 6.6082f, 6)
                            .setFilter(SlotFilters.hasAllTags("on_rail", "m_lok_rail_fit", "all_rail_direction")))
                    .addSlot(MLokRail.of("RAIL_RIGHT", Direction.LOWER, 6.3311f, -12.8852f, -38.6771f, 0.3311f, 6.6082f, 6)
                            .setFilter(SlotFilters.hasAllTags("on_rail", "m_lok_rail_fit", "all_rail_direction"))),
            new VoxelHandler(RL("common/voxel_shapes/urgi_handguard_voxel.geo.json")),
            new IArmHandlerModular.AdditionalPropModifier(0.1f,0.12f,0.1f, 0.065f)
    ).addTags("handguard", "ar");

    public static final IModular AK_POLYMER_HANDGUARD_LOWER = new Handguard
            (RL("ak_polymer_handguard_lower"),
                    0.09f, 0.015f,
                    EmptySlotProvider.INSTANCE,
                    new VoxelHandler(RL("common/voxel_shapes/ak_polymer_handguard_lower_voxel.geo.json")),
                    new IArmHandlerModular.AdditionalPropModifier(0.07f,0.07f,0.1f, 0.05f)
            ).addTags("handguard", "ak", "lower");

    public static final IModular AK_POLYMER_HANDGUARD_UPPER = new SlotProviderVoxelModule(
            RL("ak_polymer_handguard_upper"),
            true,
            0.04f,
            Direction.NONE,
            EmptySlotProvider.INSTANCE,
            new VoxelHandler(RL("common/voxel_shapes/ak_polymer_handguard_upper_voxel.geo.json"))
            )
            .addTags("handguard", "ak", "upper");

    public static final IModular B10_HANDGUARD = new Handguard
            (RL("b10_handguard"),
                    0.15f, 0.012f,
                    new SlotProvider(RL( "common/pivot_maps/b10_handguard_pivot.geo.json"))
                            .addSlot(new Rail("RAIL_LOWER", Direction.LOWER, 9.2f, 1.6207f, -9f)
                                    .setFilter(SlotFilters.hasTag("on_rail").and(
                                            SlotFilters.hasTag("lower").or(SlotFilters.hasTag("all_rail_direction"))
                                    )))
                            .addSlot(new SingleFixedSlot("RAIL_MOUNT_LEFT", Direction.LOWER)
                                    .setFilter(SlotFilters.hasTag("b10_mount_rail")))
                            .addSlot(new SingleFixedSlot("RAIL_MOUNT_RIGHT", Direction.LOWER)
                                    .setFilter(SlotFilters.hasTag("b10_mount_rail"))),
                    new VoxelHandler(RL("common/voxel_shapes/b10_handguard_voxel.geo.json")),
                    new IArmHandlerModular.AdditionalPropModifier(0.08f,0.08f,0.1f, 0.05f)
            ).addTags("handguard", "ak", "lower", "zenitco");

    public static final IModular B10_MOUNT_RAIL = new SlotProviderVoxelModule(
            RL("b10_mount_rail"),
            true,
            0.01f,
            Direction.NONE,
            new SlotProvider(RL("common/pivot_maps/b10_mount_rail.pivot.geo.json"))
                    .addSlot(new Rail("RAIL", Direction.LOWER, 11.8513f, 0f, -11.9301f)
                    .setFilter(SlotFilters.hasAllTags("on_rail", "all_rail_direction"))),
            new VoxelHandler(RL("common/voxel_shapes/b10_mount_rail_voxel.geo.json"))
    ).addTags("b10_mount_rail");

    public static final IModular B19_HANDGUARD = new ZenitcoUpperHandguard(
            RL("b19_handguard"),
            true,
            0.075f,
            Direction.NONE,
            new SlotProvider(RL( "common/pivot_maps/b19_handguard_pivot.geo.json"))
                    .addSlot(new Rail("RAIL_UPPER", Direction.UPPER, 6.1258f, -1.5542f, -9.2342f)
                            .setFilter(SlotFilters.hasTag("on_rail").and(
                                    SlotFilters.hasTag("upper").or(SlotFilters.hasTag("all_rail_direction"))
                            ))
                    ),
            new VoxelHandler(RL("common/voxel_shapes/b19_handguard_voxel.geo.json"))
    ).addTags("handguard", "ak", "upper");

    public static final IModular B13_BRACKET = new SlotProviderVoxelModule(
            RL("b13_bracket"),
            true,
            0.175f,
            Direction.NONE,
            new SlotProvider(RL("common/pivot_maps/b13_bracket.pivot.geo.json"))
                    .addSlot(new Rail("RAIL", Direction.UPPER, 12.5686f, -3.4314f, -19.4315f)
                            .setFilter(SlotFilters.hasAllTags("sight", "upper", "on_rail"))),
            new VoxelHandler(RL("common/voxel_shapes/b13_bracket.voxel.geo.json"))
    ).addTags("ak", "mount");

    public static final IModular KAC_FORWARD_GRIP = new ForwardGrip(
            RL( "kac_forward_grip"),
            new MLokFitVoxelHandler(RL( "common/voxel_shapes/kac_forward_grip.voxel.geo.json")),
            0.06f,
            new IArmHandlerModular.AdditionalPropModifier(0.22f,0.15f,0.28f, 0.15f))
            .addTags("m_lok_rail_fit");

    public static final IModular RK_2_GRIP = new ForwardGrip(
            RL( "rk_2_grip"),
            new MLokFitVoxelHandler(RL( "common/voxel_shapes/rk_2_grip.voxel.geo.json")),
            0.07f,
            new IArmHandlerModular.AdditionalPropModifier(0.14f,0.2f,0.36f, 0.2f))
            .addTags("m_lok_rail_fit");

    public static final IModular M4A1 = new AR(
            RL( "m4a1"),
            RL( "common/pivot_maps/m4a1_main.pivot.geo.json"),

            new BaseProperties(850, 1.15f, 0.18f, 3.5f,
                    0.00075f, 0.1f,
                    1.3f, 4f,
                    30f,
                    0.005f,
                    0.05f / 60,
                    6,
                    RL("m4a1_fire"),
                    RL("m4a1_fire_suppressed"),
                    Map.of(
                            "mag_reload_length", 1.8f,
                            "mag_reload_empty_length", 2.45f,
                            "mag_reload_charge_length", 2.8f,
                            "remove_stuck_empty_length", 0.85f,
                            "remove_stuck_length", 0.55f
                    )
            ),
            new DisplayData()
                    .setTranslation(0, 8.775F, -7.175875f, -23.037498F, 0.0F, 0.0F, 0.0F, 0.625F, 0.625F, 0.625F)
                    .setTranslation(1, 0.0F, 1.3F, -0.1F, 0.0F, 0.0F, 0.0F, 0.15F, 0.15F, 0.15F)
                    .setTranslation(2, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.15F, 0.15F, 0.15F)
                    .setTranslation(3, 0.0F, 0.0F, 0.0F, 0.0F, 90.0F, 0.0F, 0.3F, 0.3F, 0.3F)
                    .setTranslation(4, -1.6F, 0.8F, -10.5F, 0.0F, 270.0F, 0.0F, 0.15F, 0.15F, 0.15F)
                    .setTranslation(5, -16.0F, -10.5F, 4.0F, -18.621124F, 40.83802F, 26.0F, 0.15F, 0.15F, 0.15F)
                    .setAimingTranslation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F),

            new RecoilData(
                    new RecoilImpulse(
                            6.5f, 11.25f,
                            4f, 4f,
                            20, 17,
                            0.11f, 0.5f, 160.0f),
                    new RecoilController(
                            350f, 40f,
                            145.0f, 11.5f,
                            210.0f, 9f,
                            145.0f, 14.5f,
                            900.0f, 18f,
                            2.0f, 1.25f,
                            2.5f, 2f,
                            13f),
                    new VisualRecoilMix(
                            0.46f, 25, 28, 1.5f, 0.9f, 1.6f,
                            0.56f, 65f, 0.47f, 1.15f,  2.5f,
                            0.0126f, 0.35f
                    )
            ),
            List.of(ARSemi.SEMI, ARFullAuto.FULL_AUTO))
            .addSlot(new ReplaceOnlySlot("BARREL").setFilter(SlotFilters.hasAllTags("barrel", "5.56x45")))
            .addSlot(new SingleFixedSlot("HANDGUARD").setFilter(SlotFilters.hasAllTags("handguard", "ar")))
            .addSlot(new ReplaceOnlySlot("REAR_GRIP").setFilter(SlotFilters.hasAllTags("rear_grip", "ar")))
            .addSlot(new SingleFixedSlot("STOCK").setFilter(SlotFilters.hasAllTags("stock", "ar")))
            .addSlot(new ReplaceOnlySlot("MAG").setFilter(SlotFilters.hasAllTags("mag", "ar", "5.56x45")))
            .addSlot(new Rail("SCOPE", Direction.UPPER, 10f, -1.802f, -14.4f)
                            .setFilter(SlotFilters.hasAllTags("sight", "upper", "on_rail")))
            .setDefaultModuleInitHandler(workspace -> {
                Unit root = workspace.getRootUnit();
                workspace.addChild(root, "BARREL", M4_PROFILE_FSB_BARREL.getID()).ifPresent(barrel -> {
                    workspace.addChild(barrel, "MUZZLE", A2_FLASH_HINDER.getID());
                });
                workspace.addChild(root, "HANDGUARD", CAR_15_HANDGUARD.getID());
                workspace.addChild(root, "REAR_GRIP", A2_PISTOL_GRIP.getID());
                workspace.addChild(root, "STOCK", M4_CARBINE_STOCK.getID());
                workspace.addChild(root, "MAG", STANAG_MAG_30R.getID());
                workspace.addChild(root, "SCOPE", A2_CARRY_HANDLE.getID());
            });


    public static final IModular DUSTCOVER_6P34 = new AKSimpleDustCover(
            RL( "6p34_dustcover"),
            0.15f,
            0.1f,
            0.025f)
            .addTags("ak", "dustcover");

    public static final IModular PDC_DUSTCOVER = new AKDustCover(
            RL( "pdc_dustcover"),
            0.194f,
            0.1f,
            0.025f,
            new SlotProvider(RL( "common/pivot_maps/pdc_dustcover_pivot.geo.json"))
                    .addSlot(
                            new Rail("SCOPE", Direction.UPPER, 11.713f, -1.802f, -16.5f)
                            .setFilter(SlotFilters.hasAllTags("sight", "upper", "on_rail"))
                    ),
            new VoxelHandler(RL("common/voxel_shapes/pdc_dustcover_voxel.geo.json")))
            .addTags("ak", "dustcover");

    public static final IModular AK74M = new AK(
            RL( "ak74m"),
            RL( "common/pivot_maps/ak74m_pivot.geo.json"),

            new BaseProperties(650, 2.4f, 0.2f, 3.3f,
                    0.0003f, 0.08f,
                    1.3f, 4f,
                    30f,
                    0.004f,
                    0.05f / 60, 5.5f,
                    RL("ak74m_fire"),
                    RL("ak74m_fire_suppressed"),
                    Map.of(
                            "mag_reload_length", 2.15f,
                            "mag_reload_empty_length", 3.2f,
                            "remove_stuck_length", 0.75f,
                            "remove_stuck_empty_length", 0.75f
                    )
            ),
            new DisplayData()
                    .setTranslation(DisplayData.FIRST_PERSON, 8.7375f, -6.325f, -22.75f, 0, 0, 0, 0.625f, 0.625f, 0.625f)
                    .setTranslation(DisplayData.THIRD_PERSON, 0, 1.3f, -0.1f, 0, 0, 0, 0.15f, 0.15f, 0.15f)
                    .setTranslation(DisplayData.GROUND, 0, 0, 0, 0, 0, 0, 0.15f, 0.15f, 0.15f)
                    .setTranslation(DisplayData.FRAME, 0, 0, 0, 0, 90, 0, 0.3f, 0.3f, 0.3f)
                    .setTranslation(DisplayData.GUN_MODIFY_SCREEN, -1.6f, 0.8f, -10.5f, 0, 270, 0, 0.15f, 0.15f, 0.15f)
                    .setTranslation(DisplayData.SPRINTING, -16, -10.5f, 4, -18.621124f, 40.83802f, 26, 0.15f, 0.15f, 0.15f)
                    .setAimingTranslation(0, 0, 0, 0, 0, 0),
            new RecoilData(
                    new RecoilImpulse(
                            5f, 10f,
                            5f, 5f,
                            15, 13,
                            0.15f, 0.6f,200.0f),
                    new RecoilController(
                            350f, 40f,
                            150.0f, 11f,
                            200.0f, 8f,
                            150.0f, 14.5f,
                            900.0f, 18f,
                            2.0f, 1.25f,
                            2.5f, 2f,
                            13f),
                    new VisualRecoilMix(
                            0.5f, 25.5f, 28, 1.5f, 0.9f, 1.6f,
                            0.625f, 60f, 0.48f, 1.25f,  2.5f,
                            0.01f, 0.35f
                    )
            ),
            List.of(AKSemi.SEMI, AKFullAuto.FULL_AUTO))
            .addSlot(new SingleFixedSlot("HANDGUARD_LOWER").setFilter(SlotFilters.hasAllTags("handguard", "ak", "lower")))
            .addSlot(new SingleFixedSlot("HANDGUARD_UPPER").setFilter(SlotFilters.hasAllTags("handguard", "ak", "upper")))
            .addSlot(new ReplaceOnlySlot("REAR_GRIP").setFilter(SlotFilters.hasAllTags("rear_grip", "ak")))
            .addSlot(new SingleFixedSlot("STOCK").setFilter(SlotFilters.hasAllTags("stock", "ak")))
            .addSlot(new ReplaceOnlySlot("MAG").setFilter(SlotFilters.hasAllTags("mag", "ak", "5.45x39")))
            .addSlot(new SingleFixedSlot("DUSTCOVER").setFilter(SlotFilters.hasAllTags("ak", "dustcover")))
            .addSlot(new SingleFixedSlot("MUZZLE").setFilter(SlotFilters.hasAllTags("ak", "muzzle", "5.45x39")))
            .addSlot(new SingleFixedSlot("MOUNT").setFilter(SlotFilters.hasAllTags("ak", "mount")))
            .setDefaultModuleInitHandler(workspace -> {
                Unit root = workspace.getRootUnit();
                workspace.addChild(root, "MUZZLE", AK74_MUZZLE_BRAKE.getID());
                workspace.addChild(root, "HANDGUARD_LOWER", AK_POLYMER_HANDGUARD_LOWER.getID());
                workspace.addChild(root, "HANDGUARD_UPPER", AK_POLYMER_HANDGUARD_UPPER.getID());
                workspace.addChild(root, "REAR_GRIP", AK_POLYMER_GRIP.getID());
                workspace.addChild(root, "STOCK", STOCK_6P34.getID());
                workspace.addChild(root, "MAG", MAG_6L23.getID());
                workspace.addChild(root, "DUSTCOVER", DUSTCOVER_6P34.getID());
            });

    public static final IModular VORTEX_RAZOR_RED_DOT = new RedDot(
            RL( "vortex_razor_red_dot"),
            new VoxelHandler(RL( "common/voxel_shapes/vortex_razor_red_dot.voxel.geo.json")),
            0.05f,
            false,
            1.1f
    ).addTags("sight", "upper", "red_dot", "on_rail", "canted_sight");

    public static final IModular EOTECH_EXPS3 = new RedDot(
            RL( "eotech_exps3"),
            new VoxelHandler(RL( "common/voxel_shapes/eotech_exps3_voxel.geo.json")),
            0.3f,
            false,
            1.1f
    ).addTags("sight", "upper", "red_dot", "on_rail");

    public static final IModular PEQ_15 = new PEQ15(
            RL( "peq_15"),
            0.22f, 13, 60, 12,
            Direction.UPPER,
            new MLokFitVoxelHandler(RL( "common/voxel_shapes/peq_15_voxel.geo.json"))
    ).addTags("on_rail", "all_rail_direction", "m_lok_rail_fit");

    private static ResourceLocation RL(String path) {
        return GCR.RL(path);
    }
}
