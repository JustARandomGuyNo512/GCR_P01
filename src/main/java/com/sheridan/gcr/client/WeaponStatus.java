package com.sheridan.gcr.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.model.modular.IModularModel;
import com.sheridan.gcr.client.model.modular.IScopeModel;
import com.sheridan.gcr.client.model.modular.ModuleModelRegister;
import com.sheridan.gcr.client.recoil.RecoilData;
import com.sheridan.gcr.client.recoil.RecoilHandler;
import com.sheridan.gcr.client.render.ModuleRenderNode;
import com.sheridan.gcr.client.render.SightPoseHandler;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.ModuleHandler;
import com.sheridan.gcr.modularSys.builder.Node;
import com.sheridan.gcr.modularSys.fire.IFireMode;
import com.sheridan.gcr.modularSys.modules.IArmHandlerModular;
import com.sheridan.gcr.modularSys.modules.IInteractiveModular;
import com.sheridan.gcr.modularSys.modules.ISight;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
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
    private Object2ObjectOpenHashMap<String, ModuleRenderNode> flatRenderNodeMap = new Object2ObjectOpenHashMap<>();
    private String identityID;
    private String modifyID;
    private IFireMode<?> fireMode;
    private int fireDelay;
    public int fireCount;
    public long lastShoot;
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

    private float cantedAdsIncSpeedFactor = 1;
    private float cantedAdsDecSpeedFactor = 1;

    private IArmHandlerModular leftArmHold;
    private IArmHandlerModular rightArmHold;

    public WeaponStatus() {

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
        if (!Client.RIGHT_BUTTON_PRESSED.get()) {
            aimingProgressLast = aimingProgress;
            float exitSpeed = Math.max(0.3f, aimingSpeed);
            aimingProgress = Math.max(0, aimingProgress - exitSpeed * cantedAdsDecSpeedFactor);
            isAiming = false;
        }
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
                fireDelay = IFireMode.rpmToDelay(fireMode.modifyRpm(gun.getRpm(itemStack)));
            }
            String lastModifyID = gun.getModifyID(itemStack);
            if (!reInitModules && !Objects.equals(lastModifyID, modifyID)) {
                reInitModules = true;
            }
            modifyID = lastModifyID;
            if (reInitModules) {
                onModuleTreeChange();
                leftArmHold = gun.getLeftArmHolding(itemStack);
                rightArmHold = gun.getRightArmHolding(itemStack);
                handlePropertiesUpdate(gun, itemStack);
                RecoilData recoilData = getGun().getRecoilData();
                RecoilHandler.INSTANCE.getRecoilUpdater().setRecoilData(recoilData);
            }
            checkSight();
            normalTick(localPlayer, gun, itemStack);
        }
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

    private void normalTick(@NotNull Player localPlayer, IGun gun, ItemStack itemStack) {
        if (Client.RIGHT_BUTTON_PRESSED.get()) {
            aimingProgressLast = aimingProgress;
            //if (aimingProgress < 1) {
                aimingProgress = Math.min(1, aimingProgress + aimingSpeed * cantedAdsIncSpeedFactor);

//                float sprinting = 1 - SprintingHandler.INSTANCE.getSprintingProgress();
//                float r1 = 1.001f - aimingProgress;
//                float r2 = 1.001f - sprinting;
//                aimingProgress = (aimingProgress * r1 + sprinting * r2) / (r1 + r2);
            //}
            isAiming = true;
        } else {
            isAiming = false;
        }
        if (aimingProgress > 0) {
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
        handleInteractiveModules();
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

    public float getFireInterval() {
        return fireDelay * 0.005f;
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
            factor += 0.2f;
        } else if (player.isSprinting()) {
            factor -= 0.05f;
        }
        float jumpDist = (System.currentTimeMillis() - lastJump) * 0.001f;
        if (jumpDist < 1f) {
            factor = Math.max((factor - (1f - jumpDist) * 0.5f), 0.4f);
        }
        return factor;
    }

    public float getImpulse() {
        return impulse;
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
            });
        }
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

    public IGun getGun() {
        return gun;
    }

    public boolean isSightActivated(String sightID) {
        return activeSight != null && activeSight.getID().equals(sightID);
    }

    public float getLerpAdsZCompensation(float partialTicks) {
        float switchProgress = SightPoseHandler.INSTANCE.getSwitchProgress(partialTicks);
        return Mth.lerp(switchProgress, lastAdsZCompensation, adsZCompensation);
    }

    private void clearGunState() {
        isAiming = false;
        activeSight = null;
        identityID = null;
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

    public void setMuzzleFlashPos(PoseStack.Pose pose) {
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

    public void setMuzzleFlashIntensity(float muzzleFlashIntensity) {
        this.muzzleFlashIntensity = muzzleFlashIntensity;
    }

    public boolean isUsingScope() {
        return activeSight != null && ModuleModelRegister.get(activeSight.getModule()) instanceof IScopeModel;
    }

    public void setMuzzleFlashRadius(float muzzleFlashRadius) {
        this.muzzleFlashRadius = muzzleFlashRadius;
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
}
