package com.sheridan.gcr.client.events;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.ClassHotReloader;
import com.sheridan.gcr.client.PhysicalMagazineInput;
import com.sheridan.gcr.client.ClientWeaponLooper;
import com.sheridan.gcr.client.DrawHolsterHandler;
import com.sheridan.gcr.client.KeyBinds;
import com.sheridan.gcr.client.SprintingHandler;
import com.sheridan.gcr.client.screen.DebugDisplayDataAdjustScreen;
import com.sheridan.gcr.client.screen.ldlib2Remake.GunModifyUI;
import com.sheridan.gcr.client.stuck.ClientGunStuckCache;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.items.MagazineItem;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.InputEvent;

import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ControllerEvents {
    public static boolean debug_i = false;
    public static int adsDelay = 0;

    @SubscribeEvent
    public static void onKeyBoardEvent(InputEvent.Key event) {
        //if (event.getAction() == 1) {
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
        //}
    }

    private static void handleGunFunctionalInput(IGun gunModule, ItemStack itemStack, InputEvent.Key event) {
        if (DrawHolsterHandler.get().getEquipProgress() < 1 || Minecraft.getInstance().screen != null) {
            return;
        }
        if (event.getAction() == 1) {
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
            // Physical-magazine R handling is performed by PhysicalMagazineInput.
            // GCR's original reload refills magazines without consuming items,
            // so it must not run alongside physical magazine transfers.
            if (KeyBinds.SWITCH_EFFECTIVE_SIGHT.isDown()) {
                if (gunModule instanceof ISlottedGun) {
                    IGunTask<?> task = gunModule.getTask(itemStack, IGunTask.TaskType.SWITCH_USING_SIGHT, Map.of());
                    if (task != null) {
                        task.start();
                    }
                }
            }
            // Physical-magazine mechanics: removal of a jam cannot be triggered by the old U key.
            // It requires holding R and is verified again by the server.
            if (KeyBinds.DEBUG_HOT_RELOAD_CLASS.isDown() && GCR.IS_DEVELOPMENT) {
                //ClassHotReloader.reload();
                //Client.DEBUG_ALWAYS_STUCK = !Client.DEBUG_ALWAYS_STUCK;
                //player.sendSystemMessage(Component.literal("Debug Always Stuck: " + Client.DEBUG_ALWAYS_STUCK).withColor(0xFF00FF));
            }
        }

        Map<String, Node> idToNodes = Client.WEAPON_STATUS.getIDToNodes();
        for (Map.Entry<String, Node> entry : idToNodes.entrySet()) {
            String id = entry.getKey();
            Node node = entry.getValue();
            Unit unit = node.getUnit();
            if (unit.getModule() instanceof IInteractiveModular iInteractiveModular) {
                iInteractiveModular.onKeyPressed(event.getKey(), event.getAction(), id, unit, gunModule, itemStack);
            }
        }
    }

    @SubscribeEvent
    public static void onTickPre(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (Client.LEFT_BUTTON_PRESSED.get()) {
            if (minecraft.screen != null) {
                Client.LEFT_BUTTON_PRESSED.set(false);
            }
        }
    }

    @SubscribeEvent
    public static void onTickPost(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        adsDelay = Math.max(0, adsDelay - 1);
        PhysicalMagazineInput.tick();
        // Physical-magazine mechanics: release ADS whenever mouse input is no longer valid.
        // In particular, opening a screen or losing focus may swallow the
        // physical right-button release event.
        if (!shouldHandleMouseEvent() || !Client.WEAPON_STATUS.isHoldingGun()) {
            Client.RIGHT_BUTTON_PRESSED.set(false);
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
                double scrollDeltaX = 0.0;
                double scrollDeltaY = event.getScrollDelta();
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

    // Suppress vanilla right-click use while physically unloading a magazine,
    // without preventing the use key from tracking whether RMB remains down.
    @SubscribeEvent
    public static void onMagazineUse(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.isUseItem() && mc.player != null && mc.screen == null
                && mc.player.getMainHandItem().getItem() instanceof MagazineItem) {
            event.setCanceled(true);
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
            if (btn >= 2 && action == 1) {
                // Mouse-bound GCR actions never generate an InputEvent.Key.
                // Dispatch press actions without intercepting the key mapping.
                dispatchRemappedMouseAction(btn);
            }
            if (action == 1) {//press
                if (btn == 0) {//left
                    Client.LEFT_BUTTON_PRESSED.set(true);
                    // 立刻叫醒武器线程，让第一发不用等它的下一个周期
                    ClientWeaponLooper.wake();
                } else if (btn == 1) {//right
                    // Physical-magazine mechanics: hold RMB to aim; do not toggle ADS on clicks.
                    if (adsDelay <= 0) {
                        Client.RIGHT_BUTTON_PRESSED.set(true);
                    }
                }
                // Only intercept primary firearm attack/use buttons.
                // Canceling MMB here prevents vanilla/Forge from updating
                // controls remapped to middle or other mouse buttons.
                if (btn == 0 || btn == 1) event.setCanceled(true);
            } else if (action == 0) {//up
                if (btn == 0) {//left
                    Client.LEFT_BUTTON_PRESSED.set(false);
                    Client.WEAPON_STATUS.fireCount = 0;
                } else if (btn == 1) {//right
                    // Releasing RMB must always end ADS, even during adsDelay.
                    Client.RIGHT_BUTTON_PRESSED.set(false);
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

    private static void dispatchRemappedMouseAction(int button) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem weapon)) return;
        IGun gun = weapon.getGun();
        if (GCR.IS_DEVELOPMENT && KeyBinds.OPEN_DISPLAY_ADJUST_SCREEN.matchesMouse(button)) {
            mc.setScreen(new DebugDisplayDataAdjustScreen());
            return;
        }
        if (KeyBinds.OPEN_GUN_MODIFY_SCREEN.matchesMouse(button)) {
            GunModifyUI.open();
            return;
        }
        if (DrawHolsterHandler.get().getEquipProgress() < 1) return;
        if (KeyBinds.CHECK_MAG.matchesMouse(button) && !SprintingHandler.INSTANCE.isSprinting()) {
            IGunTask<?> task = gun.getTask(stack, IGunTask.TaskType.CHECKING,
                    Map.of("type", CheckingTask.CHECK_MAG));
            if (task != null) GunTaskHandler.INSTANCE.setTask(task);
        }
        if (KeyBinds.CHECK_CHAMBER.matchesMouse(button)) {
            IGunTask<?> task = gun.getTask(stack, IGunTask.TaskType.CHECKING,
                    Map.of("type", CheckingTask.CHECK_CHAMBER));
            if (task != null) GunTaskHandler.INSTANCE.setTask(task);
        }
        if (KeyBinds.SWITCH_FIRE_MODE.matchesMouse(button)) {
            IGunTask<?> task = gun.getTask(stack, IGunTask.TaskType.SWITCH_FIRE_MODE, Map.of());
            if (task != null) GunTaskHandler.INSTANCE.setTask(task);
        }
        if (KeyBinds.SWITCH_EFFECTIVE_SIGHT.matchesMouse(button)
                && gun instanceof ISlottedGun) {
            IGunTask<?> task = gun.getTask(stack, IGunTask.TaskType.SWITCH_USING_SIGHT, Map.of());
            if (task != null) task.start();
        }
    }

    private static boolean shouldHandleMouseEvent() {
        return Minecraft.getInstance().isWindowActive() && !Minecraft.getInstance().isPaused() && Minecraft.getInstance().screen == null;
    }
}
