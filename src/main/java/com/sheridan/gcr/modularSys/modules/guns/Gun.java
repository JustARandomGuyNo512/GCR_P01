package com.sheridan.gcr.modularSys.modules.guns;

import com.google.gson.JsonObject;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.GunEffect;
import com.sheridan.gcr.client.GunEffectManager;
import com.sheridan.gcr.client.KeyBinds;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.client.recoil.RecoilData;
import com.sheridan.gcr.client.recoil.RecoilHandler;
import com.sheridan.gcr.common.Commons;
import com.sheridan.gcr.entity.ModEntities;
import com.sheridan.gcr.entity.projectile.BulletEntity;
import com.sheridan.gcr.items.DisplayData;
import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.IDGenerator;
import com.sheridan.gcr.modularSys.ModuleRegister;
import com.sheridan.gcr.modularSys.builder.*;
import com.sheridan.gcr.modularSys.fire.IFireMode;
import com.sheridan.gcr.modularSys.modules.*;
import com.sheridan.gcr.modularSys.modules.Module;
import com.sheridan.gcr.modularSys.modules.gunProperties.IProperties;
import com.sheridan.gcr.modularSys.modules.gunProperties.PropertiesAccessor;
import com.sheridan.gcr.modularSys.modules.gunProperties.impl.BaseProperties;
import com.sheridan.gcr.modularSys.modules.impl.Muzzle;
import com.sheridan.gcr.modularSys.modules.states.Str;
import com.sheridan.gcr.modularSys.modules.views.IAmmoSourceView;
import com.sheridan.gcr.modularSys.task.IGunTask;
import com.sheridan.gcr.modularSys.task.other.CheckingTask;
import com.sheridan.gcr.modularSys.task.other.FireModeSwitchTask;
import com.sheridan.gcr.modularSys.task.other.RemoveStuckTask;
import com.sheridan.gcr.network.c2s.GunFirePacket;
import com.sheridan.gcr.network.s2c.BroadcastLivingFirePacket;
import com.sheridan.gcr.network.s2c.GunFireAckPacket;
import com.sheridan.gcr.sound.ModSounds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

public class Gun extends Module implements IGun, ISight, IArmHandlerModular {
    protected final DisplayData displayData;
    protected Map<String, IProperties> propertiesMap;
    public final BaseProperties baseProperties;
    protected final RecoilData recoilData;
    private final List<IFireMode<?>> fireModes;
    private final Map<String, IFireMode<?>> fireModeMap;
    private final SplittableRandom random = new SplittableRandom((long) (Math.random() * 100000));

    public Gun(ResourceLocation id, BaseProperties baseProperties, DisplayData displayData, RecoilData recoilData, List<IFireMode<?>> fireModes) {
        super(id, true, baseProperties.weight.getDefault(), Direction.NONE);
        this.baseProperties = baseProperties;
        this.displayData = displayData;
        this.recoilData = recoilData;
        this.propertiesMap = new HashMap<>();
        this.fireModes = fireModes;
        this.fireModeMap = new HashMap<>();
        for (IFireMode<?> fireMode : fireModes) {
            fireModeMap.put(fireMode.getName(), fireMode);
        }
        registerProperties(baseProperties);
    }

    protected void registerProperties(IProperties properties) {
        propertiesMap.put(properties.getId(), properties);
    }

    @Override
    public void modifyProperties(PropertiesAccessor accessor) {
        //这里不干任何事情
    }

    @Override
    public void writeToJson(JsonObject jsonObject) {}

    @Override
    public void loadFromJson(JsonObject jsonObject) {}


    @Override
    public void fullSyncFromServer(ItemStack itemStack, CompoundTag data) {
        CustomData.set(DataComponents.CUSTOM_DATA, itemStack, data);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void clientShoot(Player player, ItemStack itemStack) {
        RecoilHandler.INSTANCE.onShoot(player);
        SoundEvent fireSound = getFireSound(itemStack);
        if (fireSound != null) {
            ModSounds.sound(1, (float) (0.9f + Math.random() * 0.1f), player, fireSound);
        }
        GunEffectManager.updateEffectTimestamp(player.getId(), GunEffect.SHOOT, rootNodeId(itemStack), System.currentTimeMillis());
        Client.WEAPON_STATUS.lastShoot = System.nanoTime();
        Client.getGunRenderer().dispatchAnimationEvent(EventType.CLEAR_TRACK, Map.of("name", "check"));
        //ModularModel.debugHeat = Mth.clamp(ModularModel.debugHeat + 0.1f, 0, 1);
    }

    @Override
    public void serverShoot(LivingEntity shooter, ItemStack itemStack, int shootID, GunFirePacket packet) {
        Level level = shooter.level();
        if (level.isClientSide) {
            return;
        }

        float yaw = shooter.getYRot();
        float pitch = shooter.getXRot();

        pitch += packet.gunKickPitch;
        yaw += packet.gunKickYaw;

        float spread = getSpread(itemStack);

        float randPitch = (float) ((random.nextGaussian() - 0.5f) * spread);
        float randYaw = (float) ((random.nextGaussian() - 0.5f) * spread);

        pitch += randPitch;
        yaw += randYaw;

        Vec3 dir = Vec3.directionFromRotation(pitch, yaw).normalize();
        Vec3 pos = shooter.getEyePosition();
        double speed = 30f;

        BulletEntity bullet = new BulletEntity(ModEntities.BULLET.get(), level);
        bullet.setPos(pos);
        Vec3 velocity = dir.scale(speed);

        bullet.setDeltaMovement(velocity);
        bullet.setShooter(shooter);
        bullet.start();
        level.addFreshEntity(bullet);
        int latency = 0;
        if (shooter instanceof ServerPlayer player) {
            latency = player.connection.latency();
            SoundEvent fireSound = getFireSound(itemStack);
            if (fireSound != null) {
                ModSounds.sound(getFireSoundRange(itemStack), (float) (0.9f + Math.random() * 0.1f), player, fireSound);
            }
            updateHeat(itemStack, getShootHeat(itemStack), shooter.level().getGameTime(), true);
            CompoundTag states = rootNodeTag(itemStack);
            GunFireAckPacket ackPacket = new GunFireAckPacket(
                    getIdentityID(itemStack),
                    getAmmoLeft(itemStack),
                    shootID,
                    isStuck(itemStack),
                    getHeatLastUpdate(states),
                    getHeat(states)
            );
            PacketDistributor.sendToPlayer(
                    player,
                    ackPacket
            );
        }

        BroadcastLivingFirePacket firePacket = new BroadcastLivingFirePacket(
                shooter.getId(),
                rootNodeId(itemStack),
                getIdentityID(itemStack),
                latency
        );
        PacketDistributor.sendToPlayersTrackingEntity(
                shooter,
                firePacket
        );
    }

    protected SoundEvent getFireSound(ItemStack itemStack) {
        int type = FIRE_SOUND_TYPE.get(rootNodeTag(itemStack));
        if (type == FIRE_SOUND_NORMAL) {
            return baseProperties.getFireSoundNormal();
        } else if (type == FIRE_SOUND_SUPPRESSED) {
            return baseProperties.getFireSoundSuppressed();
        }
        return baseProperties.getFireSoundNormal();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void serverShootAck(GunFireAckPacket packet, ItemStack itemStack) {
        setStuck(packet.stuck, rootNodeTag(itemStack));
        setCurrHeat(itemStack, packet.heat, packet.heatUpdateTime);
    }

    @Override
    public DisplayData getDisplayData() {
        return displayData;
    }

    @Override
    public long getDataDate(ItemStack itemStack) {
        CustomData orDefault = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (orDefault.isEmpty()) {
            return 0;
        }
        CompoundTag compoundTag = orDefault.getUnsafe();
        return compoundTag.getLong(MODULE_DATE_KEY);
    }

    @Override
    public void setDataDate(ItemStack itemStack, long date) {
        CompoundTag compoundTag = checkAndGetRaw(itemStack);
        compoundTag.putLong(MODULE_DATE_KEY, date);
    }

    @Override
    public void reload(ItemStack itemStack, Player player) {
        setAmmoLeft(itemStack, getMaxCapacity());
    }

    @Override
    public void serverInitData(ItemStack itemStack) {
        CompoundTag compoundTag = checkAndGetRaw(itemStack);
        compoundTag.putString(IDENTITY_ID_KEY, IDGenerator.randomId());
        compoundTag.putInt(STUCK_SEED, (int) (Math.random() * 1000000));
        CustomData.set(DataComponents.CUSTOM_DATA, itemStack, compoundTag.copy());
    }

    @Override
    public CompoundTag checkAndGet(ItemStack itemStack) {
        return checkAndGetRaw(itemStack).copy();
    }

    @SuppressWarnings("deprecation")
    @Override
    public CompoundTag checkAndGetRaw(ItemStack itemStack) {
        CustomData orDefault = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (orDefault.isEmpty()) {
            CompoundTag tag = getInitialDataTag();
            CustomData.set(DataComponents.CUSTOM_DATA, itemStack, tag);
            return tag;
        }
        return orDefault.getUnsafe();
    }


    @Override
    public ListTag getModulesTag(ItemStack itemStack) {
        CompoundTag compoundTag = checkAndGetRaw(itemStack);
        return compoundTag.getList(MODULES_KEY, Tag.TAG_COMPOUND);
    }

    @Override
    public CompoundTag getPropertiesTag(ItemStack itemStack) {
        CompoundTag compoundTag = checkAndGetRaw(itemStack);
        return compoundTag.getCompound(PROPERTIES_KEY);
    }

    @Override
    public String rootNodeId(ItemStack itemStack) {
        CompoundTag compoundTag = checkAndGetRaw(itemStack);
        return compoundTag.getString(ROOT_NODE_ID_KEY);
    }

    @Override
    public CompoundTag rootNodeTag(ItemStack itemStack) {
        return getNodeStatesTag(itemStack, rootNodeId(itemStack));
    }

    @Override
    public @Nullable CompoundTag getNodeStatesTag(ItemStack itemStack, String nodeId) {
        CompoundTag compoundTag = checkAndGetRaw(itemStack);
        CompoundTag allStates = compoundTag.getCompound(STATES_KEY);
        if (allStates.contains(nodeId)) {
            return allStates.getCompound(nodeId);
        }
        return null;
    }

    @Override
    public CompoundTag getAmmoSourceTag(ItemStack itemStack) {
        String s = USING_AMMO_SOURCE.get(rootNodeTag(itemStack));
        return getNodeStatesTag(itemStack, s);
    }

    @Override
    public CompoundTag getStatesTag(ItemStack itemStack) {
        CompoundTag compoundTag = checkAndGetRaw(itemStack);
        return compoundTag.getCompound(STATES_KEY);
    }

    @Override
    public void setStatesTag(ItemStack itemStack, CompoundTag states) {
        CompoundTag compoundTag = checkAndGet(itemStack);
        compoundTag.put(STATES_KEY, states);
        CustomData.set(DataComponents.CUSTOM_DATA, itemStack, compoundTag);
    }

    @Override
    public void setModulesTag(ItemStack itemStack, ListTag nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        CompoundTag tag = checkAndGet(itemStack);
        tag.put(MODULES_KEY, nodes);
        int anInt = tag.getInt(MODIFY_ID_KEY);
        tag.putInt(IGun.MODIFY_ID_KEY, anInt + 1);
        CustomData.set(DataComponents.CUSTOM_DATA, itemStack, tag);
    }

    @Override
    public void setPropertiesTag(ItemStack itemStack, CompoundTag properties) {
        if (properties == null || properties.isEmpty()) {
            return;
        }
        CompoundTag tag = checkAndGet(itemStack);
        tag.put(PROPERTIES_KEY, properties);
        CustomData.set(DataComponents.CUSTOM_DATA, itemStack, tag);
    }

    @Override
    public String getIdentityID(ItemStack itemStack) {
        CustomData orDefault = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (orDefault.isEmpty()) {
            return IGun.NONE;
        }
        return orDefault.getUnsafe().getString(IDENTITY_ID_KEY);
    }

    @Override
    public int getModifyID(ItemStack itemStack) {
        CompoundTag compoundTag = checkAndGetRaw(itemStack);
        return compoundTag.getInt(MODIFY_ID_KEY);
    }

    @Override
    public void setModifyID(ItemStack itemStack, int modifyID, boolean copy) {
        CompoundTag compoundTag = checkAndGetRaw(itemStack);
        compoundTag.putInt(MODIFY_ID_KEY, modifyID);
    }

    @Override
    public int getRpm(ItemStack itemStack) {
        CompoundTag tag = getPropertiesTag(itemStack);
        CompoundTag compound = tag.getCompound(baseProperties.getId());
        return (int) baseProperties.rpm.get(compound);
    }

    @Override
    public boolean isStuck(ItemStack itemStack) {
        return STUCK.get(rootNodeTag(itemStack));
    }

    @Override
    public void removeStuck(ItemStack itemStack) {
        setStuck(false, rootNodeTag(itemStack));
    }

    @Override
    public void setStuck(boolean stuck, CompoundTag states) {
        STUCK.set(stuck, states);
    }

    @Override
    public IFireMode<?> getFireMode(ItemStack itemStack) {
        String name = FIRE_MODEL_ID.get(rootNodeTag(itemStack));
        IFireMode<?> fireMode = fireModeMap.get(name);
        return fireMode == null ? fireModes.getFirst() : fireMode;
    }

    @Override
    public List<IFireMode<?>> getAllFireModes() {
        return fireModes;
    }

    @Override
    public void setFireMode(ItemStack itemStack, IFireMode<?> fireMode) {
        if (fireModeMap.containsKey(fireMode.getName())) {
            FIRE_MODEL_ID.set(fireMode.getName(), rootNodeTag(itemStack));
        }
    }

    @Override
    public void toNextFireMode(ItemStack itemStack) {
        IFireMode<?> fireMode = getFireMode(itemStack);
        int index = fireModes.indexOf(fireMode);
        index = (index + 1) % fireModes.size();
        setFireMode(itemStack, fireModes.get(index));
    }

    @Override
    public String getUsingSightID(ItemStack itemStack) {
        return USING_SIGHT.get(rootNodeTag(itemStack));
    }

    @Override
    public String getLeftArmHoldID(ItemStack itemStack) {
        return LEFT_ARM_HOLD.get(rootNodeTag(itemStack));
    }

    @Override
    public String getRightArmHoldID(ItemStack itemStack) {
        return RIGHT_ARM_HOLD.get(rootNodeTag(itemStack));
    }

    @Override
    public void setUsingSightID(ItemStack itemStack, String sightID) {
        USING_SIGHT.set(sightID, rootNodeTag(itemStack));
    }

    @Override
    public float getHeatStuckRatio(ItemStack itemStack) {
        CompoundTag compoundTag = rootNodeTag(itemStack);
        return HEAT_STUCK_RATIO.get(compoundTag);
    }

    @Override
    public int getAmmoLeft(ItemStack itemStack) {
        return AMMO_LEFT.get(rootNodeTag(itemStack));
    }

    @Override
    public void setAmmoLeft(ItemStack itemStack, int ammoLeft) {
        AMMO_LEFT.set(ammoLeft, rootNodeTag(itemStack));
    }

    @Nullable
    @Override
    public IGunTask<?> getTask(ItemStack itemStack, IGunTask.TaskType type, Map<String, Object> args) {
        if (type == IGunTask.TaskType.SWITCH_FIRE_MODE) {
            return new FireModeSwitchTask(itemStack, this);
        }
        if (type == IGunTask.TaskType.CHECKING && !Client.isAiming()) {
            String animType = args == null ? null : (String) args.get("type");
            return new CheckingTask(itemStack, this, animType);
        }
        if (type == IGunTask.TaskType.REMOVE_STUCK && isStuck(itemStack)) {
            return new RemoveStuckTask<>(itemStack, this);
        }
        return null;
    }

    @Override
    public RecoilData getRecoilData() {
        return recoilData;
    }

    @Override
    public float getCurrentWeight(ItemStack itemStack) {
        CompoundTag propertiesTag = getPropertiesTag(itemStack);
        CompoundTag compound = propertiesTag.getCompound(baseProperties.getId());
        return baseProperties.weight.get(compound);
    }

    //TODO: 接入数据模型之后替换为详细计算
    @Override
    public float getAimingSpeed(ItemStack itemStack) {
        CompoundTag compound = baseProperties.pick(itemStack, this);
        float ratio = baseProperties.aimingSpeed.getRatio(compound);
        AdditionalPropModifier leftArmModifier = getLeftArmModifier(itemStack);
        AdditionalPropModifier rightArmModifier = getRightArmModifier(itemStack);
        ratio = leftArmModifier == null ? ratio : ratio + leftArmModifier.getAimingSpeedInc();
        ratio = rightArmModifier == null ? ratio : ratio + rightArmModifier.getAimingSpeedInc();
        return ratio * baseProperties.aimingSpeed.value;
    }

    @Override
    public float getStuckRate(ItemStack itemStack) {
        CompoundTag pick = baseProperties.pick(itemStack, this);
        return baseProperties.stuckRate.get(pick);
    }

    @Override
    public float getMaxStuckRate(ItemStack itemStack) {
        CompoundTag pick = baseProperties.pick(itemStack, this);
        return baseProperties.maxStuckRate.get(pick);
    }

    @Override
    public float getRecoilControl(ItemStack itemStack) {
        CompoundTag compound = baseProperties.pick(itemStack, this);
        float ratio = baseProperties.recoilControl.getRatio(compound);
        AdditionalPropModifier leftArmModifier = getLeftArmModifier(itemStack);
        AdditionalPropModifier rightArmModifier = getRightArmModifier(itemStack);
        ratio = leftArmModifier == null ? ratio : ratio + leftArmModifier.getRecoilControlInc();
        ratio = rightArmModifier == null ? ratio : ratio + rightArmModifier.getRecoilControlInc();
        return ratio * baseProperties.recoilControl.value;
    }

    @Override
    public float getStability(ItemStack itemStack) {
        CompoundTag compound = baseProperties.pick(itemStack, this);
        float ratio = baseProperties.stability.getRatio(compound);
        AdditionalPropModifier leftArmModifier = getLeftArmModifier(itemStack);
        AdditionalPropModifier rightArmModifier = getRightArmModifier(itemStack);
        ratio = leftArmModifier == null ? ratio : ratio + leftArmModifier.getStabilityInc();
        ratio = rightArmModifier == null ? ratio : ratio + rightArmModifier.getStabilityInc();
        return ratio * baseProperties.stability.value;
    }


    @Override
    public Map<String, IProperties> getProperties() {
        return propertiesMap;
    }

    @Override
    public void notifyDataChanged(ItemStack itemStack) {
        CompoundTag compoundTag = checkAndGetRaw(itemStack);
        compoundTag.putBoolean(DATA_CHANGED_KEY, true);
    }

    @Override
    public boolean dataChanged(ItemStack itemStack) {
        CompoundTag compoundTag = checkAndGetRaw(itemStack);
        return compoundTag.getBoolean(DATA_CHANGED_KEY);
    }

    @Override
    public void onReceiveStatesDataFormClient(CompoundTag payLoad, ItemStack itemStack, String nodeId, Player player) {
        CompoundTag nodeStatusTag = getNodeStatesTag(itemStack, nodeId);
        if (nodeStatusTag == null) {
            return;
        }
        boolean changed = false;
        for (String key : payLoad.getAllKeys()) {
            Tag tag = payLoad.get(key);
            if (tag != null) {
                nodeStatusTag.put(key, tag);
                changed = true;
            }
        }
        if (changed) {
            notifyDataChanged(itemStack);
        }
    }

    @NotNull
    protected CompoundTag getInitialDataTag() {
        CompoundTag dataModel = new CompoundTag();
        dataModel.putString(IDENTITY_ID_KEY, NONE);
        dataModel.putInt(MODIFY_ID_KEY, -1);
        dataModel.putBoolean(DATA_CHANGED_KEY, false);
        dataModel.putInt(STUCK_SEED, -1);

        IBuilder builder = getBuilderForInit();
        IWorkSpace workspace = builder.getWorkspace();
        onModuleTreeInit(workspace, builder);
        IReadOnlyTree warehouse = tryInitialCommit(builder);
        ListTag gcrModules = warehouse.write();
        dataModel.put(MODULES_KEY, gcrModules);


        String rootNodeId = gcrModules.getCompound(0).getString(Unit.IN_TIME_ID);
        dataModel.putString(ROOT_NODE_ID_KEY, rootNodeId);


        ShadowNode shadowTree = warehouse.getShadowTree();
        CompoundTag statesTag = onStatesInit(shadowTree);
        StatesUpdateContext context = new StatesUpdateContext(this, shadowTree, statesTag);
        context.autoExec();
        dataModel.put(STATES_KEY, statesTag);

        CompoundTag properties = reCalculateProperties(warehouse);
        dataModel.put(PROPERTIES_KEY, properties);
        dataModel.putLong(MODULE_DATE_KEY, Commons.getServerStartTime());
        return dataModel;
    }

    @Override
    public CompoundTag reCalculateProperties(IReadOnlyTree warehouse) {
        List<Unit> sequencedUnits = warehouse.getSequencedUnits();
        CompoundTag properties = genInitialProperties();
        PropertiesAccessor propertiesAccessor = PropertiesAccessor.of(properties, getProperties());
        for (Unit unit : sequencedUnits) {
            unit.getModule().modifyProperties(propertiesAccessor);
        }
        return properties;
    }

    @Override
    public BaseProperties getBaseProperties() {
        return baseProperties;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        int rpm = getRpm(stack);
        float recoilControl = getRecoilControl(stack);
        float stability = getStability(stack);
        float currentWeight = getCurrentWeight(stack);
        float spread = baseProperties.spread.get(baseProperties.pick(stack, this));
        float faultRate = getStuckRate(stack) * 100f;
        float agility = getAgility(stack);
        float impulse = getImpulseRatio(stack) * 100f;

        tooltipComponents.addAll(List.of(
                Component.literal(baseProperties.rpm.getFullName() + ": " + rpm),
                Component.literal(baseProperties.recoilControl.getFullName()  + ": " + String.format("%.3f", recoilControl)),
                Component.literal(baseProperties.stability.getFullName() + ": " + String.format("%.3f", stability)),
                Component.literal(baseProperties.weight.getFullName() + ": " + String.format("%.3f", currentWeight)),
                Component.literal(baseProperties.spread.getFullName() + ": " + String.format("%.3f", spread)),
                Component.literal(baseProperties.stuckRate.getFullName() + ": " + String.format("%.3f", faultRate) + " %"),
                Component.literal(baseProperties.agility.getFullName() + ": " + String.format("%.3f", agility)),
                Component.literal(baseProperties.impulse.getFullName() + ": " + String.format("%.3f", impulse) + " %")
        ));
        String string = Component.translatable("tooltip.util.gun_modify").getString();
        String msg = string.replace("$key", KeyBinds.OPEN_GUN_MODIFY_SCREEN.getTranslatedKeyMessage().getString());
        tooltipComponents.add(Component.literal(msg).setStyle(Style.EMPTY.withColor(Color.GRAY.getRGB())));
    }


    @Override
    public IArmHandlerModular getLeftArmHolding(ItemStack itemStack) {
        return getArmHolding(itemStack, LEFT_ARM_HOLD);
    }

    @Override
    public IArmHandlerModular getRightArmHolding(ItemStack itemStack) {
        return getArmHolding(itemStack, RIGHT_ARM_HOLD);
    }

    @Override
    public @Nullable IArmHandlerModular.AdditionalPropModifier getLeftArmModifier(ItemStack itemStack) {
        IArmHandlerModular leftArmHolding = getLeftArmHolding(itemStack);
        return leftArmHolding == null ? null : leftArmHolding.getModifier();
    }

    @Override
    public @Nullable IArmHandlerModular.AdditionalPropModifier getRightArmModifier(ItemStack itemStack) {
        IArmHandlerModular rightArmHolding = getRightArmHolding(itemStack);
        return rightArmHolding == null ? null : rightArmHolding.getModifier();
    }

    protected IArmHandlerModular getArmHolding(ItemStack itemStack, Str stateKey) {
        String id = stateKey.get(rootNodeTag(itemStack));
        ListTag modulesTag = getModulesTag(itemStack);
        for (int i = 0; i < modulesTag.size(); i++) {
            CompoundTag moduleTag = modulesTag.getCompound(i);
            if (id.equals(moduleTag.getString(Unit.IN_TIME_ID))) {
                String string = moduleTag.getString(Unit.MODULE_ID);
                return ModuleRegister.get(string, IArmHandlerModular.class);
            }
        }
        return null;
    }

    @Override
    public float getImpulseRatio(ItemStack itemStack) {
        CompoundTag pick = baseProperties.pick(itemStack, this);
        return baseProperties.impulse.get(pick);
    }

    @Override
    public float getAgility(ItemStack itemStack) {
        CompoundTag pick = baseProperties.pick(itemStack, this);
        AdditionalPropModifier leftArmModifier = getLeftArmModifier(itemStack);
        AdditionalPropModifier rightArmModifier = getLeftArmModifier(itemStack);
        float ratio = baseProperties.agility.getRatio(pick);
        ratio = leftArmModifier == null ? ratio : ratio + leftArmModifier.getAgilityInc();
        ratio = rightArmModifier == null ? ratio : ratio + rightArmModifier.getAgilityInc();
        return ratio * baseProperties.agility.value;
    }

    @Override
    public float getFireSoundRange(ItemStack itemStack) {
        CompoundTag pick = baseProperties.pick(itemStack, this);
        return baseProperties.fireSoundRange.get(pick);
    }

    @Override
    public boolean isPistol() {
        return false;
    }

    @Override
    public float getSpread(ItemStack itemStack) {
        CompoundTag pick = baseProperties.pick(itemStack, this);
        return baseProperties.spread.get(pick);
    }


    @Override
    public float getShootHeat(ItemStack itemStack) {
        CompoundTag pick = baseProperties.pick(itemStack, this);
        return baseProperties.shootHeatInc.get(pick);
    }

    @Override
    public float getHeatDecSpeed(ItemStack itemStack) {
        CompoundTag pick = baseProperties.pick(itemStack, this);
        return baseProperties.heatDecSpeed.get(pick);
    }

    @Override
    public float getCurrHeat(ItemStack itemStack, long now) {
        CompoundTag states = rootNodeTag(itemStack);
        float heat = getHeat(states);
        long heatLastUpdate = getHeatLastUpdate(states);
        float heatDecSpeed = getHeatDecSpeed(itemStack);
        int tickNum = (int) (now - heatLastUpdate);
        float heatDec = heatDecSpeed * tickNum;
        heat -= heatDec;
        return Mth.clamp(heat, 0, 1);
    }

    @Override
    public void setCurrHeat(ItemStack itemStack, float heat, long heatLastUpdate) {
        heat = Mth.clamp(heat, 0, 1);
        heatLastUpdate = Math.max(heatLastUpdate, 0);
        CompoundTag states = rootNodeTag(itemStack);
        HEAT.set(heat, states);
        HEAT_LAST_UPDATE.set(heatLastUpdate, states);
    }

    @Override
    public void updateHeat(ItemStack itemStack, float heatInc, long time, boolean setLastShootTime) {
        CompoundTag states = rootNodeTag(itemStack);
        float heat = getHeat(states);
        long lastShootTime = getLastShootTime(states);
        boolean coolDown = time - lastShootTime >= 20;
        if (coolDown) {
            long heatLastUpdate = getHeatLastUpdate(states);
            float heatDecSpeed = getHeatDecSpeed(itemStack);
            int tickNum = (int) (time - heatLastUpdate);
            float heatDec = heatDecSpeed * tickNum;
            heat -= heatDec;
        }
        heat += heatInc;
        HEAT_LAST_UPDATE.set(time, states);
        if (setLastShootTime) {
            LAST_SHOOT_TIME.set(time, states);
        }
        setHeat(itemStack, heat);
    }

    public void setHeat(ItemStack itemStack, float heat) {
        CompoundTag states = rootNodeTag(itemStack);
        heat = Mth.clamp(heat, 0, 1);
        HEAT.set(heat, states);
    }

    protected IReadOnlyTree tryInitialCommit(IBuilder builder) {
        if (GCR.IS_DEVELOPMENT) {
            builder.commit();
            return builder.getWarehouse();
        }
        try {
            builder.commit();
            return builder.getWarehouse();
        } catch (Exception e) {
            GCR.LOGGER.error("Failed to commit initial module tree, fallback to root-only workspace", e);
            return new WorkSpace(this);
        }
    }

    protected CompoundTag genInitialProperties() {
        CompoundTag dataModules = new CompoundTag();
        for (IProperties properties : this.propertiesMap.values()) {
            CompoundTag moduleInitial = properties.genInitialTag();
            dataModules.put(properties.getId(), moduleInitial);
        }
        return dataModules;
    }

    protected CompoundTag onStatesInit(ShadowNode shadowTreeRoot) {
        CompoundTag statesTag = new CompoundTag();
        List<ShadowNode> allNodesOfThisTree = shadowTreeRoot.getAllNodesOfThisTree();
        for (ShadowNode node : allNodesOfThisTree) {
            if (node.unit.getModule() instanceof IStateModular stateModular) {
                CompoundTag states = new CompoundTag();
                String nodeId = node.nodeId;
                stateModular.onInitStates(states, nodeId, node.unit.getModuleId());
                statesTag.put(nodeId, states);
            }
        }
        return statesTag;
    }

    protected void onModuleTreeInit(IWorkSpace workspace, IBuilder builder) {

    }

    protected IBuilder getBuilderForInit() {
        return new Builder(this);
    }

    @Override
    public float getWeight() {
        return 3.75f;
    }


    @Override
    public float getAdsSpeedModifier() {
        return 1.0f;
    }

    @Override
    public int defaultSightPriority(Unit unit) {
        return GUN_BASE;
    }

    @Override
    public int getAmmoLeft(CompoundTag states) {
        return AMMO_LEFT.get(states);
    }

    @Override
    public int getMaxCapacity() {
        return 1;
    }

    @Override
    public int getPriority() {
        return IAmmoSourceView.GUN_BASE_PRIORITY;
    }

    @Override
    public boolean stuck(CompoundTag states) {
        return STUCK.get(states);
    }

    @Override
    public String getFireModeId(CompoundTag states) {
        return FIRE_MODEL_ID.get(states);
    }

    @Override
    public boolean hasMagAttachment(CompoundTag states) {
        return !(USING_AMMO_SOURCE.get(states).equals(states.getString("node_id")));
    }

    @Override
    public float getHeat(CompoundTag states) {
        return HEAT.get(states);
    }

    @Override
    public long getLastShootTime(CompoundTag states) {
        return LAST_SHOOT_TIME.get(states);
    }

    @Override
    public long getHeatLastUpdate(CompoundTag states) {
        return HEAT_LAST_UPDATE.get(states);
    }

    @Override
    public void onInitStates(CompoundTag states, String nodeId, String moduleId) {
        USING_SIGHT.init(states);
        USING_AMMO_SOURCE.init(states);
        LEFT_ARM_HOLD.init(states);
        RIGHT_ARM_HOLD.init(states);
        FIRE_MODEL_ID.init(states);
        STUCK.init(states);
        FIRE_SOUND_TYPE.init(states);
        HEAT.init(states);
        LAST_SHOOT_TIME.init(states);
        HEAT_LAST_UPDATE.init(states);
        HEAT_STUCK_RATIO.init(states);
    }

    @Override
    public void setAmmoLeft(int ammoLeft, CompoundTag states) {
        ammoLeft = Mth.clamp(ammoLeft, 0, getMaxCapacity());
        AMMO_LEFT.set(ammoLeft, states);
    }

    @Override
    public void onUpdate(StatesUpdateContext context) {
        if (!context.thisNodeIsRoot()) {
            return;
        }
        if (context.gun instanceof ISlottedGun) {
            List<ShadowNode> allNodesOfThisTree = context.getAllNodesOfThisTree();
            String oldSightId = context.get(USING_SIGHT);
            if (!context.hasNode(oldSightId)) {
                String usingSightID = findUsingSightID(allNodesOfThisTree, Integer.MIN_VALUE);
                context.set(USING_SIGHT, usingSightID);
            } else  {
                ShadowNode nodeById = context.getNodeById(oldSightId);
                Unit unit = nodeById.unit;
                if (unit.getModule() instanceof ISight sight &&
                        (sight.getSightPriority(unit) == ISight.GUN_BASE || sight.getSightPriority(unit) == ISight.IGNORE)) {
                    int sightPriority = sight.getSightPriority(unit);
                    String usingSightID = findUsingSightID(allNodesOfThisTree, Math.min(sightPriority, ISight.GUN_BASE));
                    context.set(USING_SIGHT, usingSightID);
                }
            }
            String usingAmmoSourceID = findUsingAmmoSourceID(allNodesOfThisTree, context);
            context.set(USING_AMMO_SOURCE, usingAmmoSourceID);
            String leftArmHoldID = findLeftArmHoldID(allNodesOfThisTree, context);
            context.set(LEFT_ARM_HOLD, leftArmHoldID);
            String rightArmHoldID = findRightArmHoldID(allNodesOfThisTree, context);
            context.set(RIGHT_ARM_HOLD, rightArmHoldID);
            int fireSoundType = decideFireSoundType(allNodesOfThisTree);
            context.set(FIRE_SOUND_TYPE, fireSoundType);
            float heatStuckRate = calcHeatStuckRate(allNodesOfThisTree);
            context.set(HEAT_STUCK_RATIO, heatStuckRate);
        } else {
            /*
              如果不是可装配件的枪械，瞄具模块直接默认使用当前节点,弹药源模块同理
              */
            context.set(USING_SIGHT, context.thisNodeId());
            context.set(USING_AMMO_SOURCE, context.thisNodeId());
            context.set(LEFT_ARM_HOLD, context.thisNodeId());
            context.set(RIGHT_ARM_HOLD, context.thisNodeId());
            context.set(FIRE_SOUND_TYPE, FIRE_SOUND_NORMAL);
            context.set(HEAT_STUCK_RATIO, 1f);
        }
        List<IFireMode<?>> fireModes = context.gun.getAllFireModes();
        if (fireModes != null && !fireModes.isEmpty()) {
            String s = context.get(FIRE_MODEL_ID);
            if (FIRE_MODEL_ID.getDefaultValue().equals(s)) {
                String name = fireModes.getFirst().getName();
                context.set(FIRE_MODEL_ID, name);
            }
        }
    }

    protected float calcHeatStuckRate(List<ShadowNode> nodes) {
        float maxFactor = 1.0f;
        for (ShadowNode node : nodes) {
            if (node.unit.getModule() instanceof IHeatSensitiveModular modular) {
                float heatSensitive = modular.getHeatSensitive();
                if (heatSensitive > maxFactor) {
                    maxFactor = heatSensitive;
                }
            }
        }
        return maxFactor;
    }

    protected String findRightArmHoldID(List<ShadowNode> nodes, StatesUpdateContext context) {
        return context.thisNodeId();
    }

    protected String findLeftArmHoldID(List<ShadowNode> nodes, StatesUpdateContext context) {
        return context.thisNodeId();
    }

    protected String findUsingAmmoSourceID(List<ShadowNode> nodes, StatesUpdateContext context) {
        return nodes.getFirst().nodeId;
    }

    protected int decideFireSoundType(List<ShadowNode> nodes) {
        for (ShadowNode node : nodes) {
            if (node.unit.getModule() instanceof Muzzle muzzle) {
                return muzzle.getFireSoundType();
            }
        }
        return FIRE_SOUND_NORMAL;
    }


    protected String findUsingSightID(List<ShadowNode> nodes, int maxSightPriority) {
        String id = nodes.getFirst().nodeId;
        for (ShadowNode node : nodes) {
            Unit unit = node.unit;
            if (unit == null || !(unit.getModule() instanceof ISight sight)) {
                continue;
            }
            int sightPriority = sight.getSightPriority(unit);
            if (sightPriority > maxSightPriority) {
                maxSightPriority = sightPriority;
                id = node.nodeId;
            }
        }
        return id;
    }


    @Override
    public final int getPriority(boolean rightArm) {
        return DEFAULT_PRIORITY;
    }

    @Override
    public @Nullable AdditionalPropModifier getModifier() {
        return null;
    }
}
