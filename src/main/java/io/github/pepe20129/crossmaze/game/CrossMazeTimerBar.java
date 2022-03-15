package io.github.pepe20129.crossmaze.game;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.TranslatableText;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.widget.BossBarWidget;

public final class CrossMazeTimerBar {
	private final BossBarWidget widget;

	public CrossMazeTimerBar(GlobalWidgets widgets) {
		widget = widgets.addBossBar(new TranslatableText("crossmaze.game.waiting"), BossBar.Color.RED, BossBar.Style.PROGRESS);
	}

	public void update(long ticksUntilEnd, long totalTicksForItemSpawn) {
		if (ticksUntilEnd % 20 == 0) {
			widget.setTitle(new TranslatableText("crossmaze.game.next_item", ticksUntilEnd / 20));
			widget.setProgress((float)ticksUntilEnd / totalTicksForItemSpawn);
		}
	}
}