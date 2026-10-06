package com.avrgaming.civcraft.util;

import java.util.LinkedList;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import com.avrgaming.civcraft.nms.Nms;

public class EntityProximity {


	/*
	 * Grab an axis aligned bounding box around an area to
	 * determine which entities are within this radius.
	 * Optionally provide an entity that is exempt from these checks.
	 * Also optionally provide a filter (a Bukkit type such as Player.class) so we can only capture specific types of entities.
	 */
	public static LinkedList<Entity> getNearbyEntities(Entity exempt, Location loc, double radius, Class<?> filter) {
		return new LinkedList<Entity>(Nms.get().getNearbyEntities(exempt, loc, radius, filter));
	}

}
