package com.sang.musicnpc.client.screen;

import com.sang.musicnpc.network.ModNetwork;
import com.sang.musicnpc.network.SetMusicKeyPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;

public class MusicSelectScreen extends Screen {

    private final BlockPos pos;
    private String currentKey;

    private MusicKeyList list;

    // ✅ 서버/블록에 저장되는 키(= ModSounds register 이름, PlayNpcMusicPacket resolve 키)
    public static final List<String> MUSIC_KEYS = List.of(
            "music_test",
            "music_maple"
    );

    public MusicSelectScreen(BlockPos pos, String currentKey) {
        super(Component.literal("Select Music"));
        this.pos = pos;
        this.currentKey = (currentKey == null) ? "" : currentKey;
    }

    public MusicSelectScreen(BlockPos pos) {
        this(pos, "");
    }

    @Override
    protected void init() {
        int listTop = 40;
        int listBottom = this.height - 50;

        this.list = new MusicKeyList(
                this.minecraft,
                this.width,
                this.height,
                listTop,
                listBottom,
                24,
                this::onEntryClicked,
                () -> currentKey
        );

        for (String key : MUSIC_KEYS) {
            this.list.addMusicEntry(new MusicKeyEntry(key));
        }

        this.addRenderableWidget(list);

        this.addRenderableWidget(
                Button.builder(Component.literal("Close"), btn -> onClose())
                        .bounds(this.width / 2 - 60, this.height - 35, 120, 20)
                        .build()
        );
    }

    /** ✅ 클릭 시 화면 재초기화 X (크래시 원인 제거) */
    private void onEntryClicked(MusicKeyEntry entry) {
        String key = entry.getKey();

        // 같은 키를 여러 번 눌러도 불필요 패킷/갱신을 하지 않음 (안정)
        if (key.equals(currentKey)) {
            this.list.setSelected(entry);
            return;
        }

        this.currentKey = key;
        this.list.setSelected(entry);

        // ✅ 서버로 저장 요청
        ModNetwork.CHANNEL.sendToServer(new SetMusicKeyPacket(pos, key));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, "Music Block", this.width / 2, 12, 0xFFFFFF);
        g.drawCenteredString(this.font, "Scroll & click to select", this.width / 2, 24, 0xA0A0A0);
        super.render(g, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
