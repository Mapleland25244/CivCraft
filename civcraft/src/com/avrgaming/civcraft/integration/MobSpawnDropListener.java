package com.avrgaming.civcraft.integration;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/** The vanilla drops every custom mob has, per the CivCraft wiki "General Drops" table. */
class MobSpawnDropListener implements Listener {

	private static final Material[] ITEMS = {
		Material.BONE, Material.SUGAR, Material.GUNPOWDER, Material.POTATO,
		Material.CARROT, Material.COAL, Material.STRING, Material.SLIME_BALL
	};
	private static final double[] CHANCES = { 0.10, 0.10, 0.25, 0.10, 0.10, 0.10, 0.10, 0.02 };

	private final Random random = new Random();

	@EventHandler
	public void onDeath(EntityDeathEvent event) {
		LivingEntity mob = event.getEntity();
		if (!mob.getScoreboardTags().contains(NativeMobSpawnProvider.MOB_TAG)) {
			return;
		}

		/* Replace the vanilla loot so only the wiki table applies. */
		event.getDrops().clear();
		for (int i = 0; i < ITEMS.length; i++) {
			if (random.nextDouble() < CHANCES[i]) {
				event.getDrops().add(new ItemStack(ITEMS[i]));
			}
		}
	}
}
