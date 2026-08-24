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

public class AncientCityLootModifier extends TargetedLootModifier {

    public static final Supplier<Codec<AncientCityLootModifier>> CODEC = () ->
            RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, AncientCityLootModifier::new));

    public AncientCityLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn, Set.of(chest("ancient_city"), chest("ancient_city_ice_box")));
    }

    @NotNull
    @Override
    protected ObjectArrayList<ItemStack> applyToTargetTable(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        var rand = context.getRandom();

        // 30% Beheading-Buch (öfter als End-Ship, Warden-Area = Reaper-Theme)
        if (rand.nextFloat() < 0.30F) {
            generatedLoot.add(EnchantedBookItem.createForEnchantment(
                    new EnchantmentInstance(ModEnchantments.BEHEADING.get(), 1 + rand.nextInt(3))));
        }
        // 15% Death Mark Buch
        if (rand.nextFloat() < 0.15F) {
            generatedLoot.add(EnchantedBookItem.createForEnchantment(
                    new EnchantmentInstance(ModEnchantments.DEATH_MARK.get(), 1 + rand.nextInt(2))));
        }
        if (rand.nextFloat() < 0.45F) {
            generatedLoot.add(AncientLoreBooks.create(AncientLoreBooks.pick(rand,
                    AncientLoreBooks.LoreBook.CORRUPTED_DEEP,
                    AncientLoreBooks.LoreBook.VOID_REAPER,
                    AncientLoreBooks.LoreBook.TRUE_CROWN)));
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
