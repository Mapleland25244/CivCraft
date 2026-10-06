package com.avrgaming.civcraft.integration;

import org.bukkit.Location;

public interface BorderProvider {
	/** True when the location is inside the world border (or the world has none). */
	boolean isInsideBorder(Location loc);
}
