package com.sheridan.gcr.common;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.TickEvent;

public class TestEvents {

    static int i = 0, j = 0;
    @SubscribeEvent
    public static void test(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Player entity = event.player;
        float attackStrengthScale = entity.getAttackStrengthScale(0);
       // System.out.println(attackStrengthScale);
//        if (mainHandItem.getItem() == Items.APPLE) {
//            ARStates holder = StateHolder.getHolder(ARStates.TYPE, ARStates.class);
//            System.out.println(System.identityHashCode(holder) + " " + entity.level().isClientSide);
//        }
    }

    @SubscribeEvent
    public static void test2(LivingEquipmentChangeEvent event) {

//        System.out.println("event triggered! " + System.identityHashCode(event.getFrom()) + " " + System.identityHashCode(event.getTo()));
//        ItemStack eventTo = event.getTo();
//        if (eventTo.getItem() == Items.APPLE) {
//            CustomData orDefault = eventTo.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
//            if (orDefault.isEmpty()) {
//                CompoundTag tag = new CompoundTag();
//                tag.putInt("test", 0);
//                System.out.println("write to stack: " + System.identityHashCode(eventTo));
//                CustomData.set(DataComponents.CUSTOM_DATA, eventTo, tag);
//            }
//        }
    }
}
