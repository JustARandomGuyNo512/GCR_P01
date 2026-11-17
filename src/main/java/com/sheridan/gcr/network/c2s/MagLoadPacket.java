package com.sheridan.gcr.network.c2s;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.MagClientBridge;
import com.sheridan.gcr.client.MagLoadingHandler;
import com.sheridan.gcr.items.AmmoItem;
import com.sheridan.gcr.items.MagItem;
import com.sheridan.gcr.network.Codec;
import com.sheridan.gcr.network.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class MagLoadPacket implements CustomPacketPayload, IPacket<MagLoadPacket> {
    public static final ResourceLocation ID = GCR.RL("mag_load");
    public static final Type<MagLoadPacket> TYPE = new Type<>(ID);
    public static final Codec<MagLoadPacket> STREAM_CODEC = new Codec<>(
            MagLoadPacket::decode,
            (buf, p) -> p.encode(buf)
    );

    public static final int ACTION_START = 0;
    public static final int ACTION_LOAD_ROUND = 1;
    public static final int ACTION_STOP = 2;
    public static final int ACTION_START_FROM_SLOT = 3;

    public int action;
    public String caliber;
    public int slotIndex;
    public boolean isCarriedMag;

    public MagLoadPacket() {
        this(0, "", -1, false);
    }

    public MagLoadPacket(int action, String caliber) {
        this(action, caliber, -1, false);
    }

    public MagLoadPacket(int action, String caliber, int slotIndex, boolean isCarriedMag) {
        this.action = action;
        this.caliber = caliber != null ? caliber : "";
        this.slotIndex = slotIndex;
        this.isCarriedMag = isCarriedMag;
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeInt(action);
        buf.writeUtf(caliber);
        buf.writeInt(slotIndex);
        buf.writeBoolean(isCarriedMag);
    }

    private static MagLoadPacket decode(FriendlyByteBuf buf) {
        return new MagLoadPacket(buf.readInt(), buf.readUtf(), buf.readInt(), buf.readBoolean());
    }

    @Override
    public void onClient(MagLoadPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.action == ACTION_START) {
                MagClientBridge.startLoadingFromInventory(packet.caliber);
            } else if (packet.action == ACTION_STOP) {
                MagLoadingHandler.stopLoading(false);
            }
        });
    }

    @Override
    public void onServer(MagLoadPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();

            if (packet.action == ACTION_START_FROM_SLOT) {
                ItemStack carried = player.containerMenu.getCarried();
                player.containerMenu.setCarried(ItemStack.EMPTY);
                if (!carried.isEmpty()) {
                    if (!player.getInventory().add(carried)) {
                        player.drop(carried, false);
                    }
                }

                int selectedHotbar = player.getInventory().selected;
                ItemStack oldHeld = player.getInventory().getItem(selectedHotbar);

                if (packet.isCarriedMag) {
                    if (carried.getItem() instanceof MagItem) {
                        if (!oldHeld.isEmpty()) {
                            if (!player.getInventory().add(oldHeld)) {
                                player.drop(oldHeld, false);
                            }
                        }
                        player.getInventory().setItem(selectedHotbar, carried);
                    }
                } else if (packet.slotIndex >= 0 && packet.slotIndex < player.containerMenu.slots.size()) {
                    Slot slot = player.containerMenu.slots.get(packet.slotIndex);
                    if (slot != null && slot.getItem().getItem() instanceof MagItem) {
                        ItemStack magStack = slot.getItem().copy();
                        slot.set(oldHeld);
                        player.getInventory().setItem(selectedHotbar, magStack);
                    }
                }

                player.closeContainer();
                player.inventoryMenu.broadcastChanges();
                return;
            }

            ItemStack held = player.getMainHandItem();
            if (!(held.getItem() instanceof MagItem magItem)) {
                PacketDistributor.sendToPlayer(player, new MagLoadPacket(ACTION_STOP, ""));
                return;
            }

            if (packet.action == ACTION_LOAD_ROUND) {
                int ammoLeft = MagItem.getAmmoLeft(held);
                int maxCap = magItem.getModule().getMaxCapacity();
                if (ammoLeft >= maxCap) {
                    PacketDistributor.sendToPlayer(player, new MagLoadPacket(ACTION_STOP, ""));
                    return;
                }

                Inventory inv = player.getInventory();
                boolean found = false;
                for (int i = 0; i < inv.items.size(); i++) {
                    ItemStack s = inv.items.get(i);
                    if (s.getItem() instanceof AmmoItem ai && magItem.acceptsAmmo(ai)) {
                        s.shrink(1);
                        MagItem.setAmmoLeft(held, ammoLeft + 1);
                        found = true;
                        break;
                    }
                }

                if (found) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 0.7f, 1.35f);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8f, 1.1f);

                    if (MagItem.getAmmoLeft(held) >= maxCap) {
                        PacketDistributor.sendToPlayer(player, new MagLoadPacket(ACTION_STOP, ""));
                    }
                } else {
                    PacketDistributor.sendToPlayer(player, new MagLoadPacket(ACTION_STOP, ""));
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
