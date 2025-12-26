package com.sang.musicnpc.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MusicKeyEntry extends ObjectSelectionList.Entry<MusicKeyEntry> {

    private final String key;

    private Consumer<MusicKeyEntry> onClickEntry;
    private Supplier<String> currentKeySupplier;

    public MusicKeyEntry(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    void bind(Consumer<MusicKeyEntry> onClickEntry, Supplier<String> currentKeySupplier) {
        this.onClickEntry = onClickEntry;
        this.currentKeySupplier = currentKeySupplier;
    }

    @Override
    public void render(
            GuiGraphics g,
            int index,
            int y,
            int x,
            int rowWidth,
            int rowHeight,
            int mouseX,
            int mouseY,
            boolean hovered,
            float partialTick
    ) {
        String current = (currentKeySupplier == null) ? "" : currentKeySupplier.get();
        boolean selected = key.equals(current);

        if (selected) {
            g.fill(x, y, x + rowWidth, y + rowHeight, 0x6600FF00);
        } else if (hovered) {
            g.fill(x, y, x + rowWidth, y + rowHeight, 0x33000000);
        }

        g.drawString(
                Minecraft.getInstance().font,
                Component.literal(selected ? key + "  [Selected]" : key),
                x + 6,
                y + 6,
                0xFFFFFF
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && onClickEntry != null) {
            onClickEntry.accept(this);
            return true;
        }
        return false;
    }

    @Override
    public Component getNarration() {
        return null;
    }

    // ✅ 크래시 원인 해결: 내레이터가 null Component 받지 않게 항상 제공
    @Override
    public void updateNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, Component.literal(key));
    }
}
