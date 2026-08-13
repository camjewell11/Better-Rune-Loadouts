package com.camjewell.betterruneloadouts;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Better Rune Loadouts",
	description = "TODO: describe Better Rune Loadouts",
	tags = {}
)
public class BetterRuneLoadoutsPlugin extends Plugin
{
	@Inject
	private BetterRuneLoadoutsConfig config;

	@Provides
	BetterRuneLoadoutsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BetterRuneLoadoutsConfig.class);
	}

	@Override
	protected void startUp()
	{
		log.info("Better Rune Loadouts started");
	}

	@Override
	protected void shutDown()
	{
		log.info("Better Rune Loadouts stopped");
	}
}
