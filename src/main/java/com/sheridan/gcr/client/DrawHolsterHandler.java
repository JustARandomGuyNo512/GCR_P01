package com.sheridan.gcr.client;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.mixinUtils.DualHandItemAccessor;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class DrawHolsterHandler {

    private static final DrawHolsterHandler INSTANCE = new DrawHolsterHandler();

    public static DrawHolsterHandler get() {
        return INSTANCE;
    }

    public enum State {
        IDLE,
        HOLSTERING,
        DRAWING
    }

    private State state = State.IDLE;

    private float equipProgress = 1.0f;

    private float equipProgressLast = 1.0f;

    private ItemStack prevStack = ItemStack.EMPTY;

    // 当前正在渲染/操作的枪
    private ItemStack currentStack = ItemStack.EMPTY;

    // 目标枪（holster结束后切换）
    private ItemStack targetStack = ItemStack.EMPTY;

    // render锁
    private ItemStack renderLockedStack = ItemStack.EMPTY;

    private float timer = 0f;
    private float duration = 1f;

    private DrawHolsterHandler() {}

    // =========================
    // Tick
    // =========================
    public void tick(ItemStack newStack) {
        boolean prevIsGun = isGun(prevStack, "prev");
        boolean currIsGun = isGun(newStack, "curr");
        if (!prevIsGun && currIsGun) {
            handleNonGunToGun(newStack);
        } else if (prevIsGun && !currIsGun) {
            handleGunToNonGun();
        } else if (prevIsGun && currIsGun) {
            if (isGunChanged(prevStack, newStack)) {
                handleGunToGun(newStack);
            }
        }

        updateProgress();

        prevStack = newStack;
    }

    private void handleNonGunToGun(ItemStack newStack) {
        startDraw(newStack);
    }

    private void handleGunToNonGun() {
        startHolster(prevStack, ItemStack.EMPTY);
        Client.exitAds();
    }

    private void handleGunToGun(ItemStack newStack) {
        startHolster(prevStack, newStack);
        Client.exitAds();
    }

    private void updateProgress() {

        if (state == State.IDLE) {
            equipProgressLast = equipProgress;
            return;
        }

        // ✅ 先保存上一帧
        equipProgressLast = equipProgress;

        timer += 1f;
        float t = timer / duration;

        if (state == State.HOLSTERING) {

            equipProgress = 1f - t;

            if (equipProgress <= 0f) {
                equipProgress = 0f;

                // 收枪完成 → 切枪
                currentStack = targetStack;

                if (isGun(targetStack, "target")) {
                    startDraw(targetStack);
                } else {
                    state = State.IDLE;
                }
            }

        } else if (state == State.DRAWING) {

            equipProgress = t;

            if (equipProgress >= 1f) {
                equipProgress = 1f;
                state = State.IDLE;
            }
        }
    }

    private void startHolster(ItemStack from, ItemStack to) {
        state = State.HOLSTERING;
        timer = 0f;
        equipProgressLast = equipProgress;
        duration = getHolsterDuration(from);
        renderLockedStack = from;
        targetStack = to;
        onHolsterStart(from);
    }

    public State getState() {
        return state;
    }

    private void startDraw(ItemStack stack) {
        state = State.DRAWING;
        timer = 0f;
        duration = getDrawDuration(stack);
        renderLockedStack = stack;
        equipProgressLast = equipProgress;
    }


    public void onRenderTick(ItemInHandRenderer renderer) {

        if (!(renderer instanceof DualHandItemAccessor accessor)) {
            return;
        }
        if (equipProgress > 0f && equipProgress < 1f) {
            if (state == State.HOLSTERING) {
                accessor.setMainHandItem(renderLockedStack);
            } else if (state == State.DRAWING) {
                accessor.setMainHandItem(renderLockedStack);
            }
        }
    }

    public ItemStack getRenderLockedStack() {
        return renderLockedStack;
    }


    private boolean isGun(ItemStack stack, String debugMsg) {
        if (!stack.isEmpty() && stack.getItem() instanceof GunItem gunItem) {
            return true;
        }
        return false;
    }

    public float getEquipProgress() {
        return equipProgress;
    }

    public float getEquipProgress(float partialTicks) {
        return equipProgressLast + (equipProgress - equipProgressLast) * partialTicks;
    }

    public boolean isBusy() {
        return state != State.IDLE;
    }

    private void onHolsterStart(ItemStack stack) {
        Client.getGunRenderer().dispatchAnimationEvent(EventType.HOLSTER);
    }


    private float getHolsterDuration(ItemStack stack) {
        float speedFactor = getSpeedFactor();
        return Mth.clamp(4.7f * speedFactor, 4, 8);
    }

    private float getSpeedFactor() {
        float agility = Client.WEAPON_STATUS.getAgility();
        float weight = Client.WEAPON_STATUS.getWeight();
        float k = agility / weight;
        k = (float) Math.pow(k, 0.75f);
        float factor = (float) (- Math.exp(-k) + 1);
        return 0.5f + factor;
    }

    private float getDrawDuration(ItemStack stack) {
        float speedFactor = getSpeedFactor();
        return Mth.clamp(9.2f * speedFactor, 5, 20);
    }

    private boolean isGunChanged(ItemStack prevStack, ItemStack newStack) {
        if (prevStack.getItem() != newStack.getItem()) {
            return true;
        } else {
            IGun prevGun = ((GunItem) prevStack.getItem()).getGun();
            IGun newGun = ((GunItem) newStack.getItem()).getGun();
            if (prevGun != newGun) {
                return true;
            }
            String prevID = prevGun.getIdentityID(prevStack);
            String newID = newGun.getIdentityID(newStack);
            return !Objects.equals(prevID, newID);
        }
    }
}