package com.sheridan.gcr;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.sheridan.gcr.blocks.VendingMachine;
import com.sheridan.gcr.client.GunEffectManager;
import com.sheridan.gcr.client.KeyBinds;
import com.sheridan.gcr.client.events.ClientEvents;
import com.sheridan.gcr.client.events.ControllerEvents;
import com.sheridan.gcr.client.events.RenderEvents;
import com.sheridan.gcr.client.render.delayed.DelayedRenderTaskHandler;
import com.sheridan.gcr.client.render.entity.BulletRenderer;
import com.sheridan.gcr.client.render.entity.GrenadeRenderer;
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
import com.sheridan.gcr.config.PlayerHitZoneConfig;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.items.ModuleItem;
import com.sheridan.gcr.items.MagazineItem;
import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.ISlotProviderModular;
import com.sheridan.gcr.modularSys.ModuleRegister;
import com.sheridan.gcr.modularSys.modules.IVoxelHandlerModule;
import com.sheridan.gcr.modularSys.modules.guns.Gun;
import com.sheridan.gcr.modularSys.modules.guns.ak.AK;
import com.sheridan.gcr.modularSys.modules.guns.ar.AR;
import com.sheridan.gcr.modularSys.util.io.PivotMapLoader;
import com.sheridan.gcr.modularSys.util.io.VoxelLoader;
import com.sheridan.gcr.network.GCRNetwork;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;


@Mod(GCR.MODID)
public class GCR {

    public static boolean IS_DEVELOPMENT;
    // Define mod id in a common place for everything to reference
    public static final String MODID = "gcr";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "gcr" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "gcr" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "gcr" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);



    public static final RegistryObject<Item> UNIVERSAL_CARTRIDGE_ITEM =
            ITEMS.register("universal_cartridge", () -> new Item(new Item.Properties().stacksTo(64)));

    // Legacy universal ammo ID is retained for old saved inventories only.
    // Physical magazine loading now uses caliber-compatible weapon cartridges.
    public static final RegistryObject<Item> M4A1_CARTRIDGE_ITEM =
            ITEMS.register("m4a1_cartridge", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> AK74M_CARTRIDGE_ITEM =
            ITEMS.register("ak74m_cartridge", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> AK24_CARTRIDGE_ITEM =
            ITEMS.register("ak24_cartridge", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> MCX_SPEAR_CARTRIDGE_ITEM =
            ITEMS.register("mcx_spear_cartridge", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> AKMN_CARTRIDGE_ITEM =
            ITEMS.register("akmn_cartridge", () -> new Item(new Item.Properties().stacksTo(64)));

    // Canonical physical ammunition is identified by caliber, never gun model.
    // Old *_cartridge IDs remain registered (but hidden from Creative) so
    // previously saved inventories can still be loaded and consumed.
    public static final RegistryObject<Item> AMMO_556X45_ITEM =
            ITEMS.register("ammo_5_56x45", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> AMMO_545X39_ITEM =
            ITEMS.register("ammo_5_45x39", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> AMMO_68X51_ITEM =
            ITEMS.register("ammo_6_8x51", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> AMMO_762X39_ITEM =
            ITEMS.register("ammo_7_62x39", () -> new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> M4A1_ITEM =
            ITEMS.register(GCRModules.M4A1.getSimpleID(), () -> new GunItem((AR) GCRModules.M4A1));
    public static final RegistryObject<Item> AK74M_ITEM =
            ITEMS.register(GCRModules.AK74M.getSimpleID(), () -> new GunItem((AK) GCRModules.AK74M));
    public static final RegistryObject<Item> AK24_ITEM =
            ITEMS.register(GCRModules.AK24.getSimpleID(), () -> new GunItem((AR) GCRModules.AK24));
    public static final RegistryObject<Item> MCX_SPEAR_ITEM =
            ITEMS.register(GCRModules.MCX_SPEAR.getSimpleID(), () -> new GunItem((AR) GCRModules.MCX_SPEAR));
    public static final RegistryObject<Item> AKMN_ITEM =
            ITEMS.register(GCRModules.AKMN.getSimpleID(), () -> new GunItem((AK) GCRModules.AKMN));


    public static final RegistryObject<Item> ACOG_ITEM =
            ITEMS.register(GCRModules.ACOG.getSimpleID(), () -> new ModuleItem<>(GCRModules.ACOG));
    public static final RegistryObject<Item> ELCAN_ITEM =
            ITEMS.register(GCRModules.ELCAN.getSimpleID(), () -> new ModuleItem<>(GCRModules.ELCAN));
    public static final RegistryObject<Item> VORTEX_RAZOR_HD_ITEM =
            ITEMS.register(GCRModules.VORTEX_RAZOR_HD.getSimpleID(), () -> new ModuleItem<>(GCRModules.VORTEX_RAZOR_HD));
    public static final RegistryObject<Item> CANTED_RAIL_ITEM =
            ITEMS.register(GCRModules.CANTED_RAIL.getSimpleID(), () -> new ModuleItem<>(GCRModules.CANTED_RAIL));
    public static final RegistryObject<Item> A2_CARRY_HANDLE_ITEM =
            ITEMS.register(GCRModules.A2_CARRY_HANDLE.getSimpleID(), () -> new ModuleItem<>(GCRModules.A2_CARRY_HANDLE));
    public static final RegistryObject<Item> KAC_FOLDING_SIGHT_REAR_ITEM =
            ITEMS.register(GCRModules.KAC_FOLDING_SIGHT_REAR.getSimpleID(), () -> new ModuleItem<>(GCRModules.KAC_FOLDING_SIGHT_REAR));
    public static final RegistryObject<Item> KAC_FOLDING_SIGHT_FAR_ITEM =
            ITEMS.register(GCRModules.KAC_FOLDING_SIGHT_FAR.getSimpleID(), () -> new ModuleItem<>(GCRModules.KAC_FOLDING_SIGHT_FAR));
    public static final RegistryObject<Item> MAGPUL_MBUS_PRO_SIGHT_REAR_ITEM =
            ITEMS.register(GCRModules.MAGPUL_MBUS_PRO_SIGHT_REAR.getSimpleID(), () -> new ModuleItem<>(GCRModules.MAGPUL_MBUS_PRO_SIGHT_REAR));
    public static final RegistryObject<Item> MAGPUL_MBUS_PRO_SIGHT_FAR_ITEM =
            ITEMS.register(GCRModules.MAGPUL_MBUS_PRO_SIGHT_FAR.getSimpleID(), () -> new ModuleItem<>(GCRModules.MAGPUL_MBUS_PRO_SIGHT_FAR));


    public static final RegistryObject<Item> A2_PISTOL_GRIP_ITEM =
            ITEMS.register(GCRModules.A2_PISTOL_GRIP.getSimpleID(), () -> new ModuleItem<>(GCRModules.A2_PISTOL_GRIP));
    public static final RegistryObject<Item> MOE_GRIP_ITEM =
            ITEMS.register(GCRModules.MOE_GRIP.getSimpleID(), () -> new ModuleItem<>(GCRModules.MOE_GRIP));
    public static final RegistryObject<Item> AK_POLYMER_GRIP_ITEM =
            ITEMS.register(GCRModules.AK_POLYMER_GRIP.getSimpleID(), () -> new ModuleItem<>(GCRModules.AK_POLYMER_GRIP));
    public static final RegistryObject<Item> RK9_GRIP_ITEM =
            ITEMS.register(GCRModules.RK9_GRIP.getSimpleID(), () -> new ModuleItem<>(GCRModules.RK9_GRIP));
    public static final RegistryObject<Item> SAKO_ARG_GRIP_ITEM =
            ITEMS.register(GCRModules.SAKO_ARG_GRIP.getSimpleID(), () -> new ModuleItem<>(GCRModules.SAKO_ARG_GRIP));
    public static final RegistryObject<Item> MCX_M400_GRIP_ITEM =
            ITEMS.register(GCRModules.MCX_M400_GRIP.getSimpleID(), () -> new ModuleItem<>(GCRModules.MCX_M400_GRIP));
    public static final RegistryObject<Item> AK_BAKELITE_GRIP_ITEM =
            ITEMS.register(GCRModules.AK_BAKELITE_GRIP.getSimpleID(), () -> new ModuleItem<>(GCRModules.AK_BAKELITE_GRIP));


    public static final RegistryObject<Item> M4_PROFILE_FSB_BARREL_ITEM =
            ITEMS.register(GCRModules.M4_PROFILE_FSB_BARREL.getSimpleID(), () -> new ModuleItem<>(GCRModules.M4_PROFILE_FSB_BARREL));
    public static final RegistryObject<Item> STANAG_MAG_30R_ITEM =
            ITEMS.register(GCRModules.STANAG_MAG_30R.getSimpleID(), () -> new MagazineItem((com.sheridan.gcr.modularSys.modules.Mag) GCRModules.STANAG_MAG_30R));
    public static final RegistryObject<Item> PMAG_40R_ITEM =
            ITEMS.register(GCRModules.PMAG_40R.getSimpleID(), () -> new MagazineItem((com.sheridan.gcr.modularSys.modules.Mag) GCRModules.PMAG_40R));
    public static final RegistryObject<Item> SUREFIRE_MAG_60R_ITEM =
            ITEMS.register(GCRModules.SUREFIRE_MAG_60R.getSimpleID(), () -> new MagazineItem((com.sheridan.gcr.modularSys.modules.Mag) GCRModules.SUREFIRE_MAG_60R));
    public static final RegistryObject<Item> USGI_MAG_20R_ITEM =
            ITEMS.register(GCRModules.USGI_MAG_20R.getSimpleID(), () -> new MagazineItem((com.sheridan.gcr.modularSys.modules.Mag) GCRModules.USGI_MAG_20R));
    public static final RegistryObject<Item> PMAG_30R_ITEM =
            ITEMS.register(GCRModules.PMAG_30R.getSimpleID(), () -> new MagazineItem((com.sheridan.gcr.modularSys.modules.Mag) GCRModules.PMAG_30R));


    public static final RegistryObject<Item> MAG_6L18_ITEM =
            ITEMS.register(GCRModules.MAG_6L18.getSimpleID(), () -> new MagazineItem((com.sheridan.gcr.modularSys.modules.Mag) GCRModules.MAG_6L18));
    public static final RegistryObject<Item> MAG_6L23_ITEM =
            ITEMS.register(GCRModules.MAG_6L23.getSimpleID(), () -> new MagazineItem((com.sheridan.gcr.modularSys.modules.Mag) GCRModules.MAG_6L23));
    public static final RegistryObject<Item> MAG_6L31_ITEM =
            ITEMS.register(GCRModules.MAG_6L31.getSimpleID(), () -> new MagazineItem((com.sheridan.gcr.modularSys.modules.Mag) GCRModules.MAG_6L31));
    public static final RegistryObject<Item> SPEAR_MAG_20R_ITEM =
            ITEMS.register(GCRModules.SPEAR_MAG_20R.getSimpleID(), () -> new MagazineItem((com.sheridan.gcr.modularSys.modules.Mag) GCRModules.SPEAR_MAG_20R));
    public static final RegistryObject<Item> AKM_STEEL_MAG_ITEM =
            ITEMS.register(GCRModules.AKM_STEEL_MAG.getSimpleID(), () -> new MagazineItem((com.sheridan.gcr.modularSys.modules.Mag) GCRModules.AKM_STEEL_MAG));


    public static final RegistryObject<Item> M4_CARBINE_STOCK_ITEM =
            ITEMS.register(GCRModules.M4_CARBINE_STOCK.getSimpleID(), () -> new ModuleItem<>(GCRModules.M4_CARBINE_STOCK));
    public static final RegistryObject<Item> MOE_CARBINE_STOCK_ITEM =
            ITEMS.register(GCRModules.MOE_CARBINE_STOCK.getSimpleID(), () -> new ModuleItem<>(GCRModules.MOE_CARBINE_STOCK));
    public static final RegistryObject<Item> STOCK_6P34_ITEM =
            ITEMS.register(GCRModules.STOCK_6P34.getSimpleID(), () -> new ModuleItem<>(GCRModules.STOCK_6P34));
    public static final RegistryObject<Item> PT1_STOCK_ITEM =
            ITEMS.register(GCRModules.PT1_STOCK.getSimpleID(), () -> new ModuleItem<>(GCRModules.PT1_STOCK));
    public static final RegistryObject<Item> AR_STOCK_ADAPTER_TYPE1_ITEM =
            ITEMS.register(GCRModules.AR_STOCK_ADAPTER_TYPE1.getSimpleID(), () -> new ModuleItem<>(GCRModules.AR_STOCK_ADAPTER_TYPE1));
    public static final RegistryObject<Item> MCX_STOCK_TUBE_ITEM =
            ITEMS.register(GCRModules.MCX_STOCK_TUBE.getSimpleID(), () -> new ModuleItem<>(GCRModules.MCX_STOCK_TUBE));
    public static final RegistryObject<Item> CTR_STOCK_ITEM =
            ITEMS.register(GCRModules.CTR_STOCK.getSimpleID(), () -> new ModuleItem<>(GCRModules.CTR_STOCK));
    public static final RegistryObject<Item> MAGPUL_SL_M_STOCK_ITEM =
            ITEMS.register(GCRModules.MAGPUL_SL_M_STOCK.getSimpleID(), () -> new ModuleItem<>(GCRModules.MAGPUL_SL_M_STOCK));
    public static final RegistryObject<Item> AKM_STOCK_ITEM =
            ITEMS.register(GCRModules.AKM_STOCK.getSimpleID(), () -> new ModuleItem<>(GCRModules.AKM_STOCK));


    public static final RegistryObject<Item> A2_FLASH_HINDER_ITEM =
            ITEMS.register(GCRModules.A2_FLASH_HINDER.getSimpleID(), () -> new ModuleItem<>(GCRModules.A2_FLASH_HINDER));
    public static final RegistryObject<Item> ASE_FLASH_HIDER_ITEM =
            ITEMS.register(GCRModules.ASE_FLASH_HIDER.getSimpleID(), () -> new ModuleItem<>(GCRModules.ASE_FLASH_HIDER));
    public static final RegistryObject<Item> CLUTCH_LOK_QD_FLASH_HIDER_ITEM =
            ITEMS.register(GCRModules.CLUTCH_LOK_QD_FLASH_HIDER.getSimpleID(), () -> new ModuleItem<>(GCRModules.CLUTCH_LOK_QD_FLASH_HIDER));
    public static final RegistryObject<Item> SOCOM_RC2_ITEM =
            ITEMS.register(GCRModules.SOCOM_RC2.getSimpleID(), () -> new ModuleItem<>(GCRModules.SOCOM_RC2));
    public static final RegistryObject<Item> SLX_SUPPRESSOR_ITEM =
            ITEMS.register(GCRModules.SLX_SUPPRESSOR.getSimpleID(), () -> new ModuleItem<>(GCRModules.SLX_SUPPRESSOR));
    public static final RegistryObject<Item> AR15_MUZZLE_BRAKE_ITEM =
            ITEMS.register(GCRModules.AR15_MUZZLE_BRAKE.getSimpleID(), () -> new ModuleItem<>(GCRModules.AR15_MUZZLE_BRAKE));
    public static final RegistryObject<Item> AK74_MUZZLE_BRAKE_ITEM =
            ITEMS.register(GCRModules.AK74_MUZZLE_BRAKE.getSimpleID(), () -> new ModuleItem<>(GCRModules.AK74_MUZZLE_BRAKE));
    public static final RegistryObject<Item> PBS_4_ITEM =
            ITEMS.register(GCRModules.PBS_4.getSimpleID(), () -> new ModuleItem<>(GCRModules.PBS_4));
    public static final RegistryObject<Item> DTK1_COMPENSATOR_ITEM =
            ITEMS.register(GCRModules.DTK1_COMPENSATOR.getSimpleID(), () -> new ModuleItem<>(GCRModules.DTK1_COMPENSATOR));
    public static final RegistryObject<Item> DTKP_545_ITEM =
            ITEMS.register(GCRModules.DTKP_545.getSimpleID(), () -> new ModuleItem<>(GCRModules.DTKP_545));
    public static final RegistryObject<Item> AKM_MUZZLE_BRAKE_ITEM =
            ITEMS.register(GCRModules.AKM_MUZZLE_BRAKE.getSimpleID(), () -> new ModuleItem<>(GCRModules.AKM_MUZZLE_BRAKE));


    public static final RegistryObject<Item> KAC_FORWARD_GRIP_ITEM =
            ITEMS.register(GCRModules.KAC_FORWARD_GRIP.getSimpleID(), () -> new ModuleItem<>(GCRModules.KAC_FORWARD_GRIP));
    public static final RegistryObject<Item> RK_6_GRIP_ITEM =
            ITEMS.register(GCRModules.RK_2_GRIP.getSimpleID(), () -> new ModuleItem<>(GCRModules.RK_2_GRIP));
    public static final RegistryObject<Item> M203_ITEM =
            ITEMS.register(GCRModules.M203.getSimpleID(), () -> new ModuleItem<>(GCRModules.M203));
    public static final RegistryObject<Item> URGI_BARREL_ITEM =
            ITEMS.register(GCRModules.URGI_BARREL.getSimpleID(), () -> new ModuleItem<>(GCRModules.URGI_BARREL));
    public static final RegistryObject<Item> AK24A_BARREL_ITEM =
            ITEMS.register(GCRModules.AK24A_BARREL.getSimpleID(), () -> new ModuleItem<>(GCRModules.AK24A_BARREL));
    public static final RegistryObject<Item> GP25_ITEM =
            ITEMS.register(GCRModules.GP25.getSimpleID(), () -> new ModuleItem<>(GCRModules.GP25));
    public static final RegistryObject<Item> AK24B_BARREL_ITEM =
            ITEMS.register(GCRModules.AK24B_BARREL.getSimpleID(), () -> new ModuleItem<>(GCRModules.AK24B_BARREL));
    public static final RegistryObject<Item> SIG_FURY_BARREL_ITEM =
            ITEMS.register(GCRModules.SIG_FURY_BARREL.getSimpleID(), () -> new ModuleItem<>(GCRModules.SIG_FURY_BARREL));


    public static final RegistryObject<Item> URGI_HANDGUARD_ITEM =
            ITEMS.register(GCRModules.URGI_HANDGUARD.getSimpleID(), () -> new ModuleItem<>(GCRModules.URGI_HANDGUARD));
    public static final RegistryObject<Item> AK24A_HANDGUARD_ITEM =
            ITEMS.register(GCRModules.AK24A_HANDGUARD.getSimpleID(), () -> new ModuleItem<>(GCRModules.AK24A_HANDGUARD));
    public static final RegistryObject<Item> CAR_15_HANDGUARD_ITEM =
            ITEMS.register(GCRModules.CAR_15_HANDGUARD.getSimpleID(), () -> new ModuleItem<>(GCRModules.CAR_15_HANDGUARD));
    public static final RegistryObject<Item> KAC_RAS_HANDGUARD_ITEM =
            ITEMS.register(GCRModules.KAC_RAS_HANDGUARD.getSimpleID(), () -> new ModuleItem<>(GCRModules.KAC_RAS_HANDGUARD));
    public static final RegistryObject<Item> DANIEL_DEFENSE_RIS_II_HANDGUARD_ITEM =
            ITEMS.register(GCRModules.DANIEL_DEFENSE_RIS_II_HANDGUARD.getSimpleID(), () -> new ModuleItem<>(GCRModules.DANIEL_DEFENSE_RIS_II_HANDGUARD));
    public static final RegistryObject<Item> AK_POLYMER_HANDGUARD_LOWER_ITEM =
            ITEMS.register(GCRModules.AK_POLYMER_HANDGUARD_LOWER.getSimpleID(), () -> new ModuleItem<>(GCRModules.AK_POLYMER_HANDGUARD_LOWER));
    public static final RegistryObject<Item> AK_POLYMER_HANDGUARD_UPPER_ITEM =
            ITEMS.register(GCRModules.AK_POLYMER_HANDGUARD_UPPER.getSimpleID(), () -> new ModuleItem<>(GCRModules.AK_POLYMER_HANDGUARD_UPPER));
    public static final RegistryObject<Item> B10_HANDGUARD_ITEM =
            ITEMS.register(GCRModules.B10_HANDGUARD.getSimpleID(), () -> new ModuleItem<>(GCRModules.B10_HANDGUARD));
    public static final RegistryObject<Item> B19_HANDGUARD_ITEM =
            ITEMS.register(GCRModules.B19_HANDGUARD.getSimpleID(), () -> new ModuleItem<>(GCRModules.B19_HANDGUARD));
    public static final RegistryObject<Item> B10_MOUNT_RAIL_ITEM =
            ITEMS.register(GCRModules.B10_MOUNT_RAIL.getSimpleID(), () -> new ModuleItem<>(GCRModules.B10_MOUNT_RAIL));
    public static final RegistryObject<Item> B13_BRACKET_ITEM =
            ITEMS.register(GCRModules.B13_BRACKET.getSimpleID(), () -> new ModuleItem<>(GCRModules.B13_BRACKET));
    public static final RegistryObject<Item> SPEAR_HANDGUARD_ITEM =
            ITEMS.register(GCRModules.SPEAR_HANDGUARD.getSimpleID(), () -> new ModuleItem<>(GCRModules.SPEAR_HANDGUARD));
    public static final RegistryObject<Item> SPEAR_LT_HANDGUARD_ITEM =
            ITEMS.register(GCRModules.SPEAR_LT_HANDGUARD.getSimpleID(), () -> new ModuleItem<>(GCRModules.SPEAR_LT_HANDGUARD));
    public static final RegistryObject<Item> AK_WOODEN_HANDGUARD_LOWER_ITEM =
            ITEMS.register(GCRModules.AK_WOODEN_HANDGUARD_LOWER.getSimpleID(), () -> new ModuleItem<>(GCRModules.AK_WOODEN_HANDGUARD_LOWER));
    public static final RegistryObject<Item> AK_WOODEN_HANDGUARD_UPPER_ITEM =
            ITEMS.register(GCRModules.AK_WOODEN_HANDGUARD_UPPER.getSimpleID(), () -> new ModuleItem<>(GCRModules.AK_WOODEN_HANDGUARD_UPPER));


    public static final RegistryObject<Item> VORTEX_RAZOR_RED_DOT_ITEM =
            ITEMS.register(GCRModules.VORTEX_RAZOR_RED_DOT.getSimpleID(), () -> new ModuleItem<>(GCRModules.VORTEX_RAZOR_RED_DOT));
    public static final RegistryObject<Item> EOTECH_EXPS3_ITEM =
            ITEMS.register(GCRModules.EOTECH_EXPS3.getSimpleID(), () -> new ModuleItem<>(GCRModules.EOTECH_EXPS3));
    public static final RegistryObject<Item> PEQ_15_ITEM =
            ITEMS.register(GCRModules.PEQ_15.getSimpleID(), () -> new ModuleItem<>(GCRModules.PEQ_15));

    public static final RegistryObject<Item> DUSTCOVER_6P34_ITEM =
            ITEMS.register(GCRModules.DUSTCOVER_6P34.getSimpleID(), () -> new ModuleItem<>(GCRModules.DUSTCOVER_6P34));
    public static final RegistryObject<Item> PDC_DUSTCOVER_ITEM =
            ITEMS.register(GCRModules.PDC_DUSTCOVER.getSimpleID(), () -> new ModuleItem<>(GCRModules.PDC_DUSTCOVER));
    public static final RegistryObject<Item> AK_RIBBED_DUSTCOVER_ITEM =
            ITEMS.register(GCRModules.AK_RIBBED_DUSTCOVER.getSimpleID(), () -> new ModuleItem<>(GCRModules.AK_RIBBED_DUSTCOVER));

    public static final RegistryObject<CreativeModeTab> GUN_TAB =
            CREATIVE_MODE_TABS.register("gun",
                    () -> CreativeModeTab
                            .builder()
                            .title(Component.translatable("itemGroup.gcr.gun"))
                            .withTabsBefore(CreativeModeTabs.COMBAT)
                            .icon(() -> M4A1_ITEM.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                output.accept(M4A1_ITEM.get());
                                output.accept(AK74M_ITEM.get());
                                output.accept(AKMN_ITEM.get());
                                output.accept(AK24_ITEM.get());
                                output.accept(MCX_SPEAR_ITEM.get());
                            }).build());

    public static final RegistryObject<CreativeModeTab> ATTACHMENT_TAB =
            CREATIVE_MODE_TABS.register("attachment",
                    () -> CreativeModeTab
                            .builder()
                            .title(Component.translatable("itemGroup.gcr.attachment"))
                            .withTabsBefore(GUN_TAB.getId())
                            .icon(() -> ACOG_ITEM.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                output.accept(ACOG_ITEM.get());
                                output.accept(ELCAN_ITEM.get());
                                output.accept(VORTEX_RAZOR_HD_ITEM.get());
                                output.accept(CANTED_RAIL_ITEM.get());
                                output.accept(A2_CARRY_HANDLE_ITEM.get());
                                output.accept(KAC_FOLDING_SIGHT_REAR_ITEM.get());
                                output.accept(KAC_FOLDING_SIGHT_FAR_ITEM.get());
                                output.accept(MAGPUL_MBUS_PRO_SIGHT_REAR_ITEM.get());
                                output.accept(MAGPUL_MBUS_PRO_SIGHT_FAR_ITEM.get());

                                output.accept(A2_PISTOL_GRIP_ITEM.get());
                                output.accept(MOE_GRIP_ITEM.get());
                                output.accept(SAKO_ARG_GRIP_ITEM.get());
                                output.accept(MCX_M400_GRIP_ITEM.get());
                                output.accept(AK_POLYMER_GRIP_ITEM.get());
                                output.accept(AK_BAKELITE_GRIP_ITEM.get());
                                output.accept(RK9_GRIP_ITEM.get());

                                output.accept(M4_PROFILE_FSB_BARREL_ITEM.get());
                                output.accept(URGI_BARREL_ITEM.get());
                                output.accept(AK24A_BARREL_ITEM.get());
                                output.accept(AK24B_BARREL_ITEM.get());
                                output.accept(SIG_FURY_BARREL_ITEM.get());

                                output.accept(M4_CARBINE_STOCK_ITEM.get());
                                output.accept(MOE_CARBINE_STOCK_ITEM.get());
                                output.accept(CTR_STOCK_ITEM.get());
                                output.accept(MAGPUL_SL_M_STOCK_ITEM.get());

                                output.accept(STOCK_6P34_ITEM.get());
                                output.accept(AKM_STOCK_ITEM.get());
                                output.accept(PT1_STOCK_ITEM.get());
                                output.accept(AR_STOCK_ADAPTER_TYPE1_ITEM.get());
                                output.accept(MCX_STOCK_TUBE_ITEM.get());

                                output.accept(A2_FLASH_HINDER_ITEM.get());
                                output.accept(ASE_FLASH_HIDER_ITEM.get());
                                output.accept(CLUTCH_LOK_QD_FLASH_HIDER_ITEM.get());

                                output.accept(SOCOM_RC2_ITEM.get());
                                output.accept(AR15_MUZZLE_BRAKE_ITEM.get());
                                output.accept(AK74_MUZZLE_BRAKE_ITEM.get());
                                output.accept(AKM_MUZZLE_BRAKE_ITEM.get());
                                output.accept(PBS_4_ITEM.get());
                                output.accept(DTKP_545_ITEM.get());
                                output.accept(DTK1_COMPENSATOR_ITEM.get());
                                output.accept(SLX_SUPPRESSOR_ITEM.get());

                                output.accept(CAR_15_HANDGUARD_ITEM.get());
                                output.accept(KAC_RAS_HANDGUARD_ITEM.get());
                                output.accept(DANIEL_DEFENSE_RIS_II_HANDGUARD_ITEM.get());
                                output.accept(URGI_HANDGUARD_ITEM.get());
                                output.accept(AK24A_HANDGUARD_ITEM.get());
                                output.accept(AK_POLYMER_HANDGUARD_LOWER_ITEM.get());
                                output.accept(AK_POLYMER_HANDGUARD_UPPER_ITEM.get());
                                output.accept(AK_WOODEN_HANDGUARD_LOWER_ITEM.get());
                                output.accept(AK_WOODEN_HANDGUARD_UPPER_ITEM.get());
                                output.accept(B10_HANDGUARD_ITEM.get());
                                output.accept(B19_HANDGUARD_ITEM.get());
                                output.accept(B10_MOUNT_RAIL_ITEM.get());
                                output.accept(B13_BRACKET_ITEM.get());
                                output.accept(SPEAR_HANDGUARD_ITEM.get());
                                output.accept(SPEAR_LT_HANDGUARD_ITEM.get());


                                output.accept(KAC_FORWARD_GRIP_ITEM.get());
                                output.accept(RK_6_GRIP_ITEM.get());
                                output.accept(M203_ITEM.get());
                                output.accept(GP25_ITEM.get());
                                output.accept(VORTEX_RAZOR_RED_DOT_ITEM.get());
                                output.accept(EOTECH_EXPS3_ITEM.get());
                                output.accept(PEQ_15_ITEM.get());

                                output.accept(DUSTCOVER_6P34_ITEM.get());
                                output.accept(PDC_DUSTCOVER_ITEM.get());
                                output.accept(AK_RIBBED_DUSTCOVER_ITEM.get());
                            }).build());

    public static final RegistryObject<CreativeModeTab> MAGAZINE_TAB =
            CREATIVE_MODE_TABS.register("magazines",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.gcr.magazines"))
                            .withTabsBefore(ATTACHMENT_TAB.getId())
                            .icon(() -> STANAG_MAG_30R_ITEM.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                output.accept(AMMO_556X45_ITEM.get());
                                output.accept(AMMO_545X39_ITEM.get());
                                output.accept(AMMO_68X51_ITEM.get());
                                output.accept(AMMO_762X39_ITEM.get());
                                addMagazineVariants(output, STANAG_MAG_30R_ITEM);
                                addMagazineVariants(output, PMAG_40R_ITEM);
                                addMagazineVariants(output, PMAG_30R_ITEM);
                                addMagazineVariants(output, SUREFIRE_MAG_60R_ITEM);
                                addMagazineVariants(output, USGI_MAG_20R_ITEM);
                                addMagazineVariants(output, MAG_6L18_ITEM);
                                addMagazineVariants(output, MAG_6L23_ITEM);
                                addMagazineVariants(output, MAG_6L31_ITEM);
                                addMagazineVariants(output, AKM_STEEL_MAG_ITEM);
                                addMagazineVariants(output, SPEAR_MAG_20R_ITEM);
                            }).build());

    /**
     * Creative inventory presets: two independent ItemStacks of the SAME
     * registered magazine item. They differ only by the persisted ammo NBT.
     * Do not register extra magazine IDs or duplicate module bindings.
     */
    private static void addMagazineVariants(CreativeModeTab.Output output, RegistryObject<Item> entry) {
        MagazineItem magazine = (MagazineItem) entry.get();

        ItemStack empty = new ItemStack(magazine);
        magazine.setRounds(empty, 0);
        output.accept(empty);

        ItemStack full = new ItemStack(magazine);
        magazine.setRounds(full, magazine.capacity());
        output.accept(full);
    }

    public static ResourceLocation RL(String modId, String path) {
        return new ResourceLocation(modId, path);
    }

    public static ResourceLocation RL(String path) {
        return new ResourceLocation(MODID, path);
    }

    public GCR() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        IS_DEVELOPMENT = !FMLEnvironment.production;
        if (IS_DEVELOPMENT) {
            LOGGER.info("Guns craft is running in dev mode");
        }
        GCRNetwork.register();
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

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(CommonEvents.class);
        MinecraftForge.EVENT_BUS.register(com.sheridan.gcr.common.TestEvents.class);
        MinecraftForge.EVENT_BUS.register(PlayerStatusEvents.class);
        MinecraftForge.EVENT_BUS.register(GunHeatHandler.class);
        // TacZ Additions-style vanilla light-block fallback for gunshots.
        MinecraftForge.EVENT_BUS.register(com.sheridan.gcr.common.MuzzleFlashLightBlockManager.class);
        MinecraftForge.EVENT_BUS.register(com.sheridan.gcr.entity.projectile.GcrBulletWaterEffects.class);
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);


        if (FMLLoader.getDist() == Dist.CLIENT) {
            modEventBus.addListener(ClientEvents::registerCustomVanillaShader);
        }
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PlayerHitZoneConfig.SPEC,
                "gcr-player-hit-zones.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT,
                com.sheridan.gcr.config.AftermatchMotionConfig.SPEC, "gcr-motion-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,
                com.sheridan.gcr.config.AftermatchWallConfig.SPEC, "gcr-wall-common.toml");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        System.out.println("server started!");
    }

    /**
     * 服务端在专用服务器上真正可用的最早时机：此时 Minecraft 的注册已完成，
     * 数据包（含 GCR 的 pivot/voxel 资源）已经通过 {@link #onAddReloadListeners} 注册的
     * reload listener 装载完毕。在这里预热枪械初始数据缓存，任何生成失败都会当场崩溃，
     * 而不是等到玩家实际用枪时才发现数据错误。
     */
    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        warmUpGunInitialData();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        Commons.onServerStarted(event);
    }


    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(PivotMapLoader.getServer());
        event.addListener(VoxelLoader.getServer());
        //资源重载会重新填充 pivot/voxel，缓存的初始数据可能基于旧资源，必须重建
        reWarmUpGunInitialData();
    }

    /**
     * 为所有已注册的枪械物品预热 InitialDataTag 缓存。
     *
     * <p>生成失败不吞异常：{@link Gun#warmUpInitialDataTag} 会带上枪械 id 抛出，
     * 在这里直接冲出启动流程让游戏崩溃——枪械初始数据写坏的代价是存档数据被污染甚至锁死，
     * 必须在启动阶段就暴露出来。</p>
     *
     * <p>客户端与服务端都会调用：两侧各自持有独立的 {@link Gun} 实例与资源，
     * 且两侧的 public 渲染/预览路径确实会自行构造 ItemStack 数据。</p>
     */
    public static void warmUpGunInitialData() {
        String side = FMLEnvironment.dist.isClient() ? "client" : "dedicated_server";
        long startedAt = System.nanoTime();
        int warmed = 0;
        for (Map.Entry<String, IModular> entry : ModuleRegister.all().entrySet()) {
            if (entry.getValue() instanceof Gun gun) {
                gun.warmUpInitialDataTag(entry.getKey());
                warmed++;
            }
        }
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;
        if (warmed == 0) {
            //注册表里一把枪都没有，多半是模块注册还没跑完就被调用了
            LOGGER.warn("Precomputed initial data tags on {}, but no gun module was found "
                    + "(ModuleRegister has {} module(s))", side, ModuleRegister.all().size());
            return;
        }
        LOGGER.info("Precomputed initial data tags for {} gun module(s) on {} in {} ms",
                warmed, side, elapsedMs);
    }

    /** 资源重载后重新预热：先整体清空再重建，失败同样当场抛出。 */
    private static void reWarmUpGunInitialData() {
        for (IModular modular : ModuleRegister.all().values()) {
            if (modular instanceof Gun gun) {
                gun.clearInitialDataTagCache();
            }
        }
        warmUpGunInitialData();
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
        Collection<RegistryObject<Item>> entries = ITEMS.getEntries();
        for (RegistryObject<Item> entry : entries) {
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

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

            MinecraftForge.EVENT_BUS.register(GuiEvents.class);
            MinecraftForge.EVENT_BUS.register(ClientEvents.class);
            MinecraftForge.EVENT_BUS.register(RenderEvents.class);
            MinecraftForge.EVENT_BUS.register(ControllerEvents.class);
            MinecraftForge.EVENT_BUS.register(FPMuzzleFlashEnvLightingRenderer.class);
            MuzzleFlashDynamicLight.initialize();
            MinecraftForge.EVENT_BUS.register(FabulousDepthTextureHandler.class);
            MinecraftForge.EVENT_BUS.register(FlashLightRenderer.class);
            MinecraftForge.EVENT_BUS.register(DelayedRenderTaskHandler.class);
            MinecraftForge.EVENT_BUS.register(GunEffectManager.class);
            MinecraftForge.EVENT_BUS.register(LaserEffectRenderer.class);
            MinecraftForge.EVENT_BUS.register(com.sheridan.gcr.client.render.fx.BulletImpactDecals.class);
            Client.onClientSetup(event);

            Collection<RegistryObject<Item>> entries = ITEMS.getEntries();
            for (RegistryObject<Item> entry : entries) {
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

            //客户端侧的建模资源已经就绪，预热枪械初始数据缓存；生成失败直接崩溃
            warmUpGunInitialData();

            RenderSystem.recordRenderCall(MuzzleFlashEnvShader::init);
            RenderSystem.recordRenderCall(FabulousMergeDepthShader::init);
            RenderSystem.recordRenderCall(ScopeViewShadingShader::init);
            RenderSystem.recordRenderCall(DepthCopyShader::init);
            RenderSystem.recordRenderCall(LaserGlowShader::init);
            RenderSystem.recordRenderCall(IrisGunPostShader::init);
            MinecraftForge.EVENT_BUS.register(IrisGunPostRenderer.class);
        }

        @SubscribeEvent
        public static void registerParticles(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ModParticles.FLASH.get(), FlashParticle.Provider::new);
            event.registerSpriteSet(ModParticles.FRAGMENT.get(), FragmentParticle.Provider::new);
            event.registerSpriteSet(ModParticles.SPARK.get(), SparkParticle.Provider::new);
            event.registerSpriteSet(ModParticles.HEAT_SMOKE.get(), EmberParticle.Provider::new);
            event.registerSpriteSet(ModParticles.WATER_PUFF.get(),
                    com.sheridan.gcr.client.render.fx.particles.GcrWaterParticles.PuffProvider::new);
            event.registerSpriteSet(ModParticles.WATER_DROP.get(),
                    com.sheridan.gcr.client.render.fx.particles.GcrWaterParticles.DropProvider::new);
            event.registerSpriteSet(ModParticles.WATER_BUBBLE.get(),
                    com.sheridan.gcr.client.render.fx.particles.GcrWaterParticles.BubbleProvider::new);
        }
        @SubscribeEvent
        public static void registerKeyMapping(RegisterKeyMappingsEvent event) {
            KeyBinds.register(event);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.BULLET.get(), BulletRenderer::new);
            event.registerEntityRenderer(ModEntities.GRENADE.get(), GrenadeRenderer::new);
        }
    }
}
