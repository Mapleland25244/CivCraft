package com.avrgaming.civcraft.util;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.SkullMeta;

import com.avrgaming.civcraft.compat.LegacyBridge;
import com.avrgaming.civcraft.compat.LegacyEnchantments;


/*
 * The ItemManager class is the boundary between CivCraft's stored "numeric id : data" pairs (blueprints,
 * materials.yml, the database) and the server's materials. Callers keep passing and receiving the old pairs;
 * this class (through LegacyBridge) converts them. Nothing else may use Bukkit's legacy id API.
 */

public class ItemManager {

	/** Items whose old data is wear (tools, armor) rather than a variant. */
	private static boolean isDamageable(int typeId) {
		Material legacy = LegacyBridge.legacyMaterial(typeId);
		return legacy != null && legacy.getMaxDurability() > 0;
	}

	public static ItemStack createItemStack(int typeId, int amount, short damage) {
		Material material = LegacyBridge.itemMaterial(typeId, damage);
		if (isDamageable(typeId)) {
			return new ItemStack(material, amount, damage);
		}
		return new ItemStack(material, amount);
	}

	public static ItemStack createItemStack(int typeId, int amount) {
		return createItemStack(typeId, amount, (short)0);
	}

	@SuppressWarnings("deprecation")
	public static Enchantment getEnchantById(int id) {
		return LegacyEnchantments.fromId(id);
	}

	public static int getId(Material material) {
		return LegacyBridge.idOf(material);
	}

	@SuppressWarnings("deprecation")
	public static int getId(Enchantment e) {
		return LegacyEnchantments.toId(e);
	}

	public static int getId(ItemStack stack) {
		return LegacyBridge.idOf(stack.getType());
	}

	public static int getId(Block block) {
		return LegacyBridge.idOf(block.getBlockData());
	}

	public static void setTypeId(Block block, int typeId) {
		block.setBlockData(LegacyBridge.blockData(typeId, 0), true);
		BlockConnections.update(block);
	}

	public static void setTypeId(BlockState block, int typeId) {
		block.setBlockData(LegacyBridge.blockData(typeId, 0));
	}

	@SuppressWarnings("deprecation")
	public static byte getData(Block block) {
		BlockData blockData = block.getBlockData();
		return (byte) LegacyBridge.dataOf(blockData, block.getData());
	}

	/** Variant of an item (old data), or its wear for tools and armor. */
	public static short getData(ItemStack stack) {
		if (stack.getType().getMaxDurability() > 0) {
			return stack.getDurability();
		}
		return (short) LegacyBridge.variantOf(stack.getType());
	}

	@SuppressWarnings("deprecation")
	public static byte getData(BlockState state) {
		return (byte) LegacyBridge.dataOf(state.getBlockData(), state.getRawData());
	}

	public static void setData(Block block, int data) {
		setData(block, data, true);
	}

	public static void setData(Block block, int data, boolean update) {
		block.setBlockData(LegacyBridge.blockData(getId(block), data), update);
		BlockConnections.update(block);
	}

	/** Item material for an old id:data. */
	public static Material getItemMaterial(int typeId, int data) {
		return LegacyBridge.itemMaterial(typeId, data);
	}

	/** Recipe ingredient for an old id:data; data -1 means any variant of the old id. */
	public static RecipeChoice getRecipeChoice(int typeId, int data) {
		if (data < 0) {
			java.util.List<Material> variants = LegacyBridge.itemVariants(typeId);
			if (!variants.isEmpty()) {
				return new RecipeChoice.MaterialChoice(variants);
			}
		}
		return new RecipeChoice.MaterialChoice(LegacyBridge.itemMaterial(typeId, data));
	}

	public static Material getMaterial(int material) {
		return LegacyBridge.itemMaterial(material, 0);
	}

	public static int getBlockTypeId(ChunkSnapshot snapshot, int x, int y, int z) {
		return LegacyBridge.idOf(snapshot.getBlockData(x, y, z));
	}

	public static int getBlockData(ChunkSnapshot snapshot, int x, int y, int z) {
		return LegacyBridge.dataOf(snapshot.getBlockData(x, y, z), 0);
	}

	public static void sendBlockChange(Player player, Location loc, int type, int data) {
		player.sendBlockChange(loc, LegacyBridge.blockData(type, data));
	}

	public static int getBlockTypeIdAt(World world, int x, int y, int z) {
		return getId(world.getBlockAt(x, y, z));
	}

	public static int getId(BlockState newState) {
		return LegacyBridge.idOf(newState.getBlockData());
	}

	@SuppressWarnings("deprecation")
	public static short getId(EntityType entity) {
		return entity.getTypeId();
	}

	public static void setTypeIdAndData(Block block, int type, int data, boolean update) {
		block.setBlockData(LegacyBridge.blockData(type, data), update);
		BlockConnections.update(block);
	}

	@SuppressWarnings("deprecation")
	public static ItemStack spawnPlayerHead(String playerName, String itemDisplayName) {
		ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
		SkullMeta meta = (SkullMeta) skull.getItemMeta();
		meta.setOwner(playerName);
		meta.setDisplayName(itemDisplayName);
		skull.setItemMeta(meta);
		return skull;
	}

	/**
	 * Facing of a placed dispenser.
	 */
	public static BlockFace getDispenserFacing(BlockState dispenser) {
		return ((Directional) dispenser.getBlockData()).getFacing();
	}

	/**
	 * Set the facing of a sign state; the caller still has to call update().
	 */
	public static void setSignFacing(BlockState sign, BlockFace face) {
		BlockData data = sign.getBlockData();
		if (data instanceof Rotatable) {
			((Rotatable) data).setRotation(face);
		} else if (data instanceof Directional) {
			((Directional) data).setFacing(face);
		}
		sign.setBlockData(data);
	}

	/**
	 * Write the state's own data back to it (the caller still has to call update()).
	 */
	public static void reapplyData(BlockState state) {
		state.setBlockData(state.getBlockData());
	}

	/**
	 * Wear of a tool or armor piece (0 = new).
	 */
	public static short getDamage(ItemStack stack) {
		return stack.getDurability();
	}

	public static void setDamage(ItemStack stack, short damage) {
		stack.setDurability(damage);
	}

	/**
	 * Copy of a stack with another amount, keeping its type and wear. Item meta is not copied.
	 */
	public static ItemStack copyWithAmount(ItemStack from, int amount) {
		return new ItemStack(from.getType(), amount, from.getDurability());
	}

	public static boolean removeItemFromPlayer(Player player, Material mat, int amount) {
		ItemStack m = new ItemStack(mat, amount);
		if (player.getInventory().contains(mat)) {
			player.getInventory().removeItem(m);
			return true;
		}
		return false;
	}

}
