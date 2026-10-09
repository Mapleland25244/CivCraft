package com.avrgaming.civcraft.compat;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.enchantments.Enchantment;

/**
 * Numeric enchantment ids (1.12 and earlier), which CivCraft stores in serialized inventories and in
 * item text. 1.13 removed the numeric ids from Bukkit, so the table lives here.
 */
public final class LegacyEnchantments {

	private static final Map<Integer, Enchantment> BY_ID = new HashMap<Integer, Enchantment>();
	private static final Map<Enchantment, Integer> BY_ENCHANTMENT = new HashMap<Enchantment, Integer>();

	private static void add(int id, Enchantment enchantment) {
		BY_ID.put(id, enchantment);
		BY_ENCHANTMENT.put(enchantment, id);
	}

	static {
		add(0, Enchantment.PROTECTION_ENVIRONMENTAL);
		add(1, Enchantment.PROTECTION_FIRE);
		add(2, Enchantment.PROTECTION_FALL);
		add(3, Enchantment.PROTECTION_EXPLOSIONS);
		add(4, Enchantment.PROTECTION_PROJECTILE);
		add(5, Enchantment.OXYGEN);
		add(6, Enchantment.WATER_WORKER);
		add(7, Enchantment.THORNS);
		add(8, Enchantment.DEPTH_STRIDER);
		add(9, Enchantment.FROST_WALKER);
		add(10, Enchantment.BINDING_CURSE);
		add(16, Enchantment.DAMAGE_ALL);
		add(17, Enchantment.DAMAGE_UNDEAD);
		add(18, Enchantment.DAMAGE_ARTHROPODS);
		add(19, Enchantment.KNOCKBACK);
		add(20, Enchantment.FIRE_ASPECT);
		add(21, Enchantment.LOOT_BONUS_MOBS);
		add(32, Enchantment.DIG_SPEED);
		add(33, Enchantment.SILK_TOUCH);
		add(34, Enchantment.DURABILITY);
		add(35, Enchantment.LOOT_BONUS_BLOCKS);
		add(48, Enchantment.ARROW_DAMAGE);
		add(49, Enchantment.ARROW_KNOCKBACK);
		add(50, Enchantment.ARROW_FIRE);
		add(51, Enchantment.ARROW_INFINITE);
		add(61, Enchantment.LUCK);
		add(62, Enchantment.LURE);
		add(70, Enchantment.MENDING);
		add(71, Enchantment.VANISHING_CURSE);
	}

	private LegacyEnchantments() {
	}

	/** The enchantment for an old numeric id, or null. */
	public static Enchantment fromId(int id) {
		return BY_ID.get(id);
	}

	/** The old numeric id of an enchantment, or -1 when it has none. */
	public static int toId(Enchantment enchantment) {
		Integer id = BY_ENCHANTMENT.get(enchantment);
		return id == null ? -1 : id;
	}
}
