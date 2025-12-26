package com.sang.musicnpc.registry;

import com.sang.musicnpc.server.MusicBlockIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MusicPlayerBlock extends Block implements EntityBlock {

    public MusicPlayerBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MusicPlayerBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MusicPlayerBlockEntity musicBe) {
            // 1차: test만 고정 (나중에 GUI/목록 확장)
            musicBe.setSoundKey("music.test");

            if (player instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.literal("Music set to: music.test"), true);
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        if (!level.isClientSide && level instanceof ServerLevel sl) {
            MusicBlockIndex.get(sl).add(sl, pos); // ✅ 이게 핵심
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);

        if (!level.isClientSide && level instanceof ServerLevel sl) {
            // "같은 블록이 단순 상태 변경"이 아니라 진짜 블록이 바뀌는 경우만
            if (state.getBlock() != newState.getBlock()) {
                MusicBlockIndex.get(sl).remove(sl, pos); // ✅ 이게 핵심
            }
        }
    }
}