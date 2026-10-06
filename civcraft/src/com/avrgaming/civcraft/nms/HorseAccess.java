package com.avrgaming.civcraft.nms;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** One horse's NBT-backed values. Reads come from the snapshot taken when the horse was wrapped; writes update the horse. */
public interface HorseAccess {

	int getInt(String key);

	boolean getBoolean(String key);

	void setInt(String key, int value);

	void setBoolean(String key, boolean value);

	ItemStack getArmorItem();

	/** A null item clears the armor. */
	void setArmorItem(ItemStack item);

	void openInventory(Player player);

	LivingEntity getHorse();
}
