package com.sang.musicnpc.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MusicKeyList extends ObjectSelectionList<MusicKeyEntry> {

    private final Consumer<MusicKeyEntry> onClickEntry;
    private final Supplier<String> currentKeySupplier;

    public MusicKeyList(
            Minecraft mc,
            int width,
            int height,
            int top,
            int bottom,
            int itemHeight,
            Consumer<MusicKeyEntry> onClickEntry,
            Supplier<String> currentKeySupplier
    ) {
        super(mc, width, height, top, bottom, itemHeight);
        this.onClickEntry = onClickEntry;
        this.currentKeySupplier = currentKeySupplier;

        this.setRenderBackground(false);
        this.setRenderTopAndBottom(false);
    }

    public void addMusicEntry(MusicKeyEntry entry) {
        entry.bind(onClickEntry, currentKeySupplier);
        this.addEntry(entry);
    }

    @Override
    public int getRowWidth() {
        return Math.min(300, this.width - 40);
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getRowLeft() + this.getRowWidth() + 6;
    }
}
