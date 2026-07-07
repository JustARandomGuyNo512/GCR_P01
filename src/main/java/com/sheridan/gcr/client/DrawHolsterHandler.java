package com.sheridan.gcr.client;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.animation.AnimationHandler;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.mixinUtils.DualHandItemAccessor;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

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
    private int lastSelected = -1;
    private State state = State.IDLE;

    private float equipProgress = 0;
    private float equipProgressLast = 0;

    private ItemStack prevStack = ItemStack.EMPTY;
    private ItemStack currentStack = ItemStack.EMPTY;
    private ItemStack targetStack = ItemStack.EMPTY;

    private ItemStack renderLockedStack = ItemStack.EMPTY;
    // ✅ 新增：用于在渲染线程记录上一帧的锁定物品
    private ItemStack lastRenderLockedStack = ItemStack.EMPTY;

    private float timer = 0f;
    private float duration = 1f;

    private DrawHolsterHandler() {}

    // =========================
    // Tick
    // =========================
    public void tick(ItemStack newStack, int selectedSlot) {
        boolean prevIsGun = isGun(prevStack, "prev");
        boolean currIsGun = isGun(newStack, "curr");

        if (!prevIsGun && currIsGun) {
            handleNonGunToGun(newStack);
        } else if (prevIsGun && !currIsGun) {
            handleGunToNonGun();
        } else if (prevIsGun) {
            if (lastSelected != selectedSlot) {
                handleGunToGun(newStack);
            } else if (isGunChanged(prevStack, newStack)) {
                handleGunToGun(newStack);
            }
        } else {
            if (equipProgress > 0f && state != State.HOLSTERING) {
                startHolster(prevStack, ItemStack.EMPTY);
            }
        }

        updateProgress();
        lastSelected = selectedSlot;
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

        equipProgressLast = equipProgress;
        timer += 1f;
        float t = timer / duration;

        if (state == State.HOLSTERING) {
            equipProgress = 1f - t;

            if (equipProgress <= 0f) {
                equipProgress = 0f;
                currentStack = targetStack;

                if (isGun(targetStack, "target")) {
                    startDraw(targetStack);
                } else {
                    state = State.IDLE;
                    renderLockedStack = ItemStack.EMPTY;
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
        duration = isGun(from, "from") ? getHolsterDuration(from) : 4.0f;
        renderLockedStack = from;
        targetStack = to;
        if (isGun(from, "from")) {
            onHolsterStart(from);
        }
    }

    public State getState() {
        return state;
    }

    private void startDraw(ItemStack stack) {
        AnimationHandler.INSTANCE.clearAllAnimation();
        state = State.DRAWING;
        timer = 0f;
        duration = getDrawDuration(stack);
        renderLockedStack = stack;
        equipProgressLast = equipProgress;
        Client.getGunRenderer().dispatchAnimationEvent(EventType.DRAW);
    }


    public void onRenderTick(ItemInHandRenderer renderer) {
        if (isRenderLockedStackChanged(lastRenderLockedStack, renderLockedStack)) {
            AnimationHandler.INSTANCE.clearAllAnimation();
            lastRenderLockedStack = renderLockedStack;
        }
        if (!(renderer instanceof DualHandItemAccessor accessor)) {
            return;
        }
        if (equipProgress > 0f && equipProgress < 1f && isGun(renderLockedStack, "render")) {
            accessor.setMainHandItem(renderLockedStack);
        }
    }


    private boolean isRenderLockedStackChanged(ItemStack oldStack, ItemStack newStack) {
        if (oldStack == newStack) {
            return false;
        }
        if (oldStack.isEmpty() != newStack.isEmpty()) {
            return true;
        }
        if (oldStack.isEmpty()) {
            return false;
        }

        if (oldStack.getItem() != newStack.getItem()) {
            return true;
        }

        if (oldStack.getItem() instanceof GunItem && newStack.getItem() instanceof GunItem) {
            IGun oldGun = ((GunItem) oldStack.getItem()).getGun();
            IGun newGun = ((GunItem) newStack.getItem()).getGun();
            return oldGun != newGun;
        }

        return false;
    }

    public ItemStack getRenderLockedStack() {
        return renderLockedStack;
    }

    private boolean isGun(ItemStack stack, String debugMsg) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof GunItem;
    }

    public float getEquipProgress() {
        return equipProgress;
    }

    public float getEquipProgress(float partialTicks) {
        return Mth.lerp(partialTicks, equipProgressLast, equipProgress);
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
            return prevGun != newGun;
        }
    }
}