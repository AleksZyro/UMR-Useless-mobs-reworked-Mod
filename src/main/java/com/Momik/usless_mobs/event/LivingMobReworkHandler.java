package com.Momik.usless_mobs.event;

import com.Momik.usless_mobs.entity.CoralDrownedEntity;
import com.Momik.usless_mobs.entity.OctopusEntity;
import com.Momik.usless_mobs.entity.WitchBossEntity;
import com.Momik.usless_mobs.Usless_mobs;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.Ocelot;
import com.Momik.usless_mobs.entity.LivingOcelotEntity;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Usless_mobs.MODID)
public final class LivingMobReworkHandler {
    private static final String NEXT_ATTACK_KEY = "UmrNatureNextAttack";
    private static final String POLAR_CHARGE_KEY = "UmrPolarCharge";
    private static final String POLAR_CHARGE_UNTIL_KEY = "UmrPolarChargeUntil";
    private static final String OCELOT_OWNER_KEY = "UmrOcelotOwner";
    private static final String OCELOT_NEXT_POUNCE_KEY = "UmrOcelotNextPounce";
    private static final String OCELOT_MARK_OWNER_KEY = "UmrOcelotMarkOwner";
    private static final String OCELOT_MARK_UNTIL_KEY = "UmrOcelotMarkUntil";
    private static final String SQUID_INK_BURST_KEY = "UmrSquidInkBurstUntil";
    private static final String GLOW_SQUID_NEXT_GIFT_KEY = "UmrGlowSquidNextGift";
    private static final String GLOW_SQUID_FLASH_COOLDOWN_KEY = "UmrGlowSquidFlashCooldown";

    private LivingMobReworkHandler() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (entity.getPersistentData().getBoolean(WitchBossEntity.HUNT_HOUND_KEY)) {
            tickWitchHound(entity, serverLevel);
            return;
        }
        if (entity.getPersistentData().getBoolean(WitchBossEntity.DECOY_KEY)) {
            tickWitchDecoy(entity, serverLevel);
            return;
        }
        if (entity.getPersistentData().getBoolean(WitchBossEntity.ROOT_SPIRIT_KEY)) {
            tickWitchRootSpirit(entity, serverLevel);
            return;
        }

        if (entity instanceof Axolotl axolotl) {
            tickAxolotl(axolotl, serverLevel);
        } else if (entity instanceof Drowned drowned && !(entity instanceof CoralDrownedEntity)) {
            tickDrowned(drowned, serverLevel);
        } else if (entity instanceof GlowSquid glowSquid) {
            tickGlowSquid(glowSquid, serverLevel);
        } else if (entity instanceof Squid squid && !(entity instanceof OctopusEntity)) {
            tickSquid(squid, serverLevel);
        } else if (entity instanceof PolarBear polarBear) {
            tickPolarBear(polarBear, serverLevel);
        } else if (entity instanceof LivingOcelotEntity ocelot) {
            tickOcelot(ocelot, serverLevel);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof PolarBear bear
                && bear.getPersistentData().getBoolean(POLAR_CHARGE_KEY)) {
            event.setAmount(event.getAmount() + 5.0F);
            bear.getPersistentData().remove(POLAR_CHARGE_KEY);
            bear.getPersistentData().remove(POLAR_CHARGE_UNTIL_KEY);
            pushAway(bear, event.getEntity(), 0.9D);
            event.getEntity().setTicksFrozen(Math.max(event.getEntity().getTicksFrozen(), 120));
            if (event.getEntity().level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                        event.getEntity().getX(), event.getEntity().getY(0.65D), event.getEntity().getZ(),
                        18, 0.35D, 0.25D, 0.35D, 0.04D);
            }
        }

        Entity source = event.getSource().getEntity();
        LivingEntity hurtEntity = event.getEntity();
        if (hurtEntity.level() instanceof ServerLevel serverLevel) {
            if (source instanceof Player owner && hurtEntity instanceof Monster monster) {
                markOcelotTarget(serverLevel, owner, monster);
            } else if (hurtEntity instanceof Player owner && source instanceof Monster monster) {
                markOcelotTarget(serverLevel, owner, monster);
            }

            if (hurtEntity instanceof GlowSquid glowSquid && source instanceof LivingEntity attacker) {
                glowSquidFlash(glowSquid, attacker, serverLevel);
            } else if (hurtEntity instanceof Squid squid && !(hurtEntity instanceof OctopusEntity) && source instanceof LivingEntity attacker) {
                squidInkBurst(squid, attacker, serverLevel);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getTarget() instanceof LivingOcelotEntity ocelot) {
            bindOcelot(event, serverLevel, player, ocelot);
        } else if (event.getTarget() instanceof GlowSquid glowSquid) {
            feedGlowSquid(event, serverLevel, player, glowSquid);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Level level = event.getLevel();
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.INK_SAC) || player.getCooldowns().isOnCooldown(Items.INK_SAC)) {
            return;
        }

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            AABB area = player.getBoundingBox().inflate(5.5D);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, area,
                    mob -> mob.isAlive() && mob.getTarget() == player)) {
                mob.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                mob.setTarget(null);
            }
            serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                    player.getX(), player.getY(0.7D), player.getZ(),
                    44, 0.75D, 0.45D, 0.75D, 0.08D);
            level.playSound(null, player.blockPosition(), SoundEvents.SQUID_SQUIRT, SoundSource.PLAYERS, 0.9F, 0.85F);
            player.getCooldowns().addCooldown(Items.INK_SAC, 12 * 20);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void tickAxolotl(Axolotl axolotl, ServerLevel level) {
        LivingEntity target = axolotl.getTarget();
        if ((target == null || !target.isAlive()) && axolotl.isInWaterOrBubble() && cooldownReady(axolotl, 95)) {
            AABB area = axolotl.getBoundingBox().inflate(4.0D);
            int healed = 0;
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area,
                    living -> living.isAlive() && living != axolotl && !(living instanceof Monster))) {
                if ((living instanceof Player || living instanceof Animal) && living.isInWaterOrBubble() && living.getHealth() < living.getMaxHealth()) {
                    living.heal(2.0F);
                    healed++;
                }
            }
            if (healed > 0) {
                level.sendParticles(ParticleTypes.BUBBLE_POP,
                        axolotl.getX(), axolotl.getY(0.6D), axolotl.getZ(),
                        18, 0.45D, 0.25D, 0.45D, 0.03D);
                level.playSound(null, axolotl.blockPosition(), SoundEvents.AXOLOTL_IDLE_WATER, SoundSource.NEUTRAL, 0.9F, 1.35F);
            }
            return;
        }

        if (target == null || !target.isAlive() || axolotl.distanceToSqr(target) > 8.0D * 8.0D
                || !cooldownReady(axolotl, 60)) {
            return;
        }
        target.hurt(axolotl.damageSources().mobAttack(axolotl), 2.5F);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 65, 0));
        pushAway(axolotl, target, 0.42D);
        level.sendParticles(ParticleTypes.BUBBLE,
                target.getX(), target.getY(0.55D), target.getZ(),
                20, 0.35D, 0.25D, 0.35D, 0.04D);
        level.playSound(null, target.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.NEUTRAL, 0.8F, 1.4F);
    }

    // Hinweis: Das alte PDC-basierte "Pseudo-Coral-Drowned"-System (Custom Name + Persistenz
    // auf Vanilla-Drowned) wurde entfernt. Coral Drowned ist jetzt ausschliesslich die echte
    // CoralDrownedEntity (Konversion in OceanMobSpawnHandler) - mit eigenen Visuals und Faehigkeiten.
    private static void tickDrowned(Drowned drowned, ServerLevel level) {
        LivingEntity target = drowned.getTarget();
        if (target == null || !target.isAlive() || drowned.distanceToSqr(target) > 10.0D * 10.0D
                || drowned.distanceToSqr(target) < 2.0D * 2.0D || !drowned.isInWaterOrBubble()
                || !cooldownReady(drowned, 75)) {
            return;
        }
        target.hurt(drowned.damageSources().mobAttack(drowned), 3.0F);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 85, 1));
        Vec3 pull = drowned.position().subtract(target.position()).normalize().scale(0.45D);
        target.setDeltaMovement(target.getDeltaMovement().add(pull.x, 0.08D, pull.z));
        target.hurtMarked = true;
        level.sendParticles(ParticleTypes.SPLASH,
                target.getX(), target.getY(0.55D), target.getZ(),
                24, 0.45D, 0.25D, 0.45D, 0.06D);
        level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.HOSTILE, 0.9F, 1.1F);
    }

    private static void tickGlowSquid(GlowSquid glowSquid, ServerLevel level) {
        if (glowSquid.tickCount % 80 != 0) {
            return;
        }
        AABB area = glowSquid.getBoundingBox().inflate(7.0D);
        for (Monster monster : level.getEntitiesOfClass(Monster.class, area, LivingEntity::isAlive)) {
            monster.addEffect(new MobEffectInstance(MobEffects.GLOWING, 8 * 20, 0));
        }
        level.sendParticles(ParticleTypes.GLOW,
                glowSquid.getX(), glowSquid.getY(0.5D), glowSquid.getZ(),
                10, 0.45D, 0.3D, 0.45D, 0.03D);
    }

    private static void tickSquid(Squid squid, ServerLevel level) {
        Player player = level.getNearestPlayer(squid, 2.7D);
        if (player == null || player.getAbilities().instabuild || !player.isInWaterOrBubble()
                || !cooldownReady(squid, 65)) {
            return;
        }
        player.hurt(squid.damageSources().mobAttack(squid), 1.5F);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 45, 0));
        level.sendParticles(ParticleTypes.SQUID_INK,
                player.getX(), player.getY(0.55D), player.getZ(),
                20, 0.35D, 0.2D, 0.35D, 0.04D);
        level.playSound(null, player.blockPosition(), SoundEvents.SQUID_SQUIRT, SoundSource.NEUTRAL, 0.7F, 1.0F);
    }

    private static void tickPolarBear(PolarBear bear, ServerLevel level) {
        if (bear.getHealth() <= bear.getMaxHealth() * 0.4F && bear.tickCount % 35 == 0) {
            bear.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0));
        }
        long now = level.getGameTime();
        if (bear.getPersistentData().getBoolean(POLAR_CHARGE_KEY)) {
            if (bear.getPersistentData().getLong(POLAR_CHARGE_UNTIL_KEY) <= now) {
                bear.getPersistentData().remove(POLAR_CHARGE_KEY);
                bear.getPersistentData().remove(POLAR_CHARGE_UNTIL_KEY);
            } else if (bear.tickCount % 4 == 0) {
                level.sendParticles(ParticleTypes.SNOWFLAKE,
                        bear.getX(), bear.getY(0.1D), bear.getZ(),
                        7, 0.35D, 0.05D, 0.35D, 0.04D);
            }
        }

        LivingEntity target = bear.getTarget();
        if (target == null || !target.isAlive() || bear.distanceToSqr(target) > 11.0D * 11.0D
                || bear.distanceToSqr(target) < 3.0D * 3.0D || !cooldownReady(bear, 130)) {
            return;
        }
        Vec3 charge = target.position().subtract(bear.position()).normalize().scale(0.82D);
        bear.setDeltaMovement(bear.getDeltaMovement().add(charge.x, 0.08D, charge.z));
        bear.getPersistentData().putBoolean(POLAR_CHARGE_KEY, true);
        bear.getPersistentData().putLong(POLAR_CHARGE_UNTIL_KEY, now + 45L);
        bear.hurtMarked = true;
        level.sendParticles(ParticleTypes.SNOWFLAKE,
                bear.getX(), bear.getY(0.4D), bear.getZ(),
                18, 0.45D, 0.15D, 0.45D, 0.08D);
        level.playSound(null, bear.blockPosition(), com.Momik.usless_mobs.registry.ModSounds.POLAR_BEAR_CHARGE.get(), SoundSource.HOSTILE, 1.1F, 0.75F);
    }

    private static void tickOcelot(Ocelot ocelot, ServerLevel level) {
        if (!ocelot.getPersistentData().hasUUID(OCELOT_OWNER_KEY)) {
            return;
        }
        UUID ownerId = ocelot.getPersistentData().getUUID(OCELOT_OWNER_KEY);
        Player owner = level.getPlayerByUUID(ownerId);
        if (owner == null || !owner.isAlive()) {
            return;
        }
        if (ocelot.distanceToSqr(owner) > 28.0D * 28.0D) {
            ocelot.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            return;
        }
        if (ocelot.distanceToSqr(owner) > 8.0D * 8.0D) {
            ocelot.getNavigation().moveTo(owner, 1.35D);
        }
        long now = level.getGameTime();
        if (ocelot.getPersistentData().getLong(OCELOT_NEXT_POUNCE_KEY) > now) {
            return;
        }
        Monster target = level.getEntitiesOfClass(Monster.class, owner.getBoundingBox().inflate(9.0D),
                        monster -> monster.isAlive()
                                && monster.getPersistentData().hasUUID(OCELOT_MARK_OWNER_KEY)
                                && monster.getPersistentData().getUUID(OCELOT_MARK_OWNER_KEY).equals(ownerId)
                                && monster.getPersistentData().getLong(OCELOT_MARK_UNTIL_KEY) > now)
                .stream()
                .min(Comparator.comparingDouble(owner::distanceToSqr))
                .orElse(null);
        if (target == null) {
            target = level.getEntitiesOfClass(Monster.class, owner.getBoundingBox().inflate(7.0D),
                            monster -> monster.isAlive() && monster.hasLineOfSight(owner))
                    .stream()
                    .min(Comparator.comparingDouble(owner::distanceToSqr))
                    .orElse(null);
        }
        if (target == null) {
            return;
        }
        ocelot.getNavigation().moveTo(target, 1.65D);
        pushAway(ocelot, target, 0.35D);
        target.hurt(ocelot.damageSources().mobAttack(ocelot), 4.5F);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 65, 0));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, 0));
        ocelot.getPersistentData().putLong(OCELOT_NEXT_POUNCE_KEY, now + 55L);
        level.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY(0.55D), target.getZ(),
                8, 0.25D, 0.25D, 0.25D, 0.02D);
    }

    private static void bindOcelot(PlayerInteractEvent.EntityInteract event, ServerLevel serverLevel, ServerPlayer player, Ocelot ocelot) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.COD) && !stack.is(Items.SALMON) && !stack.is(com.Momik.usless_mobs.registry.ModItems.NATURE_CRYSTAL.get())) {
            return;
        }

        ocelot.getPersistentData().putUUID(OCELOT_OWNER_KEY, player.getUUID());
        ocelot.setPersistenceRequired();
        ocelot.setCustomName(Component.translatable("entity.usless_mobs.raid_ocelot"));
        if (ocelot.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            ocelot.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.42D);
        }
        if (!player.getAbilities().instabuild && !stack.is(com.Momik.usless_mobs.registry.ModItems.NATURE_CRYSTAL.get())) {
            stack.shrink(1);
        }
        serverLevel.sendParticles(ParticleTypes.HEART,
                ocelot.getX(), ocelot.getY(0.7D), ocelot.getZ(),
                8, 0.3D, 0.25D, 0.3D, 0.02D);
        serverLevel.playSound(null, ocelot.blockPosition(), SoundEvents.CAT_PURREOW, SoundSource.NEUTRAL, 1.0F, 1.2F);
        player.displayClientMessage(Component.translatable("entity.usless_mobs.raid_ocelot.recruited")
                .withStyle(ChatFormatting.GREEN), true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void feedGlowSquid(PlayerInteractEvent.EntityInteract event, ServerLevel serverLevel, ServerPlayer player, GlowSquid glowSquid) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.GLOW_INK_SAC)) {
            return;
        }

        long now = serverLevel.getGameTime();
        if (glowSquid.getPersistentData().getLong(GLOW_SQUID_NEXT_GIFT_KEY) > now) {
            return;
        }
        glowSquid.getPersistentData().putLong(GLOW_SQUID_NEXT_GIFT_KEY, now + 20L * 60L * 5L);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        ItemStack reward = new ItemStack(com.Momik.usless_mobs.registry.ModItems.GLOW_FLARE.get());
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
        serverLevel.sendParticles(ParticleTypes.GLOW,
                glowSquid.getX(), glowSquid.getY(0.6D), glowSquid.getZ(),
                28, 0.45D, 0.35D, 0.45D, 0.03D);
        serverLevel.playSound(null, glowSquid.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.9F, 1.35F);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void markOcelotTarget(ServerLevel level, Player owner, Monster monster) {
        boolean hasBoundOcelot = !level.getEntitiesOfClass(LivingOcelotEntity.class, owner.getBoundingBox().inflate(12.0D),
                ocelot -> ocelot.isAlive()
                        && ocelot.getPersistentData().hasUUID(OCELOT_OWNER_KEY)
                        && ocelot.getPersistentData().getUUID(OCELOT_OWNER_KEY).equals(owner.getUUID())).isEmpty();
        if (!hasBoundOcelot) {
            return;
        }

        monster.getPersistentData().putUUID(OCELOT_MARK_OWNER_KEY, owner.getUUID());
        monster.getPersistentData().putLong(OCELOT_MARK_UNTIL_KEY, level.getGameTime() + 120L);
        monster.addEffect(new MobEffectInstance(MobEffects.GLOWING, 50, 0));
        level.sendParticles(ParticleTypes.CRIT,
                monster.getX(), monster.getY(0.75D), monster.getZ(),
                5, 0.2D, 0.25D, 0.2D, 0.02D);
    }

    private static void squidInkBurst(Squid squid, LivingEntity attacker, ServerLevel level) {
        long now = level.getGameTime();
        if (squid.getPersistentData().getLong(SQUID_INK_BURST_KEY) > now) {
            return;
        }
        squid.getPersistentData().putLong(SQUID_INK_BURST_KEY, now + 140L);

        AABB area = squid.getBoundingBox().inflate(4.8D);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area, mob -> mob.isAlive() && mob.getTarget() == squid)) {
            mob.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 70, 0));
            mob.setTarget(null);
        }
        attacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 55, 0));
        pushAway(squid, attacker, 0.5D);
        Vec3 flee = squid.position().subtract(attacker.position()).normalize().scale(0.45D);
        squid.setDeltaMovement(squid.getDeltaMovement().add(flee.x, 0.05D, flee.z));
        squid.hurtMarked = true;
        level.sendParticles(ParticleTypes.SQUID_INK,
                squid.getX(), squid.getY(0.5D), squid.getZ(),
                36, 0.65D, 0.35D, 0.65D, 0.07D);
        level.playSound(null, squid.blockPosition(), SoundEvents.SQUID_SQUIRT, SoundSource.NEUTRAL, 0.9F, 0.8F);
    }

    private static void glowSquidFlash(GlowSquid squid, LivingEntity attacker, ServerLevel level) {
        long now = level.getGameTime();
        if (squid.getPersistentData().getLong(GLOW_SQUID_FLASH_COOLDOWN_KEY) > now) {
            return;
        }
        squid.getPersistentData().putLong(GLOW_SQUID_FLASH_COOLDOWN_KEY, now + 180L);

        AABB area = squid.getBoundingBox().inflate(5.5D);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area,
                living -> living.isAlive() && living != squid)) {
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 45, 0));
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 90, 0));
        }
        pushAway(squid, attacker, 0.65D);
        Vec3 flee = squid.position().subtract(attacker.position()).normalize().scale(0.52D);
        squid.setDeltaMovement(squid.getDeltaMovement().add(flee.x, 0.10D, flee.z));
        squid.hurtMarked = true;
        level.sendParticles(ParticleTypes.FLASH,
                squid.getX(), squid.getY(0.55D), squid.getZ(),
                4, 0.28D, 0.20D, 0.28D, 0.0D);
        level.sendParticles(ParticleTypes.END_ROD,
                squid.getX(), squid.getY(0.55D), squid.getZ(),
                46, 0.85D, 0.55D, 0.85D, 0.12D);
        level.playSound(null, squid.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.NEUTRAL, 1.15F, 1.65F);
    }

    private static void tickWitchDecoy(LivingEntity decoy, ServerLevel level) {
        int ticks = decoy.getPersistentData().getInt(WitchBossEntity.DECOY_TICKS_KEY) - 1;
        decoy.getPersistentData().putInt(WitchBossEntity.DECOY_TICKS_KEY, ticks);
        if (ticks <= 0) {
            level.sendParticles(ParticleTypes.POOF,
                    decoy.getX(), decoy.getY(0.5D), decoy.getZ(),
                    12, 0.25D, 0.25D, 0.25D, 0.02D);
            decoy.discard();
        }
    }

    private static void tickWitchRootSpirit(LivingEntity spirit, ServerLevel level) {
        int ticks = spirit.getPersistentData().getInt(WitchBossEntity.ROOT_SPIRIT_TICKS_KEY) - 1;
        spirit.getPersistentData().putInt(WitchBossEntity.ROOT_SPIRIT_TICKS_KEY, ticks);
        if (spirit instanceof Vex vex && ticks % 20 == 0 && vex.getTarget() != null) {
            vex.getTarget().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, 0));
        }
        if (ticks % 10 == 0) {
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                    spirit.getX(), spirit.getY(0.5D), spirit.getZ(),
                    4, 0.2D, 0.2D, 0.2D, 0.01D);
        }
        if (ticks <= 0 || !spirit.isAlive()) {
            level.sendParticles(ParticleTypes.POOF,
                    spirit.getX(), spirit.getY(0.5D), spirit.getZ(),
                    10, 0.25D, 0.25D, 0.25D, 0.02D);
            spirit.discard();
        }
    }

    private static void tickWitchHound(LivingEntity hound, ServerLevel level) {
        int ticks = hound.getPersistentData().getInt(WitchBossEntity.HUNT_HOUND_TICKS_KEY) - 1;
        hound.getPersistentData().putInt(WitchBossEntity.HUNT_HOUND_TICKS_KEY, ticks);

        boolean ownerAlive = false;
        if (hound.getPersistentData().hasUUID(WitchBossEntity.HUNT_OWNER_KEY)) {
            UUID ownerId = hound.getPersistentData().getUUID(WitchBossEntity.HUNT_OWNER_KEY);
            ownerAlive = level.getEntity(ownerId) instanceof WitchBossEntity owner && owner.isAlive();
        }
        if (ticks <= 0 || !ownerAlive || !hound.isAlive()) {
            level.sendParticles(ParticleTypes.POOF,
                    hound.getX(), hound.getY(0.5D), hound.getZ(),
                    10, 0.3D, 0.25D, 0.3D, 0.02D);
            hound.discard();
        }
    }

    private static boolean isNight(ServerLevel level) {
        long time = level.getDayTime() % 24000L;
        return time >= 13000L && time <= 23000L;
    }

    private static boolean cooldownReady(LivingEntity entity, int cooldownTicks) {
        long now = entity.level().getGameTime();
        long next = entity.getPersistentData().getLong(NEXT_ATTACK_KEY);
        if (next > now) {
            return false;
        }
        entity.getPersistentData().putLong(NEXT_ATTACK_KEY, now + cooldownTicks);
        return true;
    }

    private static void pushAway(LivingEntity source, LivingEntity target, double strength) {
        Vec3 push = target.position().subtract(source.position()).normalize().scale(strength);
        target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.1D, push.z));
        target.hurtMarked = true;
    }
}
