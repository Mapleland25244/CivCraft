package com.avrgaming.civcraft.nms.v1_12_R1;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.nms.HorseAccess;
import com.avrgaming.civcraft.nms.ItemNbt;
import com.avrgaming.civcraft.nms.NmsAdapter;

import net.minecraft.server.v1_12_R1.AttributeInstance;
import net.minecraft.server.v1_12_R1.AttributeModifier;
import net.minecraft.server.v1_12_R1.AxisAlignedBB;
import net.minecraft.server.v1_12_R1.DamageSource;
import net.minecraft.server.v1_12_R1.EntityInsentient;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.GenericAttributes;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.Vec3D;

public class NmsAdapter_v1_12_R1 implements NmsAdapter {

	@Override
	public String getVersion() {
		return "v1_12_R1";
	}

	@Override
	public ItemNbt itemNbt(ItemStack stack) {
		return new ItemNbt_v1_12_R1(stack);
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

	private static List<net.minecraft.server.v1_12_R1.Entity> entitiesInBox(Entity exempt, Location loc, double radius) {
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
		for (net.minecraft.server.v1_12_R1.Entity e : entitiesInBox(exempt, loc, radius)) {
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
		for (net.minecraft.server.v1_12_R1.Entity e : entitiesInBox(attacker, loc, radius)) {
			if (e instanceof EntityPlayer) {
				EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(attacker, ((EntityPlayer) e).getBukkitEntity(),
						DamageCause.ENTITY_ATTACK, damage);
				Bukkit.getServer().getPluginManager().callEvent(event);
				e.damageEntity(DamageSource.GENERIC, (float) event.getDamage());
			}
		}
	}

	@Override
	public boolean isHorse(LivingEntity entity) {
		return HorseAccess_v1_12_R1.isHorse(entity);
	}

	@Override
	public HorseAccess wrapHorse(LivingEntity horse) {
		return new HorseAccess_v1_12_R1(horse);
	}

	@Override
	public HorseAccess spawnHorse(Location loc) {
		return HorseAccess_v1_12_R1.spawn(loc);
	}

	@Override
	public void setHorseSpeedModifier(LivingEntity horse, UUID modifierId, String name, double amount) {
		EntityInsentient nmsEntity = (EntityInsentient) ((CraftLivingEntity) horse).getHandle();
		AttributeInstance attributes = nmsEntity.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED);
		AttributeModifier modifier = new AttributeModifier(modifierId, name, amount, 0);
		attributes.b(modifier); // remove the modifier, adding a duplicate causes errors
		attributes.a(modifier); // add the modifier
	}

	@Override
	public boolean hasHorseSpeedModifier(LivingEntity horse, UUID modifierId) {
		EntityInsentient nmsEntity = (EntityInsentient) ((CraftLivingEntity) horse).getHandle();
		AttributeInstance attributes = nmsEntity.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED);
		return attributes.a(modifierId) != null;
	}
}
