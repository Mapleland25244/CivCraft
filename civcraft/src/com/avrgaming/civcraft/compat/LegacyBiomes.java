package com.avrgaming.civcraft.compat;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.block.Biome;

/**
 * Biome names as CivCraft stored them (1.12 names): culture.yml, the CHUNK_BIOMES cache table and the farm
 * configuration all use them. 1.13 renamed most biomes, so every place that turns a stored name into a Biome,
 * or a Biome into a stored name, goes through this class.
 */
public final class LegacyBiomes {

	private static final Map<String, Biome> BY_LEGACY_NAME = new HashMap<String, Biome>();
	private static final Map<Biome, String> BY_BIOME = new HashMap<Biome, String>();

	private static void add(String legacyName, Biome biome) {
		BY_LEGACY_NAME.put(legacyName, biome);
		BY_BIOME.put(biome, legacyName);
	}

	static {
		add("OCEAN", Biome.OCEAN);
		add("PLAINS", Biome.PLAINS);
		add("DESERT", Biome.DESERT);
		add("EXTREME_HILLS", Biome.MOUNTAINS);
		add("FOREST", Biome.FOREST);
		add("TAIGA", Biome.TAIGA);
		add("SWAMPLAND", Biome.SWAMP);
		add("RIVER", Biome.RIVER);
		add("HELL", Biome.NETHER);
		add("SKY", Biome.THE_END);
		add("FROZEN_OCEAN", Biome.FROZEN_OCEAN);
		add("FROZEN_RIVER", Biome.FROZEN_RIVER);
		add("ICE_FLATS", Biome.SNOWY_TUNDRA);
		add("ICE_MOUNTAINS", Biome.SNOWY_MOUNTAINS);
		add("MUSHROOM_ISLAND", Biome.MUSHROOM_FIELDS);
		add("MUSHROOM_ISLAND_SHORE", Biome.MUSHROOM_FIELD_SHORE);
		add("BEACHES", Biome.BEACH);
		add("DESERT_HILLS", Biome.DESERT_HILLS);
		add("FOREST_HILLS", Biome.WOODED_HILLS);
		add("TAIGA_HILLS", Biome.TAIGA_HILLS);
		add("SMALLER_EXTREME_HILLS", Biome.MOUNTAIN_EDGE);
		add("JUNGLE", Biome.JUNGLE);
		add("JUNGLE_HILLS", Biome.JUNGLE_HILLS);
		add("JUNGLE_EDGE", Biome.JUNGLE_EDGE);
		add("DEEP_OCEAN", Biome.DEEP_OCEAN);
		add("STONE_BEACH", Biome.STONE_SHORE);
		add("COLD_BEACH", Biome.SNOWY_BEACH);
		add("BIRCH_FOREST", Biome.BIRCH_FOREST);
		add("BIRCH_FOREST_HILLS", Biome.BIRCH_FOREST_HILLS);
		add("ROOFED_FOREST", Biome.DARK_FOREST);
		add("TAIGA_COLD", Biome.SNOWY_TAIGA);
		add("TAIGA_COLD_HILLS", Biome.SNOWY_TAIGA_HILLS);
		add("REDWOOD_TAIGA", Biome.GIANT_TREE_TAIGA);
		add("REDWOOD_TAIGA_HILLS", Biome.GIANT_TREE_TAIGA_HILLS);
		add("EXTREME_HILLS_WITH_TREES", Biome.WOODED_MOUNTAINS);
		add("SAVANNA", Biome.SAVANNA);
		add("SAVANNA_ROCK", Biome.SAVANNA_PLATEAU);
		add("MESA", Biome.BADLANDS);
		add("MESA_ROCK", Biome.WOODED_BADLANDS_PLATEAU);
		add("MESA_CLEAR_ROCK", Biome.BADLANDS_PLATEAU);
		add("VOID", Biome.THE_VOID);
		add("MUTATED_PLAINS", Biome.SUNFLOWER_PLAINS);
		add("MUTATED_DESERT", Biome.DESERT_LAKES);
		add("MUTATED_EXTREME_HILLS", Biome.GRAVELLY_MOUNTAINS);
		add("MUTATED_FOREST", Biome.FLOWER_FOREST);
		add("MUTATED_TAIGA", Biome.TAIGA_MOUNTAINS);
		add("MUTATED_SWAMPLAND", Biome.SWAMP_HILLS);
		add("MUTATED_ICE_FLATS", Biome.ICE_SPIKES);
		add("MUTATED_JUNGLE", Biome.MODIFIED_JUNGLE);
		add("MUTATED_JUNGLE_EDGE", Biome.MODIFIED_JUNGLE_EDGE);
		add("MUTATED_BIRCH_FOREST", Biome.TALL_BIRCH_FOREST);
		add("MUTATED_BIRCH_FOREST_HILLS", Biome.TALL_BIRCH_HILLS);
		add("MUTATED_ROOFED_FOREST", Biome.DARK_FOREST_HILLS);
		add("MUTATED_TAIGA_COLD", Biome.SNOWY_TAIGA_MOUNTAINS);
		add("MUTATED_REDWOOD_TAIGA", Biome.GIANT_SPRUCE_TAIGA);
		add("MUTATED_REDWOOD_TAIGA_HILLS", Biome.GIANT_SPRUCE_TAIGA_HILLS);
		add("MUTATED_EXTREME_HILLS_WITH_TREES", Biome.MODIFIED_GRAVELLY_MOUNTAINS);
		add("MUTATED_SAVANNA", Biome.SHATTERED_SAVANNA);
		add("MUTATED_SAVANNA_ROCK", Biome.SHATTERED_SAVANNA_PLATEAU);
		add("MUTATED_MESA", Biome.ERODED_BADLANDS);
		add("MUTATED_MESA_ROCK", Biome.MODIFIED_WOODED_BADLANDS_PLATEAU);
		add("MUTATED_MESA_CLEAR_ROCK", Biome.MODIFIED_BADLANDS_PLATEAU);
	}

	private LegacyBiomes() {
	}

	/** The biome for a stored name (1.12 or current). Throws IllegalArgumentException when neither exists. */
	public static Biome fromName(String name) {
		Biome biome = BY_LEGACY_NAME.get(name);
		return biome != null ? biome : Biome.valueOf(name);
	}

	/** The stored (1.12) name of a biome; biomes that did not exist in 1.12 keep their current name. */
	public static String toName(Biome biome) {
		String name = BY_BIOME.get(biome);
		return name != null ? name : biome.name();
	}

	/** The biome the old Nether biome constant stood for. */
	public static Biome nether() {
		return Biome.NETHER;
	}

	/** Biomes of the beach family (beach, stone shore, snowy beach). */
	public static boolean isBeach(Biome biome) {
		return biome == Biome.BEACH || biome == Biome.STONE_SHORE || biome == Biome.SNOWY_BEACH;
	}
}
