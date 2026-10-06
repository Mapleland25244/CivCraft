package com.avrgaming.civcraft.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.kitteh.vanish.VanishPlugin;

class VanishNoPacketProvider implements VanishProvider {

	@Override
	public boolean isVanished(Player player) {
		VanishPlugin vnp = (VanishPlugin) Bukkit.getPluginManager().getPlugin("VanishNoPacket");
		return vnp != null && vnp.getManager().isVanished(player);
	}
}
