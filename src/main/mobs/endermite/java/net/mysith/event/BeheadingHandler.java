package net.mysith.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.registry.ModEnchantments;

@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID)
public class BeheadingHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity killer)) return;

        ItemStack weapon = killer.getMainHandItem();
        int level = weapon.getEnchantmentLevel(ModEnchantments.BEHEADING.get());
        if (level <= 0) return;

        LivingEntity target = event.getEntity();
        float chance = level * 0.05f;
        if (target.getRandom().nextFloat() >= chance) return;

        ItemStack head = getHeadFor(target);
        if (head.isEmpty()) return;

        target.spawnAtLocation(head);
    }

    private static ItemStack getHeadFor(LivingEntity entity) {
        if (entity instanceof WitherSkeleton) return new ItemStack(Items.WITHER_SKELETON_SKULL);
        if (entity instanceof Skeleton) return new ItemStack(Items.SKELETON_SKULL);
        if (entity instanceof Zombie) return new ItemStack(Items.ZOMBIE_HEAD);
        if (entity instanceof Creeper) return new ItemStack(Items.CREEPER_HEAD);
        if (entity instanceof EnderDragon) return new ItemStack(Items.DRAGON_HEAD);
        if (entity instanceof Player player) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            CompoundTag tag = head.getOrCreateTag();
            tag.putString("SkullOwner", player.getGameProfile().getName());
            return head;
        }
        return ItemStack.EMPTY;
    }
}
