package com.avrgaming.civcraft.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;

class VaultEconomyProvider implements EconomyProvider {

	private final Economy econ;

	private VaultEconomyProvider(Economy econ) {
		this.econ = econ;
	}

	/** Returns null while no economy plugin has registered with Vault. */
	static VaultEconomyProvider tryCreate() {
		RegisteredServiceProvider<Economy> reg = Bukkit.getServicesManager().getRegistration(Economy.class);
		if (reg == null) {
			return null;
		}
		return new VaultEconomyProvider(reg.getProvider());
	}

	@Override
	public double getBalance(Player player) {
		return econ.getBalance(player);
	}

	@Override
	public boolean has(Player player, double amount) {
		return econ.has(player, amount);
	}

	@Override
	public void deposit(Player player, double amount) {
		econ.depositPlayer(player, amount);
	}

	@Override
	public void withdraw(Player player, double amount) {
		econ.withdrawPlayer(player, amount);
	}
}
