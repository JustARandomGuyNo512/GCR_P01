package com.sheridan.gcr;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.sheridan.gcr.client.GunEffectManager;
import com.sheridan.gcr.client.KeyBinds;
import com.sheridan.gcr.client.events.ClientEvents;
import com.sheridan.gcr.client.events.ControllerEvents;
import com.sheridan.gcr.client.events.RenderEvents;
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
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.items.ModuleItem;
import com.sheridan.gcr.modularSys.*;
import com.sheridan.gcr.modularSys.modules.*;
import com.sheridan.gcr.modularSys.modules.guns.ak.AK;
import com.sheridan.gcr.modularSys.modules.guns.ar.AR;
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


    public static final DeferredItem<Item> M4A1_ITEM =
            ITEMS.register(GCRModules.M4A1.getSimpleID(), () -> new GunItem((AR) GCRModules.M4A1));
    public static final DeferredItem<Item> AK74M_ITEM =
            ITEMS.register(GCRModules.AK74M.getSimpleID(), () -> new GunItem((AK) GCRModules.AK74M));


    public static final DeferredItem<Item> ACOG_ITEM =
            ITEMS.register(GCRModules.ACOG.getSimpleID(), () -> new ModuleItem<>(GCRModules.ACOG));
    public static final DeferredItem<Item> VORTEX_RAZOR_HD_ITEM =
            ITEMS.register(GCRModules.VORTEX_RAZOR_HD.getSimpleID(), () -> new ModuleItem<>(GCRModules.VORTEX_RAZOR_HD));
    public static final DeferredItem<Item> CANTED_RAIL_ITEM =
            ITEMS.register(GCRModules.CANTED_RAIL.getSimpleID(), () -> new ModuleItem<>(GCRModules.CANTED_RAIL));
    public static final DeferredItem<Item> A2_CARRY_HANDLE_ITEM =
            ITEMS.register(GCRModules.A2_CARRY_HANDLE.getSimpleID(), () -> new ModuleItem<>(GCRModules.A2_CARRY_HANDLE));
    public static final DeferredItem<Item> KAC_FOLDING_SIGHT_REAR_ITEM =
            ITEMS.register(GCRModules.KAC_FOLDING_SIGHT_REAR.getSimpleID(), () -> new ModuleItem<>(GCRModules.KAC_FOLDING_SIGHT_REAR));
    public static final DeferredItem<Item> KAC_FOLDING_SIGHT_FAR_ITEM =
            ITEMS.register(GCRModules.KAC_FOLDING_SIGHT_FAR.getSimpleID(), () -> new ModuleItem<>(GCRModules.KAC_FOLDING_SIGHT_FAR));


    public static final DeferredItem<Item> A2_PISTOL_GRIP_ITEM =
            ITEMS.register(GCRModules.A2_PISTOL_GRIP.getSimpleID(), () -> new ModuleItem<>(GCRModules.A2_PISTOL_GRIP));
    public static final DeferredItem<Item> MOE_GRIP_ITEM =
            ITEMS.register(GCRModules.MOE_GRIP.getSimpleID(), () -> new ModuleItem<>(GCRModules.MOE_GRIP));
    public static final DeferredItem<Item> AK_POLYMER_GRIP_ITEM =
            ITEMS.register(GCRModules.AK_POLYMER_GRIP.getSimpleID(), () -> new ModuleItem<>(GCRModules.AK_POLYMER_GRIP));


    public static final DeferredItem<Item> M4_PROFILE_FSB_BARREL_ITEM =
            ITEMS.register(GCRModules.M4_PROFILE_FSB_BARREL.getSimpleID(), () -> new ModuleItem<>(GCRModules.M4_PROFILE_FSB_BARREL));
    public static final DeferredItem<Item> STANAG_MAG_30R_ITEM =
            ITEMS.register(GCRModules.STANAG_MAG_30R.getSimpleID(), () -> new ModuleItem<>(GCRModules.STANAG_MAG_30R));
    public static final DeferredItem<Item> PMAG_40R_ITEM =
            ITEMS.register(GCRModules.PMAG_40R.getSimpleID(), () -> new ModuleItem<>(GCRModules.PMAG_40R));
    public static final DeferredItem<Item> SUREFIRE_MAG_60R_ITEM =
            ITEMS.register(GCRModules.SUREFIRE_MAG_60R.getSimpleID(), () -> new ModuleItem<>(GCRModules.SUREFIRE_MAG_60R));
    public static final DeferredItem<Item> USGI_MAG_20R_ITEM =
            ITEMS.register(GCRModules.USGI_MAG_20R.getSimpleID(), () -> new ModuleItem<>(GCRModules.USGI_MAG_20R));


    public static final DeferredItem<Item> MAG_6L18_ITEM =
            ITEMS.register(GCRModules.MAG_6L18.getSimpleID(), () -> new ModuleItem<>(GCRModules.MAG_6L18));
    public static final DeferredItem<Item> MAG_6L23_ITEM =
            ITEMS.register(GCRModules.MAG_6L23.getSimpleID(), () -> new ModuleItem<>(GCRModules.MAG_6L23));
    public static final DeferredItem<Item> MAG_6L31_ITEM =
            ITEMS.register(GCRModules.MAG_6L31.getSimpleID(), () -> new ModuleItem<>(GCRModules.MAG_6L31));



    public static final DeferredItem<Item> M4_CARBINE_STOCK_ITEM =
            ITEMS.register(GCRModules.M4_CARBINE_STOCK.getSimpleID(), () -> new ModuleItem<>(GCRModules.M4_CARBINE_STOCK));
    public static final DeferredItem<Item> MOE_CARBINE_STOCK_ITEM =
            ITEMS.register(GCRModules.MOE_CARBINE_STOCK.getSimpleID(), () -> new ModuleItem<>(GCRModules.MOE_CARBINE_STOCK));
    public static final DeferredItem<Item> STOCK_6P34_ITEM =
            ITEMS.register(GCRModules.STOCK_6P34.getSimpleID(), () -> new ModuleItem<>(GCRModules.STOCK_6P34));


    public static final DeferredItem<Item> A2_FLASH_HINDER_ITEM =
            ITEMS.register(GCRModules.A2_FLASH_HINDER.getSimpleID(), () -> new ModuleItem<>(GCRModules.A2_FLASH_HINDER));
    public static final DeferredItem<Item> SOCOM_RC2_ITEM =
            ITEMS.register(GCRModules.SOCOM_RC2.getSimpleID(), () -> new ModuleItem<>(GCRModules.SOCOM_RC2));
    public static final DeferredItem<Item> AR15_MUZZLE_BRAKE_ITEM =
            ITEMS.register(GCRModules.AR15_MUZZLE_BRAKE.getSimpleID(), () -> new ModuleItem<>(GCRModules.AR15_MUZZLE_BRAKE));
    public static final DeferredItem<Item> AK74_MUZZLE_BRAKE_ITEM =
            ITEMS.register(GCRModules.AK74_MUZZLE_BRAKE.getSimpleID(), () -> new ModuleItem<>(GCRModules.AK74_MUZZLE_BRAKE));
    public static final DeferredItem<Item> PBS_4_ITEM =
            ITEMS.register(GCRModules.PBS_4.getSimpleID(), () -> new ModuleItem<>(GCRModules.PBS_4));
    public static final DeferredItem<Item> DTK1_COMPENSATOR_ITEM =
            ITEMS.register(GCRModules.DTK1_COMPENSATOR.getSimpleID(), () -> new ModuleItem<>(GCRModules.DTK1_COMPENSATOR));
    public static final DeferredItem<Item> DTKP_545_ITEM =
            ITEMS.register(GCRModules.DTKP_545.getSimpleID(), () -> new ModuleItem<>(GCRModules.DTKP_545));


    public static final DeferredItem<Item> KAC_FORWARD_GRIP_ITEM =
            ITEMS.register(GCRModules.KAC_FORWARD_GRIP.getSimpleID(), () -> new ModuleItem<>(GCRModules.KAC_FORWARD_GRIP));
    public static final DeferredItem<Item> RK_6_GRIP_ITEM =
            ITEMS.register(GCRModules.RK_2_GRIP.getSimpleID(), () -> new ModuleItem<>(GCRModules.RK_2_GRIP));
    public static final DeferredItem<Item> M203_ITEM =
            ITEMS.register(GCRModules.M203.getSimpleID(), () -> new ModuleItem<>(GCRModules.M203));
    public static final DeferredItem<Item> URGI_BARREL_ITEM =
            ITEMS.register(GCRModules.URGI_BARREL.getSimpleID(), () -> new ModuleItem<>(GCRModules.URGI_BARREL));


    public static final DeferredItem<Item> URGI_HANDGUARD_ITEM =
            ITEMS.register(GCRModules.URGI_HANDGUARD.getSimpleID(), () -> new ModuleItem<>(GCRModules.URGI_HANDGUARD));
    public static final DeferredItem<Item> CAR_15_HANDGUARD_ITEM =
            ITEMS.register(GCRModules.CAR_15_HANDGUARD.getSimpleID(), () -> new ModuleItem<>(GCRModules.CAR_15_HANDGUARD));
    public static final DeferredItem<Item> KAC_RAS_HANDGUARD_ITEM =
            ITEMS.register(GCRModules.KAC_RAS_HANDGUARD.getSimpleID(), () -> new ModuleItem<>(GCRModules.KAC_RAS_HANDGUARD));
    public static final DeferredItem<Item> DANIEL_DEFENSE_RIS_II_HANDGUARD_ITEM =
            ITEMS.register(GCRModules.DANIEL_DEFENSE_RIS_II_HANDGUARD.getSimpleID(), () -> new ModuleItem<>(GCRModules.DANIEL_DEFENSE_RIS_II_HANDGUARD));
    public static final DeferredItem<Item> AK_POLYMER_HANDGUARD_LOWER_ITEM =
            ITEMS.register(GCRModules.AK_POLYMER_HANDGUARD_LOWER.getSimpleID(), () -> new ModuleItem<>(GCRModules.AK_POLYMER_HANDGUARD_LOWER));
    public static final DeferredItem<Item> AK_POLYMER_HANDGUARD_UPPER_ITEM =
            ITEMS.register(GCRModules.AK_POLYMER_HANDGUARD_UPPER.getSimpleID(), () -> new ModuleItem<>(GCRModules.AK_POLYMER_HANDGUARD_UPPER));
    public static final DeferredItem<Item> B10_HANDGUARD_ITEM =
            ITEMS.register(GCRModules.B10_HANDGUARD.getSimpleID(), () -> new ModuleItem<>(GCRModules.B10_HANDGUARD));
    public static final DeferredItem<Item> B19_HANDGUARD_ITEM =
            ITEMS.register(GCRModules.B19_HANDGUARD.getSimpleID(), () -> new ModuleItem<>(GCRModules.B19_HANDGUARD));
    public static final DeferredItem<Item> B10_MOUNT_RAIL_ITEM =
            ITEMS.register(GCRModules.B10_MOUNT_RAIL.getSimpleID(), () -> new ModuleItem<>(GCRModules.B10_MOUNT_RAIL));
    public static final DeferredItem<Item> B13_BRACKET_ITEM =
            ITEMS.register(GCRModules.B13_BRACKET.getSimpleID(), () -> new ModuleItem<>(GCRModules.B13_BRACKET));



    public static final DeferredItem<Item> VORTEX_RAZOR_RED_DOT_ITEM =
            ITEMS.register(GCRModules.VORTEX_RAZOR_RED_DOT.getSimpleID(), () -> new ModuleItem<>(GCRModules.VORTEX_RAZOR_RED_DOT));
    public static final DeferredItem<Item> EOTECH_EXPS3_ITEM =
            ITEMS.register(GCRModules.EOTECH_EXPS3.getSimpleID(), () -> new ModuleItem<>(GCRModules.EOTECH_EXPS3));
    public static final DeferredItem<Item> PEQ_15_ITEM =
            ITEMS.register(GCRModules.PEQ_15.getSimpleID(), () -> new ModuleItem<>(GCRModules.PEQ_15));

    public static final DeferredItem<Item> DUSTCOVER_6P34_ITEM =
            ITEMS.register(GCRModules.DUSTCOVER_6P34.getSimpleID(), () -> new ModuleItem<>(GCRModules.DUSTCOVER_6P34));
    public static final DeferredItem<Item> PDC_DUSTCOVER_ITEM =
            ITEMS.register(GCRModules.PDC_DUSTCOVER.getSimpleID(), () -> new ModuleItem<>(GCRModules.PDC_DUSTCOVER));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GUN_TAB =
            CREATIVE_MODE_TABS.register("gun",
                    () -> CreativeModeTab
                            .builder()
                            .title(Component.translatable("itemGroup.gcr.gun"))
                            .withTabsBefore(CreativeModeTabs.COMBAT)
                            .icon(() -> M4A1_ITEM.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                output.accept(M4A1_ITEM.get());
                                output.accept(AK74M_ITEM.get());
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
                                output.accept(AK_POLYMER_GRIP_ITEM.get());

                                output.accept(M4_PROFILE_FSB_BARREL_ITEM.get());
                                output.accept(URGI_BARREL_ITEM.get());
                                output.accept(STANAG_MAG_30R_ITEM.get());
                                output.accept(PMAG_40R_ITEM.get());
                                output.accept(SUREFIRE_MAG_60R_ITEM.get());
                                output.accept(USGI_MAG_20R_ITEM.get());
                                output.accept(MAG_6L18_ITEM.get());
                                output.accept(MAG_6L23_ITEM.get());
                                output.accept(MAG_6L31_ITEM.get());

                                output.accept(M4_CARBINE_STOCK_ITEM.get());
                                output.accept(MOE_CARBINE_STOCK_ITEM.get());
                                output.accept(STOCK_6P34_ITEM.get());

                                output.accept(A2_FLASH_HINDER_ITEM.get());
                                output.accept(SOCOM_RC2_ITEM.get());
                                output.accept(AR15_MUZZLE_BRAKE_ITEM.get());
                                output.accept(AK74_MUZZLE_BRAKE_ITEM.get());
                                output.accept(PBS_4_ITEM.get());
                                output.accept(DTKP_545_ITEM.get());
                                output.accept(DTK1_COMPENSATOR_ITEM.get());

                                output.accept(CAR_15_HANDGUARD_ITEM.get());
                                output.accept(KAC_RAS_HANDGUARD_ITEM.get());
                                output.accept(DANIEL_DEFENSE_RIS_II_HANDGUARD_ITEM.get());
                                output.accept(URGI_HANDGUARD_ITEM.get());
                                output.accept(AK_POLYMER_HANDGUARD_LOWER_ITEM.get());
                                output.accept(AK_POLYMER_HANDGUARD_UPPER_ITEM.get());
                                output.accept(B10_HANDGUARD_ITEM.get());
                                output.accept(B19_HANDGUARD_ITEM.get());
                                output.accept(B10_MOUNT_RAIL_ITEM.get());
                                output.accept(B13_BRACKET_ITEM.get());


                                output.accept(KAC_FORWARD_GRIP_ITEM.get());
                                output.accept(RK_6_GRIP_ITEM.get());
                                output.accept(M203_ITEM.get());
                                output.accept(VORTEX_RAZOR_RED_DOT_ITEM.get());
                                output.accept(EOTECH_EXPS3_ITEM.get());
                                output.accept(PEQ_15_ITEM.get());

                                output.accept(DUSTCOVER_6P34_ITEM.get());
                                output.accept(PDC_DUSTCOVER_ITEM.get());
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

//            registrar.playBidirectional(
//                    SyncHeatDataPacket.TYPE,
//                    SyncHeatDataPacket.STREAM_CODEC,
//                    new DirectionalPayloadHandler<>(
//                            (packet, iPayloadContext) -> packet.onClient(packet, iPayloadContext),
//                            (packet, iPayloadContext) -> packet.onServer(packet, iPayloadContext)
//                    )
//            );
        }
    }


    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

            NeoForge.EVENT_BUS.register(GuiEvents.class);
            NeoForge.EVENT_BUS.register(ClientEvents.class);
            NeoForge.EVENT_BUS.register(RenderEvents.class);
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
