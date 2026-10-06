package com.avrgaming.civcraft.integration;

import org.bukkit.Location;

public interface MobSpawnProvider {
	/** Creates (active) or removes (inactive) a CustomMobs spawner for {@code mobName} at the location. */
	void setSpawnerActive(String mobName, Location loc, boolean active);
}
