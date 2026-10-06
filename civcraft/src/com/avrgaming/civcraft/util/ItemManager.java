package com.avrgaming.civcraft.util;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;


/*
 * The ItemManager class is going to be used to wrap itemstack operations that have now
 * been deprecated by Bukkit. If bukkit ever actually takes these methods away from us,
 * we'll just have to use NMS or be a little creative. Doing it on spot (here) will be 
 * better than having fragile code scattered everywhere. 
 * 
 * Additionally it gives us an opportunity to unit test certain item operations that we
 * want to use with our new custom item stacks.
 */

public class ItemManager {

	@SuppressWarnings("deprecation")
	public static ItemStack createItemStack(int typeId, int amount, short damage) {
		return new ItemStack(typeId, amount, damage);
	}

	public static ItemStack createItemStack(int typeId, int amount) {
		return createItemStack(typeId, amount, (short)0);
	}

	@SuppressWarnings("deprecation")
	public static MaterialData getMaterialData(int type_id, int data) {
		return new MaterialData(type_id, (byte)data);
	}
	
	@SuppressWarnings("deprecation")
	public static Enchantment getEnchantById(int id) {
		return Enchantment.getById(id);
	}
	
	@SuppressWarnings("deprecation")
	public static int getId(Material material) {
		return material.getId();
	}
	
	@SuppressWarnings("deprecation")
	public static int getId(Enchantment e) {
		return e.getId();
	}
	
	@SuppressWarnings("deprecation")
	public static int getId(ItemStack stack) {
		return stack.getTypeId();
	}
	
	@SuppressWarnings("deprecation")
	public static int getId(Block block) {
		return block.getTypeId();
	}
	
	@SuppressWarnings("deprecation")
	public static void setTypeId(Block block, int typeId) {
		block.setTypeId(typeId);
	}
	
	@SuppressWarnings("deprecation")
	public static void setTypeId(BlockState block, int typeId) {
		block.setTypeId(typeId);
	}
	
	@SuppressWarnings("deprecation")
	public static byte getData(Block block) {
		return block.getData();
	}
	
	public static short getData(ItemStack stack) {
		return stack.getDurability();
	}
	
	@SuppressWarnings("deprecation")
	public static byte getData(MaterialData data) {
		return data.getData();
	}

	@SuppressWarnings("deprecation")
	public static byte getData(BlockState state) {
		return state.getRawData();
	}
	
	@SuppressWarnings("deprecation")
	public static void setData(Block block, int data) {
		block.setData((byte)data);
	}

	@SuppressWarnings("deprecation")
	public static void setData(Block block, int data, boolean update) {
		block.setData((byte) data, update);
	}
	
	@SuppressWarnings("deprecation")
	public static Material getMaterial(int material) {
		return Material.getMaterial(material);
	}
	
	@SuppressWarnings("deprecation")
	public static int getBlockTypeId(ChunkSnapshot snapshot, int x, int y, int z) {
		return snapshot.getBlockTypeId(x, y, z);
	}
	
	@SuppressWarnings("deprecation")
	public static int getBlockData(ChunkSnapshot snapshot, int x, int y, int z) {
		return snapshot.getBlockData(x, y, z);
	}
	
	@SuppressWarnings("deprecation")
	public static void sendBlockChange(Player player, Location loc, int type, int data) {
		player.sendBlockChange(loc, type, (byte)data);
	}
	
	@SuppressWarnings("deprecation")
	public static int getBlockTypeIdAt(World world, int x, int y, int z) {
		return world.getBlockTypeIdAt(x, y, z);
	}

	@SuppressWarnings("deprecation")
	public static int getId(BlockState newState) {
		return newState.getTypeId();
	}

	@SuppressWarnings("deprecation")
	public static short getId(EntityType entity) {
		return entity.getTypeId();
	}

	@SuppressWarnings("deprecation")
	public static void setData(MaterialData data, byte chestData) {
		data.setData(chestData);
	}

	@SuppressWarnings("deprecation")
	public static void setTypeIdAndData(Block block, int type, int data, boolean update) {
		block.setTypeIdAndData(type, (byte)data, update);
	}
	
	@SuppressWarnings("deprecation")
	public static ItemStack spawnPlayerHead(String playerName, String itemDisplayName) {		
		ItemStack skull = ItemManager.createItemStack(ItemManager.getId(Material.SKULL_ITEM), 1, (short)3);
		SkullMeta meta = (SkullMeta) skull.getItemMeta();
		meta.setOwner(playerName);
		meta.setDisplayName(itemDisplayName);
		skull.setItemMeta(meta);
		return skull;
	}

	/**
	 * Facing of a placed dispenser. Legacy: stored in the block data (org.bukkit.material.Dispenser).
	 */
	@SuppressWarnings("deprecation")
	public static BlockFace getDispenserFacing(BlockState dispenser) {
		return ((org.bukkit.material.Dispenser) dispenser.getData()).getFacing();
	}

	/**
	 * Set the facing of a sign state; the caller still has to call update().
	 */
	@SuppressWarnings("deprecation")
	public static void setSignFacing(BlockState sign, BlockFace face) {
		((org.bukkit.material.Sign) sign.getData()).setFacingDirection(face);
	}

	/**
	 * Write the state's own data back to it (the caller still has to call update()).
	 */
	@SuppressWarnings("deprecation")
	public static void reapplyData(BlockState state) {
		state.setData(state.getData());
	}

	/**
	 * Wear of a tool or armor piece (0 = new). Legacy: stored in the item's durability field.
	 */
	public static short getDamage(ItemStack stack) {
		return stack.getDurability();
	}

	public static void setDamage(ItemStack stack, short damage) {
		stack.setDurability(damage);
	}

	/**
	 * Copy of a stack with another amount, keeping its type, variant (legacy damage/data bits) and
	 * material data. Item meta is not copied.
	 */
	@SuppressWarnings("deprecation")
	public static ItemStack copyWithAmount(ItemStack from, int amount) {
		ItemStack copy = new ItemStack(from.getType(), amount, from.getDurability());
		copy.setData(from.getData());
		return copy;
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
