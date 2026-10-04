package net.md_5.itag;

import java.util.Collection;

import org.bukkit.entity.Player;

/**
 * Compile-time stub of the iTag plugin API. Only the members CivCraft uses are declared.
 * The real class is provided by the iTag plugin on the server; this stub must not be packaged.
 */
public class iTag {

	public static iTag getInstance() {
		throw new UnsupportedOperationException("iTag stub: the real iTag plugin must provide this class at runtime");
	}

	public void refreshPlayer(Player player, Player viewer) {
		throw new UnsupportedOperationException();
	}

	public void refreshPlayer(Player player, Collection<? extends Player> viewers) {
		throw new UnsupportedOperationException();
	}
}
