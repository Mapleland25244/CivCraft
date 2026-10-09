package com.avrgaming.civcraft.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.material.MaterialData;

/**
 * Translates CivCraft's stored "numeric id : data" pairs (blueprints, materials.yml, the database) to 1.13+
 * materials and back, using Bukkit's own legacy tables. This is the U1a stage; U1b replaces it with an explicit
 * table that is checked against this one.
 *
 * Only {@link com.avrgaming.civcraft.util.ItemManager} should call this.
 */
@SuppressWarnings("deprecation")
public final class LegacyBridge {

	private static final int MAX_ID = 4096;

	/** The old spawn egg item; its data is the numeric entity type id. */
	private static final int SPAWN_EGG_ID = 383;

	/** id -> LEGACY_* material, filled once. */
	private static final Material[] LEGACY_BY_ID = new Material[MAX_ID];

	private static final Map<Integer, Material> ITEM_CACHE = new ConcurrentHashMap<Integer, Material>();
	private static final Map<Integer, Material> BLOCK_CACHE = new ConcurrentHashMap<Integer, Material>();
	private static final Map<Integer, BlockData> BLOCK_DATA_CACHE = new ConcurrentHashMap<Integer, BlockData>();

	/** block state -> (id << 8 | data). Built on first use. */
	private static volatile Map<BlockData, Integer> reverseBlockData;

	/** What the static initializer found, for the startup log. */
	private static final String INIT_REPORT;

	static {
		int fromValues = 0;
		int total = 0;
		for (Material m : Material.values()) {
			total++;
			if (m.isLegacy() && m.getId() >= 0 && m.getId() < MAX_ID) {
				LEGACY_BY_ID[m.getId()] = m;
				fromValues++;
			}
		}
		// values() has been seen to omit the LEGACY_ constants on a running server, so also read them as fields
		int fromFields = 0;
		for (java.lang.reflect.Field field : Material.class.getDeclaredFields()) {
			if (field.getType() == Material.class && field.getName().startsWith("LEGACY_")
					&& java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				try {
					Material m = (Material) field.get(null);
					if (m != null && m.getId() >= 0 && m.getId() < MAX_ID && LEGACY_BY_ID[m.getId()] == null) {
						LEGACY_BY_ID[m.getId()] = m;
						fromFields++;
					}
				} catch (IllegalAccessException e) {
					// not readable: skip
				}
			}
		}
		INIT_REPORT = "Material.values()=" + total + ", legacy via values()=" + fromValues + ", added via fields=" + fromFields;
	}

	private LegacyBridge() {
	}

	private static int key(int id, int data) {
		return (id << 8) | (data & 0xFF);
	}

	/** The LEGACY_* material for an old numeric id, or null when the id does not exist. */
	public static Material legacyMaterial(int id) {
		if (id < 0 || id >= MAX_ID) {
			return null;
		}
		return LEGACY_BY_ID[id];
	}

	/** True for old items that carry wear in their damage value instead of a variant. */
	private static boolean isDamageable(Material legacy) {
		return legacy.getMaxDurability() > 0;
	}

	/** The block material for id:data (AIR for an unknown id). */
	public static Material blockMaterial(int id, int data) {
		return blockData(id, data).getMaterial();
	}

	/** The item material for id:data. For tools and armor the data is wear, not a variant, so it is ignored. */
	public static Material itemMaterial(int id, int data) {
		if (data < 0) {
			data = 0; // -1 meant "any data" in recipes; callers that need every variant use itemVariants
		}
		Material legacy = legacyMaterial(id);
		if (legacy == null) {
			if (id != 0) {
				Bukkit.getLogger().warning("[CivCraft] legacy item id " + id + " is unknown (no LEGACY_ material with that id)");
			}
			return Material.AIR;
		}
		if (id == SPAWN_EGG_ID) {
			return spawnEgg(data);
		}
		int variant = isDamageable(legacy) ? 0 : data;
		Integer k = key(id, variant);
		Material m = ITEM_CACHE.get(k);
		if (m == null) {
			MaterialData legacyData = new MaterialData(legacy, (byte) (variant & 0xF));
			m = Bukkit.getUnsafe().fromLegacy(legacyData, true);
			if (m == Material.AIR && id != 0) {
				m = blockOnlyItem(id);
			}
			if (m == Material.AIR && id != 0) {
				// the item table has no entry: try the block table, then the block state, and say so in the log
				Material asBlock = Bukkit.getUnsafe().fromLegacy(legacyData, false);
				Material asState = fromLegacyOrAir(legacy, variant & 0xF).getMaterial();
				if (asBlock != Material.AIR) {
					m = asBlock;
				} else if (asState != null && asState != Material.AIR) {
					m = asState; // e.g. 181:1-7, invalid data bits of the double red sandstone slab
				} else {
					Bukkit.getLogger().warning("[CivCraft] legacy item " + id + ":" + variant + " (" + legacy + ") has no item mapping; block table="
							+ asBlock + ", block state=" + asState);
				}
			}
			ITEM_CACHE.put(k, m);
		}
		return m;
	}

	/**
	 * Old ids that were only blocks (placed state of an item): the server's table has no item for them, so name the
	 * item they came from. AIR when the id has no such alias.
	 */
	private static Material blockOnlyItem(int id) {
		switch (id) {
		case 62:
			return Material.FURNACE; // burning furnace
		case 63:
		case 68:
			return Material.SIGN; // standing and wall sign (1.13 has one sign item)
		case 74:
			return Material.REDSTONE_ORE; // lit redstone ore
		case 75:
		case 76:
			return Material.REDSTONE_TORCH; // redstone torch off / on
		case 93:
		case 94:
			return Material.REPEATER;
		case 123:
		case 124:
			return Material.REDSTONE_LAMP;
		case 177:
			return Material.WHITE_BANNER; // wall banner
		default:
			return Material.AIR;
		}
	}

	/** Spawn egg for an old entity type id (the old data of item 383). AIR when the entity has no egg. */
	private static Material spawnEgg(int entityId) {
		org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.fromId(entityId);
		if (type == null) {
			return Material.AIR;
		}
		String name = type.name();
		if (name.equals("PIG_ZOMBIE")) {
			name = "ZOMBIE_PIGMAN";
		} else if (name.equals("MUSHROOM_COW")) {
			name = "MOOSHROOM";
		}
		Material egg = Material.getMaterial(name + "_SPAWN_EGG");
		return egg == null ? Material.AIR : egg;
	}

	/** Every distinct item material the old id had across its data values (what "data -1" in a recipe stood for). */
	public static java.util.List<Material> itemVariants(int id) {
		java.util.List<Material> result = new java.util.ArrayList<Material>();
		Material legacy = legacyMaterial(id);
		if (legacy == null) {
			return result;
		}
		int variants = isDamageable(legacy) || id == SPAWN_EGG_ID ? 1 : 16;
		for (int d = 0; d < variants; d++) {
			Material m = itemMaterial(id, d);
			if (m != Material.AIR && !result.contains(m)) {
				result.add(m);
			}
		}
		return result;
	}

	/** The full block state for id:data. Data bits the old format never used are dropped, so the result is never AIR for a real id. */
	public static BlockData blockData(int id, int data) {
		Material legacy = legacyMaterial(id);
		if (legacy == null) {
			return Material.AIR.createBlockData();
		}
		Integer k = key(id, data);
		BlockData d = BLOCK_DATA_CACHE.get(k);
		if (d == null) {
			d = convertBlock(id, legacy, data & 0xF);
			if (d instanceof org.bukkit.block.data.type.Leaves) {
				// blueprint leaves have no logs next to them: keep them from decaying (1.12 data bit 0x4, not present in the .def files)
				d = d.clone();
				((org.bukkit.block.data.type.Leaves) d).setPersistent(true);
			}
			BLOCK_DATA_CACHE.put(k, d);
		}
		return d;
	}

	private static BlockData fromLegacyOrAir(Material legacy, int data) {
		BlockData d;
		try {
			d = Bukkit.getUnsafe().fromLegacy(legacy, (byte) data);
		} catch (NullPointerException e) {
			// the server's legacy table throws for ids that are items only (stick, bucket...): no block state exists
			d = null;
		}
		return d != null ? d : Material.AIR.createBlockData();
	}

	private static BlockData convertBlock(int id, Material legacy, int data) {
		BlockData d = convertBlockExact(id, legacy, data);
		if (d.getMaterial() == Material.AIR && id != 0) {
			// No valid state for this data at all. setTypeId(id) used to place the block and leave its data at 0, which is
			// not a valid state for facing blocks such as wall signs (2-5 only); take the first valid state so the block
			// is still placed and a following setData can give it the real facing.
			for (int any = 0; any < 16 && d.getMaterial() == Material.AIR; any++) {
				d = fromLegacyOrAir(legacy, any);
			}
		}
		return d;
	}

	/** Like convertBlock but without the last-resort "any valid state", so each old data keeps its own state (reverse table). */
	private static BlockData convertBlockExact(int id, Material legacy, int data) {
		if (id == 144) {
			return skull(data);
		}
		BlockData d = fromLegacyOrAir(legacy, data);
		if (d.getMaterial() == Material.AIR && id != 0) {
			// Junk data bits in old blueprints (for example a slab with variant bits): keep only the top/bottom flag, then none.
			d = fromLegacyOrAir(legacy, data & 0x8);
			if (d.getMaterial() == Material.AIR) {
				d = fromLegacyOrAir(legacy, 0);
			}
		}
		return d;
	}

	/**
	 * Old skull block (id 144): the data is only the placement (1 = on the floor, 2-5 = on a wall); the skull type was
	 * stored in the tile entity. Blueprints carry no tile entity, so a skeleton skull is placed.
	 */
	private static BlockData skull(int data) {
		switch (data) {
		case 2:
			return wallSkull(org.bukkit.block.BlockFace.NORTH);
		case 3:
			return wallSkull(org.bukkit.block.BlockFace.SOUTH);
		case 4:
			return wallSkull(org.bukkit.block.BlockFace.WEST);
		case 5:
			return wallSkull(org.bukkit.block.BlockFace.EAST);
		default:
			return Material.SKELETON_SKULL.createBlockData();
		}
	}

	private static BlockData wallSkull(org.bukkit.block.BlockFace face) {
		BlockData d = Material.SKELETON_WALL_SKULL.createBlockData();
		((org.bukkit.block.data.Directional) d).setFacing(face);
		return d;
	}

	/* ---------------------------------------------------------------- reverse */

	private static Map<BlockData, Integer> reverseBlockData() {
		Map<BlockData, Integer> map = reverseBlockData;
		if (map == null) {
			synchronized (LegacyBridge.class) {
				map = reverseBlockData;
				if (map == null) {
					map = new HashMap<BlockData, Integer>();
					for (int id = 0; id < MAX_ID; id++) {
						Material legacy = LEGACY_BY_ID[id];
						if (legacy == null || !legacy.isBlock()) {
							continue;
						}
						for (int data = 0; data < 16; data++) {
							BlockData d = convertBlockExact(id, legacy, data);
							Integer existing = map.get(d);
							if (existing == null || (existing >> 8 != canonicalId(d.getMaterial()) && id == canonicalId(d.getMaterial()))) {
								map.put(d, key(id, data));
							}
						}
					}
					reverseBlockData = map;
				}
			}
		}
		return map;
	}

	private static final Map<Material, Integer> MATERIAL_KEY_CACHE = new ConcurrentHashMap<Material, Integer>();

	/** (id << 8 | variant) of a material, taken from Bukkit's toLegacy and confirmed against the forward tables. */
	private static int materialKey(Material material) {
		Integer cached = MATERIAL_KEY_CACHE.get(material);
		if (cached != null) {
			return cached;
		}
		int result = 0;
		if (material.name().endsWith("_SPAWN_EGG")) {
			org.bukkit.entity.EntityType egg = eggEntity(material);
			MATERIAL_KEY_CACHE.put(material, key(SPAWN_EGG_ID, egg == null ? 0 : egg.getTypeId()));
			return MATERIAL_KEY_CACHE.get(material);
		}
		Material legacy = material.isLegacy() ? material : Bukkit.getUnsafe().toLegacy(material);
		if (legacy != null && legacy != Material.LEGACY_AIR) {
			int id = legacy.getId();
			int variants = isDamageable(legacy) ? 1 : 16;
			int variant = 0;
			for (int d = 0; d < variants; d++) {
				if (itemMaterial(id, d) == material || blockMaterial(id, d) == material) {
					variant = d;
					break;
				}
			}
			result = key(id, variant);
		}
		MATERIAL_KEY_CACHE.put(material, result);
		return result;
	}

	/** The entity a spawn egg material spawns (inverse of spawnEgg), or null. */
	private static org.bukkit.entity.EntityType eggEntity(Material egg) {
		String name = egg.name().substring(0, egg.name().length() - "_SPAWN_EGG".length());
		if (name.equals("ZOMBIE_PIGMAN")) {
			name = "PIG_ZOMBIE";
		} else if (name.equals("MOOSHROOM")) {
			name = "MUSHROOM_COW";
		}
		try {
			return org.bukkit.entity.EntityType.valueOf(name);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/** Old numeric id of a material; 0 when the material has no old equivalent. */
	public static int idOf(Material material) {
		return materialKey(material) >> 8;
	}

	/** Old variant data of a material (0 for materials without variants). */
	public static int variantOf(Material material) {
		return materialKey(material) & 0xFF;
	}

	/** The id Bukkit's own legacy table gives for a modern material (0 when none). */
	private static int canonicalId(Material material) {
		Material legacy = Bukkit.getUnsafe().toLegacy(material);
		return legacy == null || legacy == Material.LEGACY_AIR ? 0 : legacy.getId();
	}

	/**
	 * Liquids: 1.12 had flowing (8, 10) and stationary (9, 11) ids. A source block is stationary, anything with a
	 * level is flowing.
	 */
	private static int liquidKey(BlockData data) {
		if (data instanceof org.bukkit.block.data.Levelled) {
			int level = ((org.bukkit.block.data.Levelled) data).getLevel();
			if (data.getMaterial() == Material.WATER) {
				return key(level == 0 ? 9 : 8, level);
			}
			if (data.getMaterial() == Material.LAVA) {
				return key(level == 0 ? 11 : 10, level);
			}
		}
		return -1;
	}

	/** Old id of a block state. */
	public static int idOf(BlockData data) {
		int liquid = liquidKey(data);
		if (liquid >= 0) {
			return liquid >> 8;
		}
		Integer k = reverseBlockData().get(data);
		return k != null ? k >> 8 : idOf(data.getMaterial());
	}

	/** Old data of a block state; the fallback when the state has no exact old equivalent (fences, stairs, doors...). */
	public static int dataOf(BlockData data, int fallback) {
		int liquid = liquidKey(data);
		if (liquid >= 0) {
			return liquid & 0xFF;
		}
		Integer k = reverseBlockData().get(data);
		return k == null ? fallback : k & 0xFF;
	}

	/** Lines for the startup log: how many old ids are known and what a few common ones convert to. */
	public static java.util.List<String> selfTest() {
		java.util.List<String> lines = new java.util.ArrayList<String>();
		int known = 0;
		for (Material m : LEGACY_BY_ID) {
			if (m != null) {
				known++;
			}
		}
		lines.add("LegacyBridge: " + known + " old ids known (" + INIT_REPORT + ")");
		int[] ids = { 1, 4, 5, 12, 17, 53, 280, 287, 334, 353, 372 };
		for (int id : ids) {
			Material legacy = legacyMaterial(id);
			lines.add("LegacyBridge: " + id + " -> " + legacy + " item=" + itemMaterial(id, 0) + " block=" + blockMaterial(id, 0));
		}
		return lines;
	}
}
