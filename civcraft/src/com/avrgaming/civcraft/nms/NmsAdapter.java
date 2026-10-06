package com.avrgaming.civcraft.nms;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

/**
 * Everything CivCraft needs from net.minecraft.server / org.bukkit.craftbukkit, in terms of plain Bukkit types.
 * One implementation per server version (see {@link Nms}); no other class may import those packages
 * (tools/check-nms.sh enforces this).
 */
public interface NmsAdapter {

	/** The CraftBukkit package version this adapter was written for, e.g. "v1_12_R1". */
	String getVersion();

	/** Wraps a copy of the stack so its NBT (attributes, lore, civcraft properties...) can be read and written. */
	ItemNbt itemNbt(ItemStack stack);

	/** The JSON/NBT text of the stack, as used by the SHOW_ITEM hover event. Null if it cannot be produced. */
	String itemToJson(ItemStack stack);

	/**
	 * Entities within a cube of the given radius around the center of the block at loc.
	 * @param exempt optional entity that is left out of the result
	 * @param filter optional type the entities must be an instance of (a Bukkit type, e.g. Player.class)
	 */
	List<Entity> getNearbyEntities(Entity exempt, Location loc, double radius, Class<?> filter);

	/** True when no block is in the way on the straight line between the two points (both in world). */
	boolean isLineClear(World world, double x1, double y1, double z1, double x2, double y2, double z2);

	/** The mob's current attack damage attribute value. */
	double getAttackDamage(LivingEntity entity);

	/** Remaining "in love" ticks of an animal; 0 when it is not in love. */
	int getLoveTicks(Entity entity);

	/**
	 * Damages every player inside the cube around loc (centered on the block), after firing an
	 * EntityDamageByEntityEvent for each so other plugins can modify or observe it.
	 */
	void damagePlayersAround(Entity attacker, Location loc, double radius, double damage);

	/** Horse access that the Bukkit API does not provide. */
	boolean isHorse(LivingEntity entity);

	HorseAccess wrapHorse(LivingEntity horse);

	HorseAccess spawnHorse(Location loc);

	void setHorseSpeedModifier(LivingEntity horse, java.util.UUID modifierId, String name, double amount);

	boolean hasHorseSpeedModifier(LivingEntity horse, java.util.UUID modifierId);
}
