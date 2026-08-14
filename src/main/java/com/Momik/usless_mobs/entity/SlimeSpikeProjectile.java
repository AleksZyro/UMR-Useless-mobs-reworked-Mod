package com.Momik.usless_mobs.entity;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

public class SlimeSpikeProjectile extends ThrowableItemProjectile {

    private static final float SPIKE_DAMAGE = 3.0F;
    private static final int SLOW_DURATION_TICKS = 70;
    private static final int SLOW_AMPLIFIER = 1;

    public SlimeSpikeProjectile(EntityType<? extends SlimeSpikeProjectile> type, Level level) {
        super(type, level);
    }

    public SlimeSpikeProjectile(Level level, LivingEntity owner) {
        super(com.Momik.usless_mobs.registry.ModEntities.SLIME_SPIKE.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return com.Momik.usless_mobs.registry.ModItems.BLAUER_SCHLEIMBALL.get();
    }

    private ParticleOptions getTrailParticle() {
        ItemStack stack = this.getItemRaw();
        return stack.isEmpty()
            ? new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(this.getDefaultItem()))
            : new ItemParticleOption(ParticleTypes.ITEM, stack);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            ParticleOptions trail = this.getTrailParticle();
            for (int i = 0; i < 8; i++) {
                this.level().addParticle(trail, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 2 == 0) {
            serverLevel.sendParticles(this.getTrailParticle(),
                this.getX(), this.getY(), this.getZ(), 1, 0.05D, 0.05D, 0.05D, 0.01D);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity hit = result.getEntity();
        Entity owner = this.getOwner();
        if (hit == owner) {
            return;
        }

        hit.hurt(this.damageSources().mobProjectile(this, owner instanceof LivingEntity living ? living : null), SPIKE_DAMAGE);
        if (hit instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_DURATION_TICKS, SLOW_AMPLIFIER));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
