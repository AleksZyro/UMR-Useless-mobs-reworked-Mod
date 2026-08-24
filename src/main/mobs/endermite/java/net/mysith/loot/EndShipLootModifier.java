package net.mysith.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.mysith.registry.ModEnchantments;
import org.jetbrains.annotations.NotNull;

public class EndShipLootModifier extends TargetedLootModifier {

    public static final Supplier<Codec<EndShipLootModifier>> CODEC = () ->
            RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, EndShipLootModifier::new));

    public EndShipLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn, Set.of(chest("end_city_treasure")));
    }

    @NotNull
    @Override
    protected ObjectArrayList<ItemStack> applyToTargetTable(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        var rand = context.getRandom();

        // 25% chance: Beheading book (random level 1-3, weighted toward lower)
        if (rand.nextFloat() < 0.25F) {
            int level = weightedLevel(rand);
            ItemStack book = EnchantedBookItem.createForEnchantment(
                    new EnchantmentInstance(ModEnchantments.BEHEADING.get(), level));
            generatedLoot.add(book);
        }

        // 10% chance: Soul Drain book
        if (rand.nextFloat() < 0.10F) {
            int level = weightedLevel(rand);
            ItemStack book = EnchantedBookItem.createForEnchantment(
                    new EnchantmentInstance(ModEnchantments.SOUL_DRAIN.get(), level));
            generatedLoot.add(book);
        }

        if (rand.nextFloat() < 0.35F) {
            generatedLoot.add(AncientLoreBooks.create(AncientLoreBooks.pick(rand,
                    AncientLoreBooks.LoreBook.CELESTIAL_SLIME,
                    AncientLoreBooks.LoreBook.VOID_REAPER,
                    AncientLoreBooks.LoreBook.THREE_PATHS)));
        }

        return generatedLoot;
    }

    private int weightedLevel(net.minecraft.util.RandomSource rand) {
        float roll = rand.nextFloat();
        if (roll < 0.60F) return 1;
        if (roll < 0.90F) return 2;
        return 3;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
