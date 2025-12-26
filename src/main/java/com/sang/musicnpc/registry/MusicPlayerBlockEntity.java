package com.sang.musicnpc.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MusicPlayerBlockEntity extends BlockEntity {

    private String soundKey = "music.test"; // 기본값

    public MusicPlayerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MUSIC_PLAYER_BE.get(), pos, state);
    }

    public String getSoundKey() {
        return soundKey;
    }

    public void setSoundKey(String key) {
        this.soundKey = key;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("soundKey", soundKey);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        String v = tag.getString("soundKey");
        this.soundKey = (v == null || v.isBlank()) ? "music.test" : v;
    }
}