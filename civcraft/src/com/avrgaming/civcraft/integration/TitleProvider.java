package com.avrgaming.civcraft.integration;

import org.bukkit.entity.Player;

public interface TitleProvider {
	void send(Player player, int fadeIn, int show, int fadeOut, String title, String subTitle);
}
