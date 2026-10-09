package com.avrgaming.civcraft.nms;

import org.bukkit.Bukkit;

import com.avrgaming.civcraft.nms.v1_13_R2.NmsAdapter_v1_13_R2;

/**
 * Picks the {@link NmsAdapter} for the running server. To support a new version, add an adapter class and one case
 * to {@link #create(String)}; an unknown version stops the plugin at startup instead of failing later in some feature.
 */
public final class Nms {

	// volatile: get() is called from async tasks and from hot listener paths, so reads must not take the lock
	private static volatile NmsAdapter adapter;

	private Nms() {
	}

	/** Call once from onEnable. Throws IllegalStateException when no adapter matches the server. */
	public static synchronized void init() {
		String version = serverVersion();
		adapter = create(version);
		if (adapter == null) {
			throw new IllegalStateException("CivCraft has no NMS adapter for server version '" + version
					+ "'. Supported: v1_13_R2.");
		}
	}

	public static NmsAdapter get() {
		NmsAdapter a = adapter;
		if (a == null) {
			init();
			a = adapter;
		}
		return a;
	}

	private static NmsAdapter create(String version) {
		switch (version) {
		case "v1_13_R2":
			return new NmsAdapter_v1_13_R2();
		default:
			return null;
		}
	}

	/** The last package segment of the CraftBukkit server class, e.g. "v1_13_R2". */
	private static String serverVersion() {
		String name = Bukkit.getServer().getClass().getPackage().getName();
		return name.substring(name.lastIndexOf('.') + 1);
	}
}
