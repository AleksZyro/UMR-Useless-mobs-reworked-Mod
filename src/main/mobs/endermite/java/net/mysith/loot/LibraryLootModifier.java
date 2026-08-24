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

public class LibraryLootModifier extends TargetedLootModifier {

    public static final Supplier<Codec<LibraryLootModifier>> CODEC = () ->
            RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, LibraryLootModifier::new));

    public LibraryLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn, Set.of(chest("stronghold_library")));
    }

    @NotNull
    @Override
    protected ObjectArrayList<ItemStack> applyToTargetTable(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        var rand = context.getRandom();

        // 8% Reaping Buch
        if (rand.nextFloat() < 0.08F) {
            generatedLoot.add(EnchantedBookItem.createForEnchantment(
                    new EnchantmentInstance(ModEnchantments.REAPING.get(), 1 + rand.nextInt(3))));
        }
        // 5% Crimson Edge Buch
        if (rand.nextFloat() < 0.05F) {
            generatedLoot.add(EnchantedBookItem.createForEnchantment(
                    new EnchantmentInstance(ModEnchantments.CRIMSON_EDGE.get(), 1 + rand.nextInt(3))));
        }
        // 3% Beheading Buch (selten)
        if (rand.nextFloat() < 0.03F) {
            generatedLoot.add(EnchantedBookItem.createForEnchantment(
                    new EnchantmentInstance(ModEnchantments.BEHEADING.get(), 1 + rand.nextInt(2))));
        }
        if (rand.nextFloat() < 0.28F) {
            generatedLoot.add(AncientLoreBooks.create(AncientLoreBooks.pick(rand,
                    AncientLoreBooks.LoreBook.THREE_PATHS,
                    AncientLoreBooks.LoreBook.LIVING_ROOTS,
                    AncientLoreBooks.LoreBook.BRIGHT_SLIME)));
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
