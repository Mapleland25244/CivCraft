package com.avrgaming.civcraft.compat;

import java.util.Collection;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.main.CivData;

/**
 * Material knowledge that depends on the Minecraft version. Together with ItemManager this is the only
 * place that may use pre-1.13 Material names (SIGN_POST, INK_SACK, SOIL, ...). When the server version
 * changes, only this class has to be re-mapped; callers ask semantic questions ("is this a sign?")
 * instead of comparing against version specific constants.
 */
@SuppressWarnings("deprecation")
public final class LegacyMaterials {

	private LegacyMaterials() {
	}

	/* ---------------------------------------------------------------- semantic checks */

	public static boolean isSign(Material m) {
		return m == Material.SIGN_POST || m == Material.WALL_SIGN;
	}

	/** The material used when placing a wall mounted sign. */
	public static Material wallSign() {
		return Material.WALL_SIGN;
	}

	public static boolean isRedstoneTorch(Material m) {
		return m == Material.REDSTONE_TORCH_OFF || m == Material.REDSTONE_TORCH_ON;
	}

	/** Any piece of gold armor (boots, chestplate, helmet or leggings). */
	public static boolean isGoldArmor(Material m) {
		return m == Material.GOLD_BOOTS || m == Material.GOLD_CHESTPLATE
				|| m == Material.GOLD_HELMET || m == Material.GOLD_LEGGINGS;
	}

	/** Dye item (legacy INK_SACK, whose variants are distinguished by damage). */
	public static boolean isDye(Material m) {
		return m == Material.INK_SACK;
	}

	/** Bone meal (legacy INK_SACK with damage 15). */
	public static boolean isBoneMeal(ItemStack stack) {
		return stack != null && stack.getType() == Material.INK_SACK && stack.getDurability() == 15;
	}

	/** Blocks that players must not trample: farmland and crops. */
	public static boolean isTrampleable(Material m) {
		return m == Material.SOIL || m == Material.CROPS;
	}

	public static boolean isCarrotItem(Material m) {
		return m == Material.CARROT_ITEM;
	}

	public static boolean isEnchantingTable(Material m) {
		return m == Material.ENCHANTMENT_TABLE;
	}

	/** Enchanted (notch) golden apple: legacy golden apple with damage 1. */
	public static boolean isNotchApple(ItemStack stack) {
		return stack != null && stack.getType() == Material.GOLDEN_APPLE && stack.getDurability() == (short) 0x1;
	}

	/** Potion of invisibility: legacy potion whose low four data bits are 0xE. */
	public static boolean isInvisibilityPotion(ItemStack stack) {
		return stack != null && stack.getType() == Material.POTION && (stack.getDurability() & 0x000F) == 0xE;
	}

	/** Mundane or thick potion base, which may not be brewed further. */
	public static boolean isUnbrewablePotionBase(ItemStack stack) {
		return stack.getDurability() == CivData.MUNDANE_POTION_DATA
				|| stack.getDurability() == CivData.MUNDANE_POTION_EXT_DATA
				|| stack.getDurability() == CivData.THICK_POTION_DATA;
	}

	/** Gunpowder (legacy SULPHUR). */
	public static Material gunpowder() {
		return Material.SULPHUR;
	}

	/* ---------------------------------------------------------------- configuration sets */

	/** Blocks that are not restored when a build is undone. */
	public static void fillRestrictedUndoBlocks(Collection<Material> out) {
		out.add(Material.CROPS);
		out.add(Material.CARROT);
		out.add(Material.POTATO);
		out.add(Material.REDSTONE);
		out.add(Material.REDSTONE_WIRE);
		out.add(Material.REDSTONE_TORCH_OFF);
		out.add(Material.REDSTONE_TORCH_ON);
		out.add(Material.DIODE_BLOCK_OFF);
		out.add(Material.DIODE_BLOCK_ON);
		out.add(Material.REDSTONE_COMPARATOR_OFF);
		out.add(Material.REDSTONE_COMPARATOR_ON);
		out.add(Material.REDSTONE_COMPARATOR);
		out.add(Material.STRING);
		out.add(Material.TRIPWIRE);
		out.add(Material.SUGAR_CANE_BLOCK);
		out.add(Material.BEETROOT_SEEDS);
		out.add(Material.LONG_GRASS);
		out.add(Material.RED_ROSE);
		out.add(Material.RED_MUSHROOM);
		out.add(Material.DOUBLE_PLANT);
		out.add(Material.CAKE_BLOCK);
		out.add(Material.CACTUS);
		out.add(Material.PISTON_BASE);
		out.add(Material.PISTON_EXTENSION);
		out.add(Material.PISTON_MOVING_PIECE);
		out.add(Material.PISTON_STICKY_BASE);
		out.add(Material.TRIPWIRE_HOOK);
		out.add(Material.SAPLING);
		out.add(Material.PUMPKIN_STEM);
		out.add(Material.MELON_STEM);
		
	}

	/** Items that players may not use inside other civilizations. */
	public static void fillRestrictedItems(Map<Material, Integer> out) {
		// TODO make this configurable? 
		out.put(Material.FLINT_AND_STEEL, 0);
		out.put(Material.BUCKET, 0);
		out.put(Material.WATER_BUCKET, 0);
		out.put(Material.LAVA_BUCKET, 0);
		out.put(Material.CAKE_BLOCK, 0);
		out.put(Material.CAULDRON, 0);
		out.put(Material.DIODE, 0);
		out.put(Material.INK_SACK, 0);
		out.put(Material.ITEM_FRAME, 0);
		out.put(Material.PAINTING, 0);
		out.put(Material.SHEARS, 0);
		out.put(Material.STATIONARY_LAVA, 0);
		out.put(Material.STATIONARY_WATER, 0);
		out.put(Material.TNT, 0);
	}

	/** Blocks that count as switches (interaction needs permission). */
	public static void fillSwitchItems(Collection<Material> out) {
		//TODO make this configurable?
		out.add(Material.ANVIL);
		out.add(Material.BEACON);
		out.add(Material.BREWING_STAND);
		out.add(Material.BURNING_FURNACE);
		out.add(Material.CAKE_BLOCK);
		out.add(Material.CAULDRON);
		out.add(Material.CHEST);
		out.add(Material.TRAPPED_CHEST);
		out.add(Material.COMMAND);
		out.add(Material.DIODE);
		out.add(Material.DIODE_BLOCK_OFF);
		out.add(Material.DIODE_BLOCK_ON);
		out.add(Material.DISPENSER);
		out.add(Material.FENCE_GATE);
		out.add(Material.FURNACE);
		out.add(Material.JUKEBOX);
		out.add(Material.LEVER);
	//	out.add(Material.LOCKED_CHEST);
		out.add(Material.STONE_BUTTON);
		out.add(Material.STONE_PLATE);
		out.add(Material.IRON_DOOR);
		out.add(Material.TNT);
		out.add(Material.TRAP_DOOR);
		out.add(Material.WOOD_DOOR);
		out.add(Material.WOODEN_DOOR);
		out.add(Material.WOOD_PLATE);
		//switchItems.put(Material.WOOD_BUTTON, 0); //intentionally left out
		
		// 1.5 additions.
		out.add(Material.HOPPER);
		out.add(Material.HOPPER_MINECART);
		out.add(Material.DROPPER);
		out.add(Material.REDSTONE_COMPARATOR);
		out.add(Material.REDSTONE_COMPARATOR_ON);
		out.add(Material.REDSTONE_COMPARATOR_OFF);
		out.add(Material.TRAPPED_CHEST);
		out.add(Material.GOLD_PLATE);
		out.add(Material.IRON_PLATE);
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
	}

	/** Blocks that may be placed regardless of permissions (portal or fire creation). */
	public static void fillBlockPlaceExceptions(Map<Material, Integer> out) {
		/* These blocks can be placed regardless of permissions.
		 * this is currently used only for blocks that are generated
		 * by specific events such as portal or fire creation.
		 */
		out.put(Material.FIRE, 0);
		out.put(Material.PORTAL, 0);
	}
}
