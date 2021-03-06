/*
 * Copyright � 2014 - 2016 | Wurst-Imperium | All rights reserved.
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package tk.wurst_client.special;

import java.util.HashSet;

import tk.wurst_client.mods.Mod;
import tk.wurst_client.mods.Mod.Bypasses;
import tk.wurst_client.navigator.settings.CheckboxSetting;
import tk.wurst_client.navigator.settings.ModeSetting;

@Spf.Info(
	description = "Makes other features bypass AntiCheat plugins or blocks them if they can't.",
	name = "YesCheat+",
	tags = "YesCheatPlus, NoCheat+, NoCheatPlus, AntiMAC, yes cheat plus, no cheat plus, anti mac, ncp bypasses",
	help = "Special_Features/YesCheat")
public class YesCheatSpf extends Spf
{
	private final HashSet<Mod> blockedMods = new HashSet<Mod>();
	private BypassLevel bypassLevel = BypassLevel.OFF;
	
	public CheckboxSetting modeIndicator =
		new CheckboxSetting("Mode Indicator", true);
	
	public YesCheatSpf()
	{
		settings.add(new ModeSetting("Bypass Level", BypassLevel.getNames(),
			bypassLevel.ordinal())
		{
			@Override
			public void update()
			{
				bypassLevel = BypassLevel.values()[getSelected()];
				
				blockedMods.forEach((mod) -> mod.setBlocked(false));
				
				blockedMods.clear();
				wurst.mods.getAllMods().forEach((mod) -> {
					if(!bypassLevel.doesBypass(mod.getBypasses()))
						blockedMods.add(mod);
				});
				
				blockedMods.forEach((mod) -> mod.setBlocked(true));
				
				wurst.mods.getAllMods()
					.forEach((mod) -> mod.onYesCheatUpdate(bypassLevel));
			}
		});
		settings.add(modeIndicator);
	}
	
	public BypassLevel getBypassLevel()
	{
		return bypassLevel;
	}
	
	private interface BypassTest
	{
		public boolean doesBypass(Bypasses b);
	}
	
	public static enum BypassLevel
	{
		OFF("Off", (b) -> {
			return true;
		}),
		MINEPLEX_ANTICHEAT("Mineplex AntiCheat", (b) -> b.mineplexAntiCheat()),
		ANTICHEAT("AntiCheat", (b) -> b.antiCheat()),
		OLDER_NCP("Older NoCheat+", (b) -> b.olderNCP()),
		LATEST_NCP("Latest NoCheat+", (b) -> b.latestNCP()),
		GHOST_MODE("Ghost Mode", (b) -> b.ghostMode());
		
		private final String name;
		private final BypassTest test;
		
		private BypassLevel(String name, BypassTest test)
		{
			this.name = name;
			this.test = test;
		}
		
		public boolean doesBypass(Bypasses bypasses)
		{
			return test.doesBypass(bypasses);
		}
		
		public String getName()
		{
			return name;
		}
		
		public static String[] getNames()
		{
			String[] names = new String[values().length];
			for(int i = 0; i < names.length; i++)
				names[i] = values()[i].name;
			return names;
		}
	}
}
