package com.camjewell.betterruneloadouts;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BetterRuneLoadoutsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BetterRuneLoadoutsPlugin.class);
		RuneLite.main(args);
	}
}
