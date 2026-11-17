package com.sheridan.gcr.client.events;


import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.MagLoadingHandler;
import com.sheridan.gcr.client.DrawHolsterHandler;
import com.sheridan.gcr.client.GunEffect;
import com.sheridan.gcr.client.GunEffectManager;
import com.sheridan.gcr.client.SprintingHandler;
import com.sheridan.gcr.client.animation.AnimationHandler;
import com.sheridan.gcr.client.render.GunPoseHandler;
import com.sheridan.gcr.client.render.HardCodeAnimationHandler;
import com.sheridan.gcr.client.render.Shaders;
import com.sheridan.gcr.client.stuck.ClientGunStuckCache;
import com.sheridan.gcr.events.LivingFireEvent;
import com.sheridan.gcr.modularSys.task.GunTaskHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

import java.util.concurrent.locks.ReentrantLock;


@OnlyIn(Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(ClientTickEvent.Pre event) {
        if (Minecraft.getInstance().screen == null) {
            Client.getGunRenderer().setHideFPRender(false);
        }
        // 上一个 tick 如果在持锁期间异常退出，Post 就不会再被派发，这把锁会永远留在主线程手里，
        // 武器线程随即永久阻塞在 Client.LOCK.lock() 上——整局都开不了火。进临界区之前先把
        // 自己残留的持有次数还清；正常情况下这里本来就持有 0 次，是个空操作。
        releaseTickLock();
        Client.LOCK.lock();
        try {
            AnimationHandler.INSTANCE.onClientTick();
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                Client.LOCAL_PLAYER_ID = player.getId();
                Client.WEAPON_STATUS.onTickStart(player);
                SprintingHandler.INSTANCE.tick(player);
                GunTaskHandler.INSTANCE.tick(player);
            }
        } catch (Throwable t) {
            // 本 tick 提前失败：立刻归还，不能让异常把锁带走
            releaseTickLock();
            throw t;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(ClientTickEvent.Post event) {
        try {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                MagLoadingHandler.tickClient(player);
                Client.WEAPON_STATUS.onTickEnd(player);
                GunPoseHandler.INSTANCE.tick(player);
                Client.getGunRenderer().tick(player);
                HardCodeAnimationHandler.getInstance().clientTick(player);
                DrawHolsterHandler.get().tick(player, player.getMainHandItem(), player.getInventory().selected);
                // 客户端卡壳缓存：超时的本地预测自愈 + 把信念投影回手持枪的 states。
                // 换维度不会中断它（同一把枪、同一个 identityID），只有断线才清空。
                ClientGunStuckCache.get().tick(player);
            }
            // 刷新开火线程的无锁预检：没拿枪 / 拔枪动画没完 / 换弹 / 冲刺都在这里体现，
            // 让武器线程平时不必为了「能不能开火」去抢这把锁。
            Client.WEAPON_STATUS.updateFireBlocked();
        } finally {
            releaseTickLock();
        }
        //ModularModel.debugHeat = Math.max(0, ModularModel.debugHeat - 0.005f);
    }

    /**
     * 归还当前线程（客户端主线程）持有的 tick 锁。
     *
     * <p>按持有次数归还而不是只 unlock 一次：{@code ClientModelHotReloader} 之类会在 tick 里
     * 重入这把锁，异常路径下可能留下多层持有。</p>
     */
    private static void releaseTickLock() {
        ReentrantLock lock = Client.LOCK;
        for (int holds = lock.getHoldCount(); holds > 0; holds = lock.getHoldCount()) {
            lock.unlock();
        }
    }

    /**
     * 断线时清空客户端卡壳缓存。
     *
     * <p>真实卡壳由服务端保存在物品数据里，重进后随背包同步回来，既有 NBT 读取路径照常显示；
     * 反向把本地预测带过连接边界才是危险的——那个预测可能永远等不到确认，就变成跨存档的虚假卡壳。</p>
     */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientGunStuckCache.get().clearAll();
    }

    public static void registerCustomVanillaShader(RegisterShadersEvent event) {
        Shaders.init(event.getResourceProvider());
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getId() == entity.getId()) {
            Client.WEAPON_STATUS.onPlayerJump();
        }
    }

    @SubscribeEvent
    public static void onOtherLivingFire(LivingFireEvent event) {
        GunEffectManager.updateEffectTimestamp(
                event.getEntity().getId(),
                GunEffect.SHOOT,
                event.getFireModuleId(),
                System.currentTimeMillis() - event.getShooterLatency()
        );
    }

// Pixel coordinates (0..15) and normalized clockwise angles (0.0..1.0) for a 16x16 circular loading ring
    private static final int[] RING_X = new int[] {6, 7, 8, 4, 5, 6, 7, 8, 9, 10, 3, 4, 5, 9, 10, 11, 2, 3, 4, 10, 11, 12, 2, 3, 11, 12, 1, 2, 12, 13, 1, 2, 12, 13, 1, 2, 12, 13, 2, 3, 11, 12, 2, 3, 4, 10, 11, 12, 3, 4, 5, 9, 10, 11, 4, 5, 6, 7, 8, 9, 10, 6, 7, 8};
    private static final int[] RING_Y = new int[] {1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 8, 9, 9, 9, 9, 10, 10, 10, 10, 10, 10, 11, 11, 11, 11, 11, 11, 12, 12, 12, 12, 12, 12, 12, 13, 13, 13};
    private static final float[] RING_ANGLE = new float[] {0.9737f, 0.0f, 0.0263f, 0.914f, 0.9394f, 0.9686f, 0.0f, 0.0314f, 0.0606f, 0.086f, 0.875f, 0.8976f, 0.9262f, 0.0738f, 0.1024f, 0.125f, 0.836f, 0.8524f, 0.875f, 0.125f, 0.1476f, 0.164f, 0.8106f, 0.8238f, 0.1762f, 0.1894f, 0.7763f, 0.7814f, 0.2186f, 0.2237f, 0.75f, 0.75f, 0.25f, 0.25f, 0.7237f, 0.7186f, 0.2814f, 0.2763f, 0.6894f, 0.6762f, 0.3238f, 0.3106f, 0.664f, 0.6476f, 0.625f, 0.375f, 0.3524f, 0.336f, 0.625f, 0.6024f, 0.5738f, 0.4262f, 0.3976f, 0.375f, 0.586f, 0.5606f, 0.5314f, 0.5f, 0.4686f, 0.4394f, 0.414f, 0.5263f, 0.5f, 0.4737f};

    // Colors (ARGB)
    private static final int COLOR_TRACK = (75 << 24) | 0xFFFFFF;  // 30% semi-transparent white
    private static final int COLOR_ACTIVE = (235 << 24) | 0xFFFFFF; // 92% bright semi-transparent white

    public static void registerItemDecorations(net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent event) {
        net.neoforged.neoforge.client.IItemDecorator decorator = (graphics, font, stack, xOffset, yOffset) -> {
            if (MagLoadingHandler.isGuiLoading() && MagLoadingHandler.isTargetStack(stack)) {
                float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
                float progress = MagLoadingHandler.getProgress(partialTick);
                renderLoadingRingAt(graphics, xOffset, yOffset, progress);
            }
            return false;
        };

        for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item instanceof com.sheridan.gcr.items.MagItem) {
                event.register(item, decorator);
            }
        }
    }

    @SubscribeEvent
    public static void onContainerScreenForeground(net.neoforged.neoforge.client.event.ContainerScreenEvent.Render.Foreground event) {
        if (!MagLoadingHandler.isGuiLoading()) {
            return;
        }

        net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen = event.getContainerScreen();
        int slotIndex = MagLoadingHandler.getLoadingSlotIndex();
        if (slotIndex < 0 || slotIndex >= screen.getMenu().slots.size()) {
            return;
        }

        net.minecraft.world.inventory.Slot slot = screen.getMenu().slots.get(slotIndex);
        if (slot == null || !(slot.getItem().getItem() instanceof com.sheridan.gcr.items.MagItem)) {
            return;
        }

        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        float progress = MagLoadingHandler.getProgress(partialTick);
        renderLoadingRingAt(event.getGuiGraphics(), slot.x, slot.y, progress);
    }

    public static void renderLoadingRingAt(net.minecraft.client.gui.GuiGraphics graphics, int slotX, int slotY, float progress) {
        float clamped = Math.min(1.0f, Math.max(0.0f, progress));
        net.minecraft.client.renderer.RenderType overlayType = net.minecraft.client.renderer.RenderType.guiOverlay();

        for (int i = 0; i < RING_X.length; i++) {
            int px = slotX + RING_X[i];
            int py = slotY + RING_Y[i];
            float angle = RING_ANGLE[i];

            boolean isActive = clamped > 0.005f && angle <= clamped;
            int color = isActive ? COLOR_ACTIVE : COLOR_TRACK;

            graphics.fill(overlayType, px, py, px + 1, py + 1, color);
        }
    }

    @SubscribeEvent
    public static void onScreenMouseClicked(net.neoforged.neoforge.client.event.ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> containerScreen) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) return;

            net.minecraft.world.inventory.Slot slot = containerScreen.getSlotUnderMouse();
            if (slot == null || !slot.hasItem()) return;

            // If clicking the slot currently loading: stop loading
            if (MagLoadingHandler.isGuiLoading() && MagLoadingHandler.getLoadingSlotIndex() == slot.index) {
                event.setCanceled(true);
                MagLoadingHandler.stopLoading(true);
                return;
            }

            net.minecraft.world.item.ItemStack carried = containerScreen.getMenu().getCarried();
            net.minecraft.world.item.ItemStack slotItem = slot.getItem();

            if (carried.getItem() instanceof com.sheridan.gcr.items.AmmoItem ammoItem && slotItem.getItem() instanceof com.sheridan.gcr.items.MagItem magItem) {
                if (magItem.acceptsAmmo(ammoItem)) {
                    int currentAmmo = com.sheridan.gcr.items.MagItem.getAmmoLeft(slotItem);
                    int maxCap = magItem.getModule().getMaxCapacity();
                    if (currentAmmo >= maxCap) {
                        return;
                    }

                    event.setCanceled(true);

                    // Clear carried ammo back to player inventory
                    net.minecraft.world.item.ItemStack ammoToReturn = carried.copy();
                    containerScreen.getMenu().setCarried(net.minecraft.world.item.ItemStack.EMPTY);
                    if (!player.getInventory().add(ammoToReturn)) {
                        player.drop(ammoToReturn, false);
                    }

                    // Start GUI loading without closing inventory
                    MagLoadingHandler.startGuiLoading(slot.index, magItem.getCaliber(), slotItem);
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                            new com.sheridan.gcr.network.c2s.MagLoadPacket(com.sheridan.gcr.network.c2s.MagLoadPacket.ACTION_START_GUI, magItem.getCaliber(), slot.index, false)
                    );
                }
            } else if (carried.getItem() instanceof com.sheridan.gcr.items.MagItem magItem && slotItem.getItem() instanceof com.sheridan.gcr.items.AmmoItem ammoItem) {
                if (magItem.acceptsAmmo(ammoItem)) {
                    int currentAmmo = com.sheridan.gcr.items.MagItem.getAmmoLeft(carried);
                    int maxCap = magItem.getModule().getMaxCapacity();
                    if (currentAmmo >= maxCap) {
                        return;
                    }

                    event.setCanceled(true);

                    net.minecraft.world.item.ItemStack magStack = carried.copy();
                    net.minecraft.world.item.ItemStack ammoInSlot = slotItem.copy();
                    containerScreen.getMenu().setCarried(net.minecraft.world.item.ItemStack.EMPTY);
                    slot.set(magStack);
                    if (!player.getInventory().add(ammoInSlot)) {
                        player.drop(ammoInSlot, false);
                    }

                    MagLoadingHandler.startGuiLoading(slot.index, magItem.getCaliber(), magStack);
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                            new com.sheridan.gcr.network.c2s.MagLoadPacket(com.sheridan.gcr.network.c2s.MagLoadPacket.ACTION_START_GUI, magItem.getCaliber(), slot.index, true)
                    );
                }
            }
        }
    }

    @SubscribeEvent
    public static void onScreenClosing(net.neoforged.neoforge.client.event.ScreenEvent.Closing event) {
        if (MagLoadingHandler.isGuiLoading()) {
            MagLoadingHandler.stopLoading(true);
        }
    }
}
