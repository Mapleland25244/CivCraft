package com.avrgaming.civcraft.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Wolf;

import com.avrgaming.civcraft.config.ConfigMobSpawner;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.civcraft.threading.TaskMaster;

/**
 * Built-in mob spawners (CustomMobs is no longer used). Every spawner id in spawners.yml is
 * "<family>_<tier>" (e.g. yobo_greater); the family picks the vanilla entity type and the
 * tier scales health and damage. A spawner only produces mobs while a player is nearby, and
 * never keeps more than {@link #MAX_ALIVE} of its own mobs alive.
 *
 * All world access happens in the sync timer; {@link #setSpawnerActive} only touches the maps
 * so it is safe to call from any thread.
 */
class NativeMobSpawnProvider implements MobSpawnProvider {

	private static final String TIMER_NAME = "NativeMobSpawner";
	private static final long CYCLE_TICKS = 100;
	private static final double ACTIVATION_RADIUS = 40.0;
	private static final int SPAWN_SPREAD = 4;
	private static final int SPAWN_ATTEMPTS = 8;
	private static final int MAX_ALIVE = 4;

	private static final Map<String, EntityType> FAMILIES = new HashMap<String, EntityType>();
	private static final Map<String, Double> TIER_SCALE = new HashMap<String, Double>();
	private static final Map<String, double[]> TIER_HP = new HashMap<String, double[]>();
	static {
		FAMILIES.put("yobo", EntityType.ZOMBIE);
		FAMILIES.put("ruffian", EntityType.WITCH);
		FAMILIES.put("behemoth", EntityType.HUSK);
		FAMILIES.put("undead", EntityType.SKELETON);
		FAMILIES.put("kodiac", EntityType.POLAR_BEAR);
		FAMILIES.put("cube", EntityType.SLIME);
		FAMILIES.put("arachne", EntityType.SPIDER);
		FAMILIES.put("direwolf", EntityType.WOLF);
		FAMILIES.put("kraken", EntityType.GUARDIAN);

		TIER_SCALE.put("lesser", 1.0);
		TIER_SCALE.put("greater", 2.0);
		TIER_SCALE.put("elite", 4.0);
		TIER_SCALE.put("brutal", 8.0);

		/* HP per tier (lesser, greater, elite, brutal) from the CivCraft wiki "Custom Mobs" page. */
		TIER_HP.put("yobo", new double[] { 20, 25, 30, 40 });
		TIER_HP.put("ruffian", new double[] { 10, 15, 20, 30 });
		TIER_HP.put("behemoth", new double[] { 75, 125, 150, 175 });
	}

	private static final String[] TIERS = { "lesser", "greater", "elite", "brutal" };

	/** Scoreboard tag put on every mob this provider spawns; {@link MobSpawnDropListener} keys on it. */
	static final String MOB_TAG = "civcraft_spawner_mob";

	private static class Spawner {
		final String id;
		final Location loc;
		final EntityType type;
		final double scale;
		final boolean water;
		final List<UUID> alive = new ArrayList<UUID>();

		Spawner(String id, Location loc, EntityType type, double scale, boolean water) {
			this.id = id;
			this.loc = loc;
			this.type = type;
			this.scale = scale;
			this.water = water;
		}
	}

	private final Map<String, Spawner> spawners = new ConcurrentHashMap<String, Spawner>();
	private final Queue<List<UUID>> pendingRemoval = new ConcurrentLinkedQueue<List<UUID>>();
	private final Random random = new Random();

	/* Ambient spawning around players inside culture (spawners.yml "ambient" section). */
	private static final String AMBIENT_TIMER_NAME = "NativeMobAmbient";
	private static final Map<String, String> BIOME_SPAWNS = new HashMap<String, String>();
	private final List<UUID> ambientAlive = new ArrayList<UUID>();
	private int ambientRadius;
	private int ambientMinDistance;
	private int ambientMax;
	private double ambientChance;

	static {
		/* Biomes per tier from the wiki "Custom Mobs" page, using the 1.13 biome names. */
		biomes("yobo_lesser", "PLAINS", "FOREST", "BIRCH_FOREST");
		biomes("yobo_greater", "WOODED_HILLS", "BIRCH_FOREST_HILLS", "MOUNTAIN_EDGE");
		biomes("yobo_elite", "MOUNTAINS", "WOODED_MOUNTAINS", "TALL_BIRCH_FOREST");
		biomes("yobo_brutal", "GRAVELLY_MOUNTAINS", "MODIFIED_GRAVELLY_MOUNTAINS", "TALL_BIRCH_HILLS");
		biomes("ruffian_lesser", "JUNGLE", "JUNGLE_EDGE");
		biomes("ruffian_greater", "JUNGLE_HILLS", "DARK_FOREST", "MODIFIED_JUNGLE_EDGE");
		biomes("ruffian_elite", "SUNFLOWER_PLAINS", "FLOWER_FOREST", "GIANT_TREE_TAIGA", "GIANT_TREE_TAIGA_HILLS");
		biomes("ruffian_brutal", "MODIFIED_JUNGLE", "DARK_FOREST_HILLS", "GIANT_SPRUCE_TAIGA", "GIANT_SPRUCE_TAIGA_HILLS");
		biomes("behemoth_lesser", "SNOWY_TUNDRA", "TAIGA");
		biomes("behemoth_greater", "SNOWY_TAIGA", "SNOWY_TAIGA_HILLS");
		biomes("behemoth_elite", "TAIGA_HILLS", "SNOWY_MOUNTAINS");
		biomes("behemoth_brutal", "ICE_SPIKES", "TAIGA_MOUNTAINS", "SNOWY_TAIGA_MOUNTAINS");
	}

	private static void biomes(String spawnerId, String... biomeNames) {
		for (String b : biomeNames) {
			BIOME_SPAWNS.put(b, spawnerId);
		}
	}

	NativeMobSpawnProvider(boolean spawnersEnabled, boolean ambientEnabled) {
		if (spawnersEnabled) {
			TaskMaster.syncTimer(TIMER_NAME, new Runnable() {
				@Override
				public void run() {
					tick();
				}
			}, CYCLE_TICKS, CYCLE_TICKS);
		}

		if (ambientEnabled) {
			org.bukkit.configuration.file.FileConfiguration cfg = CivSettings.plugin.getConfig();
			ambientRadius = cfg.getInt("ambient_mobs.radius", 24);
			ambientMinDistance = cfg.getInt("ambient_mobs.min_distance", 10);
			ambientMax = cfg.getInt("ambient_mobs.max_per_player", 3);
			ambientChance = cfg.getDouble("ambient_mobs.chance", 0.5);
			long interval = Math.max(1, cfg.getInt("ambient_mobs.interval_seconds", 10)) * 20L;
			TaskMaster.syncTimer(AMBIENT_TIMER_NAME, new Runnable() {
				@Override
				public void run() {
					ambientTick();
				}
			}, interval, interval);
		}
	}

	private void ambientTick() {
		/* BlockListener cancels every spawn during war, which would only make spawnEntity throw. */
		if (War.isWarTime()) {
			return;
		}

		ambientAlive.removeIf(id -> {
			Entity e = Bukkit.getEntity(id);
			return e == null || !e.isValid();
		});

		for (Player player : Bukkit.getOnlinePlayers()) {
			try {
				ambientSpawnFor(player);
			} catch (Exception e) {
				CivLog.warning("Ambient mob spawn near " + player.getName() + " failed: " + e.getMessage());
			}
		}
	}

	private void ambientSpawnFor(Player player) {
		if (player.getGameMode() == org.bukkit.GameMode.CREATIVE || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
			return;
		}
		if (random.nextDouble() >= ambientChance || CivGlobal.getCultureChunk(player.getLocation()) == null) {
			return;
		}

		int nearby = 0;
		for (Entity e : player.getNearbyEntities(ambientRadius, ambientRadius, ambientRadius)) {
			if (e.getScoreboardTags().contains(MOB_TAG)) {
				nearby++;
			}
		}
		if (nearby >= ambientMax) {
			return;
		}

		World world = player.getWorld();
		for (int i = 0; i < SPAWN_ATTEMPTS; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double dist = ambientMinDistance + random.nextDouble() * Math.max(1, ambientRadius - ambientMinDistance);
			int x = player.getLocation().getBlockX() + (int) Math.round(Math.cos(angle) * dist);
			int z = player.getLocation().getBlockZ() + (int) Math.round(Math.sin(angle) * dist);
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				continue;
			}

			int y = world.getHighestBlockYAt(x, z);
			Block feet = world.getBlockAt(x, y, z);
			Block ground = feet.getRelative(0, -1, 0);
			if (!ground.getType().isSolid() || isBlocked(feet) || isBlocked(feet.getRelative(0, 1, 0))) {
				continue;
			}

			Location spot = feet.getLocation().add(0.5, 0, 0.5);
			if (CivGlobal.getCultureChunk(spot) == null || CivGlobal.getFarmChunk(new ChunkCoord(spot)) != null) {
				continue;
			}

			String id = BIOME_SPAWNS.get(feet.getBiome().name());
			Double scale = id == null ? null : TIER_SCALE.get(id.substring(id.lastIndexOf('_') + 1));
			EntityType type = id == null ? null : FAMILIES.get(id.substring(0, id.lastIndexOf('_')));
			if (type == null || scale == null) {
				continue;
			}

			Entity entity = world.spawnEntity(spot, type);
			if (entity instanceof LivingEntity) {
				applyStats((LivingEntity) entity, new Spawner(id, spot, type, scale, false));
				ambientAlive.add(entity.getUniqueId());
			} else {
				entity.remove();
			}
			return;
		}
	}

	private static String key(Location loc) {
		return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
	}

	@Override
	public void setSpawnerActive(String mobName, Location loc, boolean active) {
		String key = key(loc);
		if (!active) {
			Spawner removed = spawners.remove(key);
			if (removed != null) {
				CivLog.debug("Spawner Disabled at " + loc);
				pendingRemoval.add(new ArrayList<UUID>(removed.alive));
			}
			return;
		}

		if (spawners.containsKey(key)) {
			return;
		}

		int split = mobName.lastIndexOf('_');
		String family = split < 0 ? mobName : mobName.substring(0, split);
		String tier = split < 0 ? "lesser" : mobName.substring(split + 1);
		EntityType type = FAMILIES.get(family);
		Double scale = TIER_SCALE.get(tier);
		if (type == null || scale == null) {
			CivLog.warning("Unable to create Spawner; " + mobName + " does not exist");
			return;
		}

		ConfigMobSpawner config = CivSettings.spawners.get(mobName);
		boolean water = config != null && config.water;
		spawners.put(key, new Spawner(mobName, loc.clone(), type, scale, water));
	}

	/** Removes every mob this provider spawned. Call on plugin disable. */
	void shutdown() {
		TaskMaster.cancelTimer(TIMER_NAME);
		TaskMaster.cancelTimer(AMBIENT_TIMER_NAME);
		removeAll(ambientAlive);
		ambientAlive.clear();
		for (Spawner s : spawners.values()) {
			removeAll(s.alive);
		}
		spawners.clear();
		List<UUID> ids;
		while ((ids = pendingRemoval.poll()) != null) {
			removeAll(ids);
		}
	}

	private void tick() {
		List<UUID> ids;
		while ((ids = pendingRemoval.poll()) != null) {
			removeAll(ids);
		}

		for (Spawner s : spawners.values()) {
			try {
				tickSpawner(s);
			} catch (Exception e) {
				CivLog.warning("Native mob spawner " + s.id + " at " + s.loc + " failed: " + e.getMessage());
			}
		}
	}

	private void tickSpawner(Spawner s) {
		World world = s.loc.getWorld();
		if (world == null || !world.isChunkLoaded(s.loc.getBlockX() >> 4, s.loc.getBlockZ() >> 4)) {
			return;
		}

		/* Forget mobs that died or despawned. */
		List<UUID> stillAlive = new ArrayList<UUID>();
		for (UUID id : s.alive) {
			Entity e = Bukkit.getEntity(id);
			if (e != null && e.isValid()) {
				stillAlive.add(id);
			}
		}
		s.alive.clear();
		s.alive.addAll(stillAlive);

		if (s.alive.size() >= MAX_ALIVE || !playerNearby(s.loc)) {
			return;
		}

		Location spot = findSpawnSpot(s);
		if (spot == null) {
			return;
		}

		Entity entity = world.spawnEntity(spot, s.type);
		if (!(entity instanceof LivingEntity)) {
			entity.remove();
			return;
		}
		applyStats((LivingEntity) entity, s);
		s.alive.add(entity.getUniqueId());
	}

	private boolean playerNearby(Location loc) {
		double maxSq = ACTIVATION_RADIUS * ACTIVATION_RADIUS;
		for (Player p : loc.getWorld().getPlayers()) {
			if (p.getLocation().distanceSquared(loc) <= maxSq) {
				return true;
			}
		}
		return false;
	}

	private Location findSpawnSpot(Spawner s) {
		World world = s.loc.getWorld();
		for (int i = 0; i < SPAWN_ATTEMPTS; i++) {
			int x = s.loc.getBlockX() + random.nextInt(SPAWN_SPREAD * 2 + 1) - SPAWN_SPREAD;
			int z = s.loc.getBlockZ() + random.nextInt(SPAWN_SPREAD * 2 + 1) - SPAWN_SPREAD;
			int y = s.loc.getBlockY() + random.nextInt(3);
			if (!world.isChunkLoaded(x >> 4, z >> 4)) {
				continue;
			}

			Block feet = world.getBlockAt(x, y, z);
			Block head = feet.getRelative(0, 1, 0);
			if (s.water) {
				if (feet.getType() == Material.WATER && head.getType() == Material.WATER) {
					return feet.getLocation().add(0.5, 0, 0.5);
				}
				continue;
			}

			Block ground = feet.getRelative(0, -1, 0);
			if (ground.getType().isSolid() && !isBlocked(feet) && !isBlocked(head)) {
				return feet.getLocation().add(0.5, 0, 0.5);
			}
		}
		return null;
	}

	private static boolean isBlocked(Block b) {
		Material m = b.getType();
		return m.isSolid() || m == Material.WATER || m == Material.LAVA;
	}

	private void applyStats(LivingEntity mob, Spawner s) {
		ConfigMobSpawner config = CivSettings.spawners.get(s.id);
		if (config != null && config.name != null) {
			mob.setCustomName(config.name);
			mob.setCustomNameVisible(true);
		}

		/* Resizing a slime resets its stats, so do it before scaling them. */
		if (mob instanceof Slime) {
			((Slime) mob).setSize(1 + (int) Math.round(s.scale));
		}

		mob.addScoreboardTag(MOB_TAG);

		AttributeInstance health = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
		if (health != null) {
			double max = health.getBaseValue() * s.scale;
			double[] wikiHp = TIER_HP.get(s.id.substring(0, Math.max(0, s.id.lastIndexOf('_'))));
			int tier = java.util.Arrays.asList(TIERS).indexOf(s.id.substring(s.id.lastIndexOf('_') + 1));
			if (wikiHp != null && tier >= 0) {
				max = wikiHp[tier];
			}
			health.setBaseValue(max);
			mob.setHealth(max);
		}

		AttributeInstance damage = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
		if (damage != null) {
			damage.setBaseValue(damage.getBaseValue() * s.scale);
		}

		if (mob instanceof Wolf) {
			((Wolf) mob).setAngry(true);
		}
		mob.setRemoveWhenFarAway(true);
	}

	private static void removeAll(List<UUID> ids) {
		for (UUID id : ids) {
			Entity e = Bukkit.getEntity(id);
			if (e != null) {
				e.remove();
			}
		}
	}
}
