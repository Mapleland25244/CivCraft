package com.avrgaming.civcraft.integration;

import org.bukkit.Location;

import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.Config;

class WorldBorderProvider implements BorderProvider {

	@Override
	public boolean isInsideBorder(Location loc) {
		BorderData border = Config.Border(loc.getWorld().getName());
		if (border == null) {
			return true;
		}
		return border.insideBorder(loc.getX(), loc.getZ(), Config.ShapeRound());
	}
}
