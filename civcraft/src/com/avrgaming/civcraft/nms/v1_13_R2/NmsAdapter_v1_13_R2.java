package com.avrgaming.civcraft.nms.v1_13_R2;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.nms.ItemNbt;
import com.avrgaming.civcraft.nms.NmsAdapter;

import net.minecraft.server.v1_13_R2.AttributeInstance;
import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import net.minecraft.server.v1_13_R2.DamageSource;
import net.minecraft.server.v1_13_R2.EntityInsentient;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.GenericAttributes;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.Vec3D;

public class NmsAdapter_v1_13_R2 implements NmsAdapter {

	@Override
	public String getVersion() {
		return "v1_13_R2";
	}

	@Override
	public ItemNbt itemNbt(ItemStack stack) {
		return new ItemNbt_v1_13_R2(stack);
	}

	@Override
	public String itemToJson(ItemStack stack) {
		try {
			return CraftItemStack.asNMSCopy(stack).save(new NBTTagCompound()).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static List<net.minecraft.server.v1_13_R2.Entity> entitiesInBox(Entity exempt, Location loc, double radius) {
		double x = loc.getX() + 0.5;
		double y = loc.getY() + 0.5;
		double z = loc.getZ() + 0.5;
		double r = radius;

		CraftWorld craftWorld = (CraftWorld) loc.getWorld();
		AxisAlignedBB bb = new AxisAlignedBB(x - r, y - r, z - r, x + r, y + r, z + r);

		if (exempt != null) {
			return craftWorld.getHandle().getEntities(((CraftEntity) exempt).getHandle(), bb);
		}
		return craftWorld.getHandle().getEntities(null, bb);
	}

	@Override
	public List<Entity> getNearbyEntities(Entity exempt, Location loc, double radius, Class<?> filter) {
		List<Entity> entities = new LinkedList<Entity>();
		for (net.minecraft.server.v1_13_R2.Entity e : entitiesInBox(exempt, loc, radius)) {
			Entity bukkitEntity = e.getBukkitEntity();
			if (filter == null || filter.isInstance(bukkitEntity)) {
				entities.add(bukkitEntity);
			}
		}
		return entities;
	}

	@Override
	public boolean isLineClear(World world, double x1, double y1, double z1, double x2, double y2, double z2) {
		Vec3D vec1 = new Vec3D(x1, y1, z1);
		Vec3D vec2 = new Vec3D(x2, y2, z2);
		return ((CraftWorld) world).getHandle().rayTrace(vec1, vec2) == null;
	}

	@Override
	public double getAttackDamage(LivingEntity entity) {
		EntityInsentient nmsEntity = (EntityInsentient) ((CraftLivingEntity) entity).getHandle();
		AttributeInstance attribute = nmsEntity.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE);
		return attribute.getValue();
	}

	@Override
	public int getLoveTicks(Entity entity) {
		NBTTagCompound tag = new NBTTagCompound();
		((CraftEntity) entity).getHandle().c(tag);
		return tag.getInt("InLove");
	}

	@Override
	public void damagePlayersAround(Entity attacker, Location loc, double radius, double damage) {
		for (net.minecraft.server.v1_13_R2.Entity e : entitiesInBox(attacker, loc, radius)) {
			if (e instanceof EntityPlayer) {
				EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(attacker, ((EntityPlayer) e).getBukkitEntity(),
						DamageCause.ENTITY_ATTACK, damage);
				Bukkit.getServer().getPluginManager().callEvent(event);
				e.damageEntity(DamageSource.GENERIC, (float) event.getDamage());
			}
		}
	}

}
