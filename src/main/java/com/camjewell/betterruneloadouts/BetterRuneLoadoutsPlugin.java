package com.camjewell.betterruneloadouts;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import java.awt.Color;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Menu;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Better Rune Loadouts",
	description = "Redesigns the rune pouch loadout popup into a scrollable icon grid, with custom icons and names per loadout",
	tags = {"runepouch", "rune", "pouch", "loadout", "bank"}
)
public class BetterRuneLoadoutsPlugin extends Plugin
{
	private static final String RENAME_PROMPT_FORMAT = "%s<br>" + ColorUtil.prependColorTag("(Limit %s Characters)", new Color(0, 0, 170));
	private static final int RENAME_CHARACTER_LIMIT = 40;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ChatboxPanelManager chatboxPanelManager;

	@Inject
	private RunePouchGridManager gridManager;

	@Inject
	private RunePouchLoadoutConfigStore configStore;

	private int lastViewValue;

	// Vanilla re-populates the rune-icon widgets (undoing our hide) not just
	// once on open but on other actions too, e.g. clicking Load — there's no
	// single event to hook for "vanilla just touched these widgets again".
	// So instead of guessing every trigger, keep reapplying every real game
	// tick for as long as the panel stays open; applyGrid() is idempotent
	// widget-property sets, cheap enough to redo this often while a menu is
	// actually open.
	private boolean runepouchPanelOpen;

	@Provides
	BetterRuneLoadoutsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BetterRuneLoadoutsConfig.class);
	}

	@Override
	protected void startUp()
	{
		gridManager.setRenameRequestHandler(this::renameLoadout);

		clientThread.invokeLater(() ->
		{
			Widget runepouchContainer = client.getWidget(InterfaceID.Bankside.RUNEPOUCH_CONTAINER);
			if (runepouchContainer != null && !runepouchContainer.isHidden() && lastViewValue != 0)
			{
				gridManager.applyGrid(lastViewValue);
				runepouchPanelOpen = true;
			}
		});
	}

	@Override
	protected void shutDown()
	{
		runepouchPanelOpen = false;
		clientThread.invokeLater(gridManager::restoreNativeLayout);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() != VarbitID.BANK_VIEWCONTAINER)
		{
			return;
		}

		int viewValue = event.getValue();
		if (viewValue == 3 || viewValue == 4)
		{
			lastViewValue = viewValue;
			clientThread.invokeLater(() -> gridManager.applyGrid(viewValue));
			runepouchPanelOpen = true;
		}
		else
		{
			runepouchPanelOpen = false;
			clientThread.invokeLater(gridManager::restoreNativeLayout);
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!runepouchPanelOpen)
		{
			return;
		}

		gridManager.applyGrid(lastViewValue);
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		MenuEntry firstEntry = event.getFirstEntry();
		if (firstEntry == null)
		{
			return;
		}

		Widget widget = firstEntry.getWidget();
		if (widget == null)
		{
			return;
		}

		int slotIndex = gridManager.slotIndexForLoadWidget(widget.getId());
		if (slotIndex == -1)
		{
			return;
		}

		addLoadoutMenuEntries(slotIndex);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		MenuEntry menuEntry = event.getMenuEntry();
		Widget widget = menuEntry.getWidget();
		if (widget == null)
		{
			return;
		}

		// NAME_* widgets aren't handled here — they own their own action/click
		// via setOnOpListener in RunePouchGridManager.applyLoadoutName, since
		// relabeling the menu text alone would leave vanilla's original click
		// behavior (opening its native preset-name picker) still wired underneath.
		int slotIndex = gridManager.slotIndexForLoadWidget(widget.getId());
		if (slotIndex != -1)
		{
			menuEntry.setOption("Load").setTarget(gridManager.getLoadoutName(slotIndex));
		}
	}

	private void addLoadoutMenuEntries(int slotIndex)
	{
		Menu menu = client.getMenu();

		menu.createMenuEntry(1)
			.setOption("Rename")
			.setTarget(gridManager.getLoadoutName(slotIndex))
			.setType(MenuAction.RUNELITE)
			.onClick(e -> renameLoadout(slotIndex));

		addIconMenuEntry(menu, slotIndex, 0, "Icon 1");
		addIconMenuEntry(menu, slotIndex, 1, "Icon 2");

		menu.createMenuEntry(1)
			.setOption("Reset")
			.setTarget("Icons")
			.setType(MenuAction.RUNELITE)
			.onClick(e ->
			{
				configStore.resetIcon(lastViewValue, slotIndex, 0);
				configStore.resetIcon(lastViewValue, slotIndex, 1);
				clientThread.invokeLater(gridManager::refresh);
			});
	}

	private void addIconMenuEntry(Menu menu, int slotIndex, int layer, String label)
	{
		MenuEntry entry = menu.createMenuEntry(1)
			.setOption("Change")
			.setTarget(label)
			.setType(MenuAction.RUNELITE)
			.onClick(e -> changeLoadoutIcon(slotIndex, layer));

		Menu subMenu = entry.createSubMenu();
		subMenu.createMenuEntry(-1)
			.setOption("Change")
			.setType(MenuAction.RUNELITE)
			.onClick(e -> changeLoadoutIcon(slotIndex, layer));
		subMenu.createMenuEntry(-1)
			.setOption("Remove")
			.setType(MenuAction.RUNELITE)
			.onClick(e ->
			{
				configStore.resetIcon(lastViewValue, slotIndex, layer);
				clientThread.invokeLater(gridManager::refresh);
			});
	}

	private void renameLoadout(int slotIndex)
	{
		String currentName = gridManager.getLoadoutName(slotIndex);
		chatboxPanelManager.openTextInput(String.format(RENAME_PROMPT_FORMAT, "Loadout name", RENAME_CHARACTER_LIMIT))
			.value(Strings.nullToEmpty(currentName))
			.onDone((newName) ->
			{
				if (newName == null)
				{
					return;
				}

				String cleaned = Text.removeTags(newName).trim();
				if (!cleaned.isEmpty())
				{
					configStore.setName(lastViewValue, slotIndex, cleaned);
					clientThread.invokeLater(gridManager::refresh);
				}
			})
			.build();
	}

	private void changeLoadoutIcon(int slotIndex, int layer)
	{
		int defaultSprite = layer == 0 ? RunePouchLoadoutIcon.DEFAULT_SPRITE_ID : RunePouchLoadoutIcon.NO_SECOND_ICON;
		int currentSprite = configStore.getIcon(lastViewValue, slotIndex, layer, defaultSprite);

		new RunePouchLoadoutIconPicker(client, chatboxPanelManager, currentSprite, spriteId ->
		{
			configStore.setIcon(lastViewValue, slotIndex, layer, spriteId);
			clientThread.invokeLater(gridManager::refresh);
		}).show();
	}
}
