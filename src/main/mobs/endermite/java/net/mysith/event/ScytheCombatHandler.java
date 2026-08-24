package net.mysith.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mysith.registry.ModItems;

@Mod.EventBusSubscriber(modid = com.Momik.usless_mobs.Usless_mobs.MODID)
public class ScytheCombatHandler {

    private static final String SOUL_STACKS_KEY = "MysithSoulStacks";
    private static final String LAST_KILL_KEY = "MysithLastKill";
    private static final int MAX_STACKS = 10;
    private static final long STACK_DECAY_TICKS = 200; // 10 sec

    @SubscribeEvent
    public static void onCritHit(CriticalHitEvent event) {
        Player player = event.getEntity();
        if (ModItems.isReaperScythe(player.getMainHandItem())) {
            // 2.0x crit statt vanilla 1.5x
            event.setDamageModifier(2.0F);
        }
    }

    @SubscribeEvent
    public static void onKillHeal(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer)) return;
        ItemStack weapon = killer.getMainHandItem();
        if (!ModItems.isReaperScythe(weapon)) return;

        // Passive Heilung: 0.5 HP pro Kill
        killer.heal(0.5F);

        // Soul-Stack erhöhen (max 10)
        CompoundTag data = killer.getPersistentData();
        int stacks = data.getInt(SOUL_STACKS_KEY);
        stacks = Math.min(MAX_STACKS, stacks + 1);
        data.putInt(SOUL_STACKS_KEY, stacks);
        data.putLong(LAST_KILL_KEY, killer.level().getGameTime());

        // Soul Container im Inventar füllen
        for (int i = 0; i < killer.getInventory().getContainerSize(); i++) {
            ItemStack invStack = killer.getInventory().getItem(i);
            if (invStack.is(ModItems.SOUL_CONTAINER.get())) {
                if (net.mysith.item.SoulContainerItem.addSoul(invStack)) break;
            }
        }
    }

    @SubscribeEvent
    public static void onAttack(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        ItemStack weapon = attacker.getMainHandItem();
        if (!ModItems.isReaperScythe(weapon)) return;

        // Decay Soul-Stacks wenn zu lange her
        CompoundTag data = attacker.getPersistentData();
        long lastKill = data.getLong(LAST_KILL_KEY);
        long now = attacker.level().getGameTime();
        if (now - lastKill > STACK_DECAY_TICKS) {
            data.putInt(SOUL_STACKS_KEY, 0);
            return;
        }

        // Bonus-Damage pro Stack (+1.0 pro Stack, max +10)
        int stacks = data.getInt(SOUL_STACKS_KEY);
        if (stacks > 0) {
            event.setAmount(event.getAmount() + stacks * 1.0F);
        }

        // Curse Mode: HP < 30% → 2x damage
        if (attacker.getHealth() / attacker.getMaxHealth() < 0.3F) {
            event.setAmount(event.getAmount() * 2.0F);
        }
    }

    public static int getSoulStacks(Player player) {
        CompoundTag data = player.getPersistentData();
        long lastKill = data.getLong(LAST_KILL_KEY);
        long now = player.level().getGameTime();
        if (now - lastKill > STACK_DECAY_TICKS) {
            data.putInt(SOUL_STACKS_KEY, 0);
            return 0;
        }
        return data.getInt(SOUL_STACKS_KEY);
    }
}
