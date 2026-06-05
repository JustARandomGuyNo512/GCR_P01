package com.sheridan.gcr.modularSys.modules.guns;

import com.sheridan.gcr.client.recoil.RecoilData;
import com.sheridan.gcr.items.DisplayData;
import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.builder.IReadOnlyTree;
import com.sheridan.gcr.modularSys.fire.IFireMode;
import com.sheridan.gcr.modularSys.modules.IAmmoSource;
import com.sheridan.gcr.modularSys.modules.IArmHandlerModular;
import com.sheridan.gcr.modularSys.modules.IStateModular;
import com.sheridan.gcr.modularSys.modules.gunProperties.IProperties;
import com.sheridan.gcr.modularSys.modules.gunProperties.impl.BaseProperties;
import com.sheridan.gcr.modularSys.modules.states.Bool;
import com.sheridan.gcr.modularSys.modules.states.Str;
import com.sheridan.gcr.modularSys.modules.views.IGunView;
import com.sheridan.gcr.modularSys.task.IGunTask;
import com.sheridan.gcr.network.c2s.GunFirePacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public interface IGun extends IModular, IAmmoSource, IGunView, IStateModular {
    String NONE = "__none__";
    String MODULE_DATE_KEY = "gcr_module_date";
    String IDENTITY_ID_KEY = "gcr_identity_id";
    String MODIFY_ID_KEY = "gcr_modify_id";
    String ROOT_NODE_ID_KEY = "gcr_root_node_id";

    String PROPERTIES_KEY = "gcr_properties";
    String MODULES_KEY = "gcr_modules";
    String STATES_KEY = "gcr_states";

    String DATA_CHANGED_KEY = "gcr_data_changed";

    Str USING_SIGHT = new Str("using_sight");
    Str USING_AMMO_SOURCE = new Str("using_ammo_source");
    Str LEFT_ARM_HOLD = new Str("left_arm_hold");
    Str RIGHT_ARM_HOLD = new Str("right_arm_hold");
    Str FIRE_MODEL_ID = new Str("fire_mode_id", "none");
    Bool STUCK = new Bool("stuck");

    void serverShoot(LivingEntity entity, ItemStack itemStack, int shootID, GunFirePacket packet);

    void fullSyncFromServer(ItemStack itemStack, CompoundTag data);

    @OnlyIn(Dist.CLIENT)
    void clientShoot(Player player, ItemStack itemStack);

    @OnlyIn(Dist.CLIENT)
    void serverShootAck(Vector3f bulletDirection, ItemStack itemStack, int shootID, int realAmmoLeft);

    DisplayData getDisplayData();

    long getDataDate(ItemStack itemStack);

    void setDataDate(ItemStack itemStack, long dataDate);

    void reload(ItemStack itemStack, Player player);

    void serverInitData(ItemStack itemStack);

    CompoundTag checkAndGet(ItemStack itemStack);

    CompoundTag checkAndGetRaw(ItemStack itemStack);

    ListTag getModulesTag(ItemStack itemStack);

    CompoundTag getPropertiesTag(ItemStack itemStack);

    String rootNodeId(ItemStack itemStack);

    CompoundTag rootNodeTag(ItemStack itemStack);


    @Nullable
    CompoundTag getNodeStatesTag(ItemStack itemStack, String nodeId);
    CompoundTag getAmmoSourceTag(ItemStack itemStack);
    CompoundTag getStatesTag(ItemStack itemStack);


    void setStatesTag(ItemStack itemStack, CompoundTag states);

    void setModulesTag(ItemStack itemStack, ListTag nodes);

    void setPropertiesTag(ItemStack itemStack, CompoundTag properties);

    //唯一物品id，用于判断是否切换枪械
    String getIdentityID(ItemStack itemStack);


    //模块改动id
    String getModifyID(ItemStack itemStack);

    void mutateModifyID(ItemStack itemStack, String modifyID, boolean copy);

    int getRpm(ItemStack itemStack);

    boolean isStuck(ItemStack itemStack);

    void removeStuck(ItemStack itemStack);

    void setStuck(boolean stuck, CompoundTag states);

    IFireMode<?> getFireMode(ItemStack itemStack);
    List<IFireMode<?>> getAllFireModes();
    void setFireMode(ItemStack itemStack, IFireMode<?> fireMode);
    void toNextFireMode(ItemStack itemStack);


    String getUsingSightID(ItemStack itemStack);

    String getLeftArmHoldID(ItemStack itemStack);
    String getRightArmHoldID(ItemStack itemStack);

    void setUsingSightID(ItemStack itemStack, String sightID);

    int getAmmoLeft(ItemStack itemStack);

    void setAmmoLeft(ItemStack itemStack, int ammoLeft);

    @Nullable
    IGunTask<?> getTask(ItemStack itemStack, IGunTask.TaskType type, Map<String, Object> args);

    RecoilData getRecoilData();

    Map<String, IProperties> getProperties();


    void notifyDataChanged(ItemStack itemStack);

    boolean dataChanged(ItemStack itemStack);

    void onReceiveStatesDataFormClient(CompoundTag payLoad, ItemStack itemStack, String nodeId, Player player);

    CompoundTag reCalculateProperties(IReadOnlyTree warehouse);

    BaseProperties getBaseProperties();


    @Nullable
    IArmHandlerModular getLeftArmHolding(ItemStack itemStack);
    @Nullable
    IArmHandlerModular getRightArmHolding(ItemStack itemStack);

    @Nullable
    IArmHandlerModular.AdditionalPropModifier getLeftArmModifier(ItemStack itemStack);
    @Nullable
    IArmHandlerModular.AdditionalPropModifier getRightArmModifier(ItemStack itemStack);

    float getImpulseRatio(ItemStack itemStack);

    float getCurrentWeight(ItemStack itemStack);

    float getAimingSpeed(ItemStack itemStack);

    float getFaultRate(ItemStack itemStack);

    float getRecoilControl(ItemStack itemStack);

    float getStability(ItemStack itemStack);

    float getAgility(ItemStack itemStack);

    boolean isPistol();

    float getSpread(ItemStack itemStack);
}
