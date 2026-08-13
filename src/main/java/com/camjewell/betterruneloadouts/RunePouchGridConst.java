package com.camjewell.betterruneloadouts;

final class RunePouchGridConst {
	private RunePouchGridConst() {
	}

	static final int GRID_COLUMNS = 2;
	static final int SLOT_COUNT = 10;
	static final int GRID_ROWS = (SLOT_COUNT + GRID_COLUMNS - 1) / GRID_COLUMNS;

	// Tall enough for the name strip + theme-icon row + rune row beneath it,
	// with a little breathing room at the bottom (content ends around y=62:
	// NAME_HEIGHT(16) + ROW_TOP_GAP(2) + CUSTOM_ICON_SIZE(27) +
	// RUNE_ROW_GAP(4) + RUNE_ICON_SIZE(13)).
	static final int CELL_HEIGHT = 66;
	static final int CELL_GUTTER_X = 0;
	static final int CELL_GUTTER_Y = 6;
	// We hide RUNEPOUCH_LOADOUT_SCROLLBAR outright (see RunePouchGridManager)
	// rather than reserving room for it — reserving its ~16px width wasted
	// space that isn't needed once it's not being drawn. Mouse-wheel
	// scrolling still works since it's driven by the container's own scroll
	// state (setScrollHeight/getScrollY), not by the scrollbar widget itself.
	static final int SCROLLBAR_RESERVE = 0;

	// Pinned height of the name text strip along the top of each cell — must
	// stay small so it doesn't swallow clicks meant for the load button and
	// rune icons beneath it.
	static final int NAME_HEIGHT = 16;
	static final int ROW_TOP_GAP = 2;

	// Vanilla's own un-hovered button size (confirmed via logging) — the
	// arrow graphic's children are pinned to exactly this size/position, so
	// this must stay in sync with what they're actually rendered at.
	static final int LOAD_BUTTON_WIDTH = 30;
	static final int LOAD_BUTTON_HEIGHT = 32;

	// Both custom icon slots are the same size, side by side (not stacked).
	// Sized to fill the width available to the right of the load button
	// within a cell, rather than leaving it mostly empty.
	static final int CUSTOM_ICON_SIZE = 25;
	// Must exceed 2 * ICON_BORDER_PADDING or the two slots' backdrop panels
	// touch/merge into one solid block instead of two distinct squares.
	static final int CUSTOM_ICON_GUTTER = 6;
	static final int ICON_BORDER_PADDING = 2;
	// Custom theme icons sit beside the load button, not on top of it — a
	// few px clear of it rather than flush against it.
	static final int THEME_ICON_X = LOAD_BUTTON_WIDTH + 4;

	// Vanilla's own rune-icon widgets (inside RUNEPOUCH_LOADOUT_*) can't be
	// repositioned (a vanilla script keeps re-anchoring them), so we draw our
	// own replacement icons — a row beneath the theme-icon row, centered
	// under it (RunePouchGridManager computes the row's start X per-loadout
	// since its width depends on how many runes are actually saved).
	static final int RUNE_ICON_SIZE = 13;
	static final int RUNE_ICON_GUTTER = 2;
	static final int RUNE_ICON_MAX_SLOTS = 6;
	static final int RUNE_ROW_GAP = 4;

	static final int ICON_PICKER_COLUMNS = 8;
	static final int ICON_PICKER_ICON_SIZE = 28;
	static final int ICON_PICKER_ICON_SPACING = 11;
}
