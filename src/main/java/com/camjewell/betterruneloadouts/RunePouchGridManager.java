package com.camjewell.betterruneloadouts;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;

/**
 * Reflows the rune pouch loadout list (RUNEPOUCH_LOADOUT_A..J) from vanilla's
 * single-column list into a 2-column grid, and reverts it back to vanilla's
 * own positioning on demand.
 */
@Slf4j
@Singleton
class RunePouchGridManager
{
	private static final int[] LOADOUT_WIDGET_IDS = {
		InterfaceID.Bankside.RUNEPOUCH_LOADOUT_A,
		InterfaceID.Bankside.RUNEPOUCH_LOADOUT_B,
		InterfaceID.Bankside.RUNEPOUCH_LOADOUT_C,
		InterfaceID.Bankside.RUNEPOUCH_LOADOUT_D,
		InterfaceID.Bankside.RUNEPOUCH_LOADOUT_E,
		InterfaceID.Bankside.RUNEPOUCH_LOADOUT_F,
		InterfaceID.Bankside.RUNEPOUCH_LOADOUT_G,
		InterfaceID.Bankside.RUNEPOUCH_LOADOUT_H,
		InterfaceID.Bankside.RUNEPOUCH_LOADOUT_I,
		InterfaceID.Bankside.RUNEPOUCH_LOADOUT_J,
	};

	private final Client client;

	private final Map<Integer, int[]> originalGeometry = new HashMap<>();
	private boolean gridApplied;

	@Inject
	RunePouchGridManager(Client client)
	{
		this.client = client;
	}

	void applyGrid()
	{
		Widget container = client.getWidget(InterfaceID.Bankside.RUNEPOUCH_LOADOUT_CONTAINER);
		if (container == null)
		{
			return;
		}

		int containerWidth = container.getWidth();
		if (containerWidth <= 0)
		{
			log.debug("Skipping rune pouch grid layout, container width not resolved yet: {}", containerWidth);
			return;
		}

		int cellWidth = (containerWidth - RunePouchGridConst.CELL_GUTTER_X) / RunePouchGridConst.GRID_COLUMNS;

		for (int i = 0; i < LOADOUT_WIDGET_IDS.length; i++)
		{
			Widget loadout = client.getWidget(LOADOUT_WIDGET_IDS[i]);
			if (loadout == null)
			{
				continue;
			}

			cacheOriginalGeometry(loadout);

			int col = i % RunePouchGridConst.GRID_COLUMNS;
			int row = i / RunePouchGridConst.GRID_COLUMNS;

			loadout.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
			loadout.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
			loadout.setWidthMode(WidgetSizeMode.ABSOLUTE);
			loadout.setHeightMode(WidgetSizeMode.ABSOLUTE);
			loadout.setOriginalX(col * (cellWidth + RunePouchGridConst.CELL_GUTTER_X));
			loadout.setOriginalY(row * (RunePouchGridConst.CELL_HEIGHT + RunePouchGridConst.CELL_GUTTER_Y));
			loadout.setOriginalWidth(cellWidth);
			loadout.setOriginalHeight(RunePouchGridConst.CELL_HEIGHT);
			loadout.revalidate();
		}

		int scrollHeight = RunePouchGridConst.GRID_ROWS * (RunePouchGridConst.CELL_HEIGHT + RunePouchGridConst.CELL_GUTTER_Y);
		container.setScrollHeight(scrollHeight);
		if (container.getScrollY() > scrollHeight)
		{
			container.setScrollY(Math.max(0, scrollHeight - container.getHeight()));
		}
		container.revalidateScroll();

		gridApplied = true;
	}

	void restoreNativeLayout()
	{
		if (!gridApplied)
		{
			return;
		}

		for (int widgetId : LOADOUT_WIDGET_IDS)
		{
			Widget loadout = client.getWidget(widgetId);
			int[] original = originalGeometry.get(widgetId);
			if (loadout == null || original == null)
			{
				continue;
			}

			loadout.setXPositionMode(original[0]);
			loadout.setYPositionMode(original[1]);
			loadout.setWidthMode(original[2]);
			loadout.setHeightMode(original[3]);
			loadout.setOriginalX(original[4]);
			loadout.setOriginalY(original[5]);
			loadout.setOriginalWidth(original[6]);
			loadout.setOriginalHeight(original[7]);
			loadout.revalidate();
		}

		originalGeometry.clear();
		gridApplied = false;
	}

	private void cacheOriginalGeometry(Widget widget)
	{
		originalGeometry.computeIfAbsent(widget.getId(), id -> new int[]{
			widget.getXPositionMode(),
			widget.getYPositionMode(),
			widget.getWidthMode(),
			widget.getHeightMode(),
			widget.getOriginalX(),
			widget.getOriginalY(),
			widget.getOriginalWidth(),
			widget.getOriginalHeight(),
		});
	}
}
