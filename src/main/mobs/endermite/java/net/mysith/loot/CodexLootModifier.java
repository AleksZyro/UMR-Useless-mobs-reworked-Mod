package net.mysith.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.mysith.registry.ModItems;
import org.jetbrains.annotations.NotNull;

public class CodexLootModifier extends TargetedLootModifier {
    public static final Supplier<Codec<CodexLootModifier>> CODEC = () ->
            RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, CodexLootModifier::new));

    public CodexLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn, Set.of(
                chest("simple_dungeon"),
                chest("abandoned_mineshaft"),
                chest("ruined_portal"),
                chest("shipwreck_map"),
                chest("village/village_cartographer"),
                chest("village/village_temple"),
                chest("stronghold_library")));
    }

    @NotNull
    @Override
    protected ObjectArrayList<ItemStack> applyToTargetTable(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        var random = context.getRandom();
        if (random.nextFloat() < 0.35F) {
            generatedLoot.add(new ItemStack(ModItems.SOUL_CODEX.get()));
        }
        if (random.nextFloat() < 0.45F) {
            AncientLoreBooks.LoreBook book = AncientLoreBooks.pick(random,
                    AncientLoreBooks.LoreBook.THREE_PATHS,
                    AncientLoreBooks.LoreBook.BRIGHT_SLIME,
                    AncientLoreBooks.LoreBook.CORRUPTED_DEEP,
                    AncientLoreBooks.LoreBook.LIVING_ROOTS);
            generatedLoot.add(AncientLoreBooks.create(book));
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
