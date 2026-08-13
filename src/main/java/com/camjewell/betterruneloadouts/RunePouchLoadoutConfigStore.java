package com.camjewell.betterruneloadouts;

import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;

/**
 * Per-loadout name/icon customization, stored per RuneScape profile and
 * scoped by the rune pouch tier's bank-view value (so a regular pouch and
 * a divine pouch don't share the same 10 loadout names/icons).
 */
class RunePouchLoadoutConfigStore
{
	private final ConfigManager configManager;

	@Inject
	RunePouchLoadoutConfigStore(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	String getName(int viewValue, int slotIndex, String defaultName)
	{
		String name = configManager.getRSProfileConfiguration(BetterRuneLoadoutsConfig.GROUP, nameKey(viewValue, slotIndex));
		return name == null || name.isEmpty() ? defaultName : name;
	}

	void setName(int viewValue, int slotIndex, String name)
	{
		configManager.setRSProfileConfiguration(BetterRuneLoadoutsConfig.GROUP, nameKey(viewValue, slotIndex), name);
	}

	int getIcon(int viewValue, int slotIndex, int layer, int defaultSpriteId)
	{
		String value = configManager.getRSProfileConfiguration(BetterRuneLoadoutsConfig.GROUP, iconKey(viewValue, slotIndex, layer));
		if (value == null || value.isEmpty())
		{
			return defaultSpriteId;
		}

		try
		{
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e)
		{
			return defaultSpriteId;
		}
	}

	void setIcon(int viewValue, int slotIndex, int layer, int spriteId)
	{
		configManager.setRSProfileConfiguration(BetterRuneLoadoutsConfig.GROUP, iconKey(viewValue, slotIndex, layer), String.valueOf(spriteId));
	}

	void resetIcon(int viewValue, int slotIndex, int layer)
	{
		configManager.unsetRSProfileConfiguration(BetterRuneLoadoutsConfig.GROUP, iconKey(viewValue, slotIndex, layer));
	}

	private static String nameKey(int viewValue, int slotIndex)
	{
		return viewValue + "." + slotIndex + ".name";
	}

	private static String iconKey(int viewValue, int slotIndex, int layer)
	{
		return viewValue + "." + slotIndex + (layer == 0 ? ".icon" : ".icon" + (layer + 1));
	}
}
