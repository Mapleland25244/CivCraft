package com.avrgaming.civcraft.integration;

import org.bukkit.Location;

import com.avrgaming.civcraft.main.CivLog;

import de.hellfirepvp.api.CustomMobsAPI;
import de.hellfirepvp.api.data.ICustomMob;
import de.hellfirepvp.api.data.ISpawnerEditor;
import de.hellfirepvp.api.data.ISpawnerEditor.SpawnerInfo;

class CustomMobsProvider implements MobSpawnProvider {

	@Override
	public void setSpawnerActive(String mobName, Location loc, boolean active) {
		ISpawnerEditor spawnerEditor = CustomMobsAPI.getSpawnerEditor();
		if (spawnerEditor == null) {
			CivLog.warning("Unable to create Spawners; CustomMobsAPI does not exist");
			return;
		}

		SpawnerInfo info = spawnerEditor.getSpawner(loc);
		if (active) {
			if (info.getSpawner() != null) {
				return;
			}
			ICustomMob mob = CustomMobsAPI.getCustomMob(mobName);
			if (mob == null) {
				CivLog.warning("Unable to create Spawner; " + mobName + " does not exist");
				return;
			}
			spawnerEditor.setSpawner(mob, loc, 60);
		} else if (info.getSpawner() != null) {
			CivLog.debug("Spawner Disabled at " + loc);
			spawnerEditor.resetSpawner(loc);
		}
	}
}
