package com.sang.musicnpc.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MusicPlayerBlockEntity extends BlockEntity {

    private String soundKey = "music_test"; // 기본값

    public MusicPlayerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MUSIC_PLAYER_BE.get(), pos, state);
    }

    public String getSoundKey() {
        return soundKey;
    }

    public void setSoundKey(String key) {
        if (key == null || key.isBlank()) return;
        this.soundKey = key;
        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ---- NBT 저장/로드 ----
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("SoundKey", soundKey);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("SoundKey")) {
            this.soundKey = tag.getString("SoundKey");
        }
    }

    // ---- 클라 동기화 ----
    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}