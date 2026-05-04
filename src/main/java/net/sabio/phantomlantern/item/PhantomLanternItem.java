package net.sabio.phantomlantern.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PhantomLanternItem extends Item {
    private static final int BARRIER_COOLDOWN_TICKS = 400;
    private static final double BARRIER_RADIUS = 5.0;

    public PhantomLanternItem(Properties properties) {
        super(properties);
    }

    private void sendActionBar(Player player, Component message) {
        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetActionBarTextPacket(message));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            if (player.getCooldowns().isOnCooldown(this.getDefaultInstance())) {
                sendActionBar(player, Component.literal("§7Spectral Barrier is recharging..."));
            } else {
                activateSpectralBarrier(level, player);
                player.getCooldowns().addCooldown(this.getDefaultInstance(), BARRIER_COOLDOWN_TICKS);
            }
        } else {
            net.sabio.phantomlantern.logic.PhantomLanternLogic.toggleSoulVision(player);
        }

        return InteractionResult.SUCCESS;
    }

    private void activateSpectralBarrier(Level level, Player player) {
        Vec3 center = player.position().add(0, 0.5, 0);

        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 72; i++) {
                double angle = (i / 72.0) * Math.PI * 2;
                double cx = center.x + Math.cos(angle) * BARRIER_RADIUS;
                double cz = center.z + Math.sin(angle) * BARRIER_RADIUS;
                serverLevel.sendParticles(ParticleTypes.SOUL, cx, center.y + 0.1, cz, 1, 0, 0, 0, 0.01);
                serverLevel.sendParticles(ParticleTypes.SOUL, cx, center.y + 1.8, cz, 1, 0, 0, 0, 0.01);
            }

            for (int i = 0; i < 40; i++) {
                double angle = Math.random() * Math.PI * 2;
                double r = Math.random() * BARRIER_RADIUS;
                serverLevel.sendParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        center.x + Math.cos(angle) * r,
                        center.y + Math.random() * 2.0,
                        center.z + Math.sin(angle) * r,
                        1, 0, 0.06, 0, 0.01
                );
            }
        }

        AABB box = AABB.ofSize(center, BARRIER_RADIUS * 2, BARRIER_RADIUS * 2, BARRIER_RADIUS * 2);
        level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())
                .forEach(mob -> mob.addEffect(
                        new MobEffectInstance(MobEffects.SLOWNESS, 100, 1)
                ));

        level.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0f, 0.6f);
        sendActionBar(player, Component.literal("§5✦ Spectral Barrier!"));
    }
}