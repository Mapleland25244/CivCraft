package com.avrgaming.civcraft.compat;

/**
 * Old (1.12) numeric ids of materials whose names no longer exist. They are CivCraft's stored "domain currency":
 * blueprints, materials.yml and the database all use them, so code that needs one of these asks for it by its old
 * name here instead of through a Bukkit constant. Values are Material#getId() of the 1.12.2 constant.
 */
public final class LegacyIds {

	private LegacyIds() {
	}

	public static final int BED = 355;
	public static final int BED_BLOCK = 26;
	public static final int BOOK_AND_QUILL = 386;
	public static final int FENCE = 85;
	public static final int FIREWORK = 401;
	public static final int GOLD_HELMET = 314;
	public static final int GOLD_PLATE = 147;
	public static final int IRON_PLATE = 148;
	public static final int IRON_SPADE = 256;
	public static final int LONG_GRASS = 31;
	public static final int MONSTER_EGG = 383;
	public static final int NETHER_BRICK_ITEM = 405;
	public static final int RAILS = 66;
	public static final int RAW_FISH = 349;
	public static final int REDSTONE_COMPARATOR_OFF = 149;
	public static final int REDSTONE_COMPARATOR_ON = 150;
	public static final int REDSTONE_TORCH_OFF = 75;
	public static final int REDSTONE_TORCH_ON = 76;
	public static final int RED_ROSE = 38;
	public static final int SAPLING = 6;
	public static final int SEEDS = 295;
	public static final int SIGN_POST = 63;
	public static final int STONE_PLATE = 70;
	public static final int TRAP_DOOR = 96;
	public static final int WOODEN_DOOR = 64;
	public static final int WOOD_BUTTON = 143;
	public static final int WOOD_PLATE = 72;
	public static final int WOOD_STEP = 126;
	public static final int WORKBENCH = 58;
	public static final int YELLOW_FLOWER = 37;

	/*
	 * Constants whose 1.13 namesake means something else (or is split in two): the 1.12 Material value is kept
	 * so that code written against it behaves the same.
	 */
	/** 1.12 CARROT is the block (141); the item was CARROT_ITEM. */
	public static final int CARROT = 141;
	public static final int POTATO = 142;
	public static final int FLOWER_POT = 140;
	/** 1.12 WHEAT is the item (296); the block was CROPS. */
	public static final int WHEAT = 296;
	/** 1.12 SUGAR_CANE is the item (338); the block was SUGAR_CANE_BLOCK. */
	public static final int SUGAR_CANE = 338;
	public static final int DEAD_BUSH = 32;
	/** 1.12 MAP is the filled map (358); 1.13 MAP is the empty map. */
	public static final int MAP = 358;
}
