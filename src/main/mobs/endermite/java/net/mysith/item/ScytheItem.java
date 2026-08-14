package net.mysith.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.Momik.usless_mobs.item.UmrAttributes;
import java.util.function.Consumer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ForgeMod;
import net.mysith.client.ScytheRenderer;
import net.mysith.event.EnchantmentHandlers;
import net.mysith.registry.ModParticles;
import net.mysith.registry.ModSounds;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ScytheItem extends SwordItem implements GeoItem {

    /** Cached Soul-Stacks of the holding player, written by inventoryTick so isFoil() (client) can read it. */
    public static final String DISPLAY_STACKS_TAG = "MysithDisplayStacks";
    private static final int FOIL_THRESHOLD = 5;
    private static final int MAX_STACKS_FOR_DISPLAY = 10;

    private static final RawAnimation SWING_ANIM =
            RawAnimation.begin().thenPlay("animation.sith_scythe.swing");

    private static final int WHIRLWIND_COOLDOWN_TICKS = 80;
    private static final double WHIRLWIND_RADIUS = 4.5;
    private static final float WHIRLWIND_BASE_DAMAGE = 10.0F;
    private static final float WHIRLWIND_DAMAGE_PERCENT = 0.25F;
    private static final double WHIRLWIND_KNOCKBACK = 1.4;

    // Charged Attack tiers (ticks held).
    // Tier 1 (tap)    : <5  ticks -> 1.0x dmg, 1.0x radius
    // Tier 2 (medium) : 5-29 ticks -> 1.5x dmg, 1.3x radius
    // Tier 3 (full)   : >=30 ticks -> 2.0x dmg, 1.5x radius + Reaper's Mark
    public static final int TIER_2_TICKS = 5;
    public static final int TIER_3_TICKS = 30;
    private static final int MAX_USE_DURATION = 72000;
    private static final int REAPER_MARK_DURATION_TICKS = 100;

    private static int tierFromTicks(int ticksUsed) {
        if (ticksUsed >= TIER_3_TICKS) return 3;
        if (ticksUsed >= TIER_2_TICKS) return 2;
        return 1;
    }

    private static float damageMult(int tier) {
        return tier == 3 ? 2.0F : tier == 2 ? 1.5F : 1.0F;
    }

    private static double radiusMult(int tier) {
        return tier == 3 ? 1.5 : tier == 2 ? 1.3 : 1.0;
    }

    /** True if the entity is a tamed pet owned by the given player — those should NOT be hit. */
    private static boolean isOwnPet(net.minecraft.world.entity.Entity e, Player player) {
        if (e instanceof net.minecraft.world.entity.TamableAnimal ta) {
            return ta.isTame() && player.getUUID().equals(ta.getOwnerUUID());
        }
        return false;
    }

    /** Predicate shared by Whirlwind + Air-Slash: hostile/neutral living, alive, not the player, not their pet. */
    private static java.util.function.Predicate<net.minecraft.world.entity.LivingEntity> validTargetPredicate(Player player) {
        return e -> e != player && e.isAlive() && e.isPickable() && !isOwnPet(e, player);
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private final Multimap<Attribute, AttributeModifier> reachModifier;

    public ScytheItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);

        SingletonGeoAnimatable.registerSyncedAnimatable(this);

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(
                UmrAttributes.uuid("mysith", "scythe_reach"), "Weapon reach", 2.0, AttributeModifier.Operation.ADDITION));
        this.reachModifier = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.putAll(super.getDefaultAttributeModifiers(slot));
            builder.putAll(this.reachModifier);
            return builder.build();
        }
        return super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.EPIC;
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    public int getEntityLifespan(ItemStack itemStack, Level level) {
        // Maximaler Lifespan: Sense despawnt nie. Das Alter zählt aber normal hoch,
        // sodass die Spin-Rotation im ItemEntityRenderer weiterhin funktioniert.
        return Integer.MAX_VALUE;
    }

    @Override
    public Entity createEntity(Level level, Entity oldEntity, ItemStack itemstack) {
        net.mysith.entity.ScytheItemEntity custom = new net.mysith.entity.ScytheItemEntity(
                net.mysith.registry.ModEntities.SCYTHE_ITEM.get(), level);
        custom.setItem(itemstack);
        custom.setPos(oldEntity.position());
        custom.setDeltaMovement(oldEntity.getDeltaMovement());
        if (oldEntity instanceof net.minecraft.world.entity.item.ItemEntity) {
            custom.setPickUpDelay(40);
        }
        return custom;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Glint skaliert mit Soul-Stacks. Unter Threshold -> kein Glint (Scythe wirkt "hungrig").
        // Threshold erreicht -> Vanilla-Glint. Bei MAX -> Glint + Extra-Particles (siehe inventoryTick).
        return stack.getOrCreateTag().getInt(DISPLAY_STACKS_TAG) >= FOIL_THRESHOLD;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);

        // Holder-Position kontinuierlich tracken (für Soul Compass).
        // KEIN Auto-Delete mehr: Sense bleibt im Spieler-Inventar, auch wenn Gen wechselt.
        if (level instanceof ServerLevel sl && entity instanceof net.minecraft.server.level.ServerPlayer sp) {
            net.mysith.world.ScytheTracker tracker = net.mysith.world.ScytheTracker.get(sl);
            int worldGen = tracker.getGeneration();
            int stackGen = stack.getOrCreateTag().getInt(net.mysith.event.ScytheCraftHandler.SCYTHE_GEN_TAG);
            if (worldGen > 0 && stackGen == worldGen && sp.getUUID().equals(tracker.getHolderUuid())) {
                if (sl.getGameTime() % 20 == 0) {
                    tracker.updateHolder(sp);
                }
            }
        }

        // Wenn nicht in der aktiven Hand: Glint-NBT auf 0 setzen (keine Particles).
        // Ohne diesen Reset würde eine im Inventar geparkte Scythe ihren Glint behalten obwohl
        // sie nicht "gefüttert" wird.
        if (!selected) {
            if (level instanceof ServerLevel) {
                int cached = stack.getOrCreateTag().getInt(DISPLAY_STACKS_TAG);
                if (cached != 0) stack.getOrCreateTag().putInt(DISPLAY_STACKS_TAG, 0);
            }
            return;
        }

        if (!(entity instanceof Player player) || !(level instanceof ServerLevel serverLevel)) return;

        // Idle-Hand-Partikel: dunkelrote Dust-Funken (nur in Brusthöhe und unten, nicht im Sichtfeld)
        if (level.getRandom().nextFloat() < 0.25F) {
            net.minecraft.core.particles.DustParticleOptions idleDust =
                    new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(0.6F, 0.0F, 0.05F), 0.8F);
            double dx = (level.getRandom().nextDouble() - 0.5) * 0.4;
            double dz = (level.getRandom().nextDouble() - 0.5) * 0.4;
            // Y range: 0.6 bis 1.3 (Brusthöhe abwärts, nicht im Sichtfeld)
            double dy = 0.6 + level.getRandom().nextDouble() * 0.7;
            serverLevel.sendParticles(idleDust,
                    entity.getX() + dx, entity.getY() + dy, entity.getZ() + dz,
                    1, 0.0, 0.01, 0.0, 0.0);
        }

        // Soul-Aura skaliert mit Soul-Stacks (Crimson rote Dust-Partikel)
        int stacks = net.mysith.event.ScytheCombatHandler.getSoulStacks(player);

        // Display-Stacks ins ItemStack-NBT syncen, damit isFoil() (client-side) den Glint anpasst.
        // Nur bei Wertänderung schreiben um unnötige Network-Packets zu vermeiden.
        int displayStacks = stack.getOrCreateTag().getInt(DISPLAY_STACKS_TAG);
        if (displayStacks != stacks) {
            stack.getOrCreateTag().putInt(DISPLAY_STACKS_TAG, stacks);
        }

        if (stacks > 0) {
            net.minecraft.core.particles.DustParticleOptions crimsonDust =
                    new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(0.85F, 0.05F, 0.1F), 1.2F);
            for (int i = 0; i < stacks; i++) {
                double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
                // Grösserer Radius (1.4-2.0) damit's nicht in der 1st-Person-Sicht ist
                double radius = 1.4 + level.getRandom().nextDouble() * 0.6;
                double px = entity.getX() + Math.cos(angle) * radius;
                double pz = entity.getZ() + Math.sin(angle) * radius;
                // Y range: 0 bis 1.3 (Brusthöhe abwärts)
                double py = entity.getY() + level.getRandom().nextDouble() * 1.3;
                serverLevel.sendParticles(crimsonDust, px, py, pz, 1, 0.0, 0.02, 0.0, 0.01);
            }
        }

        // Curse Mode visualisieren: rote Funken wenn HP < 30%
        if (player.getHealth() / player.getMaxHealth() < 0.3F && level.getRandom().nextFloat() < 0.4F) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR,
                    entity.getX(), entity.getY() + 1.0, entity.getZ(),
                    2, 0.4, 0.4, 0.4, 0.05);
        }

        // Bei MAX-Stacks (voll geladen): pulsierender Ring + Soul-Slash-Partikel um Spieler.
        // Phase basierend auf Game-Time → "Atmen" der Aura.
        if (stacks >= MAX_STACKS_FOR_DISPLAY) {
            long t = level.getGameTime();
            double pulse = 0.5 + 0.5 * Math.sin(t * 0.18);
            int ringPoints = 12;
            double ringRadius = 1.1 + pulse * 0.4;
            for (int i = 0; i < ringPoints; i++) {
                double angle = (i / (double) ringPoints) * Math.PI * 2.0 + t * 0.04;
                double px = entity.getX() + Math.cos(angle) * ringRadius;
                double pz = entity.getZ() + Math.sin(angle) * ringRadius;
                double py = entity.getY() + 0.05;
                net.minecraft.core.particles.DustParticleOptions chargedDust =
                        new net.minecraft.core.particles.DustParticleOptions(
                                new org.joml.Vector3f(1.0F, 0.15F, 0.2F), 1.0F + (float) pulse * 0.6F);
                serverLevel.sendParticles(chargedDust, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
            }
            // Soul-Slash-Partikel oben am Kopf zur Verstärkung des Glints
            if (level.getRandom().nextFloat() < 0.35F) {
                serverLevel.sendParticles(net.mysith.registry.ModParticles.SOUL_SLASH.get(),
                        entity.getX(), entity.getY() + entity.getBbHeight() + 0.3, entity.getZ(),
                        1, 0.2, 0.1, 0.2, 0.0);
            }
            // Sound-Tick: "hungry whisper" alle 60 Ticks
            if (t % 60 == 0) {
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        net.mysith.registry.ModSounds.SCYTHE_CHARGED.get(),
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.4F, 0.6F);
            }
        }
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (player.level() instanceof ServerLevel serverLevel) {
            long id = GeoItem.getOrAssignId(stack, serverLevel);
            triggerAnim(player, id, "controller", "swing");

            if (entity instanceof LivingEntity target) {
                EnchantmentHandlers.applyDeathMark(stack, target);
            }
        }
        return false;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (result) {
            EnchantmentHandlers.applyDeathMark(stack, target);
        }
        spawnSlashArc(attacker, target);
        Level lvl = attacker.level();
        float pitchJitter = 0.92F + lvl.getRandom().nextFloat() * 0.16F;
        // Custom Scythe-Hit-Sound (stumm bis .ogg vorhanden)
        lvl.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                ModSounds.SCYTHE_HIT.get(), SoundSource.PLAYERS, 1.0F, pitchJitter);
        // Vanilla-Layer Fallback (high pitch wither_shoot + sweep)
        lvl.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.3F, 2.0F * pitchJitter);
        lvl.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.4F * pitchJitter);
        lvl.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.4F, 0.7F);
        return result;
    }

    private void performAirSlash(Level level, Player player, ItemStack stack, int tier) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        int sharpnessLevel = stack.getEnchantmentLevel(Enchantments.SHARPNESS);
        float sharpnessBonus = sharpnessLevel > 0 ? 0.5F + 0.5F * sharpnessLevel : 0.0F;
        float damage = (8.0F + sharpnessBonus) * damageMult(tier);

        double range = 10.0 * radiusMult(tier);

        // Volle Blick-Richtung (3D) - Yaw + Pitch
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();

        // Origin: rechte Hand des Spielers (rechts versetzt + Brusthöhe)
        net.minecraft.world.phys.Vec3 right = new net.minecraft.world.phys.Vec3(look.z, 0, -look.x).normalize();
        double originX = player.getX() + right.x * 0.4;
        double originY = player.getY() + player.getEyeHeight() - 0.3;
        double originZ = player.getZ() + right.z * 0.4;

        // 20 Partikel entlang Schwung-Linie (folgt Blick-Richtung)
        for (int step = 1; step <= 20; step++) {
            double t = step / 20.0;
            double px = originX + look.x * t * range;
            double py = originY + look.y * t * range;
            double pz = originZ + look.z * t * range;
            serverLevel.sendParticles(ModParticles.SOUL_SLASH.get(),
                    px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
        }

        // Damage-Beam entlang der Blick-Richtung
        double halfWidth = 1.0;
        DamageSource src = player.damageSources().playerAttack(player);
        double endX = originX + look.x * range;
        double endY = originY + look.y * range;
        double endZ = originZ + look.z * range;

        AABB beam = new AABB(
                Math.min(originX, endX) - halfWidth, Math.min(originY, endY) - halfWidth, Math.min(originZ, endZ) - halfWidth,
                Math.max(originX, endX) + halfWidth, Math.max(originY, endY) + halfWidth, Math.max(originZ, endZ) + halfWidth);

        List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, beam,
                validTargetPredicate(player));

        // End Crystals: keine LivingEntities, separat targeten (für End-Dragon-Kämpfe).
        // Einfacher Beam-Check ist hier nicht nötig: Crystal hat sehr wenig HP, jede Berührung reicht.
        List<net.minecraft.world.entity.boss.enderdragon.EndCrystal> crystals =
                level.getEntitiesOfClass(net.minecraft.world.entity.boss.enderdragon.EndCrystal.class, beam,
                        e -> e.isAlive());
        for (net.minecraft.world.entity.boss.enderdragon.EndCrystal crystal : crystals) {
            crystal.hurt(src, damage);
        }

        for (LivingEntity target : hit) {
            // Projektion auf Blick-Strahl prüfen
            double relX = target.getX() - originX;
            double relY = target.getY() + target.getBbHeight() / 2 - originY;
            double relZ = target.getZ() - originZ;
            double projection = relX * look.x + relY * look.y + relZ * look.z;
            if (projection < 0 || projection > range) continue;

            // Distanz zum Strahl
            double closestX = originX + look.x * projection;
            double closestY = originY + look.y * projection;
            double closestZ = originZ + look.z * projection;
            double dist = Math.sqrt(
                    Math.pow(target.getX() - closestX, 2) +
                    Math.pow(target.getY() + target.getBbHeight() / 2 - closestY, 2) +
                    Math.pow(target.getZ() - closestZ, 2));
            if (dist > halfWidth) continue;

            if (target.hurt(src, damage)) {
                EnchantmentHandlers.applyDeathMark(stack, target);
            }
            if (tier == 3) {
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.mysith.registry.ModEffects.REAPERS_MARK.get(),
                        REAPER_MARK_DURATION_TICKS, 0, false, true, true));
            }
        }

        // Custom Air-Slash-Sound (stumm bis .ogg vorhanden) + Vanilla-Layer
        float airPitch = 0.95F + serverLevel.getRandom().nextFloat() * 0.1F;
        level.playSound(null, originX, originY, originZ,
                ModSounds.SCYTHE_AIR_SLASH.get(), SoundSource.PLAYERS, 1.2F, airPitch);
        level.playSound(null, originX, originY, originZ,
                SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0F, 1.8F * airPitch);
        level.playSound(null, originX, originY, originZ,
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.4F, 1.6F);
        level.playSound(null, originX, originY, originZ,
                SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.8F, 0.5F);
    }

    private void spawnSlashArc(LivingEntity attacker, LivingEntity target) {
        if (!(attacker.level() instanceof ServerLevel serverLevel)) return;

        // Zentrum: zwischen Spieler und Target
        double cx = (attacker.getX() + target.getX()) / 2;
        double cy = (attacker.getY() + target.getY()) / 2 + 0.8;
        double cz = (attacker.getZ() + target.getZ()) / 2;

        // Richtung: vom Spieler zum Target
        double dx = target.getX() - attacker.getX();
        double dz = target.getZ() - attacker.getZ();
        double horizontalDist = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
        double dirX = dx / horizontalDist;
        double dirZ = dz / horizontalDist;

        // Senkrecht zur Richtung (Slash schwingt seitlich)
        double perpX = -dirZ;
        double perpZ = dirX;

        // 1 grosser zentraler Slash auf dem Target
        serverLevel.sendParticles(ModParticles.SOUL_SLASH.get(),
                target.getX(), target.getY() + 1.0, target.getZ(),
                1, 0.0, 0.0, 0.0, 0.0);

        // 6 Partikel in Bogen entlang Slash-Bahn
        int count = 6;
        double arcWidth = 1.4;
        for (int i = 0; i < count; i++) {
            double t = (i / (double) (count - 1)) * 2.0 - 1.0;
            double offsetH = t * arcWidth;
            double offsetV = (1.0 - t * t) * 0.4;

            double px = cx + perpX * offsetH;
            double py = cy + offsetV;
            double pz = cz + perpZ * offsetH;

            serverLevel.sendParticles(ModParticles.SOUL_SLASH.get(),
                    px, py, pz,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return MAX_USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        // NONE: kein spezieller Halte-Pose. Sense bleibt in normaler Hand-Position.
        // Das "Charging" wird über Partikel + Sounds + FOV-Zoom (ab Tier 2) kommuniziert,
        // nicht über eine awkward Trident/Bow-Pose.
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksLeft) {
        if (!(entity instanceof Player player)) return;
        int ticksUsed = MAX_USE_DURATION - ticksLeft;

        // Slowness während Aufladen (vanilla Bow-Feel). Kurze Dauer, jede Tick refreshen.
        if (!level.isClientSide()) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 4, 2,
                    false, false, false));
        }

        if (!(level instanceof ServerLevel sl)) return;

        int tier = tierFromTicks(ticksUsed);

        // Tier-Wechsel-Sounds: thematische "Charge"-Klänge (keine Noten/Glocken).
        if (ticksUsed == TIER_2_TICKS) {
            // Tier 2: schwerer magischer Klick (Respawn-Anchor) + Soul-Whisper
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.SCYTHE_CHARGED.get(), SoundSource.PLAYERS, 0.6F, 0.8F);
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.6F, 1.3F);
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.5F, 0.7F);
        } else if (ticksUsed == TIER_3_TICKS) {
            // Tier 3: tiefer Conduit-Thrum + Warden-Heartbeat + Soul-Escape — alles tief und düster
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.SCYTHE_CHARGED.get(), SoundSource.PLAYERS, 1.0F, 0.5F);
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 0.7F, 0.6F);
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.9F, 0.7F);
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.2F, 0.4F);
        }

        // Lade-Partikel: dichte und Radius wachsen mit Tier.
        int particlesPerTick = tier == 3 ? 8 : tier == 2 ? 4 : 2;
        double ringRadius = tier == 3 ? 1.2 : tier == 2 ? 0.9 : 0.6;
        net.minecraft.core.particles.DustParticleOptions chargeDust =
                new net.minecraft.core.particles.DustParticleOptions(
                        tier == 3 ? new org.joml.Vector3f(1.0F, 0.15F, 0.2F)
                                  : tier == 2 ? new org.joml.Vector3f(0.85F, 0.05F, 0.1F)
                                              : new org.joml.Vector3f(0.6F, 0.0F, 0.05F),
                        0.8F + tier * 0.2F);
        for (int i = 0; i < particlesPerTick; i++) {
            double angle = (ticksUsed * 0.4 + i * (Math.PI * 2.0 / particlesPerTick)) % (Math.PI * 2.0);
            double px = player.getX() + Math.cos(angle) * ringRadius;
            double pz = player.getZ() + Math.sin(angle) * ringRadius;
            double py = player.getY() + 0.1 + sl.getRandom().nextDouble() * 1.2;
            sl.sendParticles(chargeDust, px, py, pz, 1, 0.0, 0.02, 0.0, 0.0);
        }

        // Tier 3: pulsierende Soul-Slash-Partikel oben, signalisiert "voll geladen".
        if (tier == 3 && ticksUsed % 4 == 0) {
            sl.sendParticles(net.mysith.registry.ModParticles.SOUL_SLASH.get(),
                    player.getX(), player.getY() + player.getEyeHeight() + 0.3, player.getZ(),
                    1, 0.3, 0.0, 0.3, 0.0);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int ticksLeft) {
        if (!(entity instanceof Player player)) return;
        int ticksUsed = MAX_USE_DURATION - ticksLeft;
        int tier = tierFromTicks(ticksUsed);

        if (!level.isClientSide()) {
            double effectiveRadius = WHIRLWIND_RADIUS * radiusMult(tier);
            AABB checkArea = player.getBoundingBox().inflate(effectiveRadius);
            // Pet-Auschluss damit der eigene Wolf nicht den Whirlwind triggert
            boolean hasTarget = !level.getEntitiesOfClass(LivingEntity.class, checkArea,
                    validTargetPredicate(player)).isEmpty();

            if (hasTarget) {
                performWhirlwind(level, player, stack, tier);
            } else {
                performAirSlash(level, player, stack, tier);
            }

            // Whirlwind Master reduziert Cooldown; Tier 2/3 erhöhen ihn dafür.
            int wmLevel = stack.getEnchantmentLevel(net.mysith.registry.ModEnchantments.WHIRLWIND_MASTER.get());
            int cooldown = WHIRLWIND_COOLDOWN_TICKS + (tier - 1) * 20 - (wmLevel * 20);
            player.getCooldowns().addCooldown(this, Math.max(20, cooldown));
            stack.hurtAndBreak(2 + tier, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));

            if (level instanceof ServerLevel serverLevel) {
                long id = GeoItem.getOrAssignId(stack, serverLevel);
                triggerAnim(player, id, "controller", "swing");
            }
        }

        // Impact-Sounds: Pitch sinkt mit Tier (tiefer = mächtiger).
        float impactPitch = 0.55F + level.getRandom().nextFloat() * 0.15F - (tier - 1) * 0.1F;
        float volMult = 1.0F + (tier - 1) * 0.3F;
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SCYTHE_WHIRLWIND.get(), SoundSource.PLAYERS, 1.5F * volMult, impactPitch);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SCYTHE_SWING_HEAVY.get(), SoundSource.PLAYERS, 1.0F * volMult, impactPitch + 0.2F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.0F * volMult, impactPitch);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0F * volMult, 1.4F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.8F * volMult, 1.6F * impactPitch);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0F * volMult, 0.5F);
        if (tier == 3) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.7F, 1.5F);
        }
    }

    private void performWhirlwind(Level level, Player player, ItemStack stack, int tier) {
        double effectiveRadius = WHIRLWIND_RADIUS * radiusMult(tier);
        AABB area = player.getBoundingBox().inflate(effectiveRadius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                validTargetPredicate(player));

        DamageSource source = player.damageSources().playerAttack(player);

        // End Crystals im Radius zerstören (für End-Dragon-Kämpfe)
        List<net.minecraft.world.entity.boss.enderdragon.EndCrystal> crystals =
                level.getEntitiesOfClass(net.minecraft.world.entity.boss.enderdragon.EndCrystal.class, area,
                        e -> e.isAlive());
        for (net.minecraft.world.entity.boss.enderdragon.EndCrystal crystal : crystals) {
            crystal.hurt(source, WHIRLWIND_BASE_DAMAGE * damageMult(tier));
        }

        // Sharpness-Bonus (vanilla: 0.5 + 0.5 per level → Lvl 5 = 3.0)
        int sharpnessLevel = stack.getEnchantmentLevel(Enchantments.SHARPNESS);
        float sharpnessBonus = sharpnessLevel > 0 ? 0.5F + 0.5F * sharpnessLevel : 0.0F;
        float tierDmgMult = damageMult(tier);

        for (LivingEntity target : targets) {
            // 10 Base + 25% current HP + Sharpness, dann Tier-Multiplikator
            float damage = (WHIRLWIND_BASE_DAMAGE + target.getHealth() * WHIRLWIND_DAMAGE_PERCENT + sharpnessBonus) * tierDmgMult;
            if (target.hurt(source, damage)) {
                EnchantmentHandlers.applyDeathMark(stack, target);
            }
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            if (tier == 3) {
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.mysith.registry.ModEffects.REAPERS_MARK.get(),
                        REAPER_MARK_DURATION_TICKS, 0, false, true, true));
            }
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            double dist = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
            target.knockback(WHIRLWIND_KNOCKBACK * radiusMult(tier), -dx / dist, -dz / dist);
        }

        if (level instanceof ServerLevel serverLevel) {
            // 2 konzentrische Slash-Ringe (skalieren mit Tier-Radius)
            double rMul = radiusMult(tier);
            double[] ringRadii = {2.2 * rMul, 3.6 * rMul};
            int[] ringCounts = {6, 10};

            for (int r = 0; r < ringRadii.length; r++) {
                double radius = ringRadii[r];
                int count = ringCounts[r];
                double yOffset = (r == 0) ? 1.0 : 0.6;

                for (int a = 0; a < count; a++) {
                    double angle = (a / (double) count) * Math.PI * 2.0 + r * 0.3;
                    double dirX = Math.cos(angle);
                    double dirZ = Math.sin(angle);

                    double px = player.getX() + dirX * radius;
                    double pz = player.getZ() + dirZ * radius;
                    double py = player.getY() + yOffset + serverLevel.getRandom().nextDouble() * 0.3;

                    serverLevel.sendParticles(ModParticles.SOUL_SLASH.get(),
                            px, py, pz,
                            1, 0.0, 0.0, 0.0, 0.0);
                }
            }

            // Zentraler Burst beim Spieler
            serverLevel.sendParticles(ModParticles.SOUL_SLASH.get(),
                    player.getX(), player.getY() + 1.3, player.getZ(),
                    2, 0.3, 0.2, 0.3, 0.0);

            // Bodenstaub-Ring für Impact-Feel
            int dustCount = 60;
            for (int i = 0; i < dustCount; i++) {
                double angle = (i / (double) dustCount) * Math.PI * 2.0;
                double radius = 2.5 + serverLevel.getRandom().nextDouble() * 1.5;
                double px = player.getX() + Math.cos(angle) * radius;
                double pz = player.getZ() + Math.sin(angle) * radius;
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                        px, player.getY() + 0.1, pz,
                        1, 0.05, 0.05, 0.05, 0.02);
            }

            // Rote Crit-Funken für extra Drama
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    30, 1.8, 0.6, 1.8, 0.3);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.sith_scythe.tooltip1").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.usless_mobs.sith_scythe.tooltip2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.usless_mobs.sith_scythe.tooltip3").withStyle(ChatFormatting.GRAY));

        // Soul Imprint: zeigt den vorherigen Holder wenn die Sense gestorben ist
        String imprint = stack.getOrCreateTag().getString(net.mysith.event.CombatPolishHandler.SOUL_IMPRINT_KEY);
        if (!imprint.isEmpty()) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.translatable("item.usless_mobs.sith_scythe.imprint", imprint)
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ScytheRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new ScytheRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(this, "controller", 0, state -> PlayState.STOP)
                        .triggerableAnim("swing", SWING_ANIM)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
