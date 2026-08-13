package com.camjewell.betterruneloadouts;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Better Rune Loadouts",
	description = "Redesigns the rune pouch loadout popup into a scrollable icon grid, with custom icons and names per loadout",
	tags = {"runepouch", "rune", "pouch", "loadout", "bank"}
)
public class BetterRuneLoadoutsPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private RunePouchGridManager gridManager;

	@Provides
	BetterRuneLoadoutsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BetterRuneLoadoutsConfig.class);
	}

	@Override
	protected void startUp()
	{
		clientThread.invokeLater(() ->
		{
			Widget runepouchContainer = client.getWidget(InterfaceID.Bankside.RUNEPOUCH_CONTAINER);
			if (runepouchContainer != null && !runepouchContainer.isHidden())
			{
				gridManager.applyGrid();
			}
		});
	}

	@Override
	protected void shutDown()
	{
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
			clientThread.invokeLater(gridManager::applyGrid);
		}
		else
		{
			clientThread.invokeLater(gridManager::restoreNativeLayout);
		}
	}
}
