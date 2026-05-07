package net.sabio.phantomlantern.registry;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.sabio.phantomlantern.PhantomLantern;
import net.sabio.phantomlantern.item.PhantomLanternItem;
import net.sabio.phantomlantern.logic.PhantomLanternLogic;

import java.util.function.Function;

public class ModItems {
    public static final PhantomLanternItem PHANTOM_LANTERN = register("phantom_lantern", PhantomLanternItem::new, new Item.Properties().stacksTo(1));

    public static <T extends Item> T register(String name, Function<Item.Properties, T> itemFactory, Item.Properties settings) {
        // Create the item key.
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(PhantomLantern.MOD_ID, name));

        // Create the item instance.
        T item = itemFactory.apply(settings.setId(itemKey));

        // Register the item.
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);

        return item;
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
                .register((creativeTab) -> creativeTab.accept(ModItems.PHANTOM_LANTERN));
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