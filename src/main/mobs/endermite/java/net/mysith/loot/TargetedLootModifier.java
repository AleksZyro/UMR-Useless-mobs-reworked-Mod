package net.mysith.loot;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

abstract class TargetedLootModifier extends LootModifier {
    private final Set<ResourceLocation> targetTables;

    protected TargetedLootModifier(LootItemCondition[] conditionsIn, Set<ResourceLocation> targetTables) {
        super(conditionsIn);
        this.targetTables = targetTables;
    }

    @NotNull
    @Override
    protected final ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ResourceLocation tableId = context.getQueriedLootTableId();
        if (tableId == null || !this.targetTables.contains(tableId)) {
            return generatedLoot;
        }
        return applyToTargetTable(generatedLoot, context);
    }

    protected abstract ObjectArrayList<ItemStack> applyToTargetTable(
            ObjectArrayList<ItemStack> generatedLoot, LootContext context);

    protected static ResourceLocation chest(String path) {
        return ResourceLocation.tryBuild("minecraft", "chests/" + path);
    }
}
