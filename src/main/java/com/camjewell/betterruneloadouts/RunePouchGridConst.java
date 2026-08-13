package com.camjewell.betterruneloadouts;

final class RunePouchGridConst
{
	private RunePouchGridConst()
	{
	}

	static final int GRID_COLUMNS = 2;
	static final int SLOT_COUNT = 10;
	static final int GRID_ROWS = (SLOT_COUNT + GRID_COLUMNS - 1) / GRID_COLUMNS;

	// Vanilla's single-column row (53px) fit name text beside the icon row at
	// full width; at half width in the 2-column grid the name wraps onto its
	// own line above the icons, so the cell needs more vertical room.
	static final int CELL_HEIGHT = 80;
	static final int CELL_GUTTER_X = 6;
	static final int CELL_GUTTER_Y = 6;

	// Pinned height of the name text strip along the top of each cell — must
	// stay small so it doesn't swallow clicks meant for the load button and
	// rune icons beneath it.
	static final int NAME_HEIGHT = 16;
	static final int ROW_TOP_GAP = 2;

	static final int LOAD_BUTTON_WIDTH = 32;
	static final int LOAD_BUTTON_HEIGHT = 34;

	// Both custom icon slots are the same size, side by side (not stacked).
	static final int CUSTOM_ICON_SIZE = 22;
	static final int CUSTOM_ICON_GUTTER = 4;
	// Custom theme icons sit beside the load button, not on top of it.
	static final int THEME_ICON_X = LOAD_BUTTON_WIDTH + 6;

	// Vanilla's own rune-icon widgets (inside RUNEPOUCH_LOADOUT_*) can't be
	// repositioned (a vanilla script keeps re-anchoring them), so we draw our
	// own replacement icons — a single row below the load button/theme icon,
	// matching the mockup layout.
	static final int RUNE_ICON_SIZE = 14;
	static final int RUNE_ICON_GUTTER = 2;
	static final int RUNE_ICON_MAX_SLOTS = 6;
	static final int RUNE_ROW_GAP = 4;

	static final int ICON_PICKER_COLUMNS = 8;
	static final int ICON_PICKER_ICON_SIZE = 28;
	static final int ICON_PICKER_ICON_SPACING = 11;
}
