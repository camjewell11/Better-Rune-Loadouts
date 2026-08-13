package com.camjewell.betterruneloadouts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.ScriptEvent;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.ItemQuantityMode;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetType;

/**
 * Reflows the rune pouch loadout list (RUNEPOUCH_LOADOUT_A..J) from vanilla's
 * single-column list into a 2-column grid, renders per-loadout custom names
 * and theme icons on top of it, and reverts everything back to vanilla's own
 * positioning on demand.
 */
@Slf4j
@Singleton
class RunePouchGridManager
{
	private static final String ICON_CHILD_NAME = "brl-theme-icon";
	private static final String LAYER_CHILD_NAME = "brl-theme-icon-layer";
	private static final String RUNE_ICON_CHILD_PREFIX = "brl-rune-icon-";
	// Vanilla's own "no rune assigned" item, used elsewhere in this same
	// interface for empty rune slots — reused here so our placeholder icon
	// matches natively instead of using an unrelated sprite.
	private static final int PLACEHOLDER_ITEM_ID = 11526;

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

	private static final int[] LOAD_WIDGET_IDS = {
		InterfaceID.Bankside.RUNEPOUCH_LOAD_A,
		InterfaceID.Bankside.RUNEPOUCH_LOAD_B,
		InterfaceID.Bankside.RUNEPOUCH_LOAD_C,
		InterfaceID.Bankside.RUNEPOUCH_LOAD_D,
		InterfaceID.Bankside.RUNEPOUCH_LOAD_E,
		InterfaceID.Bankside.RUNEPOUCH_LOAD_F,
		InterfaceID.Bankside.RUNEPOUCH_LOAD_G,
		InterfaceID.Bankside.RUNEPOUCH_LOAD_H,
		InterfaceID.Bankside.RUNEPOUCH_LOAD_I,
		InterfaceID.Bankside.RUNEPOUCH_LOAD_J,
	};

	private static final int[] NAME_WIDGET_IDS = {
		InterfaceID.Bankside.RUNEPOUCH_NAME_A,
		InterfaceID.Bankside.RUNEPOUCH_NAME_B,
		InterfaceID.Bankside.RUNEPOUCH_NAME_C,
		InterfaceID.Bankside.RUNEPOUCH_NAME_D,
		InterfaceID.Bankside.RUNEPOUCH_NAME_E,
		InterfaceID.Bankside.RUNEPOUCH_NAME_F,
		InterfaceID.Bankside.RUNEPOUCH_NAME_G,
		InterfaceID.Bankside.RUNEPOUCH_NAME_H,
		InterfaceID.Bankside.RUNEPOUCH_NAME_I,
		InterfaceID.Bankside.RUNEPOUCH_NAME_J,
	};

	private final Client client;
	private final RunePouchLoadoutConfigStore configStore;

	private final Map<Integer, int[]> originalGeometry = new HashMap<>();
	private boolean gridApplied;
	private int currentViewValue;
	private IntConsumer renameRequestHandler;

	@Inject
	RunePouchGridManager(Client client, RunePouchLoadoutConfigStore configStore)
	{
		this.client = client;
		this.configStore = configStore;
	}

	/**
	 * Called when the user clicks a loadout's name text. Wired by the plugin
	 * since opening the rename chatbox lives there, not here.
	 */
	void setRenameRequestHandler(IntConsumer handler)
	{
		this.renameRequestHandler = handler;
	}

	void applyGrid(int viewValue)
	{
		this.currentViewValue = viewValue;

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

			applyLoadoutName(i);
			applyLoadoutIcon(i);
			compactRuneIcons(i);
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

	/**
	 * Re-renders names/icons from the config store without touching layout —
	 * used after the user renames a loadout or changes its icon.
	 */
	void refresh()
	{
		if (!gridApplied)
		{
			return;
		}

		applyGrid(currentViewValue);
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

	String getLoadoutName(int slotIndex)
	{
		return configStore.getName(currentViewValue, slotIndex, defaultName(slotIndex));
	}

	int slotIndexForLoadWidget(int widgetId)
	{
		return indexOf(LOAD_WIDGET_IDS, widgetId);
	}

	private void applyLoadoutName(int slotIndex)
	{
		Widget nameWidget = client.getWidget(NAME_WIDGET_IDS[slotIndex]);
		if (nameWidget == null)
		{
			return;
		}

		String name = getLoadoutName(slotIndex);

		nameWidget.setHidden(false);
		nameWidget.setType(WidgetType.TEXT);
		nameWidget.setFontId(FontID.PLAIN_12);
		nameWidget.setTextColor(0xFF981F);
		nameWidget.setTextShadowed(true);
		nameWidget.setText(name);

		// Vanilla sized this widget to fill the remaining row (MINUS mode)
		// beside the load button in its single-column layout. We made the
		// cell taller for the grid without pinning this widget's own height,
		// so it silently grew to cover most of the cell — swallowing clicks
		// meant for the load button and the rune icons below it. Pin it to a
		// thin strip along the top instead.
		nameWidget.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		nameWidget.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		nameWidget.setWidthMode(WidgetSizeMode.MINUS);
		nameWidget.setHeightMode(WidgetSizeMode.ABSOLUTE);
		nameWidget.setOriginalX(0);
		nameWidget.setOriginalY(0);
		nameWidget.setOriginalWidth(0);
		nameWidget.setOriginalHeight(RunePouchGridConst.NAME_HEIGHT);

		// Vanilla's own click action on this widget opens its native preset
		// boss-name picker; override it so clicking the name opens our rename
		// chatbox instead. Setting text/target alone (without an OnOpListener)
		// would leave vanilla's original action still wired underneath.
		nameWidget.setHasListener(true);
		nameWidget.clearActions();
		nameWidget.setAction(0, "Rename");
		nameWidget.setTargetVerb(name);
		nameWidget.setOnOpListener((JavaScriptCallback) (ScriptEvent event) ->
		{
			if (event.getOp() != 1)
			{
				return;
			}

			if (renameRequestHandler != null)
			{
				renameRequestHandler.accept(slotIndex);
			}
		});
		nameWidget.revalidate();
	}

	private void applyLoadoutIcon(int slotIndex)
	{
		Widget loadWidget = client.getWidget(LOAD_WIDGET_IDS[slotIndex]);
		Widget loadoutWidget = client.getWidget(LOADOUT_WIDGET_IDS[slotIndex]);
		if (loadWidget == null || loadoutWidget == null)
		{
			return;
		}

		// Vanilla anchors this near the top of its original (shorter,
		// full-width) row. Pin it explicitly below our name strip so it
		// doesn't overlap that widget's clickable area.
		loadWidget.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		loadWidget.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		loadWidget.setWidthMode(WidgetSizeMode.ABSOLUTE);
		loadWidget.setHeightMode(WidgetSizeMode.ABSOLUTE);
		loadWidget.setOriginalX(0);
		loadWidget.setOriginalY(RunePouchGridConst.NAME_HEIGHT + RunePouchGridConst.ROW_TOP_GAP);
		loadWidget.setOriginalWidth(RunePouchGridConst.LOAD_BUTTON_WIDTH);
		loadWidget.setOriginalHeight(RunePouchGridConst.LOAD_BUTTON_HEIGHT);
		loadWidget.revalidate();

		int primarySprite = configStore.getIcon(currentViewValue, slotIndex, 0, RunePouchLoadoutIcon.DEFAULT_SPRITE_ID);
		int layerSprite = configStore.getIcon(currentViewValue, slotIndex, 1, RunePouchLoadoutIcon.NO_SECOND_ICON);
		boolean isCustomPrimary = primarySprite != RunePouchLoadoutIcon.DEFAULT_SPRITE_ID;
		boolean hasLayer = layerSprite != RunePouchLoadoutIcon.NO_SECOND_ICON;

		// The load button has ~12 of its own dynamic children (vanilla's
		// hover/pressed frame decorations), and attaching our icon as a
		// child of it put our icon in that same stack — the button's own
		// hover script swaps those decorations without knowing about ours,
		// making our icon disappear or get buried on hover. Attaching to
		// the loadout cell instead (a sibling, not nested in the button)
		// avoids that entirely — this widget has no competing hover behavior.
		int buttonTop = RunePouchGridConst.NAME_HEIGHT + RunePouchGridConst.ROW_TOP_GAP;
		int iconY = buttonTop + (RunePouchGridConst.LOAD_BUTTON_HEIGHT - RunePouchGridConst.CUSTOM_ICON_SIZE) / 2;
		int iconX = RunePouchGridConst.THEME_ICON_X;

		applyIconSlot(loadoutWidget, ICON_CHILD_NAME, iconX, iconY, RunePouchGridConst.CUSTOM_ICON_SIZE, primarySprite, isCustomPrimary);

		// Same size, side by side — not stacked on the primary icon.
		int layerX = iconX + RunePouchGridConst.CUSTOM_ICON_SIZE + RunePouchGridConst.CUSTOM_ICON_GUTTER;
		applyIconSlot(loadoutWidget, LAYER_CHILD_NAME, layerX, iconY, RunePouchGridConst.CUSTOM_ICON_SIZE, layerSprite, hasLayer);
	}

	/**
	 * Renders one custom icon slot — the real sprite when set, or vanilla's
	 * own "no rune assigned" item icon (11526) as a placeholder when not, so
	 * it reads the same as the rest of this interface.
	 *
	 * TODO: a backdrop panel behind the slot (so it's visible even before
	 * being set) would help, but RECTANGLE children attached here didn't
	 * render under any configuration tried (outline or filled) despite every
	 * GRAPHIC child working fine — needs more investigation, not a quick fix.
	 */
	private void applyIconSlot(Widget parent, String iconName, int x, int y, int size, int spriteId, boolean isSet)
	{
		Widget icon = findOrCreateTaggedChild(parent, iconName);
		if (isSet)
		{
			icon.setSpriteId(spriteId);
			icon.setItemId(-1);
			icon.setOpacity(0);
		}
		else
		{
			icon.setSpriteId(-1);
			icon.setItemId(PLACEHOLDER_ITEM_ID);
			icon.setItemQuantity(1);
			icon.setItemQuantityMode(ItemQuantityMode.NEVER);
			icon.setOpacity(0);
		}
		icon.setWidthMode(WidgetSizeMode.ABSOLUTE);
		icon.setHeightMode(WidgetSizeMode.ABSOLUTE);
		icon.setOriginalWidth(size);
		icon.setOriginalHeight(size);
		icon.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		icon.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		icon.setOriginalX(x);
		icon.setOriginalY(y);
		icon.setHidden(false);
		icon.revalidate();
	}

	/**
	 * Vanilla's own rune-type icon children (inside RUNEPOUCH_LOADOUT_*) are
	 * item-rendered widgets a vanilla script continuously re-anchors to the
	 * right/bottom edge of the old full-width single-column row — resizing
	 * them sticks, but any reposition we apply gets silently overwritten.
	 * So instead: read which item each one shows (before hiding it), then
	 * draw our own replacement icons — plain widgets vanilla's script has no
	 * reason to touch — in a compact grid beside the load button.
	 */
	private void compactRuneIcons(int slotIndex)
	{
		Widget loadoutWidget = client.getWidget(LOADOUT_WIDGET_IDS[slotIndex]);
		if (loadoutWidget == null)
		{
			return;
		}

		Widget[] children = loadoutWidget.getDynamicChildren();
		if (children == null)
		{
			return;
		}

		List<Integer> itemIds = new ArrayList<>();
		for (Widget child : children)
		{
			String name = child.getName();
			if (ICON_CHILD_NAME.equals(name) || LAYER_CHILD_NAME.equals(name)
				|| (name != null && name.startsWith(RUNE_ICON_CHILD_PREFIX)))
			{
				continue;
			}

			// SpriteId is always -1 on these (they're item-rendered, not
			// sprite graphics) — ItemId is what actually distinguishes a
			// populated slot (e.g. 565 = Blood rune) from an empty one (-1).
			if (child.getItemId() >= 0)
			{
				itemIds.add(child.getItemId());
			}

			child.setHidden(true);
			child.revalidate();
		}

		for (int i = 0; i < RunePouchGridConst.RUNE_ICON_MAX_SLOTS; i++)
		{
			Widget runeIcon = findOrCreateTaggedChild(loadoutWidget, RUNE_ICON_CHILD_PREFIX + i);

			if (i >= itemIds.size())
			{
				runeIcon.setHidden(true);
				runeIcon.revalidate();
				continue;
			}

			// Single row beneath the load button / theme icon row, matching
			// the mockup, rather than a grid beside it.
			int runeRowY = RunePouchGridConst.NAME_HEIGHT + RunePouchGridConst.ROW_TOP_GAP
				+ RunePouchGridConst.LOAD_BUTTON_HEIGHT + RunePouchGridConst.RUNE_ROW_GAP;

			runeIcon.setItemId(itemIds.get(i));
			runeIcon.setItemQuantity(1);
			runeIcon.setItemQuantityMode(ItemQuantityMode.NEVER);
			runeIcon.setWidthMode(WidgetSizeMode.ABSOLUTE);
			runeIcon.setHeightMode(WidgetSizeMode.ABSOLUTE);
			runeIcon.setOriginalWidth(RunePouchGridConst.RUNE_ICON_SIZE);
			runeIcon.setOriginalHeight(RunePouchGridConst.RUNE_ICON_SIZE);
			runeIcon.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
			runeIcon.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
			runeIcon.setOriginalX(i * (RunePouchGridConst.RUNE_ICON_SIZE + RunePouchGridConst.RUNE_ICON_GUTTER));
			runeIcon.setOriginalY(runeRowY);
			runeIcon.setHidden(false);
			runeIcon.revalidate();
		}
	}

	private static Widget findOrCreateTaggedChild(Widget parent, String name)
	{
		return findOrCreateTaggedChild(parent, name, WidgetType.GRAPHIC);
	}

	private static Widget findOrCreateTaggedChild(Widget parent, String name, int type)
	{
		Widget[] children = parent.getDynamicChildren();
		if (children != null)
		{
			for (Widget child : children)
			{
				if (name.equals(child.getName()))
				{
					return child;
				}
			}
		}

		Widget created = parent.createChild(-1, type);
		created.setName(name);
		return created;
	}

	private static String defaultName(int slotIndex)
	{
		return "Loadout " + (slotIndex + 1);
	}

	private static int indexOf(int[] widgetIds, int widgetId)
	{
		for (int i = 0; i < widgetIds.length; i++)
		{
			if (widgetIds[i] == widgetId)
			{
				return i;
			}
		}

		return -1;
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
