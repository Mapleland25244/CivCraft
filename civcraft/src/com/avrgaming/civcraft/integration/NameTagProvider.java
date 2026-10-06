package com.avrgaming.civcraft.integration;

import java.util.Collection;

import org.bukkit.entity.Player;

public interface NameTagProvider {
	/** Re-send {@code target}'s name tag to the given viewers. */
	void refresh(Player target, Collection<? extends Player> viewers);

	void refresh(Player target, Player viewer);
}
