package com.avrgaming.civcraft.integration;

import org.bukkit.entity.Player;

public interface EconomyProvider {
	double getBalance(Player player);

	boolean has(Player player, double amount);

	void deposit(Player player, double amount);

	void withdraw(Player player, double amount);
}
