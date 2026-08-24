package com.Momik.usless_mobs.block;

import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.ZoneId;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class KingSlimeTrophyBlockEntity extends BlockEntity {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private String defeatedBy = "";
    private String difficulty = "";
    private long defeatedAtUnix = 0L;
    private int victories = 1;

    public KingSlimeTrophyBlockEntity(BlockPos pos, BlockState state) {
        super(com.Momik.usless_mobs.registry.ModBlockEntities.KING_SLIME_TROPHY_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.defeatedBy = tag.getString("DefeatedBy");
        this.difficulty = tag.getString("Difficulty");
        this.defeatedAtUnix = tag.getLong("DefeatedAtUnix");
        this.victories = Math.max(1, tag.getInt("Victories"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("DefeatedBy", this.defeatedBy);
        tag.putString("Difficulty", this.difficulty);
        tag.putLong("DefeatedAtUnix", this.defeatedAtUnix);
        tag.putInt("Victories", this.victories);
    }

    public Component infoComponent() {
        String player = this.defeatedBy.isEmpty() ? "?" : this.defeatedBy;
        String difficultyName = this.difficulty.isEmpty() ? "?" : this.difficulty;
        String defeatedAt = this.defeatedAtUnix <= 0L ? "?" : TIME_FORMAT.format(Instant.ofEpochMilli(this.defeatedAtUnix));
        return Component.translatable("block.usless_mobs.king_slime_trophy.info", player, difficultyName, defeatedAt, this.victories);
    }
}
