package net.sabio.phantomlantern;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.sabio.phantomlantern.item.PhantomLanternItem;
import net.sabio.phantomlantern.logic.PhantomLanternLogic;
import net.sabio.phantomlantern.logic.SoulChargeHelper;
import net.sabio.phantomlantern.network.PhantomStepPayload;

public class PhantomLanternNetwork {
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(PhantomStepPayload.TYPE, PhantomStepPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(PhantomStepPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handlePhantomStep(player));
        });
    }

    private static void handlePhantomStep(ServerPlayer player) {
        ServerLevel level = player.level();

        if (!PhantomLanternItem.isUnlocked(player, 1)) {
            PhantomLanternLogic.sendActionBar(player, Component.literal("§cUnlock Phantom Step first! (60 Essence)"));
            return;
        }

        ItemStack lantern = PhantomLanternLogic.getLanternStack(player);
        if (lantern == null) return;

        if (!SoulChargeHelper.tryConsume(lantern, 7)) {
            PhantomLanternLogic.sendActionBar(player, Component.literal("§cNot enough souls! (need 7)"));
            return;
        }

        Vec3 look = player.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        if (dir.lengthSqr() < 0.001) return;
        dir = dir.normalize();

        double px = player.getX(), py = player.getY(), pz = player.getZ();
        BlockPos near = BlockPos.containing(px + dir.x * 1.3, py + 0.1, pz + dir.z * 1.3);
        BlockPos far = BlockPos.containing(px + dir.x * 2.5, py + 0.1, pz + dir.z * 2.5);

        boolean nearSolid = hasCollision(level, near) || hasCollision(level, near.above());
        boolean farClear = !hasCollision(level, far) && !hasCollision(level, far.above());

        if (!nearSolid || !farClear) {
            PhantomLanternLogic.sendActionBar(player, Component.literal("§7No clear step path."));
            return;
        }

        double tx = px + dir.x * 3.0;
        double tz = pz + dir.z * 3.0;
        player.teleportTo(tx, py, tz);

        for (int i = 0; i < 20; i++) {
            level.sendParticles(
                    ParticleTypes.SOUL,
                    tx + (Math.random() - 0.5) * 0.8,
                    py + Math.random() * 2.0,
                    tz + (Math.random() - 0.5) * 0.8,
                    1,
                    0,
                    0.04,
                    0,
                    0.01
            );
        }
        level.playSound(null, BlockPos.containing(tx, py, tz), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.6f);
        PhantomLanternLogic.sendActionBar(player, Component.literal("§5✦ Phantom Step!"));
    }

    private static boolean hasCollision(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.getCollisionShape(level, pos).isEmpty();
    }
}
