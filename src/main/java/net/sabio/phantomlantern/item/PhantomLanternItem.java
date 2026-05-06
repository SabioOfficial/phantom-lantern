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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.sabio.phantomlantern.logic.EssenceHelper;
import net.sabio.phantomlantern.logic.PhantomLanternLogic;
import net.sabio.phantomlantern.logic.SoulChargeHelper;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.Consumer;

public class PhantomLanternItem extends Item {
    private static final double BARRIER_RADIUS = 5.0;

    public PhantomLanternItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NonNull ItemStack stack, @NonNull TooltipContext context, @NonNull TooltipDisplay display, @NonNull Consumer<Component> builder, @NonNull TooltipFlag flag) {
        int souls = SoulChargeHelper.getSouls(stack);
        String col = souls > 30 ? "§5" : souls > 10 ? "§6" : "§c";
        builder.accept(Component.literal(col + "Souls: " + souls + "/" + SoulChargeHelper.MAX_SOULS));
        builder.accept(Component.literal("§8(+1/sec passive | +10 on haunt kill)"));
        builder.accept(Component.literal("§7--------------------"));
        builder.accept(Component.literal("§8[30 Essence] §7Right-click: Soul Vision §8(2 souls/sec)"));
        builder.accept(Component.literal("§8[60 Essence] §7R: Phantom Step §5(7 souls)"));
        builder.accept(Component.literal("§8[100 Essence] §7Sneak+Right-click: Spectral Barrier §5(Varies)"));
    }

    public static boolean isUnlocked(ServerPlayer player, int abilityIndex) {
        return EssenceHelper.isAbilityUnlocked(player, abilityIndex);
    }

    private void sendActionBar(Player player, Component message) {
        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetActionBarTextPacket(message));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ItemStack stack = player.getItemInHand(hand);
        ServerPlayer serverPlayer = (ServerPlayer) player;

        if (player.isShiftKeyDown()) {
            if (!isUnlocked(serverPlayer, 2)) {
                sendActionBar(player, Component.literal("§cUnlock Spectral Barrier first! (100 Essence)"));
            } else {
                activateSpectralBarrier(level, player, stack);
            }
        } else {
            if (!isUnlocked(serverPlayer, 0)) {
                sendActionBar(player, Component.literal("§cUnlock Soul Vision first! (30 Essence)"));
            } else {
                PhantomLanternLogic.toggleSoulVision(player);
            }
        }

        return InteractionResult.SUCCESS;
    }

    private void activateSpectralBarrier(Level level, Player player, ItemStack stack) {
        Vec3 center = player.position().add(0, 0.5, 0);

        AABB box = AABB.ofSize(center, BARRIER_RADIUS * 2, BARRIER_RADIUS * 2, BARRIER_RADIUS * 2);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive());

        int cost = Math.max(15, targets.size() * 5);

        if (!SoulChargeHelper.tryConsume(stack, cost)) {
            sendActionBar(player, Component.literal("§cNot enough souls! Need " + cost + ", have " + SoulChargeHelper.getSouls(stack) + "."));
            return;
        }

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

        targets.forEach(mob -> mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1)));

        level.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0f, 0.6f);
        sendActionBar(player, Component.literal("§5✦ Spectral Barrier! §7(" + targets.size() + " mob(s), cost " + cost + " souls)"));
    }
}