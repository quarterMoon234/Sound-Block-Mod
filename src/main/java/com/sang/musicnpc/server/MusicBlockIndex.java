package com.sang.musicnpc.server;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * ✅ 운영 서버용(영속화) 음악 블록 인덱스
 * - 서버 재시작/런처 재시작 후에도 기존 음악 블록을 다시 찾을 수 있음
 * - chunkKey(ChunkPos.asLong) -> positions(BlockPos.asLong) 저장
 *
 * 주의:
 * - ChunkEvent.Load 같은 곳에서 setDirty 폭주시키지 말 것 (프리징 원인)
 * - 블록 설치/파괴(onPlace/onRemove)에서만 add/remove 호출하면 안전함
 */
public class MusicBlockIndex extends SavedData {

    private static final String DATA_NAME = "musicnpc_music_block_index";

    // chunkKey -> set(posLong)
    private final Long2ObjectOpenHashMap<LongOpenHashSet> byChunk = new Long2ObjectOpenHashMap<>();

    /** 월드(차원)별 SavedData */
    public static MusicBlockIndex get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(MusicBlockIndex::load, MusicBlockIndex::new, DATA_NAME);
    }

    /** 저장 로드 */
    public static MusicBlockIndex load(CompoundTag tag) {
        MusicBlockIndex idx = new MusicBlockIndex();

        ListTag chunks = tag.getList("chunks", Tag.TAG_COMPOUND);
        for (int i = 0; i < chunks.size(); i++) {
            CompoundTag c = chunks.getCompound(i);
            long ck = c.getLong("k");
            long[] arr = c.getLongArray("p");

            LongOpenHashSet set = new LongOpenHashSet(arr);
            if (!set.isEmpty()) {
                idx.byChunk.put(ck, set);
            }
        }
        return idx;
    }

    /** 저장 */
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag chunks = new ListTag();

        byChunk.long2ObjectEntrySet().forEach(e -> {
            long ck = e.getLongKey();
            LongOpenHashSet set = e.getValue();
            if (set == null || set.isEmpty()) return;

            CompoundTag c = new CompoundTag();
            c.putLong("k", ck);
            c.putLongArray("p", set.toLongArray());
            chunks.add(c);
        });

        tag.put("chunks", chunks);
        return tag;
    }

    /** (선택) 인덱스 전체 초기화 */
    public void clearAll() {
        byChunk.clear();
        setDirty();
    }

    /** 블록 설치 시 호출 */
    public void add(ServerLevel level, BlockPos pos) {
        long ck = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        LongOpenHashSet set = byChunk.get(ck);
        if (set == null) {
            set = new LongOpenHashSet();
            byChunk.put(ck, set);
        }

        if (set.add(pos.asLong())) {
            setDirty(); // ✅ 설치/파괴처럼 "드문 이벤트"에서만 dirty
        }
    }

    /** 블록 파괴/교체 시 호출 */
    public void remove(ServerLevel level, BlockPos pos) {
        long ck = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        LongOpenHashSet set = byChunk.get(ck);
        if (set == null) return;

        if (set.remove(pos.asLong())) {
            if (set.isEmpty()) byChunk.remove(ck);
            setDirty();
        }
    }

    /**
     * 플레이어 주변 청크들에 들어있는 음악 블록 좌표만 순회
     * (기존 당신 코드처럼 for-each로 돌릴 수 있게 Iterable<Long> 반환)
     */
    public Iterable<Long> iterateNearby(BlockPos playerPos, int radiusBlocks) {
        int rChunk = (radiusBlocks + 15) >> 4;
        ChunkPos pc = new ChunkPos(playerPos);

        // 주변 청크의 set들을 모아서 순회 (객체 생성 최소화 + 안전한 iterator)
        List<LongOpenHashSet> sets = new ArrayList<>();

        for (int cx = pc.x - rChunk; cx <= pc.x + rChunk; cx++) {
            for (int cz = pc.z - rChunk; cz <= pc.z + rChunk; cz++) {
                long ck = ChunkPos.asLong(cx, cz);
                LongOpenHashSet set = byChunk.get(ck);
                if (set != null && !set.isEmpty()) sets.add(set);
            }
        }

        return () -> new java.util.Iterator<>() {
            int i = 0;
            LongIterator it = (sets.isEmpty() ? null : sets.get(0).iterator());

            @Override
            public boolean hasNext() {
                while (true) {
                    if (it != null && it.hasNext()) return true;
                    i++;
                    if (i >= sets.size()) return false;
                    it = sets.get(i).iterator();
                }
            }

            @Override
            public Long next() {
                if (!hasNext()) throw new java.util.NoSuchElementException();
                return it.nextLong();
            }
        };
    }
}
