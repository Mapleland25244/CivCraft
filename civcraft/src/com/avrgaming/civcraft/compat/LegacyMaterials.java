package com.avrgaming.civcraft.compat;

import java.util.Collection;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

/**
 * Material knowledge that depends on the Minecraft version. Together with ItemManager this is the only
 * place that may name version specific materials. When the server version changes, only this class has to
 * be re-mapped; callers ask semantic questions ("is this a sign?") instead of comparing against version
 * specific constants.
 *
 * This is the 1.13 mapping: items and blocks are no longer split by damage/data, so every old "variant"
 * question becomes a question about a distinct material or about the potion meta.
 */
public final class LegacyMaterials {

	private LegacyMaterials() {
	}

	/* ---------------------------------------------------------------- semantic checks */

	public static boolean isSign(Material m) {
		return m == Material.SIGN || m == Material.WALL_SIGN;
	}

	/** The material used when placing a wall mounted sign. */
	public static Material wallSign() {
		return Material.WALL_SIGN;
	}

	public static boolean isRedstoneTorch(Material m) {
		return m == Material.REDSTONE_TORCH || m == Material.REDSTONE_WALL_TORCH;
	}

	/** Any piece of gold armor (boots, chestplate, helmet or leggings). */
	public static boolean isGoldArmor(Material m) {
		return m == Material.GOLDEN_BOOTS || m == Material.GOLDEN_CHESTPLATE
				|| m == Material.GOLDEN_HELMET || m == Material.GOLDEN_LEGGINGS;
	}

	/** Dye item (the old ink sack, whose sixteen variants are now separate materials). */
	public static boolean isDye(Material m) {
		return LegacyBridge.idOf(m) == 351;
	}

	/** Bone meal. */
	public static boolean isBoneMeal(ItemStack stack) {
		return stack != null && stack.getType() == Material.BONE_MEAL;
	}

	/** Blocks that players must not trample: farmland and crops. */
	public static boolean isTrampleable(Material m) {
		return m == Material.FARMLAND || m == Material.WHEAT;
	}

	public static boolean isCarrotItem(Material m) {
		return m == Material.CARROT;
	}

	public static boolean isEnchantingTable(Material m) {
		return m == Material.ENCHANTING_TABLE;
	}

	/** Enchanted (notch) golden apple. */
	public static boolean isNotchApple(ItemStack stack) {
		return stack != null && stack.getType() == Material.ENCHANTED_GOLDEN_APPLE;
	}

	private static PotionType basePotionType(ItemStack stack) {
		if (stack == null || (stack.getType() != Material.POTION && stack.getType() != Material.SPLASH_POTION
				&& stack.getType() != Material.LINGERING_POTION)) {
			return null;
		}
		ItemMeta meta = stack.getItemMeta();
		if (!(meta instanceof PotionMeta)) {
			return null;
		}
		return ((PotionMeta) meta).getBasePotionData().getType();
	}

	/** Potion of invisibility (normal or extended). */
	public static boolean isInvisibilityPotion(ItemStack stack) {
		return basePotionType(stack) == PotionType.INVISIBILITY;
	}

	/** Mundane or thick potion base, which may not be brewed further. */
	public static boolean isUnbrewablePotionBase(ItemStack stack) {
		PotionType type = basePotionType(stack);
		return type == PotionType.MUNDANE || type == PotionType.THICK;
	}

	/** Gunpowder. */
	public static Material gunpowder() {
		return Material.GUNPOWDER;
	}

	/* ---------------------------------------------------------------- configuration sets */

	/** Blocks that are not restored when a build is undone. */
	public static void fillRestrictedUndoBlocks(Collection<Material> out) {
		out.add(Material.WHEAT);
		out.add(Material.CARROTS);
		out.add(Material.POTATOES);
		out.add(Material.REDSTONE);
		out.add(Material.REDSTONE_WIRE);
		out.add(Material.REDSTONE_TORCH);
		out.add(Material.REDSTONE_WALL_TORCH);
		out.add(Material.REPEATER);
		out.add(Material.COMPARATOR);
		out.add(Material.STRING);
		out.add(Material.TRIPWIRE);
		out.add(Material.SUGAR_CANE);
		out.add(Material.BEETROOT_SEEDS);
		out.add(Material.GRASS);
		out.add(Material.TALL_GRASS);
		out.add(Material.FERN);
		out.add(Material.LARGE_FERN);
		out.add(Material.DANDELION);
		out.add(Material.POPPY);
		out.add(Material.BLUE_ORCHID);
		out.add(Material.ALLIUM);
		out.add(Material.AZURE_BLUET);
		out.add(Material.RED_TULIP);
		out.add(Material.ORANGE_TULIP);
		out.add(Material.WHITE_TULIP);
		out.add(Material.PINK_TULIP);
		out.add(Material.OXEYE_DAISY);
		out.add(Material.SUNFLOWER);
		out.add(Material.LILAC);
		out.add(Material.ROSE_BUSH);
		out.add(Material.PEONY);
		out.add(Material.RED_MUSHROOM);
		out.add(Material.CAKE);
		out.add(Material.CACTUS);
		out.add(Material.PISTON);
		out.add(Material.PISTON_HEAD);
		out.add(Material.MOVING_PISTON);
		out.add(Material.STICKY_PISTON);
		out.add(Material.TRIPWIRE_HOOK);
		out.add(Material.OAK_SAPLING);
		out.add(Material.SPRUCE_SAPLING);
		out.add(Material.BIRCH_SAPLING);
		out.add(Material.JUNGLE_SAPLING);
		out.add(Material.ACACIA_SAPLING);
		out.add(Material.DARK_OAK_SAPLING);
		out.add(Material.PUMPKIN_STEM);
		out.add(Material.MELON_STEM);
		out.add(Material.ATTACHED_PUMPKIN_STEM);
		out.add(Material.ATTACHED_MELON_STEM);
	}

	/** Items that players may not use inside other civilizations. */
	public static void fillRestrictedItems(Map<Material, Integer> out) {
		// TODO make this configurable?
		out.put(Material.FLINT_AND_STEEL, 0);
		out.put(Material.BUCKET, 0);
		out.put(Material.WATER_BUCKET, 0);
		out.put(Material.LAVA_BUCKET, 0);
		out.put(Material.CAKE, 0);
		out.put(Material.CAULDRON, 0);
		out.put(Material.REPEATER, 0);
		for (Material m : Material.values()) {
			if (!m.isLegacy() && isDye(m)) {
				out.put(m, 0);
			}
		}
		out.put(Material.ITEM_FRAME, 0);
		out.put(Material.PAINTING, 0);
		out.put(Material.SHEARS, 0);
		out.put(Material.TNT, 0);
	}

	/** Blocks that count as switches (interaction needs permission). */
	public static void fillSwitchItems(Collection<Material> out) {
		//TODO make this configurable?
		out.add(Material.ANVIL);
		out.add(Material.CHIPPED_ANVIL);
		out.add(Material.DAMAGED_ANVIL);
		out.add(Material.BEACON);
		out.add(Material.BREWING_STAND);
		out.add(Material.FURNACE);
		out.add(Material.CAKE);
		out.add(Material.CAULDRON);
		out.add(Material.CHEST);
		out.add(Material.TRAPPED_CHEST);
		out.add(Material.COMMAND_BLOCK);
		out.add(Material.REPEATER);
		out.add(Material.DISPENSER);
		out.add(Material.OAK_FENCE_GATE);
		out.add(Material.JUKEBOX);
		out.add(Material.LEVER);
		out.add(Material.STONE_BUTTON);
		out.add(Material.STONE_PRESSURE_PLATE);
		out.add(Material.IRON_DOOR);
		out.add(Material.TNT);
		out.add(Material.OAK_TRAPDOOR);
		out.add(Material.OAK_DOOR);
		out.add(Material.OAK_PRESSURE_PLATE);
		//switchItems.put(Material.WOOD_BUTTON, 0); //intentionally left out

		// 1.5 additions.
		out.add(Material.HOPPER);
		out.add(Material.HOPPER_MINECART);
		out.add(Material.DROPPER);
		out.add(Material.COMPARATOR);
		out.add(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
		out.add(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
		out.add(Material.IRON_TRAPDOOR);

		// 1.6 additions.
		out.add(Material.SPRUCE_DOOR);
		out.add(Material.BIRCH_DOOR);
		out.add(Material.JUNGLE_DOOR);
		out.add(Material.ACACIA_DOOR);
		out.add(Material.DARK_OAK_DOOR);

		// 1.7 additions
		out.add(Material.ACACIA_FENCE_GATE);
		out.add(Material.BIRCH_FENCE_GATE);
		out.add(Material.DARK_OAK_FENCE_GATE);
		out.add(Material.SPRUCE_FENCE_GATE);
		out.add(Material.JUNGLE_FENCE_GATE);

		// 1.13: the old wooden trapdoor, plate and door ids are split by wood type.
		out.add(Material.SPRUCE_TRAPDOOR);
		out.add(Material.BIRCH_TRAPDOOR);
		out.add(Material.JUNGLE_TRAPDOOR);
		out.add(Material.ACACIA_TRAPDOOR);
		out.add(Material.DARK_OAK_TRAPDOOR);
		out.add(Material.SPRUCE_PRESSURE_PLATE);
		out.add(Material.BIRCH_PRESSURE_PLATE);
		out.add(Material.JUNGLE_PRESSURE_PLATE);
		out.add(Material.ACACIA_PRESSURE_PLATE);
		out.add(Material.DARK_OAK_PRESSURE_PLATE);
	}

	/** Blocks that may be placed regardless of permissions (portal or fire creation). */
	public static void fillBlockPlaceExceptions(Map<Material, Integer> out) {
		/* These blocks can be placed regardless of permissions.
		 * this is currently used only for blocks that are generated
		 * by specific events such as portal or fire creation.
		 */
		out.put(Material.FIRE, 0);
		out.put(Material.NETHER_PORTAL, 0);
	}

	/** Blocks and items that break instantly (the old willInstantBreak list, expanded to the 1.13 variants). */
	public static boolean willInstantBreak(Material type) {
		String name = type.name();
		if (name.endsWith("_BED") || name.endsWith("_SAPLING") || name.endsWith("_HEAD") || name.endsWith("_SKULL")
				|| name.endsWith("_WALL_HEAD") || name.endsWith("_WALL_SKULL")) {
			return true;
		}
		switch (type) {
		case BROWN_MUSHROOM:
		case WHEAT:
		case DEAD_BUSH:
		case REPEATER:
		case FIRE:
		case FLOWER_POT:
		case GLASS:
		case GRASS_BLOCK:
		case OAK_LEAVES:
		case SPRUCE_LEAVES:
		case BIRCH_LEAVES:
		case JUNGLE_LEAVES:
		case LEVER:
		case GRASS:
		case FERN:
		case MELON_STEM:
		case NETHER_WART:
		case PUMPKIN_STEM:
		case REDSTONE:
		case REDSTONE_TORCH:
		case REDSTONE_WALL_TORCH:
		case REDSTONE_WIRE:
		case SNOW:
		case SUGAR_CANE:
		case GLASS_PANE:
		case TNT:
		case TORCH:
		case WALL_TORCH:
		case TRIPWIRE:
		case TRIPWIRE_HOOK:
		case VINE:
		case LILY_PAD:
		case DANDELION:
			return true;
		default:
			return false;
		}
	}
}
