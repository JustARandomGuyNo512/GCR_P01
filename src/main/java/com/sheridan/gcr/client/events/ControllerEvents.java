package com.sheridan.gcr.client.events;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.DrawHolsterHandler;
import com.sheridan.gcr.client.KeyBinds;
import com.sheridan.gcr.client.SprintingHandler;
import com.sheridan.gcr.client.recoil.ClassHotReloader;
import com.sheridan.gcr.client.screen.DebugDisplayDataAdjustScreen;
import com.sheridan.gcr.client.screen.ldlib2Remake.GunModifyUI;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.modularSys.builder.Node;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.modules.IInteractiveModular;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.modules.guns.ISlottedGun;
import com.sheridan.gcr.modularSys.task.GunTaskHandler;
import com.sheridan.gcr.modularSys.task.IGunTask;
import com.sheridan.gcr.modularSys.task.other.CheckingTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ControllerEvents {
    public static boolean debug_i = false;
    @SubscribeEvent
    public static void onKeyBoardEvent(InputEvent.Key event) {
        if (event.getAction() == 1) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            ItemStack itemStack = player.getMainHandItem();
            if (itemStack.getItem() instanceof GunItem gunItem) {
                IGun gunModule = gunItem.getGun();
                if (GCR.IS_DEVELOPMENT && KeyBinds.OPEN_DISPLAY_ADJUST_SCREEN.isDown()) {
                    Minecraft.getInstance().setScreen(new DebugDisplayDataAdjustScreen());
                }
                if (KeyBinds.OPEN_GUN_MODIFY_SCREEN.isDown()) {
                    GunModifyUI.open();
                }
                handleGunFunctionalInput(gunModule, itemStack, event);
            }
        }
    }

    private static void handleGunFunctionalInput(IGun gunModule, ItemStack itemStack, InputEvent.Key event) {
        if (DrawHolsterHandler.get().getEquipProgress() < 1) {
            return;
        }
        if (KeyBinds.CHECK_MAG.isDown() && !SprintingHandler.INSTANCE.isSprinting()) {
            IGunTask<?> task = gunModule.getTask(itemStack, IGunTask.TaskType.CHECKING, Map.of("type", CheckingTask.CHECK_MAG));
            if (task != null) {
                GunTaskHandler.INSTANCE.setTask(task);
            }
        }
        if (KeyBinds.CHECK_CHAMBER.isDown()) {
            IGunTask<?> task = gunModule.getTask(itemStack, IGunTask.TaskType.CHECKING, Map.of("type", CheckingTask.CHECK_CHAMBER));
            if (task != null) {
                GunTaskHandler.INSTANCE.setTask(task);
            }
        }
        if (KeyBinds.SWITCH_FIRE_MODE.isDown()) {
            IGunTask<?> task = gunModule.getTask(itemStack, IGunTask.TaskType.SWITCH_FIRE_MODE, Map.of());
            if (task != null) {
                GunTaskHandler.INSTANCE.setTask(task);
            }
        }
        if (KeyBinds.RELOAD.isDown()) {
            if (gunModule.isStuck(itemStack)) {
                IGunTask<?> task = gunModule.getTask(itemStack, IGunTask.TaskType.REMOVE_STUCK, Map.of());
                if (task != null) {
                    GunTaskHandler.INSTANCE.setTask(task);
                }
                return;
            }
            IGunTask<?> task = gunModule.getTask(itemStack, IGunTask.TaskType.RELOAD, Map.of());
            if (task != null) {
                GunTaskHandler.INSTANCE.setTask(task);
            }
        }
        if (KeyBinds.SWITCH_EFFECTIVE_SIGHT.isDown()) {
            if (gunModule instanceof ISlottedGun) {
                IGunTask<?> task = gunModule.getTask(itemStack, IGunTask.TaskType.SWITCH_USING_SIGHT, Map.of());
                if (task != null) {
                    task.start();
                }
            }
        }
        if (KeyBinds.REMOVE_STUCK.isDown()) {
            IGunTask<?> task = gunModule.getTask(itemStack, IGunTask.TaskType.REMOVE_STUCK, Map.of());
            if (task != null) {
                GunTaskHandler.INSTANCE.setTask(task);
            }
        }
        if (KeyBinds.DEBUG_HOT_RELOAD_CLASS.isDown() && GCR.IS_DEVELOPMENT) {
            //ClassHotReloader.reload();
            //Client.DEBUG_ALWAYS_STUCK = !Client.DEBUG_ALWAYS_STUCK;
            //player.sendSystemMessage(Component.literal("Debug Always Stuck: " + Client.DEBUG_ALWAYS_STUCK).withColor(0xFF00FF));
        }

        Map<String, Node> idToNodes = Client.WEAPON_STATUS.getIDToNodes();
        for (Map.Entry<String, Node> entry : idToNodes.entrySet()) {
            String id = entry.getKey();
            Node node = entry.getValue();
            Unit unit = node.getUnit();
            if (unit.getModule() instanceof IInteractiveModular iInteractiveModular) {
                iInteractiveModular.onKeyPressed(event.getKey(), id, unit, gunModule, itemStack);
            }
        }
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (Client.LEFT_BUTTON_PRESSED.get()) {
            if (minecraft.screen != null) {
                Client.LEFT_BUTTON_PRESSED.set(false);
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (shouldHandleMouseEvent() && Client.WEAPON_STATUS.isHoldingGun()) {
            ItemStack itemStack = Client.WEAPON_STATUS.getItemStack();
            IGun gun = Client.WEAPON_STATUS.getGun();
            if (gun != null && itemStack != null) {
                double mouseX = event.getMouseX();
                double mouseY = event.getMouseY();
                double scrollDeltaX = event.getScrollDeltaX();
                double scrollDeltaY = event.getScrollDeltaY();
                Map<String, Node> idToNodes = Client.WEAPON_STATUS.getIDToNodes();
                for (Map.Entry<String, Node> entry : idToNodes.entrySet()) {
                    String id = entry.getKey();
                    Node node = entry.getValue();
                    Unit unit = node.getUnit();
                    if (unit.getModule() instanceof IInteractiveModular iInteractiveModular) {
                        boolean cancel = iInteractiveModular.onMouseScroll(mouseX, mouseY, scrollDeltaX, scrollDeltaY, id, unit, gun, itemStack);
                        if (cancel) {
                            event.setCanceled(true);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMousePress(InputEvent.MouseButton.Pre event) {
        if (!shouldHandleMouseEvent()) {
            return;
        }
        int action = event.getAction();
        int btn = event.getButton();

        if (Client.WEAPON_STATUS.isHoldingGun()) {
            if (action == 1) {//press
                if (btn == 0) {//left
                    Client.LEFT_BUTTON_PRESSED.set(true);
                } else if (btn == 1) {//right
                    boolean pressed = Client.RIGHT_BUTTON_PRESSED.get();
                    Client.RIGHT_BUTTON_PRESSED.set(!pressed);
                }
                event.setCanceled(true);
            } else if (action == 0) {//up
                if (btn == 0) {//left
                    Client.LEFT_BUTTON_PRESSED.set(false);
                    Client.WEAPON_STATUS.fireCount = 0;
                }
            }
            IGun gun = Client.WEAPON_STATUS.getGun();
            ItemStack itemStack = Client.WEAPON_STATUS.getItemStack();
            if (gun == null || itemStack == null) {
                return;
            }
            Map<String, Node> idToNodes = Client.WEAPON_STATUS.getIDToNodes();
            for (Map.Entry<String, Node> entry : idToNodes.entrySet()) {
                String id = entry.getKey();
                Node node = entry.getValue();
                Unit unit = node.getUnit();
                if (unit.getModule() instanceof IInteractiveModular iInteractiveModular) {
                    iInteractiveModular.onMousePressed(btn, action, id, unit, gun, itemStack);
                }
            }
        }
    }

    private static boolean shouldHandleMouseEvent() {
        return Minecraft.getInstance().isWindowActive() && !Minecraft.getInstance().isPaused() && Minecraft.getInstance().screen == null;
    }
}
