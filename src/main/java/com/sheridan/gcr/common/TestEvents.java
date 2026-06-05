package com.sheridan.gcr.common;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class TestEvents {

    static int i = 0, j = 0;
    @SubscribeEvent
    public static void test(PlayerTickEvent.Pre event) {
        Player entity = event.getEntity();
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
