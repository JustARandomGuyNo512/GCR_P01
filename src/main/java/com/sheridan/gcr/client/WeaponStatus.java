package com.sheridan.gcr.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.IScopeModel;
import com.sheridan.gcr.client.model.modular.ModuleModelRegister;
import com.sheridan.gcr.client.recoil.RecoilData;
import com.sheridan.gcr.client.recoil.RecoilHandler;
import com.sheridan.gcr.client.render.GunPoseHandler;
import com.sheridan.gcr.client.render.ModuleRenderNode;
import com.sheridan.gcr.client.screen.ldlib2Remake.GunModifyScreen;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.ModuleHandler;
import com.sheridan.gcr.modularSys.builder.Node;
import com.sheridan.gcr.modularSys.fire.IFireMode;
import com.sheridan.gcr.modularSys.modules.IArmHandlerModular;
import com.sheridan.gcr.modularSys.modules.IInteractiveModular;
import com.sheridan.gcr.modularSys.modules.ISight;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.modules.impl.Muzzle;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@OnlyIn(Dist.CLIENT)
public class WeaponStatus {
    public static final ResourceLocation SPEED_ID = GCR.RL("ads_speed_modifier");
    private long lastJump;
    private boolean isHoldingGun;
    private ItemStack itemStack;
    private IGun gun;
    private float aimingProgress;
    private float aimingProgressLast;
    public boolean isAiming;
    private Node root;
    private ModuleRenderNode renderRoot;

    private final Object2ObjectOpenHashMap<String, ModuleRenderNode> flatRenderNodeMap = new Object2ObjectOpenHashMap<>();
    private String identityID;
    private int modifyID;
    private IFireMode<?> fireMode;
    /** 旧口径的冷却步数（整数个 5ms），只保留给外部调用方。 */
    private int fireDelay;
    /**
     * 当前枪械每一发的精确间隔（纳秒），由 RPM 直接换算。
     *
     * <p>开火线程用它排下一发的绝对时刻，{@code volatile} 是因为后座力 / 动画推进也在这个线程上，
     * 会无条件读到它。</p>
     */
    private volatile long fireIntervalNanos;
    /**
     * 开火线程的无锁预检：主线程每 tick 刷新，为 {@code true} 时表示「现在去抢锁试一发毫无意义」。
     *
     * <p>只是个便宜的近似（允许短暂过期），真正的判定仍然在 {@link Client#handleClientShoot} 里持锁进行；
     * 初值 {@code false} 是故意的——预检失效时宁可多抢一次锁，也不能让枪彻底打不响。</p>
     */
    private volatile boolean fireBlocked;
    public volatile int fireCount;
    public volatile long lastShoot;
    public float aimingSpeed;

    private Node activeSight = null;
    private final Object2ObjectOpenHashMap<String, Node> IDToNodes = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<String, IModularModel> IDToModels = new Object2ObjectOpenHashMap<>();
    private final List<Pair<IInteractiveModular, Node>> interactiveModules = new ArrayList<>();

    private Vector3f muzzleFlashPos = null;
    private float muzzleFlashRadius = 5f;
    private float muzzleFlashIntensity =  2.5f;
    private float adsZCompensation = 1;
    private float lastAdsZCompensation = 1;


    private float recoilControl;
    private float impulse;
    private float agility;
    private float stability;
    private float length;
    private float weight;
    private float heat;
    private float lastHeat;

    public boolean isSuppressed = false;

    private float cantedAdsIncSpeedFactor = 1;
    private float cantedAdsDecSpeedFactor = 1;

    private IArmHandlerModular leftArmHold;
    private IArmHandlerModular rightArmHold;

    public float shootRandomSeed;

    public WeaponStatus() {

    }

    public ModuleRenderNode getRenderRoot() {
        return renderRoot;
    }

    public Object2ObjectOpenHashMap<String, ModuleRenderNode> getFlatRenderNodeMap() {
        return flatRenderNodeMap;
    }

    public Object2ObjectOpenHashMap<String, IModularModel> getIDToModels() {
        return IDToModels;
    }

    public float getAimingProgress() {
        return aimingProgress;
    }

    /**
     * lerped use in render
     * */
    public float getAimingProgress(float partialTicks) {
        return Mth.lerp(partialTicks, aimingProgressLast, aimingProgress);
    }

    public boolean isAiming() {
        return isAiming;
    }


    public void onTickEnd(@NotNull Player localPlayer) {
//        if (!Client.RIGHT_BUTTON_PRESSED.get()) {
//            //aimingProgressLast = aimingProgress;
//            float exitSpeed = Math.max(0.3f, aimingSpeed);
//            aimingProgress = Math.max(0, aimingProgress - exitSpeed * cantedAdsDecSpeedFactor);
//            isAiming = false;
//        }
    }

    public void onTickStart(@NotNull Player localPlayer) {
        itemStack = localPlayer.getMainHandItem();
        gun = itemStack.getItem() instanceof GunItem gunItem ? gunItem.getGun() : null;
        isHoldingGun = gun != null;
        if (gun == null) {
            clearGunState();
        } else {
            String id = gun.getIdentityID(itemStack);
            boolean reInitModules = false;
            if (!Objects.equals(id, identityID)) {
                identityID = id;
                reInitModules = true;
            }
            this.fireMode = gun.getFireMode(itemStack);
            if (fireMode != null) {
                int rpm = fireMode.modifyRpm(gun.getRpm(itemStack));
                fireDelay = rpm > 0 ? IFireMode.rpmToDelay(rpm) : 0;
                fireIntervalNanos = IFireMode.rpmToIntervalNanos(rpm);
            } else {
                fireDelay = 0;
                fireIntervalNanos = 0L;
            }
            int lastModifyID = gun.getModifyID(itemStack);
            if (!reInitModules && !Objects.equals(lastModifyID, modifyID)) {
                reInitModules = true;
            }
            modifyID = lastModifyID;
            if (reInitModules) {
                muzzleFlashRadius = 0;
                muzzleFlashIntensity = 0;
                onModuleTreeChange();
                leftArmHold = gun.getLeftArmHolding(itemStack);
                rightArmHold = gun.getRightArmHolding(itemStack);
                handlePropertiesUpdate(gun, itemStack);
                RecoilData recoilData = getGun().getRecoilData();
                RecoilHandler.INSTANCE.getRecoilUpdater().setRecoilData(recoilData);
            }
            lastHeat = heat;
            heat = gun.getCurrHeat(itemStack, localPlayer.level().getGameTime());
            checkSight();
            handleInteractiveModules();
        }
        handleAds(localPlayer);
    }

    private void checkSight() {
        String usingSightID = gun.getUsingSightID(itemStack);
        activeSight = IDToNodes.get(usingSightID);
        if (activeSight != null && activeSight.getModule() instanceof ISight sight) {
            lastAdsZCompensation = adsZCompensation;
            adsZCompensation = sight.getZCompensation();
            int isSide = activeSight.getUnit().getCustomParam(ISight.ON_SIDE_POSITION);
            if (isSide == 1) {
                cantedAdsIncSpeedFactor = 0.75f;
                cantedAdsDecSpeedFactor = 0.6f;
            } else {
                cantedAdsIncSpeedFactor = 1f;
                cantedAdsDecSpeedFactor = 1f;
            }
        }
    }

    private void handleAds(Player localPlayer) {
        if (Minecraft.getInstance().screen instanceof GunModifyScreen) {
            Client.RIGHT_BUTTON_PRESSED.set(false);
        }
        aimingProgressLast = aimingProgress;
        if (Client.RIGHT_BUTTON_PRESSED.get()) {
            if (aimingProgress < 1) {
                aimingProgress = Math.min(1, aimingProgress + aimingSpeed * cantedAdsIncSpeedFactor);
            }
            float sprintingProgress = SprintingHandler.INSTANCE.getSprintingProgress();
            float sprinting = 1 - sprintingProgress * sprintingProgress;
            float r1 = 1.001f - aimingProgress;
            float r2 = 1.001f - sprinting;
            aimingProgress = (aimingProgress * r1 + sprinting * r2) / (r1 + r2);
            isAiming = true;
        } else {
            float exitSpeed = Math.max(0.3f, aimingSpeed);
            aimingProgress = Math.max(0, aimingProgress - exitSpeed * cantedAdsDecSpeedFactor);
            isAiming = false;
        }
        if (localPlayer == null) {
            return;
        }
        AttributeInstance attr = localPlayer.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) {
            if (isAiming) {
                if (attr.getModifier(SPEED_ID) == null) {
                    double speedBonus = calcAdsSpeedModifier();
                    if (speedBonus != 0) {
                        attr.addTransientModifier(
                                new AttributeModifier(
                                        SPEED_ID,
                                        speedBonus,
                                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                                )
                        );
                    }
                }
            } else if (aimingProgress < 0.5f) {
                attr.removeModifier(SPEED_ID);
            }
        }
    }

    public float calcAdsSpeedModifier() {
        float score = agility * 2.2f / weight * 0.486f;
        score = (float) - Math.exp(-Math.pow(score, 0.66f)) * 0.7f;
        return Mth.clamp(score, -0.4f, 0);
    }

    protected void handlePropertiesUpdate(IGun gun, ItemStack itemStack) {
        impulse = gun.getImpulseRatio(itemStack);
        recoilControl = gun.getRecoilControl(itemStack);
        stability = gun.getStability(itemStack);
        agility = gun.getAgility(itemStack);
        weight = gun.getCurrentWeight(itemStack);
        aimingSpeed = calculateFinalAimingSpeed(gun.getAimingSpeed(itemStack), agility, weight);
    }

    public float calculateFinalAimingSpeed(float baseSpeed, float agility, float weight) {
        float k = agility / weight;
        k = (float) Math.pow(k, 0.7f);
        float factor = (float) (- Math.exp(-k) + 1);
        baseSpeed *= (0.5f + factor) * 0.13f;
        return Math.clamp(baseSpeed, 0.1f, 0.4f);
    }

    /**
     * 换算不出来时的兜底射速（600 RPM）。
     *
     * <p>宁可慢一点，也不能因为某个模块返回了 0 RPM 就变成每 2ms 一发的怪物。</p>
     */
    private static final long FALLBACK_FIRE_INTERVAL_NANOS = 100_000_000L;

    /**
     * 每一发的精确间隔（纳秒），恒为正数。开火时间轴以它排下一发的绝对时刻。
     */
    public long getFireIntervalNanos() {
        long interval = fireIntervalNanos;
        return interval > 0L ? interval : FALLBACK_FIRE_INTERVAL_NANOS;
    }

    /**
     * 每一发的间隔（秒）。后座力回落与动画节奏都用它，因此同样走精确值。
     */
    public float getFireInterval() {
        return getFireIntervalNanos() * 1e-9f;
    }

    /**
     * 无需持锁的开火预检：为 {@code true} 时开火线程连枪都不用碰。
     *
     * <p>只是近似值（主线程每 tick 刷新一次），判断失误最多多抢一次锁，不影响正确性。</p>
     */
    public boolean isFireBlocked() {
        return fireBlocked;
    }

    /**
     * 刷新 {@link #fireBlocked}。由客户端主线程在 tick 末尾调用。
     *
     * <p>只镜像 {@link Client#handleClientShoot} 在进入开火流程<b>之前</b>、且<b>没有副作用</b>的
     * 两种早退：根本没拿枪、拔枪动画没结束。别把冲刺 / 换弹也塞进来——那两种状态是在
     * {@code IFireMode#clientIntentToFire} 里处理的，顺手还会做「按扳机就退出冲刺」「任务挡着就把
     * 扳机状态清掉」这类副作用，跳过它们会改变手感和行为。</p>
     */
    public void updateFireBlocked() {
        boolean blocked = !isHoldingGun || DrawHolsterHandler.get().getEquipProgress() < 1f;
        if (fireBlocked == blocked) {
            return;
        }
        fireBlocked = blocked;
        // 挡着的东西刚让开（拔枪动画结束 / 重新拿上枪）而扳机还按着：
        // 立刻叫醒开火线程，别让它等到下一个周期才续上。
        if (!blocked && Client.LEFT_BUTTON_PRESSED.get()) {
            ClientWeaponLooper.wake();
        }
    }

    public float getStability() {
        return stability;
    }

    public float getRecoilControl() {
        return recoilControl;
    }

    public float getPlayerDynamicFactor() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return 1;
        }
        float factor = 1f;
        if (player.isCrouching()) {
            factor += 0.15f;
        } else if (player.xxa != 0 || player.yya != 0 || player.zza != 0) {
            factor -= 0.15f;

        } else if (player.isSprinting()) {
            factor -= 0.20f;
        }
        float jumpDist = (System.currentTimeMillis() - lastJump) * 0.001f;
        if (jumpDist < 0.7f) {
            factor = Math.max((factor - (0.7f - jumpDist) * 0.5f), 0.35f);
        }
        return factor;
    }

    public float getImpulse() {
        return impulse;
    }

    public void onShoot() {
        this.shootRandomSeed = (float) Math.random();
    }

    private void handleInteractiveModules() {
        for (Pair<IInteractiveModular, Node> pair : interactiveModules) {
            IInteractiveModular interactive = pair.getLeft();
            Node node = pair.getRight();
            interactive.onClientTick(node.getID(), node.getUnit(), gun, itemStack);
        }
    }

    public Map<String, Node> getIDToNodes() {
        return IDToNodes;
    }

    private void onModuleTreeChange() {
        IDToNodes.clear();
        IDToModels.clear();
        interactiveModules.clear();
        aimingSpeed = gun.getAimingSpeed(itemStack);
        ListTag modulesTag = gun.getModulesTag(itemStack);
        root = Node.read(modulesTag);
        final boolean[] Suppressed = {true};
        if (root != null) {
            updateRenderNodes();
            root.dfs(node -> {
                IModular module = node.getModule();
                IModularModel model = ModuleModelRegister.get(module);
                String id = node.getID();
                int depth = node.getDepth();
                IDToNodes.put(id, node);
                IDToModels.put(id, model);
                if (node.getModule() instanceof IInteractiveModular modular) {
                    interactiveModules.add(Pair.of(modular, node));
                }
                if (Suppressed[0] && node.getModule() instanceof Muzzle muzzle && !muzzle.isSuppressor()) {
                    Suppressed[0] = false;
                }
            });
        }
        isSuppressed = Suppressed[0];
    }

    private void updateRenderNodes() {
        if (root != null) {
            renderRoot = null;
            flatRenderNodeMap.clear();
            renderRoot = ModuleHandler.buildRenderTreeByNode(root);
            if (renderRoot != null) {
                renderRoot.dfsTravel(renderNode ->
                        flatRenderNodeMap.put(renderNode.id, renderNode));
            }
        }
    }

    /**
     * 丢弃所有缓存了模型实例的状态（模块节点、渲染树、当前瞄具等）。
     *
     * <p>客户端模型热重载会重建全部模型，调用本方法后下一个 tick 会因为 identityID 变化而重新
     * 构建模块树与渲染树；在此之前渲染侧会自行用新的注册表重建渲染树，不会引用旧模型。</p>
     */
    public void invalidateModelCache() {
        identityID = null;
        modifyID = 0;
        root = null;
        renderRoot = null;
        activeSight = null;
        IDToNodes.clear();
        IDToModels.clear();
        interactiveModules.clear();
        flatRenderNodeMap.clear();
    }

    public IGun getGun() {
        return gun;
    }

    public boolean isSightActivated(String sightID) {
        return activeSight != null && activeSight.getID().equals(sightID);
    }

    public float getLerpAdsZCompensation(float partialTicks) {
        float switchProgress = GunPoseHandler.INSTANCE.getSwitchProgress(partialTicks);
        return Mth.lerp(switchProgress, lastAdsZCompensation, adsZCompensation);
    }

    private void clearGunState() {
        isAiming = false;
        activeSight = null;
        identityID = null;
        muzzleFlashRadius = 0;
        muzzleFlashIntensity = 0;
        heat = 0;
        lastHeat = 0;
        fireIntervalNanos = 0L;
        fireBlocked = true;
    }

    public IFireMode<?> getPrevFireMode() {
        return fireMode;
    }

    public int getFireDelayTick() {
        return isHoldingGun ? fireDelay : 1;
    }

    public boolean isHoldingGun() {
        return isHoldingGun;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setMuzzleFlashConfig(PoseStack.Pose pose, float muzzleFlashIntensity) {
        if (muzzleFlashIntensity > this.muzzleFlashIntensity) {
            this.muzzleFlashIntensity = muzzleFlashIntensity;
            this.muzzleFlashRadius = this.muzzleFlashIntensity * 1.5f;
            setMuzzleFlashPos(pose);
        }
    }

    private void setMuzzleFlashPos(PoseStack.Pose pose) {
        PoseStack.Pose copy = pose.copy();
        copy.pose().translate(0,0.0625f,-0.0625f);
        Vector3f translation = copy.pose().getTranslation(new Vector3f());
        translation.x = 0;
        muzzleFlashPos = translation;
    }

    public Node getActiveSight() {
        return activeSight;
    }

    public Vector3f getMuzzleFlashPos() {
        return muzzleFlashPos;
    }

    public boolean isUsingScope() {
        return activeSight != null && ModuleModelRegister.get(activeSight.getModule()) instanceof IScopeModel;
    }

    public void clearMuzzleFlashModelEffect() {
        muzzleFlashRadius = 0;
        muzzleFlashIntensity = 0;
    }

    public float getMuzzleFlashIntensity() {
        return muzzleFlashIntensity;
    }

    public float getMuzzleFlashRadius() {
        return muzzleFlashRadius;
    }

    public float getWeight() {
        return weight;
    }

    public float getAgility() {
        return agility;
    }

    public IArmHandlerModular getRightArmHold() {
        return rightArmHold;
    }

    public IArmHandlerModular getLeftArmHold() {
        return leftArmHold;
    }

    public void onPlayerJump() {
        lastJump = System.currentTimeMillis();
    }

    public long getLastJump() {
        return lastJump;
    }

    public float getHeat(long nowTick) {
        return gun == null ? 0 : gun.getCurrHeat(itemStack, nowTick);
    }

    public float getHeat(float particleTicks) {
        return Mth.lerp(particleTicks, lastHeat, heat);
    }
}
