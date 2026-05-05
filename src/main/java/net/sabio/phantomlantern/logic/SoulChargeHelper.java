package net.sabio.phantomlantern.logic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class SoulChargeHelper {
    public static final int MAX_SOULS = 100;
    private static final String KEY = "SoulCharge";

    public static int getSouls(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getInt(KEY).orElse(0);
    }

    public static void setSouls(ItemStack stack, int amount) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(KEY, Math.clamp(amount, 0, MAX_SOULS));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean tryConsume(ItemStack stack, int amount) {
        int current = getSouls(stack);
        if (current < amount) return false;
        setSouls(stack, current - amount);
        return true;
    }

    public static void add(ItemStack stack, int amount) {
        setSouls(stack, getSouls(stack) + amount);
    }
}
