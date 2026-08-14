package com.Momik.usless_mobs.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.Momik.usless_mobs.allegiance.AllegiancePath;
import com.Momik.usless_mobs.allegiance.AllegianceUtil;
import com.Momik.usless_mobs.client.WornTruePathArmorModel;
import com.Momik.usless_mobs.effect.SlimePowerToggle;
import com.Momik.usless_mobs.Usless_mobs;
import com.Momik.usless_mobs.world.TrueCrownTracker;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

public class TruePathArmorItem extends ArmorItem {
    private static final String NEXT_GUARD_KEY = "UslessMobs_TruePathHelmet_NextGuard";

    public enum Path {
        VOID(ChatFormatting.DARK_PURPLE,    "true_void"),
        CELESTIAL(ChatFormatting.AQUA,      "true_celestial"),
        LIVING(ChatFormatting.GREEN,        "true_living");

        public final ChatFormatting color;
        public final String key;

        Path(ChatFormatting color, String key) {
            this.color = color;
            this.key = key;
        }
    }

    private final Path path;
    private final Multimap<Attribute, AttributeModifier> attributes;

    public TruePathArmorItem(Path path, Type type, Properties properties) {
        super(ArmorMaterials.NETHERITE, type, properties);
        this.path = path;
        this.attributes = buildAttributes(path, type);
    }

    public Path getPath() {
        return path;
    }

    private static Multimap<Attribute, AttributeModifier> buildAttributes(Path path, Type type) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        String ns = path.key;
        EquipmentSlot slot = type.getSlot();

        b.put(Attributes.ARMOR,                UmrAttributes.addition(ns, slot, "armor", "True path armor", armorValue(path, type)));
        b.put(Attributes.ARMOR_TOUGHNESS,      UmrAttributes.addition(ns, slot, "tough", "True path tough", toughnessValue(path, type)));
        b.put(Attributes.KNOCKBACK_RESISTANCE, UmrAttributes.addition(ns, slot, "kb",    "True path poise", knockbackValue(path, type)));

        double hp = bonusHealth(path, type);
        if (hp > 0.0D) {
            b.put(Attributes.MAX_HEALTH, UmrAttributes.addition(ns, slot, "hp", "True path vitality", hp));
        }
        double speed = bonusSpeed(path, type);
        if (speed > 0.0D) {
            b.put(Attributes.MOVEMENT_SPEED, UmrAttributes.multiplyTotal(ns, slot, "speed", "True path stride", speed));
        }
        double atk = bonusAttack(path, type);
        if (atk > 0.0D) {
            b.put(Attributes.ATTACK_DAMAGE, UmrAttributes.multiplyTotal(ns, slot, "atk", "True path edge", atk));
        }
        return b.build();
    }

    // ---------- per-path / per-slot stat tables ----------
    // Reference (Balance full set): 38 armor, 21 tough, 0.70 KB, +12 HP, +5% speed.

    private static double armorValue(Path path, Type type) {
        return switch (path) {
            // Void = glass cannon. LESS armor than Balance, but gains attack-damage bonus.
            case VOID      -> switch (type) { case HELMET -> 6.0D; case CHESTPLATE -> 10.0D; case LEGGINGS -> 9.0D; case BOOTS -> 5.0D; };
            // Celestial = mobility, similar armor to Balance.
            case CELESTIAL -> switch (type) { case HELMET -> 8.0D; case CHESTPLATE -> 13.0D; case LEGGINGS -> 11.0D; case BOOTS -> 6.0D; };
            // Living = tank, more armor than Balance.
            case LIVING    -> switch (type) { case HELMET -> 9.0D; case CHESTPLATE -> 15.0D; case LEGGINGS -> 13.0D; case BOOTS -> 7.0D; };
        };
    }

    private static double toughnessValue(Path path, Type type) {
        return switch (path) {
            case VOID      -> 4.0D;
            case CELESTIAL -> type == Type.CHESTPLATE ? 6.0D : 5.0D;
            case LIVING    -> type == Type.CHESTPLATE ? 7.0D : 6.0D;
        };
    }

    private static double knockbackValue(Path path, Type type) {
        return switch (path) {
            case VOID      -> 0.10D;
            case CELESTIAL -> type == Type.CHESTPLATE ? 0.20D : 0.15D;
            case LIVING    -> type == Type.CHESTPLATE ? 0.25D : 0.20D;
        };
    }

    private static double bonusHealth(Path path, Type type) {
        return switch (path) {
            case VOID      -> 0.0D;
            case CELESTIAL -> switch (type) { case HELMET -> 4.0D; case CHESTPLATE -> 6.0D; case LEGGINGS -> 2.0D; case BOOTS -> 0.0D; };
            case LIVING    -> switch (type) { case HELMET -> 6.0D; case CHESTPLATE -> 10.0D; case LEGGINGS -> 4.0D; case BOOTS -> 4.0D; };
        };
    }

    private static double bonusSpeed(Path path, Type type) {
        if (path != Path.CELESTIAL && !(path == Path.LIVING && type == Type.BOOTS)) {
            return 0.0D;
        }
        return switch (path) {
            case CELESTIAL -> switch (type) { case CHESTPLATE, LEGGINGS, BOOTS -> 0.05D; default -> 0.0D; };
            case LIVING    -> type == Type.BOOTS ? 0.03D : 0.0D;
            default        -> 0.0D;
        };
    }

    private static double bonusAttack(Path path, Type type) {
        if (path != Path.VOID) {
            return 0.0D;
        }
        return switch (type) { case HELMET -> 0.05D; case CHESTPLATE -> 0.10D; case LEGGINGS -> 0.05D; case BOOTS -> 0.05D; };
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == getType().getSlot() ? attributes : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        String layer = getType() == Type.LEGGINGS ? "_layer_2.png" : "_layer_1.png";
        return Usless_mobs.MODID + ":textures/models/armor/" + path.key + layer;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private HumanoidModel<?> model;

            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                if (slot != getType().getSlot()) {
                    return original;
                }
                if (model == null) {
                    model = WornTruePathArmorModel.create(path, getType());
                }
                ((HumanoidModel) original).copyPropertiesTo((HumanoidModel) model);
                WornTruePathArmorModel.showForType(model, getType());
                return model;
            }
        });
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!(entity instanceof Player player) || player.getItemBySlot(getType().getSlot()) != stack) {
            return;
        }
        if (SlimePowerToggle.areEffectsDisabled(player)) {
            return;
        }
        if (getType() == Type.HELMET) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && !claimOrValidatePath(serverPlayer)) {
                return;
            }
            applyCrownAura(player);
            sendCrownParticles(level, player);
            clearPathDebuffs(level, player);
            applyEmergencyGuard(level, player);
        }
        if (!hasFullSet(player, path)) {
            return;
        }

        // Constant, modest aura while full set is worn.
        switch (path) {
            case VOID -> {
                SlimePowerToggle.applyFiltered(player, MobEffects.DAMAGE_BOOST, 60, 0);
                SlimePowerToggle.applyFiltered(player, MobEffects.NIGHT_VISION, 260, 0);
            }
            case CELESTIAL -> {
                SlimePowerToggle.applyFiltered(player, MobEffects.MOVEMENT_SPEED, 60, 0);
                SlimePowerToggle.applyFiltered(player, MobEffects.JUMP, 60, 0);
            }
            case LIVING -> {
                SlimePowerToggle.applyFiltered(player, MobEffects.DAMAGE_RESISTANCE, 60, 0);
            }
        }

        if (level.isClientSide) {
            return;
        }

        // Passive healing for tank set: 1 HP every 4 s when below max.
        if (path == Path.LIVING && player.tickCount % 80 == 0 && player.getHealth() < player.getMaxHealth()) {
            player.heal(1.0F);
        }
        // Modest passive heal for Celestial: 0.5 HP every 6 s when below max.
        if (path == Path.CELESTIAL && player.tickCount % 120 == 0 && player.getHealth() < player.getMaxHealth()) {
            player.heal(0.5F);
        }
        // Void is glass-cannon: no passive heal. Lifesteal is handled by LifestealHandler on hit.
    }

    private boolean claimOrValidatePath(ServerPlayer player) {
        AllegiancePath allegiancePath = toAllegiancePath();
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        return AllegianceUtil.setIfUnset(player, allegiancePath)
                && TrueCrownTracker.get(server).claimFinalPath(allegiancePath, player.getUUID(), player.getGameProfile().getName());
    }

    private AllegiancePath toAllegiancePath() {
        return switch (path) {
            case VOID -> AllegiancePath.VOID;
            case CELESTIAL -> AllegiancePath.CELESTIAL;
            case LIVING -> AllegiancePath.LIVING;
        };
    }

    private void applyCrownAura(Player player) {
        switch (path) {
            case VOID -> {
                SlimePowerToggle.applyFiltered(player, MobEffects.DAMAGE_RESISTANCE, 80, 1);
                SlimePowerToggle.applyFiltered(player, MobEffects.DIG_SPEED, 80, 1);
                SlimePowerToggle.applyFiltered(player, MobEffects.DAMAGE_BOOST, 80, 0);
                SlimePowerToggle.applyFiltered(player, MobEffects.NIGHT_VISION, 260, 0);
            }
            case CELESTIAL -> {
                SlimePowerToggle.applyFiltered(player, MobEffects.MOVEMENT_SPEED, 80, 1);
                SlimePowerToggle.applyFiltered(player, MobEffects.JUMP, 80, 1);
                SlimePowerToggle.applyFiltered(player, MobEffects.DIG_SPEED, 80, 1);
                SlimePowerToggle.applyFiltered(player, MobEffects.NIGHT_VISION, 260, 0);
                SlimePowerToggle.applyFiltered(player, MobEffects.ABSORPTION, 100, 0);
            }
            case LIVING -> {
                SlimePowerToggle.applyFiltered(player, MobEffects.DAMAGE_RESISTANCE, 80, 0);
                SlimePowerToggle.applyFiltered(player, MobEffects.REGENERATION, 100, 0);
                SlimePowerToggle.applyFiltered(player, MobEffects.MOVEMENT_SPEED, 80, 0);
                SlimePowerToggle.applyFiltered(player, MobEffects.NIGHT_VISION, 260, 0);
            }
        }
    }

    private void clearPathDebuffs(Level level, Player player) {
        if (level.isClientSide || player.tickCount % 60 != 0) {
            return;
        }
        player.removeEffect(MobEffects.DARKNESS);
        player.removeEffect(MobEffects.WITHER);
        if (path == Path.LIVING) {
            player.removeEffect(MobEffects.POISON);
        }
    }

    private void applyEmergencyGuard(Level level, Player player) {
        int now = player.tickCount;
        int nextGuard = player.getPersistentData().getInt(NEXT_GUARD_KEY + "_" + path.name());
        if (level.isClientSide || player.getHealth() > player.getMaxHealth() * 0.35F || nextGuard > now) {
            return;
        }

        switch (path) {
            case VOID -> {
                SlimePowerToggle.applyFiltered(player, MobEffects.ABSORPTION, 260, 2);
                SlimePowerToggle.applyFiltered(player, MobEffects.DAMAGE_BOOST, 180, 1);
                SlimePowerToggle.applyFiltered(player, MobEffects.FIRE_RESISTANCE, 260, 0);
            }
            case CELESTIAL -> {
                SlimePowerToggle.applyFiltered(player, MobEffects.ABSORPTION, 320, 2);
                SlimePowerToggle.applyFiltered(player, MobEffects.REGENERATION, 120, 0);
                SlimePowerToggle.applyFiltered(player, MobEffects.MOVEMENT_SPEED, 180, 1);
            }
            case LIVING -> {
                SlimePowerToggle.applyFiltered(player, MobEffects.ABSORPTION, 240, 1);
                SlimePowerToggle.applyFiltered(player, MobEffects.REGENERATION, 220, 1);
                SlimePowerToggle.applyFiltered(player, MobEffects.DAMAGE_RESISTANCE, 180, 0);
            }
        }
        player.getPersistentData().putInt(NEXT_GUARD_KEY + "_" + path.name(), now + 1200);
    }

    private void sendCrownParticles(Level level, Player player) {
        if (!(level instanceof ServerLevel serverLevel) || player.tickCount % 14 != 0) {
            return;
        }
        ParticleOptions particle = switch (path) {
            case VOID -> ParticleTypes.SCULK_SOUL;
            case CELESTIAL -> ParticleTypes.END_ROD;
            case LIVING -> ParticleTypes.COMPOSTER;
        };
        int count = path == Path.LIVING ? 8 : path == Path.CELESTIAL ? 7 : 6;
        serverLevel.sendParticles(particle, player.getX(), player.getY(1.05D), player.getZ(),
                count, 0.45D, 0.45D, 0.45D, 0.025D);
        if (path == Path.LIVING && player.tickCount % 42 == 0) {
            serverLevel.sendParticles(ParticleTypes.HEART, player.getX(), player.getY(1.35D), player.getZ(),
                    1, 0.15D, 0.15D, 0.15D, 0.0D);
        }
    }

    private static boolean hasFullSet(Player player, Path path) {
        return matches(player, EquipmentSlot.HEAD, path)
                && matches(player, EquipmentSlot.CHEST, path)
                && matches(player, EquipmentSlot.LEGS, path)
                && matches(player, EquipmentSlot.FEET, path);
    }

    private static boolean matches(Player player, EquipmentSlot slot, Path path) {
        ItemStack stack = player.getItemBySlot(slot);
        return stack.getItem() instanceof TruePathArmorItem armor && armor.path == path;
    }

    public static @Nullable Path getWornFullSetPath(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(head.getItem() instanceof TruePathArmorItem armor)) {
            return null;
        }
        return hasFullSet(player, armor.path) ? armor.path : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.usless_mobs.true_path." + path.key + ".tooltip").withStyle(path.color));
        tooltip.add(Component.translatable("item.usless_mobs.true_path." + path.key + ".set_bonus").withStyle(ChatFormatting.GOLD));
        if (getType() == Type.HELMET) {
            tooltip.add(Component.translatable("item.usless_mobs.true_path." + path.key + ".crown").withStyle(path.color));
            tooltip.add(Component.translatable("item.usless_mobs.true_path." + path.key + ".guard").withStyle(ChatFormatting.GOLD));
        }
    }

}
