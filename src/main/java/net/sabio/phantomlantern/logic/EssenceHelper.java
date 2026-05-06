package net.sabio.phantomlantern.logic;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.sabio.phantomlantern.PhantomLantern;

import java.util.UUID;

public class EssenceHelper {
    public static final AttachmentType<Integer> ESSENCE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(PhantomLantern.MOD_ID, "essence"),
            builder -> builder
                    .persistent(Codec.INT)
                    .initializer(() -> 0)
    );

    public static final int UNLOCK_SOUL_VISION = 30;
    public static final int UNLOCK_PHANTOM_STEP = 60;
    public static final int UNLOCK_BARRIER = 100;

    public static void init() {}

    public static int getEssence(ServerPlayer player) {
        return player.getAttachedOrElse(ESSENCE, 0);
    }

    public static void addEssence(ServerPlayer player, int amount) {
        player.setAttached(ESSENCE, getEssence(player) + amount);
    }

    public static void remove(UUID uuid) {}

    public static boolean isAbilityUnlocked(ServerPlayer player, int abilityIndex) {
        int e = getEssence(player);
        return switch (abilityIndex) {
            case 0 -> e >= UNLOCK_SOUL_VISION;
            case 1 -> e >= UNLOCK_PHANTOM_STEP;
            case 2 -> e >= UNLOCK_BARRIER;
            default -> false;
        };
    }

    public static int calculateReward(float maxHealth) {
        if (maxHealth < 20f) return 0;
        return (int) Math.floor(maxHealth / 20f);
    }
}
