package net.sabio.phantomlantern.logic;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.sabio.phantomlantern.item.PhantomLanternItem;

import java.util.*;

public class PhantomLanternLogic {
    private static final Set<UUID> SOUL_VISION_ACTIVE = new HashSet<>();

    private record HauntState(UUID attackerUUID, int remainingTicks, float mobMaxHealth) {}
    private static final Map<UUID, HauntState> HAUNTED_MOBS = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                for (ServerPlayer player : List.copyOf(level.players())) {
                    tickSoulVision(player, level);

                    if (level.getGameTime() % 20 == 0) {
                        ItemStack lantern = getLanternStack(player);
                        if (lantern != null) {
                            int souls = SoulChargeHelper.getSouls(lantern);
                            int essence = EssenceHelper.getEssence(player);
                            String soulsCol = souls > 50 ? "§5" : souls > 20 ? "§6" : "§c";
                            sendActionBar(player, Component.literal(soulsCol + "⚡ " + souls + "/" + SoulChargeHelper.MAX_SOULS + " souls §7| §3✦ " + essence + " essence"));
                            SoulChargeHelper.add(lantern, 1);
                        }
                    }
                }
            }
            tickHaunt(server);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.player.getUUID();
            SOUL_VISION_ACTIVE.remove(id);
            EssenceHelper.remove(id);
        });
    }

    public static void sendActionBar(Player player, Component message) {
        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetActionBarTextPacket(message));
        }
    }

    private static boolean isHoldingLantern(Player player) {
        return !(player.getMainHandItem().getItem() instanceof PhantomLanternItem) && !(player.getOffhandItem().getItem() instanceof PhantomLanternItem);
    }

    public static ItemStack getLanternStack(Player player) {
        if (player.getMainHandItem().getItem() instanceof PhantomLanternItem) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof PhantomLanternItem) return player.getOffhandItem();
        return null;
    }

    public static void toggleSoulVision(Player player) {
        UUID id = player.getUUID();
        if (SOUL_VISION_ACTIVE.remove(id)) {
            sendActionBar(player, Component.literal("§7Soul Vision deactivated."));
            player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6f, 1.2f);
        } else {
            SOUL_VISION_ACTIVE.add(id);
            sendActionBar(player, Component.literal("§5✦ Soul Vision activated!"));
            player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.4f);
        }
    }

    private static void tickSoulVision(ServerPlayer player, ServerLevel level) {
        if (!SOUL_VISION_ACTIVE.contains(player.getUUID())) return;

        if (isHoldingLantern(player)) {
            SOUL_VISION_ACTIVE.remove(player.getUUID());
            sendActionBar(player, Component.literal("§7Soul Vision deactivated."));
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 40, 0, false, false));

        AABB box = player.getBoundingBox().inflate(20.0);
        level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())
                .forEach(mob -> mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false)));

        if (level.getGameTime() % 10 == 0) {
            ItemStack lantern = getLanternStack(player);
            if (lantern == null || !SoulChargeHelper.tryConsume(lantern, 1)) {
                SOUL_VISION_ACTIVE.remove(player.getUUID());
                sendActionBar(player, Component.literal("§cSoul vision faded - you are out of souls!"));
                return;
            }
        }

        if (level.getGameTime() % 15 == 0) {
            for (int i = 0; i < 6; i++) {
                double angle = Math.random() * Math.PI * 2;
                double r = 0.8 + Math.random() * 1.5;
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        player.getX() + Math.cos(angle) * r,
                        player.getY() + 0.8 + Math.random() * 0.8,
                        player.getZ() + Math.sin(angle) * r,
                        1, 0, 0.04, 0, 0.01);
            }
        }
    }

    private static boolean hasCollision(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    public static void startHaunt(Player attacker, LivingEntity target) {
        HAUNTED_MOBS.put(target.getUUID(), new HauntState(attacker.getUUID(), 300, target.getMaxHealth()));

        sendActionBar(attacker, Component.literal("§5✦ Haunting §d" + target.getName().getString() + "§5!"));

        if (attacker.level() instanceof ServerLevel sl) {
            for (int i = 0; i < 24; i++) {
                sl.sendParticles(
                        ParticleTypes.SOUL,
                        target.getX() + (Math.random() - 0.5),
                        target.getY() + target.getBbHeight() * Math.random(),
                        target.getZ() + (Math.random() - 0.5),
                        1, 0, 0.08, 0, 0.01
                );
            }
        }

        attacker.level().playSound(null, target.blockPosition(), SoundEvents.PHANTOM_BITE, SoundSource.NEUTRAL, 1.0f, 0.7f);
    }

    private static void tickHaunt(MinecraftServer server) {
        Iterator<Map.Entry<UUID, HauntState>> iter = HAUNTED_MOBS.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<UUID, HauntState> entry = iter.next();
            HauntState state = entry.getValue();
            int remaining = state.remainingTicks() - 1;

            if (remaining <= 0) { iter.remove(); continue; }

            LivingEntity mob = null;
            ServerLevel mobLevel = null;
            for (ServerLevel level : server.getAllLevels()) {
                Entity raw = level.getEntity(entry.getKey());
                if (raw instanceof LivingEntity le && le.isAlive()) {
                    mob = le;
                    mobLevel = level;
                    break;
                }
            }

            if (mob == null) {
                ServerPlayer attacker = server.getPlayerList().getPlayer(state.attackerUUID());
                if (attacker != null) {
                    ItemStack lantern = getLanternStack(attacker);
                    if (lantern != null) SoulChargeHelper.add(lantern, 10);

                    int essenceGain = EssenceHelper.calculateReward(state.mobMaxHealth());
                    if (essenceGain > 0) {
                        EssenceHelper.addEssence(attacker, essenceGain);
                        int total = EssenceHelper.getEssence(attacker);

                        String unlockMsg = "";
                        if (total >= EssenceHelper.UNLOCK_BARRIER && total - essenceGain < EssenceHelper.UNLOCK_BARRIER) unlockMsg = " | §a✦ Spectral Barrier unlocked!";
                        else if (total >= EssenceHelper.UNLOCK_PHANTOM_STEP && total - essenceGain < EssenceHelper.UNLOCK_PHANTOM_STEP) unlockMsg = " | §a✦ Phantom Step unlocked!";
                        else if (total >= EssenceHelper.UNLOCK_SOUL_VISION && total - essenceGain < EssenceHelper.UNLOCK_SOUL_VISION) unlockMsg = " | §a✦ Soul Vision unlocked!";

                        sendActionBar(attacker, Component.literal("§5+10 souls  §3+" + essenceGain + " essence §7(" + total + " total)" + unlockMsg));
                    } else {
                        sendActionBar(attacker, Component.literal("§5+10 souls from haunt kill!"));
                    }
                }
                iter.remove(); continue;
            }

            entry.setValue(new HauntState(state.attackerUUID(), remaining, state.mobMaxHealth()));

            if (remaining % 5 == 0) {
                mobLevel.sendParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        mob.getX() + (Math.random() - 0.5) * 0.6,
                        mob.getY() + mob.getBbHeight() * Math.random(),
                        mob.getZ() + (Math.random() - 0.5) * 0.6,
                        1,
                        0,
                        0.05,
                        0,
                        0.01
                );
            }

            if (remaining % 40 == 0) {
                mob.hurt(mobLevel.damageSources().magic(), 2.0f);

                for (int i = 0; i < 16; i++) {
                    double angle = (i / 16.0) * Math.PI * 2;
                    mobLevel.sendParticles(
                            ParticleTypes.SOUL,
                            mob.getX() + Math.cos(angle) * 0.9,
                            mob.getY() + mob.getBbHeight() * 0.5,
                            mob.getZ() + Math.sin(angle) * 0.9,
                            1,
                            0,
                            0.05,
                            0,
                            0.01
                    );
                }

                mobLevel.playSound(null, mob.blockPosition(), SoundEvents.PHANTOM_AMBIENT, SoundSource.HOSTILE, 0.4f, 1.3f);
            }
        }
    }
}