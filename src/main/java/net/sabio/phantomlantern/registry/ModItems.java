package net.sabio.phantomlantern.registry;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.sabio.phantomlantern.PhantomLantern;
import net.sabio.phantomlantern.item.PhantomLanternItem;
import net.sabio.phantomlantern.logic.PhantomLanternLogic;

public class ModItems {

    public static final PhantomLanternItem PHANTOM_LANTERN = Registry.register(
            BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath(PhantomLantern.MOD_ID, "phantom_lantern"),
            new PhantomLanternItem(
                    new Item.Properties()
                            .setId(ResourceKey.create(
                                    Registries.ITEM,
                                    Identifier.fromNamespaceAndPath(PhantomLantern.MOD_ID, "phantom_lantern")
                            ))
                            .stacksTo(1)
            )
    );

    public static void register() {

    }

    public static void registerEvents() {
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide()) return InteractionResult.PASS;
            if (!(player.getMainHandItem().getItem() instanceof PhantomLanternItem)) return InteractionResult.PASS;
            if (!(entity instanceof LivingEntity living)) return InteractionResult.PASS;
            if (living == player) return InteractionResult.PASS;
            PhantomLanternLogic.startHaunt(player, living);
            return InteractionResult.PASS;
        });
    }
}