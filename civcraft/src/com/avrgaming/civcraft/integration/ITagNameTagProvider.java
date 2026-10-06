package com.avrgaming.civcraft.integration;

import java.util.Collection;

import org.bukkit.entity.Player;

import net.md_5.itag.iTag;

class ITagNameTagProvider implements NameTagProvider {

	@Override
	public void refresh(Player target, Collection<? extends Player> viewers) {
		iTag.getInstance().refreshPlayer(target, viewers);
	}

	@Override
	public void refresh(Player target, Player viewer) {
		iTag.getInstance().refreshPlayer(target, viewer);
	}
}
