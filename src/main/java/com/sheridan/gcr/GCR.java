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
import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.ISlotProviderModular;
import com.sheridan.gcr.modularSys.SlotProvider;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.fire.closedBolt.ARFullAuto;
import com.sheridan.gcr.modularSys.fire.closedBolt.ARSemi;
import com.sheridan.gcr.modularSys.modules.*;
import com.sheridan.gcr.modularSys.modules.gunProperties.impl.BaseProperties;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
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


@Mod(GCR.MODID)
public class GCR {

    public static boolean IS_DEVELOPMENT;
    // Define mod id in a common place for everything to reference
    public static final String MODID = "gcr";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "gcr" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "gcr" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "gcr" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);


    public static final IModular M4_PROFILE_FSB_BARREL = new ARBarrel(RL( "m4_profile_fsb_barrel"), 1.0f, 0.1f,
            new SlotProvider(RL( "common/pivot_maps/m4_profile_fsb_barrel.pivot.geo.json"))
                    .addSlot(new SingleFixedSlot("UNDER_BARREL")
                            .setFilter(SlotFilters.hasAllTags("under_barrel", "ar")))
                    .addSlot(new SingleFixedSlot("MUZZLE")
                            .setFilter(SlotFilters.hasAllTags("muzzle", "ar"))),
            new VoxelHandler(RL("common/voxel_shapes/m4_profile_fsb_barrel_voxel.geo.json"))
    ).addTags("has_ar_front_sight", "barrel", "5.56x45");

    public static final IModular A2_PISTOL_GRIP = new RiflePistolGrip(
            RL( "a2_pistol_grip"), 0.08f, 0.07f, 0.12f, 0.05f)
            .addTags("rear_grip", "ar");

    public static final IModular MOE_GRIP = new RiflePistolGrip(
            RL( "moe_grip"), 0.1f, 0.1f, 0.1f, 0.06f)
            .addTags("rear_grip", "ar");

    public static final IModular URGI_BARREL = new ARBarrel(RL( "urgi_barrel"), 0.9f, 0.12f,
            new SlotProvider(RL( "common/pivot_maps/urgi_barrel.pivot.geo.json"))
                    .addSlot(new SingleFixedSlot("MUZZLE").setFilter(SlotFilters.hasAllTags("muzzle", "ar"))),
            new VoxelHandler(RL("common/voxel_shapes/urgi_barrel_voxel.geo.json"))
    ).addTags("barrel", "5.56x45");

    public static final IModular STANAG_MAG_30R = new Mag(RL( "stanag_mag_30r"), 0.11f, 30).addTags("mag", "ar", "5.56x45");
    public static final IModular PMAG_40R = new Mag(RL( "pmag_40r"), 0.18f, 40).addTags("mag", "ar", "5.56x45");
    public static final IModular SUREFIRE_MAG_60R = new Mag(RL( "surefire_mag_60r"), 0.33f, 60).addTags("mag", "ar", "5.56x45");
    public static final IModular USGI_MAG_20R = new Mag(RL( "usgi_mag_20r"), 0.07f, 20).addTags("mag", "ar", "5.56x45");


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
            0.61f, 1.0f, 1.0f, 6f, 0.15f
    ).addTags("sight", "scope", "upper", "on_rail");

    public static final IModular CANTED_RAIL = new CantedRail(
            RL( "canted_rail"),
            new VoxelHandler(RL("common/voxel_shapes/canted_rail.voxel.geo.json")),
            new SlotProvider(RL("common/pivot_maps/canted_rail.pivot.geo.json"))
                    .addSlot(new SingleFixedSlot("SIGHT")
                            .setFilter(SlotFilters.hasAllTags("sight", "on_rail", "upper", "canted_sight").and(SlotFilters.notModular(RL( "canted_rail"))))),
            1).addTags("canted_sight", "sight", "upper", "on_rail");

    public static final IModular M4_CARBINE_STOCK = new Stock(RL( "m4_carbine_stock"), 0.22f, 0.14f, 0.15f).addTags("stock", "ar");
    public static final IModular A2_FLASH_HINDER = new Muzzle(RL( "a2_flash_hinder"), 0.035f, 0.1f, 0.05f, IGun.FIRE_SOUND_NORMAL, 0).addTags("muzzle", "ar");
    public static final IModular SOCOM_RC2 = new Muzzle(RL( "socom_rc2"), 0.048f, 0.15f, 0.075f, IGun.FIRE_SOUND_SUPPRESSED, -0.35f).addTags("muzzle", "ar");

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

    public static final IModular URGI_HANDGUARD = new ARHandguard
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

            new BaseProperties(850, 1.15f, 0.25f, 3.5f,0.004f, 1.3f, 4f,
                    30f,
                    RL("m4a1_fire"),
                    RL("m4a1_fire_suppressed"),
                    Map.of(
                            "mag_reload_length", 1.8f,
                            "mag_reload_empty_length", 2.45f,
                            "mag_reload_charge_length", 2.8f,
                            "chamber_reload_length", 1.8f,
                            "chamber_reload_empty_length", 2.0f,
                            "remove_stuck_empty_length", 0.85f,
                            "remove_stuck_length", 0.55f
                    )
            ),
            new DisplayData()
                    .setTranslation(DisplayData.FIRST_PERSON, 9.075f, -7.8125f, -23.037498f, 0, 0, 0, 0.625f, 0.625f, 0.625f)
                    .setTranslation(DisplayData.THIRD_PERSON, 0, 1.3f, -0.1f, 0, 0, 0, 0.15f, 0.15f, 0.15f)
                    .setTranslation(DisplayData.GROUND, 0, 0, 0, 0, 0, 0, 0.15f, 0.15f, 0.15f)
                    .setTranslation(DisplayData.FRAME, 0, 0, 0, 0, 90, 0, 0.3f, 0.3f, 0.3f)
                    .setTranslation(DisplayData.GUN_MODIFY_SCREEN, -1.6f, 0.8f, -10.5f, 0, 270, 0, 0.15f, 0.15f, 0.15f)
                    .setTranslation(DisplayData.SPRINTING, -16, -10.5f, 4, -18.621124f, 40.83802f, 26, 0.15f, 0.15f, 0.15f)
                    .setAimingTranslation(0, 0, 0, 0, 0, 0),
            new RecoilData(
                    new RecoilImpulse(
                            30f, 1.8f,
                            20, 17f, 0.2f,
                            125.0f, 3f, 3f, 0.015f),
                    new RecoilController(
                            900.0f, 140f,
                            160.0f, 13f,
                            165.0f, 10f,
                            175.0f, 11f,
                            900.0f, 18f,
                            2.0f, 1.25f,
                            2.5f, 2f,
                            10f)
            ),
            List.of(ARSemi.SEMI, ARFullAuto.FULL_AUTO))
            .addSlot(new ReplaceOnlySlot("BARREL").setFilter(SlotFilters.hasAllTags("barrel", "5.56x45")))
            .addSlot(new SingleFixedSlot("HANDGUARD").setFilter(SlotFilters.hasAllTags("handguard", "ar")))
            .addSlot(new ReplaceOnlySlot("REAR_GRIP").setFilter(SlotFilters.hasAllTags("rear_grip", "ar")))
            .addSlot(new SingleFixedSlot("STOCK").setFilter(SlotFilters.hasAllTags("stock", "ar")))
            .addSlot(new SingleFixedSlot("MAG").setFilter(SlotFilters.hasAllTags("mag", "ar", "5.56x45")))
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

    public static final IModular VORTEX_RAZOR_RED_DOT = new RedDot(
            RL( "vortex_razor_red_dot"),
            new VoxelHandler(RL( "common/voxel_shapes/vortex_razor_red_dot.voxel.geo.json")),
            0.05f,
            false,
            1.1f
    ).addTags("sight", "upper", "red_dot", "on_rail", "canted_sight");

    public static final IModular PEQ_15 = new PEQ15(
            RL( "peq_15"),
            0.22f, 13, 60, 12,
            Direction.UPPER,
            new MLokFitVoxelHandler(RL( "common/voxel_shapes/peq_15_voxel.geo.json"))
    ).addTags("on_rail", "all_rail_direction", "m_lok_rail_fit");

    public static final DeferredItem<Item> M4A1_ITEM =
            ITEMS.register(M4A1.getSimpleID(), () -> new GunItem((AR) M4A1));
    public static final DeferredItem<Item> ACOG_ITEM =
            ITEMS.register(ACOG.getSimpleID(), () -> new ModuleItem<>(ACOG));
    public static final DeferredItem<Item> VORTEX_RAZOR_HD_ITEM =
            ITEMS.register(VORTEX_RAZOR_HD.getSimpleID(), () -> new ModuleItem<>(VORTEX_RAZOR_HD));
    public static final DeferredItem<Item> CANTED_RAIL_ITEM =
            ITEMS.register(CANTED_RAIL.getSimpleID(), () -> new ModuleItem<>(CANTED_RAIL));
    public static final DeferredItem<Item> A2_CARRY_HANDLE_ITEM =
            ITEMS.register(A2_CARRY_HANDLE.getSimpleID(), () -> new ModuleItem<>(A2_CARRY_HANDLE));
    public static final DeferredItem<Item> KAC_FOLDING_SIGHT_REAR_ITEM =
            ITEMS.register(KAC_FOLDING_SIGHT_REAR.getSimpleID(), () -> new ModuleItem<>(KAC_FOLDING_SIGHT_REAR));
    public static final DeferredItem<Item> KAC_FOLDING_SIGHT_FAR_ITEM =
            ITEMS.register(KAC_FOLDING_SIGHT_FAR.getSimpleID(), () -> new ModuleItem<>(KAC_FOLDING_SIGHT_FAR));
    public static final DeferredItem<Item> CAR_15_HANDGUARD_ITEM =
            ITEMS.register(CAR_15_HANDGUARD.getSimpleID(), () -> new ModuleItem<>(CAR_15_HANDGUARD));
    public static final DeferredItem<Item> A2_PISTOL_GRIP_ITEM =
            ITEMS.register(A2_PISTOL_GRIP.getSimpleID(), () -> new ModuleItem<>(A2_PISTOL_GRIP));
    public static final DeferredItem<Item> MOE_GRIP_ITEM =
            ITEMS.register(MOE_GRIP.getSimpleID(), () -> new ModuleItem<>(MOE_GRIP));
    public static final DeferredItem<Item> M4_PROFILE_FSB_BARREL_ITEM =
            ITEMS.register(M4_PROFILE_FSB_BARREL.getSimpleID(), () -> new ModuleItem<>(M4_PROFILE_FSB_BARREL));
    public static final DeferredItem<Item> STANAG_MAG_30R_ITEM =
            ITEMS.register(STANAG_MAG_30R.getSimpleID(), () -> new ModuleItem<>(STANAG_MAG_30R));
    public static final DeferredItem<Item> PMAG_40R_ITEM =
            ITEMS.register(PMAG_40R.getSimpleID(), () -> new ModuleItem<>(PMAG_40R));
    public static final DeferredItem<Item> SUREFIRE_MAG_60R_ITEM =
            ITEMS.register(SUREFIRE_MAG_60R.getSimpleID(), () -> new ModuleItem<>(SUREFIRE_MAG_60R));
    public static final DeferredItem<Item> USGI_MAG_20R_ITEM =
            ITEMS.register(USGI_MAG_20R.getSimpleID(), () -> new ModuleItem<>(USGI_MAG_20R));
    public static final DeferredItem<Item> M4_CARBINE_STOCK_ITEM =
            ITEMS.register(M4_CARBINE_STOCK.getSimpleID(), () -> new ModuleItem<>(M4_CARBINE_STOCK));
    public static final DeferredItem<Item> A2_FLASH_HINDER_ITEM =
            ITEMS.register(A2_FLASH_HINDER.getSimpleID(), () -> new ModuleItem<>(A2_FLASH_HINDER));
    public static final DeferredItem<Item> SOCOM_RC2_ITEM =
            ITEMS.register(SOCOM_RC2.getSimpleID(), () -> new ModuleItem<>(SOCOM_RC2));
    public static final DeferredItem<Item> KAC_RAS_HANDGUARD_ITEM =
            ITEMS.register(KAC_RAS_HANDGUARD.getSimpleID(), () -> new ModuleItem<>(KAC_RAS_HANDGUARD));
    public static final DeferredItem<Item> DANIEL_DEFENSE_RIS_II_HANDGUARD_ITEM =
            ITEMS.register(DANIEL_DEFENSE_RIS_II_HANDGUARD.getSimpleID(), () -> new ModuleItem<>(DANIEL_DEFENSE_RIS_II_HANDGUARD));
    public static final DeferredItem<Item> KAC_FORWARD_GRIP_ITEM =
            ITEMS.register(KAC_FORWARD_GRIP.getSimpleID(), () -> new ModuleItem<>(KAC_FORWARD_GRIP));
    public static final DeferredItem<Item> RK_6_GRIP_ITEM =
            ITEMS.register(RK_2_GRIP.getSimpleID(), () -> new ModuleItem<>(RK_2_GRIP));
    public static final DeferredItem<Item> M203_ITEM =
            ITEMS.register(M203.getSimpleID(), () -> new ModuleItem<>(M203));
    public static final DeferredItem<Item> URGI_BARREL_ITEM =
            ITEMS.register(URGI_BARREL.getSimpleID(), () -> new ModuleItem<>(URGI_BARREL));
    public static final DeferredItem<Item> URGI_HANDGUARD_ITEM =
            ITEMS.register(URGI_HANDGUARD.getSimpleID(), () -> new ModuleItem<>(URGI_HANDGUARD));
    public static final DeferredItem<Item> VORTEX_RAZOR_RED_DOT_ITEM =
            ITEMS.register(VORTEX_RAZOR_RED_DOT.getSimpleID(), () -> new ModuleItem<>(VORTEX_RAZOR_RED_DOT));
    public static final DeferredItem<Item> PEQ_15_ITEM =
            ITEMS.register(PEQ_15.getSimpleID(), () -> new ModuleItem<>(PEQ_15));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GUN_TAB =
            CREATIVE_MODE_TABS.register("gun",
                    () -> CreativeModeTab
                            .builder()
                            .title(Component.translatable("itemGroup.gcr.gun"))
                            .withTabsBefore(CreativeModeTabs.COMBAT)
                            .icon(() -> M4A1_ITEM.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                output.accept(M4A1_ITEM.get());
                            }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ATTACHMENT_TAB =
            CREATIVE_MODE_TABS.register("attachment",
                    () -> CreativeModeTab
                            .builder()
                            .title(Component.translatable("itemGroup.gcr.attachment"))
                            .withTabsBefore(GUN_TAB.getKey())
                            .icon(() -> ACOG_ITEM.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                output.accept(ACOG_ITEM.get());
                                output.accept(VORTEX_RAZOR_HD_ITEM.get());
                                output.accept(CANTED_RAIL_ITEM.get());
                                output.accept(A2_CARRY_HANDLE_ITEM.get());
                                output.accept(KAC_FOLDING_SIGHT_REAR_ITEM.get());
                                output.accept(KAC_FOLDING_SIGHT_FAR_ITEM.get());
                                output.accept(A2_PISTOL_GRIP_ITEM.get());
                                output.accept(MOE_GRIP_ITEM.get());
                                output.accept(M4_PROFILE_FSB_BARREL_ITEM.get());
                                output.accept(URGI_BARREL_ITEM.get());
                                output.accept(STANAG_MAG_30R_ITEM.get());
                                output.accept(PMAG_40R_ITEM.get());
                                output.accept(SUREFIRE_MAG_60R_ITEM.get());
                                output.accept(USGI_MAG_20R_ITEM.get());
                                output.accept(M4_CARBINE_STOCK_ITEM.get());
                                output.accept(A2_FLASH_HINDER_ITEM.get());
                                output.accept(SOCOM_RC2_ITEM.get());
                                output.accept(CAR_15_HANDGUARD_ITEM.get());
                                output.accept(KAC_RAS_HANDGUARD_ITEM.get());
                                output.accept(DANIEL_DEFENSE_RIS_II_HANDGUARD_ITEM.get());
                                output.accept(URGI_HANDGUARD_ITEM.get());
                                output.accept(KAC_FORWARD_GRIP_ITEM.get());
                                output.accept(RK_6_GRIP_ITEM.get());
                                output.accept(M203_ITEM.get());
                                output.accept(VORTEX_RAZOR_RED_DOT_ITEM.get());
                                output.accept(PEQ_15_ITEM.get());
                            }).build());

    public static ResourceLocation RL(String modId, String path) {
        return ResourceLocation.fromNamespaceAndPath(modId, path);
    }

    public static ResourceLocation RL(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public GCR(IEventBus modEventBus, ModContainer modContainer) {
        IS_DEVELOPMENT = !FMLEnvironment.production;
        if (IS_DEVELOPMENT) {
            LOGGER.info("Guns craft is running in dev mode");
        }
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::doAfterRegistryCallback);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ModContainers.REGISTER.register(modEventBus);
        ModSounds.handleRegister(modEventBus);
        ModData.register(modEventBus);
        ModComponents.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(CommonEvents.class);
        NeoForge.EVENT_BUS.register(com.sheridan.gcr.common.TestEvents.class);
        NeoForge.EVENT_BUS.register(PlayerStatusEvents.class);
        NeoForge.EVENT_BUS.register(GunHeatHandler.class);
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);


        if (FMLLoader.getDist() == Dist.CLIENT) {
            modEventBus.addListener(ClientEvents::registerCustomVanillaShader);
        }
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        System.out.println("server started!");
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        Commons.onServerStarted(event);
    }


    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(PivotMapLoader.getServer());
        event.addListener(VoxelLoader.getServer());
    }

    private void doAfterRegistryCallback(FMLLoadCompleteEvent event) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientTestingResources.regModels();
            ClientTestingResources.afterModelRegister();
        } else {
            System.out.println(FMLEnvironment.dist);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP: {}", FMLLoader.getDist());
        Collection<DeferredHolder<Item, ? extends Item>> entries = ITEMS.getEntries();
        for (DeferredHolder<Item, ? extends Item> entry : entries) {
            Item item = entry.get();
            if (item instanceof ModuleItem<?> moduleItem) {
                IModular module = moduleItem.getModule();
                if (module instanceof ISlotProviderModular modular) {
                    PivotMapLoader.getServer().book(modular);
                }
                if (module instanceof IVoxelHandlerModule voxelModule) {
                    VoxelLoader.getServer().book(voxelModule);
                }
                module.finalizeInit();
            }
        }
    }

    @EventBusSubscriber(modid = MODID)
    public static class PacketRegister {
        @SubscribeEvent
        public static void registerPackets(RegisterPayloadHandlersEvent event) {
            var registrar = event.registrar("114514");
            registrar.playBidirectional(
                    CommitModuleTreePacket.TYPE,
                    CommitModuleTreePacket.STREAM_CODEC,
                    new DirectionalPayloadHandler<>(
                            (packet, iPayloadContext) -> packet.onClient(packet, iPayloadContext),
                            (packet, iPayloadContext) -> packet.onServer(packet, iPayloadContext)
                    )
            );
            registrar.playBidirectional(
                    CommitModuleTreeResponsePacket.TYPE,
                    CommitModuleTreeResponsePacket.STREAM_CODEC,
                    new DirectionalPayloadHandler<>(
                            (packet, iPayloadContext) -> packet.onClient(packet, iPayloadContext),
                            (packet, iPayloadContext) -> packet.onServer(packet, iPayloadContext)
                    )
            );
            registrar.playBidirectional(
                    BroadcastPlayerStatusPacket.TYPE,
                    BroadcastPlayerStatusPacket.STREAM_CODEC,
                    new DirectionalPayloadHandler<>(
                            (packet, iPayloadContext) -> packet.onClient(packet, iPayloadContext),
                            (packet, iPayloadContext) -> packet.onServer(packet, iPayloadContext)
                    )
            );
            registrar.playBidirectional(
                    SyncPlayerStatusPacket.TYPE,
                    SyncPlayerStatusPacket.STREAM_CODEC,
                    new DirectionalPayloadHandler<>(
                            (packet, iPayloadContext) -> packet.onClient(packet, iPayloadContext),
                            (packet, iPayloadContext) -> packet.onServer(packet, iPayloadContext)
                    )
            );
            registrar.playBidirectional(
                    SyncGunStatusPacket.TYPE,
                    SyncGunStatusPacket.STREAM_CODEC,
                    new DirectionalPayloadHandler<>(
                            (packet, iPayloadContext) -> packet.onClient(packet, iPayloadContext),
                            (packet, iPayloadContext) -> packet.onServer(packet, iPayloadContext)
                    )
            );
            registrar.playBidirectional(
                    SwitchUsingSightPacket.TYPE,
                    SwitchUsingSightPacket.STREAM_CODEC,
                    new DirectionalPayloadHandler<>(
                            (packet, iPayloadContext) -> packet.onClient(packet, iPayloadContext),
                            (packet, iPayloadContext) -> packet.onServer(packet, iPayloadContext)
                    )
            );
            registrar.playBidirectional(
                    GunReloadPacket.TYPE,
                    GunReloadPacket.STREAM_CODEC,
                    new DirectionalPayloadHandler<>(
                            (packet, iPayloadContext) -> packet.onClient(packet, iPayloadContext),
                            (packet, iPayloadContext) -> packet.onServer(packet, iPayloadContext)
                    )
            );
            registrar.playBidirectional(
                    GunFirePacket.TYPE,
                    GunFirePacket.STREAM_CODEC,
                    new DirectionalPayloadHandler<>(
                            (packet, iPayloadContext) -> packet.onClient(packet, iPayloadContext),
                            (packet, iPayloadContext) -> packet.onServer(packet, iPayloadContext)
                    )
            );
            registrar.playBidirectional(
                    RemoveStuckPacket.TYPE,
                    RemoveStuckPacket.STREAM_CODEC,
                    new DirectionalPayloadHandler<>(
                            (packet, iPayloadContext) -> packet.onClient(packet, iPayloadContext),
                            (packet, iPayloadContext) -> packet.onServer(packet, iPayloadContext)
                    )
            );
            registrar.playBidirectional(
                    BroadcastLivingFirePacket.TYPE,
                    BroadcastLivingFirePacket.STREAM_CODEC,
                    new DirectionalPayloadHandler<>(
                            (packet, iPayloadContext) -> packet.onClient(packet, iPayloadContext),
                            (packet, iPayloadContext) -> packet.onServer(packet, iPayloadContext)
                    )
            );
            registrar.playBidirectional(
                    PlaySoundPacket.TYPE,
                    PlaySoundPacket.STREAM_CODEC,
                    new DirectionalPayloadHandler<>(
                            (packet, iPayloadContext) -> packet.onClient(packet, iPayloadContext),
                            (packet, iPayloadContext) -> packet.onServer(packet, iPayloadContext)
                    )
            );
            registrar.playBidirectional(
                    SubWeaponFirePacket.TYPE,
                    SubWeaponFirePacket.STREAM_CODEC,
                    new DirectionalPayloadHandler<>(
                            (packet, iPayloadContext) -> packet.onClient(packet, iPayloadContext),
                            (packet, iPayloadContext) -> packet.onServer(packet, iPayloadContext)
                    )
            );
            registrar.playBidirectional(
                    SubWeaponReloadPacket.TYPE,
                    SubWeaponReloadPacket.STREAM_CODEC,
                    new DirectionalPayloadHandler<>(
                            (packet, iPayloadContext) -> packet.onClient(packet, iPayloadContext),
                            (packet, iPayloadContext) -> packet.onServer(packet, iPayloadContext)
                    )
            );

            registrar.playBidirectional(
                    GunFireAckPacket.TYPE,
                    GunFireAckPacket.STREAM_CODEC,
                    new DirectionalPayloadHandler<>(
                            (packet, iPayloadContext) -> packet.onClient(packet, iPayloadContext),
                            (packet, iPayloadContext) -> packet.onServer(packet, iPayloadContext)
                    )
            );

            registrar.playBidirectional(
                    InitClientGunDataPacket.TYPE,
                    InitClientGunDataPacket.STREAM_CODEC,
                    new DirectionalPayloadHandler<>(
                            (packet, iPayloadContext) -> packet.onClient(packet, iPayloadContext),
                            (packet, iPayloadContext) -> packet.onServer(packet, iPayloadContext)
                    )
            );
        }
    }


    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

            NeoForge.EVENT_BUS.register(GuiEvents.class);
            NeoForge.EVENT_BUS.register(ClientEvents.class);
            NeoForge.EVENT_BUS.register(RenderEvents.class);
            NeoForge.EVENT_BUS.register(TestEvents.class);
            NeoForge.EVENT_BUS.register(ControllerEvents.class);
            NeoForge.EVENT_BUS.register(FPMuzzleFlashEnvLightingRenderer.class);
            NeoForge.EVENT_BUS.register(FabulousDepthTextureHandler.class);
            NeoForge.EVENT_BUS.register(FlashLightRenderer.class);
            NeoForge.EVENT_BUS.register(DelayedRenderTaskHandler.class);
            NeoForge.EVENT_BUS.register(IrisGunPostRenderer.class);
            NeoForge.EVENT_BUS.register(GunEffectManager.class);
            NeoForge.EVENT_BUS.register(LaserEffectRenderer.class);
            Client.onClientSetup(event);

            Collection<DeferredHolder<Item, ? extends Item>> entries = ITEMS.getEntries();
            for (DeferredHolder<Item, ? extends Item> entry : entries) {
                Item item = entry.get();
                if (item instanceof ModuleItem<?> moduleItem) {
                    IModular module = moduleItem.getModule();
                    if (module instanceof ISlotProviderModular modular) {
                        PivotMapLoader.getClient().book(modular);
                    }
                    if (module instanceof IVoxelHandlerModule voxelModule) {
                        VoxelLoader.getClient().book(voxelModule);
                    }
                    module.finalizeInit();
                }
            }

            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
            PivotMapLoader.getClient().trigger(resourceManager, false);
            VoxelLoader.getClient().trigger(resourceManager, false);

            RenderSystem.recordRenderCall(MuzzleFlashEnvShader::init);
            RenderSystem.recordRenderCall(FabulousMergeDepthShader::init);
            RenderSystem.recordRenderCall(ScopeViewShadingShader::init);
            RenderSystem.recordRenderCall(IrisGunPostShader::init);
            RenderSystem.recordRenderCall(DepthCopyShader::init);
            RenderSystem.recordRenderCall(LaserGlowShader::init);
        }

        @SubscribeEvent
        public static void registerParticles(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ModParticles.FLASH.get(), FlashParticle.Provider::new);
            event.registerSpriteSet(ModParticles.FRAGMENT.get(), FragmentParticle.Provider::new);
            event.registerSpriteSet(ModParticles.SPARK.get(), SparkParticle.Provider::new);
            event.registerSpriteSet(ModParticles.HEAT_SMOKE.get(), EmberParticle.Provider::new);
        }

        @SubscribeEvent // on the mod event bus only on the physical client
        public static void registerScreens(RegisterMenuScreensEvent event) {

        }

        @SubscribeEvent
        public static void registerKeyMapping(RegisterKeyMappingsEvent event) {
            KeyBinds.register(event);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.BULLET.get(), BulletRenderer::new);
            event.registerEntityRenderer(ModEntities.GRENADE.get(), M433Renderer::new);
        }
    }
}
