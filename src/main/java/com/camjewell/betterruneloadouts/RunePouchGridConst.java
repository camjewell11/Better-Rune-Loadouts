package com.camjewell.betterruneloadouts;

final class RunePouchGridConst
{
	private RunePouchGridConst()
	{
	}

	static final int GRID_COLUMNS = 2;
	static final int SLOT_COUNT = 10;
	static final int GRID_ROWS = (SLOT_COUNT + GRID_COLUMNS - 1) / GRID_COLUMNS;

	// Tall enough for the name strip + theme-icon row + rune row beneath it,
	// with a little breathing room at the bottom (content ends around y=55:
	// NAME_HEIGHT(16) + ROW_TOP_GAP(2) + CUSTOM_ICON_SIZE(22) +
	// RUNE_ROW_GAP(4) + RUNE_ICON_SIZE(11)).
	static final int CELL_HEIGHT = 57;
	static final int CELL_GUTTER_X = 6;
	static final int CELL_GUTTER_Y = 6;

	// Pinned height of the name text strip along the top of each cell — must
	// stay small so it doesn't swallow clicks meant for the load button and
	// rune icons beneath it.
	static final int NAME_HEIGHT = 16;
	static final int ROW_TOP_GAP = 2;

	static final int LOAD_BUTTON_WIDTH = 32;
	static final int LOAD_BUTTON_HEIGHT = 34;

	// Both custom icon slots are the same size, side by side (not stacked),
	// top-aligned with the load button row rather than vertically centered.
	static final int CUSTOM_ICON_SIZE = 22;
	static final int CUSTOM_ICON_GUTTER = 4;
	// Custom theme icons sit beside the load button, not on top of it.
	static final int THEME_ICON_X = LOAD_BUTTON_WIDTH + 2;

	// Vanilla's own rune-icon widgets (inside RUNEPOUCH_LOADOUT_*) can't be
	// repositioned (a vanilla script keeps re-anchoring them), so we draw our
	// own replacement icons — a row starting at the same X as the theme
	// icons (i.e. to the right of the load button, not under it), beneath
	// the theme-icon row, matching the mockup.
	static final int RUNE_ICON_SIZE = 11;
	static final int RUNE_ICON_GUTTER = 1;
	static final int RUNE_ICON_MAX_SLOTS = 6;
	static final int RUNE_AREA_X = THEME_ICON_X;
	static final int RUNE_ROW_GAP = 4;

	static final int ICON_PICKER_COLUMNS = 8;
	static final int ICON_PICKER_ICON_SIZE = 28;
	static final int ICON_PICKER_ICON_SPACING = 11;
}
