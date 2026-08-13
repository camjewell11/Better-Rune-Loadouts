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
}
