package com.camjewell.betterruneloadouts;

final class RunePouchGridConst {
	private RunePouchGridConst() {
	}

	static final int GRID_COLUMNS = 2;
	static final int SLOT_COUNT = 10;
	static final int GRID_ROWS = (SLOT_COUNT + GRID_COLUMNS - 1) / GRID_COLUMNS;

	// Tall enough for the name strip + (button/theme-icon row) + rune row
	// beneath both, with a little breathing room at the bottom (content
	// ends around y=71: NAME_HEIGHT(16) + ROW_TOP_GAP(2) +
	// max(LOAD_BUTTON_HEIGHT(32), CUSTOM_ICON_SIZE(22)) + RUNE_ROW_GAP(4) +
	// RUNE_ICON_SIZE(17)).
	static final int CELL_HEIGHT = 78;
	static final int CELL_GUTTER_X = 3;
	static final int CELL_GUTTER_Y = 3;
	// Clear of the container's own left/right edges — without it, column 1's
	// text sits flush against the left edge and column 2's rightmost content
	// (e.g. an icon slot's border) gets clipped right at the right edge.
	static final int CONTAINER_PADDING_X = 3;
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

	// Both custom icon slots are the same size, side by side (not stacked),
	// top-aligned in the same row as the load button, not stretched to
	// match it — a few px clear of the button rather than flush against it.
	static final int CUSTOM_ICON_SIZE = 22;
	// Must exceed 2 * ICON_BORDER_PADDING or the two slots' backdrop panels
	// touch/merge into one solid block instead of two distinct squares.
	static final int CUSTOM_ICON_GUTTER = 6;
	static final int ICON_BORDER_PADDING = 2;
	static final int BUTTON_ICON_GAP = 4;

	// Vanilla's own rune-icon widgets (inside RUNEPOUCH_LOADOUT_*) can't be
	// repositioned (a vanilla script keeps re-anchoring them), so we draw our
	// own replacement icons — a full-width row beneath the load
	// button/theme-icon row, centered in the cell (RunePouchGridManager
	// computes the row's start X per-loadout since its width depends on how
	// many runes are actually saved). Sized larger than the old
	// theme-row-relative row to actually fill that width and read clearly.
	static final int RUNE_ICON_SIZE = 17;
	static final int RUNE_ICON_GUTTER = 3;
	static final int RUNE_ICON_MAX_SLOTS = 6;
	static final int RUNE_ROW_GAP = 4;

	static final int ICON_PICKER_COLUMNS = 8;
	static final int ICON_PICKER_ICON_SIZE = 28;
	static final int ICON_PICKER_ICON_SPACING = 11;
}
