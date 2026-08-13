package com.camjewell.betterruneloadouts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.MenuAction;
import net.runelite.api.ScriptEvent;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.ItemQuantityMode;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.util.Text;

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

	// Per-loadout, per-rune-position (1-4) saved quantity cap. Confirmed via
	// logging: e.g. RUNE_POUCH_LOADOUT_A_CAP1 read 1000 after saving a
	// loadout with Cosmic rune capped at 1000, and 0 for a rune saved as
	// unlimited ("All"). Position 3 (index 2) for loadouts D and H is -1
	// (unavailable) because that one cap is split across extra bits
	// (_BITSA/_BITSB/_BITSC) whose combination formula isn't derivable from
	// the gameval constants alone — quantity is skipped just for that slot
	// rather than risk showing a wrong number.
	private static final int[][] RUNE_CAP_VARBIT_IDS = {
		{VarbitID.RUNE_POUCH_LOADOUT_A_CAP1, VarbitID.RUNE_POUCH_LOADOUT_A_CAP2, VarbitID.RUNE_POUCH_LOADOUT_A_CAP3, VarbitID.RUNE_POUCH_LOADOUT_A_CAP4},
		{VarbitID.RUNE_POUCH_LOADOUT_B_CAP1, VarbitID.RUNE_POUCH_LOADOUT_B_CAP2, VarbitID.RUNE_POUCH_LOADOUT_B_CAP3, VarbitID.RUNE_POUCH_LOADOUT_B_CAP4},
		{VarbitID.RUNE_POUCH_LOADOUT_C_CAP1, VarbitID.RUNE_POUCH_LOADOUT_C_CAP2, VarbitID.RUNE_POUCH_LOADOUT_C_CAP3, VarbitID.RUNE_POUCH_LOADOUT_C_CAP4},
		{VarbitID.RUNE_POUCH_LOADOUT_D_CAP1, VarbitID.RUNE_POUCH_LOADOUT_D_CAP2, -1, VarbitID.RUNE_POUCH_LOADOUT_D_CAP4},
		{VarbitID.RUNE_POUCH_LOADOUT_E_CAP1, VarbitID.RUNE_POUCH_LOADOUT_E_CAP2, VarbitID.RUNE_POUCH_LOADOUT_E_CAP3, VarbitID.RUNE_POUCH_LOADOUT_E_CAP4},
		{VarbitID.RUNE_POUCH_LOADOUT_F_CAP1, VarbitID.RUNE_POUCH_LOADOUT_F_CAP2, VarbitID.RUNE_POUCH_LOADOUT_F_CAP3, VarbitID.RUNE_POUCH_LOADOUT_F_CAP4},
		{VarbitID.RUNE_POUCH_LOADOUT_G_CAP1, VarbitID.RUNE_POUCH_LOADOUT_G_CAP2, VarbitID.RUNE_POUCH_LOADOUT_G_CAP3, VarbitID.RUNE_POUCH_LOADOUT_G_CAP4},
		{VarbitID.RUNE_POUCH_LOADOUT_H_CAP1, VarbitID.RUNE_POUCH_LOADOUT_H_CAP2, -1, VarbitID.RUNE_POUCH_LOADOUT_H_CAP4},
		{VarbitID.RUNE_POUCH_LOADOUT_I_CAP1, VarbitID.RUNE_POUCH_LOADOUT_I_CAP2, VarbitID.RUNE_POUCH_LOADOUT_I_CAP3, VarbitID.RUNE_POUCH_LOADOUT_I_CAP4},
		{VarbitID.RUNE_POUCH_LOADOUT_J_CAP1, VarbitID.RUNE_POUCH_LOADOUT_J_CAP2, VarbitID.RUNE_POUCH_LOADOUT_J_CAP3, VarbitID.RUNE_POUCH_LOADOUT_J_CAP4},
	};

	private final Client client;
	private final RunePouchLoadoutConfigStore configStore;

	private final Map<Integer, int[]> originalGeometry = new HashMap<>();
	// Widgets we've created ourselves (theme icons, rune icons), keyed by
	// "parentWidgetId:tag" so we can find-and-reuse them across refreshes
	// without relying on the Name field — which turned out to double as
	// (part of) the hover/menu text for dynamically-created children, unlike
	// native widgets where TargetVerb handles that separately. Reference
	// identity (not Name) is also how we tell "ours" apart from vanilla's
	// own children when hiding/restoring.
	private final Map<String, Widget> ownedWidgets = new HashMap<>();
	// Vanilla's own Load-button decorations (a hover-glow border, confirmed
	// via debug logging) grow a couple pixels on mouseover and never shrink
	// back, which reads as the whole button growing in our tightly-spaced
	// grid. Caches each decoration's first-seen (un-hovered) geometry, keyed
	// by "loadWidgetId:childIndex", so it can be pinned back every tick the
	// same way applyLoadoutIcon already pins the button itself.
	private final Map<String, int[]> loadButtonChildGeometry = new HashMap<>();
	private boolean gridApplied;
	private int currentViewValue;
	private IntConsumer renameRequestHandler;
	private BiConsumer<Integer, Integer> iconChangeRequestHandler;

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

	/**
	 * Called when the user clicks a loadout's theme icon slot (slotIndex,
	 * layer). Wired by the plugin since opening the icon picker lives there.
	 */
	void setIconChangeRequestHandler(BiConsumer<Integer, Integer> handler)
	{
		this.iconChangeRequestHandler = handler;
	}

	void applyGrid(int viewValue)
	{
		this.currentViewValue = viewValue;

		// A fresh panel open rebuilds the interface from scratch, so any
		// widgets we created last time are gone even if this map still
		// references them — start clean. Calls from refresh() (gridApplied
		// already true) intentionally skip this to reuse what's there.
		if (!gridApplied)
		{
			ownedWidgets.clear();
		}

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

		// Confirmed via logging: the container itself (170px) sits inside a
		// wider frame (190px) — vanilla's own static reservation for the
		// scrollbar track, independent of the scrollbar widget's visibility.
		// Since we hide that scrollbar entirely (below), widen the container
		// to match the frame and reclaim that space instead of leaving it as
		// dead margin down the right edge.
		// Confirmed via logging: the container itself (170px) sits inside a
		// wider frame (190px) — vanilla's own static reservation for the
		// scrollbar track, independent of the scrollbar widget's visibility.
		// Since we hide that scrollbar entirely (below), widen the container
		// to match the frame and reclaim that space instead of leaving it as
		// dead margin down the right edge.
		Widget frame = client.getWidget(InterfaceID.Bankside.RUNEPOUCH_FRAME);
		if (frame != null && frame.getWidth() > containerWidth)
		{
			cacheOriginalGeometry(container);
			container.setWidthMode(WidgetSizeMode.ABSOLUTE);
			container.setOriginalWidth(frame.getWidth());
			container.revalidate();
			containerWidth = container.getWidth();
		}

		int usableWidth = containerWidth - RunePouchGridConst.SCROLLBAR_RESERVE;
		int cellWidth = (usableWidth - RunePouchGridConst.CELL_GUTTER_X) / RunePouchGridConst.GRID_COLUMNS;

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

		// Hidden rather than repositioned/resized — reclaims the width it
		// occupied for content instead of just reserving space around it.
		// Scroll wheel/drag still works since it's driven by the container's
		// own scroll state, not by this widget's visibility.
		Widget scrollbar = client.getWidget(InterfaceID.Bankside.RUNEPOUCH_LOADOUT_SCROLLBAR);
		if (scrollbar != null)
		{
			scrollbar.setHidden(true);
			scrollbar.revalidate();
		}

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

	/**
	 * Reverts everything applyGrid() touched: geometry of the native
	 * loadout/load/name widgets, our own created child widgets (hidden, since
	 * there's no per-child removal API — only deleteAllChildren(), which
	 * would also wipe vanilla's own children of the same parent), and
	 * visibility of vanilla's rune-icon children that compactRuneIcons()
	 * hid. Idempotent and safe to call even if the panel already closed.
	 */
	void restoreNativeLayout()
	{
		if (!gridApplied)
		{
			return;
		}

		restoreGeometry(LOADOUT_WIDGET_IDS);
		restoreGeometry(LOAD_WIDGET_IDS);
		restoreGeometry(NAME_WIDGET_IDS);
		restoreGeometry(new int[]{InterfaceID.Bankside.RUNEPOUCH_LOADOUT_CONTAINER});

		Widget scrollbar = client.getWidget(InterfaceID.Bankside.RUNEPOUCH_LOADOUT_SCROLLBAR);
		if (scrollbar != null)
		{
			scrollbar.setHidden(false);
			scrollbar.revalidate();
		}

		for (int widgetId : LOADOUT_WIDGET_IDS)
		{
			Widget loadoutWidget = client.getWidget(widgetId);
			if (loadoutWidget == null)
			{
				continue;
			}

			Widget[] children = loadoutWidget.getDynamicChildren();
			if (children == null)
			{
				continue;
			}

			for (Widget child : children)
			{
				boolean isOurs = ownedWidgets.containsValue(child);
				child.setHidden(isOurs);
				child.revalidate();
			}
		}

		originalGeometry.clear();
		ownedWidgets.clear();
		loadButtonChildGeometry.clear();
		gridApplied = false;
	}

	private void restoreGeometry(int[] widgetIds)
	{
		for (int widgetId : widgetIds)
		{
			Widget widget = client.getWidget(widgetId);
			int[] original = originalGeometry.get(widgetId);
			if (widget == null || original == null)
			{
				continue;
			}

			widget.setXPositionMode(original[0]);
			widget.setYPositionMode(original[1]);
			widget.setWidthMode(original[2]);
			widget.setHeightMode(original[3]);
			widget.setOriginalX(original[4]);
			widget.setOriginalY(original[5]);
			widget.setOriginalWidth(original[6]);
			widget.setOriginalHeight(original[7]);
			widget.revalidate();
		}
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

		cacheOriginalGeometry(nameWidget);
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
		// full-width) row. Pin it explicitly below our name strip. Vertically
		// centered against the combined height of the theme-icon + rune-icon
		// rows beside it (rather than sharing their top edge) since the
		// button, at its native size, is shorter than that combined span.
		int rowsTop = RunePouchGridConst.NAME_HEIGHT + RunePouchGridConst.ROW_TOP_GAP;
		int rowsHeight = RunePouchGridConst.CUSTOM_ICON_SIZE + RunePouchGridConst.RUNE_ROW_GAP + RunePouchGridConst.RUNE_ICON_SIZE;
		int buttonY = rowsTop + (rowsHeight - RunePouchGridConst.LOAD_BUTTON_HEIGHT) / 2;

		cacheOriginalGeometry(loadWidget);
		loadWidget.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		loadWidget.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		loadWidget.setWidthMode(WidgetSizeMode.ABSOLUTE);
		loadWidget.setHeightMode(WidgetSizeMode.ABSOLUTE);
		loadWidget.setOriginalX(0);
		loadWidget.setOriginalY(buttonY);
		loadWidget.setOriginalWidth(RunePouchGridConst.LOAD_BUTTON_WIDTH);
		loadWidget.setOriginalHeight(RunePouchGridConst.LOAD_BUTTON_HEIGHT);
		loadWidget.revalidate();
		pinLoadButtonChildren(loadWidget);

		// Pinning children fights the hover-grow after the fact (and can lag
		// a tick behind since vanilla's hover script re-fires far more often
		// than our GameTick correction), so it still visibly grew and shrank
		// while hovering. Neutralize the growth at the source instead by
		// replacing vanilla's own hover listeners with no-ops — this doesn't
		// touch the Load click action, which is driven by the widget's
		// Action[] entries and MenuEntryAdded, not by these listeners.
		loadWidget.setHasListener(true);
		loadWidget.setOnMouseOverListener((JavaScriptCallback) (ScriptEvent event) -> {});
		loadWidget.setOnMouseRepeatListener((JavaScriptCallback) (ScriptEvent event) -> {});
		loadWidget.setOnMouseLeaveListener((JavaScriptCallback) (ScriptEvent event) -> {});

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
		int iconY = buttonTop;
		int iconX = RunePouchGridConst.THEME_ICON_X;

		applyIconSlot(loadoutWidget, ICON_CHILD_NAME, iconX, iconY, RunePouchGridConst.CUSTOM_ICON_SIZE, primarySprite, isCustomPrimary, slotIndex, 0);

		// Same size, side by side — not stacked on the primary icon.
		int layerX = iconX + RunePouchGridConst.CUSTOM_ICON_SIZE + RunePouchGridConst.CUSTOM_ICON_GUTTER;
		applyIconSlot(loadoutWidget, LAYER_CHILD_NAME, layerX, iconY, RunePouchGridConst.CUSTOM_ICON_SIZE, layerSprite, hasLayer, slotIndex, 1);
	}

	/**
	 * Vanilla's Load button has its own dynamic children — a hover-glow
	 * border among them — that a native script enlarges by a couple pixels
	 * on mouseover and never shrinks back (confirmed via debug logging: two
	 * of them grew from 9x14/12x9 to 9x16/14x9 and stayed there after the
	 * mouse moved away). In our tightly-spaced grid that reads as the whole
	 * button growing. Cache each child's first-seen (un-hovered) geometry
	 * and pin it back every tick, the same way the button itself is pinned.
	 */
	private void pinLoadButtonChildren(Widget loadWidget)
	{
		Widget[] children = loadWidget.getDynamicChildren();
		if (children == null)
		{
			return;
		}

		for (int i = 0; i < children.length; i++)
		{
			Widget child = children[i];
			String key = loadWidget.getId() + ":" + i;
			int[] original = loadButtonChildGeometry.computeIfAbsent(key, k -> new int[]{
				child.getOriginalWidth(),
				child.getOriginalHeight(),
				child.getOriginalX(),
				child.getOriginalY(),
			});

			child.setOriginalWidth(original[0]);
			child.setOriginalHeight(original[1]);
			child.setOriginalX(original[2]);
			child.setOriginalY(original[3]);
			child.revalidate();
		}
	}

	/**
	 * Renders one custom icon slot — the real sprite when set, or vanilla's
	 * own "no rune assigned" item icon (11526) as a placeholder when not, so
	 * it reads the same as the rest of this interface. Clicking (either
	 * button, matching how a single action shows for both) opens the icon
	 * picker for this slot/layer.
	 */
	private void applyIconSlot(Widget parent, String tag, int x, int y, int size, int spriteId, boolean isSet, int slotIndex, int layer)
	{
		// Subtle dark backdrop panel so the slot reads as a distinct target
		// even before an icon is set. RECTANGLE children do render here fine
		// (confirmed with bold debug styling) — the earlier "not visible"
		// attempts were just too faint/dark against the UI's own dark
		// background, not a rendering failure.
		int padding = RunePouchGridConst.ICON_BORDER_PADDING;
		Widget border = getOrCreateOwned(parent, tag + "-border", WidgetType.RECTANGLE);
		border.setFilled(true);
		border.setTextColor(0x000000);
		border.setOpacity(100);
		border.setWidthMode(WidgetSizeMode.ABSOLUTE);
		border.setHeightMode(WidgetSizeMode.ABSOLUTE);
		border.setOriginalWidth(size + padding * 2);
		border.setOriginalHeight(size + padding * 2);
		border.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		border.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		border.setOriginalX(x - padding);
		border.setOriginalY(y - padding);
		border.setHidden(false);
		border.revalidate();

		Widget icon = getOrCreateOwned(parent, tag, WidgetType.GRAPHIC);
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

		// TargetVerb doesn't concatenate onto Action for dynamically-created
		// children the way it does for native widgets (confirmed against our
		// own icon picker, which puts the whole label in Action alone and
		// displays correctly) — so put the full text in Action directly.
		// Two actions on one widget show as two right-click menu rows, with
		// op 1 (the first) also firing on a plain left-click.
		icon.setHasListener(true);
		icon.clearActions();
		icon.setAction(0, "Change icon");
		icon.setAction(1, "Reset icon");
		icon.setOnOpListener((JavaScriptCallback) (ScriptEvent event) ->
		{
			int op = event.getOp();
			if (op == 1 && iconChangeRequestHandler != null)
			{
				iconChangeRequestHandler.accept(slotIndex, layer);
			}
			else if (op == 2)
			{
				configStore.resetIcon(currentViewValue, slotIndex, layer);
				refresh();
			}
		});
		icon.revalidate();
	}

	/**
	 * Vanilla's own rune-type icon children (inside RUNEPOUCH_LOADOUT_*) are
	 * item-rendered widgets a vanilla script continuously re-anchors to the
	 * right/bottom edge of the old full-width single-column row — resizing
	 * them sticks, but any reposition we apply gets silently overwritten.
	 * So instead: read which item each one shows (before hiding it), then
	 * draw our own replacement icons — plain widgets vanilla's script has no
	 * reason to touch — in a compact grid to the right of the theme icons.
	 * Each replacement forwards clicks to the original (still-present, just
	 * hidden) widget's own action, so the vanilla rune picker still opens.
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

		List<Widget> originals = new ArrayList<>();
		for (Widget child : children)
		{
			if (ownedWidgets.containsValue(child))
			{
				continue;
			}

			// SpriteId is always -1 on these (they're item-rendered, not
			// sprite graphics) — ItemId is what actually distinguishes a
			// populated slot (e.g. 565 = Blood rune) from an empty one (-1).
			if (child.getItemId() >= 0)
			{
				originals.add(child);
			}

			child.setHidden(true);
			child.revalidate();
		}

		int buttonTop = RunePouchGridConst.NAME_HEIGHT + RunePouchGridConst.ROW_TOP_GAP;

		// Center the rune row under the theme-icon row above it, rather than
		// left-aligning to a fixed X — the theme row is always 2 fixed-size
		// icons, but the rune row's width varies with how many runes this
		// loadout actually has saved, so the centering offset has to be
		// computed per-loadout instead of being a constant.
		int shownCount = Math.min(originals.size(), RunePouchGridConst.RUNE_ICON_MAX_SLOTS);
		int themeRowWidth = RunePouchGridConst.CUSTOM_ICON_SIZE * 2 + RunePouchGridConst.CUSTOM_ICON_GUTTER;
		int runeRowWidth = shownCount > 0
			? shownCount * RunePouchGridConst.RUNE_ICON_SIZE + (shownCount - 1) * RunePouchGridConst.RUNE_ICON_GUTTER
			: 0;
		int runeRowX = RunePouchGridConst.THEME_ICON_X + (themeRowWidth - runeRowWidth) / 2;

		for (int i = 0; i < RunePouchGridConst.RUNE_ICON_MAX_SLOTS; i++)
		{
			Widget runeIcon = getOrCreateOwned(loadoutWidget, RUNE_ICON_CHILD_PREFIX + i, WidgetType.GRAPHIC);

			if (i >= originals.size())
			{
				runeIcon.setHasListener(false);
				runeIcon.setHidden(true);
				runeIcon.revalidate();
				continue;
			}

			Widget original = originals.get(i);

			// Single row below the theme-icon row, centered under it.
			int runeRowY = buttonTop + RunePouchGridConst.CUSTOM_ICON_SIZE + RunePouchGridConst.RUNE_ROW_GAP;

			runeIcon.setItemId(original.getItemId());
			runeIcon.setItemQuantity(1);
			runeIcon.setItemQuantityMode(ItemQuantityMode.NEVER);
			runeIcon.setWidthMode(WidgetSizeMode.ABSOLUTE);
			runeIcon.setHeightMode(WidgetSizeMode.ABSOLUTE);
			runeIcon.setOriginalWidth(RunePouchGridConst.RUNE_ICON_SIZE);
			runeIcon.setOriginalHeight(RunePouchGridConst.RUNE_ICON_SIZE);
			runeIcon.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
			runeIcon.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
			int runeIconX = runeRowX + i * (RunePouchGridConst.RUNE_ICON_SIZE + RunePouchGridConst.RUNE_ICON_GUTTER);
			runeIcon.setOriginalX(runeIconX);
			runeIcon.setOriginalY(runeRowY);
			runeIcon.setHidden(false);

			// Vanilla's own click action opens its native rune picker on the
			// original widget; forward clicks on our replacement there
			// instead of trying to reimplement or guess at that behavior.
			// TargetVerb doesn't concatenate onto Action for dynamically
			// created children the way it does for native widgets, so the
			// full label goes directly into Action instead.
			runeIcon.setHasListener(true);
			runeIcon.clearActions();
			runeIcon.setAction(0, "Change " + Text.removeTags(original.getName()) + runeCapSuffix(slotIndex, i));
			runeIcon.setOnOpListener((JavaScriptCallback) (ScriptEvent event) ->
			{
				if (event.getOp() != 1)
				{
					return;
				}

				client.menuAction(original.getIndex(), original.getId(), MenuAction.CC_OP,
					1, original.getItemId(), "Change", "");
			});
			runeIcon.revalidate();
		}
	}

	/**
	 * " (1,000)" when this rune position has a specific saved quantity cap,
	 * or "" when it's unlimited ("All", cap 0) or the cap can't be read
	 * (RUNE_CAP_VARBIT_IDS entry -1 — see its javadoc).
	 */
	private String runeCapSuffix(int slotIndex, int position)
	{
		int[] caps = RUNE_CAP_VARBIT_IDS[slotIndex];
		if (position >= caps.length || caps[position] == -1)
		{
			return "";
		}

		int cap = client.getVarbitValue(caps[position]);
		return cap > 0 ? String.format(" (%,d)", cap) : "";
	}

	private Widget getOrCreateOwned(Widget parent, String tag, int type)
	{
		String key = parent.getId() + ":" + tag;
		Widget cached = ownedWidgets.get(key);
		if (cached != null)
		{
			return cached;
		}

		Widget created = parent.createChild(-1, type);
		ownedWidgets.put(key, created);
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
