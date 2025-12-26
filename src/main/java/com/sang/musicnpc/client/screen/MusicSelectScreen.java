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
    private String selectedKey;

    // ✅ GUI 표시용 목록 (sounds.json 키와 동일)  ← 여기 핵심!
    private static final List<String> MUSIC_KEYS = List.of(
            "music_test",
            "music_maple"
    );

    public MusicSelectScreen(BlockPos pos, String currentKey) {
        super(Component.literal("Select Music"));
        this.pos = pos;

        // ✅ 기본 선택값도 언더스코어로 통일
        this.selectedKey = (currentKey == null || currentKey.isBlank()) ? "music_test" : currentKey;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 40;

        for (String key : MUSIC_KEYS) {
            String label = key + (key.equals(selectedKey) ? "  [Selected]" : "");
            this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
                selectedKey = key;

                // ✅ 서버로 선택값 전송 (여기서 key가 music_test / music_maple 로 나감)
                ModNetwork.sendToServer(new SetMusicKeyPacket(pos, key));

                // UI 갱신
                this.clearWidgets();
                this.init();
            }).bounds(centerX - 100, y, 200, 20).build());

            y += 24;
        }

        this.addRenderableWidget(Button.builder(Component.literal("Close"), btn -> onClose())
                .bounds(centerX - 100, y + 10, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, "Music Block", this.width / 2, this.height / 2 - 80, 0xFFFFFF);
        g.drawCenteredString(this.font, "Choose a track:", this.width / 2, this.height / 2 - 65, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
