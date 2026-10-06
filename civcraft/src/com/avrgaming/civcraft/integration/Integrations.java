package com.avrgaming.civcraft.integration;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivLog;

/**
 * Single access point for optional third-party plugins. Core code must only talk to the provider
 * interfaces; the classes that import a third-party API live in this package and are only loaded
 * after {@link #init(CivCraft)} saw the plugin enabled, so a missing plugin never causes a
 * NoClassDefFoundError.
 */
public final class Integrations {

	private static volatile NameTagProvider nameTags;
	private static volatile BorderProvider border;
	private static volatile TitleProvider titles;
	private static volatile VanishProvider vanish;
	private static volatile MobSpawnProvider mobSpawns;
	private static volatile EconomyProvider economy;
	private static boolean vaultEnabled;

	private Integrations() {
	}

	/** Detects enabled plugins and installs the matching hooks. Call after {@code CivSettings.init}. */
	public static void init(CivCraft plugin) {
		PluginManager pm = Bukkit.getPluginManager();

		if ((pm.isPluginEnabled("iTag") || pm.isPluginEnabled("TagAPI")) && pm.isPluginEnabled("ProtocolLib")) {
			nameTags = new ITagNameTagProvider();
			pm.registerEvents(new TagAPIListener(), plugin);
			CivLog.debug("TagAPI Registered");
		} else {
			CivLog.warning("TagAPI not found, not registering TagAPI hooks. This is fine if you're not using TagAPI.");
		}

		if (pm.isPluginEnabled("HeroChat")) {
			pm.registerEvents(new HeroChatListener(), plugin);
		}

		if (pm.isPluginEnabled("NoCheatPlus")) {
			NoCheatPlusSurvialFlyHandler.init();
		} else {
			CivLog.warning("NoCheatPlus not found, not registering NCP hooks. This is fine if you're not using NCP.");
		}

		if (pm.isPluginEnabled("WorldBorder")) {
			border = new WorldBorderProvider();
		} else {
			CivLog.warning("WorldBorder not found, buildings will not be checked against the world border.");
		}

		if (CivSettings.hasTitleAPI) {
			titles = new TitleApiProvider();
		}
		if (CivSettings.hasVanishNoPacket) {
			vanish = new VanishNoPacketProvider();
		}
		if (CivSettings.hasCustomMobs) {
			mobSpawns = new CustomMobsProvider();
		}
		vaultEnabled = pm.isPluginEnabled("Vault");
	}

	public static void refreshNameTag(Player target, Collection<? extends Player> viewers) {
		NameTagProvider p = nameTags;
		if (p != null) {
			p.refresh(target, viewers);
		}
	}

	public static void refreshNameTag(Player target, Player viewer) {
		NameTagProvider p = nameTags;
		if (p != null) {
			p.refresh(target, viewer);
		}
	}

	/** True when there is no WorldBorder plugin. */
	public static boolean isInsideBorder(Location loc) {
		BorderProvider p = border;
		return p == null || p.isInsideBorder(loc);
	}

	/** @return true if a title was actually sent. */
	public static boolean sendTitle(Player player, int fadeIn, int show, int fadeOut, String title, String subTitle) {
		TitleProvider p = titles;
		if (p == null) {
			return false;
		}
		p.send(player, fadeIn, show, fadeOut, title, subTitle);
		return true;
	}

	public static boolean isVanished(Player player) {
		VanishProvider p = vanish;
		return p != null && p.isVanished(player);
	}

	public static void setMobSpawnerActive(String mobName, Location loc, boolean active) {
		MobSpawnProvider p = mobSpawns;
		if (p == null) {
			CivLog.warning("Unable to change Spawner; CustomMobs is not enabled");
			return;
		}
		p.setSpawnerActive(mobName, loc, active);
	}

	/** @return the Vault economy, or null when Vault or an economy plugin is missing. */
	public static EconomyProvider economy() {
		EconomyProvider e = economy;
		if (e == null && vaultEnabled) {
			e = VaultEconomyProvider.tryCreate();
			economy = e;
		}
		return e;
	}
}
