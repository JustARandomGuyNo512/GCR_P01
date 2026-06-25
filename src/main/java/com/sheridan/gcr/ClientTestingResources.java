package com.sheridan.gcr;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sheridan.gcr.client.model.bulletShell.BulletShellModel;
import com.sheridan.gcr.client.model.gltf.io.GltfModelLoader;
import com.sheridan.gcr.client.model.modular.*;
import com.sheridan.gcr.client.model.modular.animation.controllers.ARMainController;
import com.sheridan.gcr.client.model.modular.animation.controllers.M203Controller;
import com.sheridan.gcr.client.model.modular.animation.eventSys.IAnimationController;
import com.sheridan.gcr.client.model.modular.modules.*;
import com.sheridan.gcr.client.model.modular.state.IStateViewer;
import com.sheridan.gcr.client.model.modular.state.IStateViewerModel;
import com.sheridan.gcr.client.model.modular.state.stateViewers.ARMagViewer;
import com.sheridan.gcr.client.model.modular.state.stateViewers.ARMainViewer;
import com.sheridan.gcr.client.model.modular.state.stateViewers.FlashLightStatesViewer;
import com.sheridan.gcr.client.model.modular.state.stateViewers.TestM203Viewer;
import com.sheridan.gcr.client.model.playerArm.BufferedPlayerArmModel;
import com.sheridan.gcr.client.render.RenderTypes;
import com.sheridan.gcr.client.render.fx.bulletShell.BulletShellDisplay;
import com.sheridan.gcr.client.render.fx.muzzleFlash.CommonMuzzleFlashes;
import com.sheridan.gcr.modularSys.modules.guns.ar.AR;
import com.sheridan.gcr.modularSys.modules.views.IAmmoSourceView;
import com.sheridan.gcr.modularSys.modules.views.IFlashLightView;
import com.sheridan.gcr.modularSys.modules.views.IM203View;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ClientTestingResources {


    public static void init(FMLClientSetupEvent event) {
        // ==================== 1. 动画整合与批量注册 ====================
        // 主武器动画
        ModelRegistrationManager.loadAndRegisterAnimations(
                "model_assets/animation/m4a1_main.animation.json",
                Map.ofEntries(
                        Map.entry("check_mag", "check_mag"), Map.entry("reload_grenade", "reload_grenade"),
                        Map.entry("reload_grenade.G", "reload_grenade.g"), Map.entry("to_semi", "to_semi"),
                        Map.entry("check_grenade.G", "check_grenade.g"),Map.entry("check_grenade", "check_grenade"),
                        Map.entry("to_auto", "to_auto"), Map.entry("shoot_last", "m4a1_shoot_last"),
                        Map.entry("mag_reload", "m4a1_mag_reload"), Map.entry("mag_reload_empty", "m4a1_mag_reload_empty"),
                        Map.entry("mag_reload_charge", "m4a1_mag_reload_charge"), Map.entry("chamber_reload", "m4a1_chamber_reload"),
                        Map.entry("chamber_reload_empty", "m4a1_chamber_reload_empty"), Map.entry("base", "m4a1_base"),
                        Map.entry("shoot", "m4a1_shoot"), Map.entry("shoot_stuck", "m4a1_shoot_stuck"),
                        Map.entry("remove_stuck", "m4a1_remove_stuck"), Map.entry("remove_stuck_empty", "m4a1_remove_stuck_empty"),
                        Map.entry("check_chamber", "m4a1_check_chamber"), Map.entry("check_chamber_simple", "m4a1_check_chamber_simple")
                )
        );
        // 弹匣状态动画
        ModelRegistrationManager.loadAndRegisterAnimations(
                "model_assets/animation/ar_mag_30r.states.json",
                Map.of(
                        "full_1", "ar_mag_30r_full_1", "full_2", "ar_mag_30r_full_2",
                        "left_1", "ar_mag_30r_left_1", "left_2", "ar_mag_30r_left_2",
                        "left_3", "ar_mag_30r_left_3", "empty", "ar_mag_30r_empty"
                )
        );
        // 全局动画
        ModelRegistrationManager.loadAndRegisterAnimations(
                "model_assets/animation/ar.global.animation.json",
                Map.of("holster", "ar_holster", "draw", "ar_draw")
        );
        // M203 状态动画
        ModelRegistrationManager.loadAndRegisterAnimations(
                "model_assets/animation/m203.states.json",
                Map.of("full", "m203_full",
                        "empty", "m203_empty",
                        "base", "m203_base",
                        "fired", "m203_fired",
                        "prepare", "m203_prepare")
        );

        // ==================== 2. 模型注册与自定义 Lambda 逻辑 ====================

        // 非常规注册示例：带 Viewer、Display 以及 Controller 初始化的复杂对象
        ModelRegistrationManager.registerModel(
                GCR.M4A1, "model_assets/gltf/m4a1_main.gltf", "model_assets/gltf/m4a1_main.png", true,
                meshData -> {
                    ARMainViewer viewer = new ARMainViewer((AR) GCR.M4A1);
                    TestARMainModel model = new TestARMainModel(meshData, new BulletShellDisplay(
                            "BULLET_SHELL", GCR.RL("shell_5_56x45"), 15f, 25f, 20f, 5f, 40f, 0.2f,
                            10, 90, 360 * 10, 0.25f, 360, 0.8f, 500, 100
                    ), viewer);

                    IAnimationController<?> controller = new ARMainController();
                    model.bindController(controller);
                    model.callInitAnimation();
                    model.callInitTrack();
                    model.callInitEventSubscriptions();
                    return model;
                }
        );

        // 如果需要取消 M203 的延迟编译，将 immediateCompile 设为 false 即可
        ModelRegistrationManager.registerModel(
                GCR.M203, "model_assets/gltf/m203.gltf", "model_assets/gltf/m203.png", true,
                meshData -> {
                    TestM203Model testM203Model = new TestM203Model(meshData, new TestM203Viewer((IM203View) GCR.M203));
                    IAnimationController<?> controller = new M203Controller();
                    testM203Model.bindController(controller);
                    testM203Model.callInitAnimation();
                    testM203Model.callInitTrack();
                    testM203Model.callInitEventSubscriptions();
                    return testM203Model;
                }
        );

        ModelRegistrationManager.registerModel(
                GCR.STANAG_MAG_30R, "model_assets/gltf/stanag_mag_30r.gltf", "model_assets/gltf/stanag_mag_30r.png", true,
                meshData -> new TestARMagModel(meshData, GCR.RL(""), new ARMagViewer((IAmmoSourceView) GCR.STANAG_MAG_30R))
        );



        // 常规单行注册
        ModelRegistrationManager.registerModel(GCR.M4_PROFILE_FSB_BARREL, "model_assets/gltf/m4_profile_fsb_barrel.gltf", "model_assets/gltf/m4_profile_fsb_barrel.png", true, d -> new BarrelModel(d, 2f, CommonMuzzleFlashes.COMMON));
        ModelRegistrationManager.registerModel(GCR.A2_FLASH_HINDER, "model_assets/gltf/a2_flash_hider.gltf", "model_assets/gltf/a2_flash_hider.png", true, d -> new MuzzleModel(d, 2f, CommonMuzzleFlashes.COMMON));
        ModelRegistrationManager.registerModel(GCR.CAR_15_HANDGUARD, "model_assets/gltf/car_15_handguard.gltf", "model_assets/gltf/car_15_handguard.png", true, SplitARHandguardModel::new);
        ModelRegistrationManager.registerModel(GCR.M4_CARBINE_STOCK, "model_assets/gltf/ar_marine_stock.gltf", "model_assets/gltf/ar_marine_stock.png", true, d -> new ModularModel(d, GCR.RL("")));
        ModelRegistrationManager.registerModel(GCR.A2_PISTOL_GRIP, "model_assets/gltf/a2_pistol_grip.gltf", "model_assets/gltf/a2_pistol_grip.png", true, d -> new ModularModel(d, GCR.RL("")));
        ModelRegistrationManager.registerModel(GCR.A2_CARRY_HANDLE, "model_assets/gltf/a2_carry_handle.gltf", "model_assets/gltf/a2_carry_handle.png", true, d -> new SightModel(d, GCR.RL("")));
        ModelRegistrationManager.registerModel(GCR.CANTED_RAIL, "model_assets/gltf/canted_rail.gltf", "model_assets/gltf/canted_rail.png", true, d -> new ModularModel(d, GCR.RL("")));
        ModelRegistrationManager.registerModel(GCR.KAC_RAS_HANDGUARD, "model_assets/gltf/kac_ras.gltf", "model_assets/gltf/kac_ras.png", true, SplitARHandguardModel::new);
        ModelRegistrationManager.registerModel(GCR.KAC_FORWARD_GRIP, "model_assets/gltf/kac_forward_grip.gltf", "model_assets/gltf/kac_forward_grip.png", true, MLokFitGripModel::new);
        ModelRegistrationManager.registerModel(GCR.URGI_HANDGUARD, "model_assets/gltf/urgi_handguard.gltf", "model_assets/gltf/urgi_handguard.png", true, d -> new ArmHandlerModel<>(d, IStateViewer.EMPTY, GCR.RL("")));
        ModelRegistrationManager.registerModel(GCR.URGI_BARREL, "model_assets/gltf/urgi_barrel.gltf", "model_assets/gltf/urgi_barrel.png", true, d -> new BarrelModel(d, 2f, CommonMuzzleFlashes.COMMON));

        ModelRegistrationManager.registerModel(GCR.ACOG, "model_assets/gltf/acog.gltf", "model_assets/gltf/acog.png", true, d -> new ScopeModel(d, GCR.RL("bruh"), 0.129f, 2f, 0.7f, 1f, 2f, 1.9f, GCR.RL("textures/sight/crosshair/acog.png")));

        BulletShellModel shell_5_56x45 = new BulletShellModel(
                GltfModelLoader.loadModel(GCR.RL("gcr", "model_assets/gltf/shell_5_56x45.gltf")),
                GCR.RL("shell_5_56x45"));
        ModelRegistrationManager.addDeferredCompileTask(() ->
                shell_5_56x45.compile(RenderTypes.getMeshCutOut(GCR.RL("gcr", "model_assets/gltf/shell_5_56x45.png")))
        );


        ModelRegistrationManager.registerModel(GCR.VORTEX_RAZOR_RED_DOT, "model_assets/gltf/vortex_razor_red_dot.gltf", "model_assets/gltf/vortex_razor_red_dot.png", true, d -> new RedDotModel(d, GCR.RL("vortex_razor_red_dot"), GCR.RL("textures/sight/crosshair/red_dot.png"), 1.1f));

        ModelRegistrationManager.registerModel(
                GCR.PEQ_15, "model_assets/gltf/peq_15.gltf", "model_assets/gltf/peq_15.png", true,
                d -> new PEQ15Model(d, GCR.RL(""), new FlashLightStatesViewer((IFlashLightView) GCR.PEQ_15)));


        BufferedPlayerArmModel.init();
        event.enqueueWork(() -> RenderSystem.recordRenderCall(ClientTestingResources::compile));
    }

    public static void afterModelRegister() {
        ModuleModelRegister.visitAll(model -> {
            if (model instanceof IStateViewerModel<?> stateListenerModel) {
                IStateViewer<?> stateViewer = stateListenerModel.getViewer();
                if (stateViewer != null) {
                    stateViewer.onRegisterStateMapping();
                }
            }
        });
    }

    public static void compile() {
        // 直接触发由管理器收集并生成的延迟编译任务列表
        ModelRegistrationManager.compileAll();
    }

    public static void regModels() {

    }
}