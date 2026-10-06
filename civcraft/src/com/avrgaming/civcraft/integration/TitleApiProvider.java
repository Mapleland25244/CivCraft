package com.avrgaming.civcraft.integration;

import org.bukkit.entity.Player;

import com.connorlinfoot.titleapi.TitleAPI;

class TitleApiProvider implements TitleProvider {

	@Override
	public void send(Player player, int fadeIn, int show, int fadeOut, String title, String subTitle) {
		TitleAPI.sendTitle(player, fadeIn, show, fadeOut, title, subTitle);
	}
}
