package com.avrgaming.civcraft.nms.v1_12_R1;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.nms.HorseAccess;

/**
 * Moved unchanged from gpl.HorseModifier (DarkBlade12's HorseModifier v1.1). It reaches the server through reflection
 * on the obfuscated 1.12 method names ("b", "a", "f"), so it only works on this version.
 */
public class HorseAccess_v1_12_R1 implements HorseAccess {

	private Object entityHorse;
	private Object nbtTagCompound;

	public HorseAccess_v1_12_R1(LivingEntity horse) {
		try {
			this.entityHorse = ReflectionUtil.getMethod("getHandle", horse.getClass(), 0).invoke(horse);
			this.nbtTagCompound = NBTUtil.getNBTTagCompound(entityHorse);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private HorseAccess_v1_12_R1(Object entityHorse) {
		this.entityHorse = entityHorse;
		try {
			this.nbtTagCompound = NBTUtil.getNBTTagCompound(entityHorse);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static HorseAccess_v1_12_R1 spawn(Location loc) {
		World w = loc.getWorld();
		try {
			Object worldServer = ReflectionUtil.getMethod("getHandle", w.getClass(), 0).invoke(w);
			Object entityHorse = ReflectionUtil.getClass("EntityHorse", worldServer);
			ReflectionUtil.getMethod("setPosition", entityHorse.getClass(), 3).invoke(entityHorse, loc.getX(), loc.getY(), loc.getZ());
			ReflectionUtil.getMethod("addEntity", worldServer.getClass(), 1).invoke(worldServer, entityHorse);
			return new HorseAccess_v1_12_R1(entityHorse);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static boolean isHorse(LivingEntity le) {
		try {
			Object entityLiving = ReflectionUtil.getMethod("getHandle", le.getClass(), 0).invoke(le);
			Object nbtTagCompound = NBTUtil.getNBTTagCompound(entityLiving);
			return NBTUtil.hasKeys(nbtTagCompound, new String[] { "Bred", "EatingHaystack", "Tame", "Temper", "Variant" });
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public int getInt(String key) {
		return (int) NBTUtil.getValue(nbtTagCompound, Integer.class, key);
	}

	@Override
	public boolean getBoolean(String key) {
		return (boolean) NBTUtil.getValue(nbtTagCompound, Boolean.class, key);
	}

	@Override
	public void setInt(String key, int value) {
		setHorseValue(key, value);
	}

	@Override
	public void setBoolean(String key, boolean value) {
		setHorseValue(key, value);
	}

	@Override
	public void setArmorItem(ItemStack i) {
		if (i != null) {
			try {
				Object itemTag = ReflectionUtil.getClass("NBTTagCompound", "ArmorItem");
				Object itemStack = ReflectionUtil.getMethod("asNMSCopy", Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".inventory.CraftItemStack"), 1).invoke(this, i);
				ReflectionUtil.getMethod("save", itemStack.getClass(), 1).invoke(itemStack, itemTag);
				setHorseValue("ArmorItem", itemTag);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			setHorseValue("ArmorItem", null);
		}
	}

	@Override
	public ItemStack getArmorItem() {
		try {
			Object itemTag = NBTUtil.getValue(nbtTagCompound, nbtTagCompound.getClass(), "ArmorItem");
			Object itemStack = ReflectionUtil.getMethod("createStack", Class.forName(ReflectionUtil.getPackageName() + ".ItemStack"), 1).invoke(this, itemTag);
			return (ItemStack) ReflectionUtil.getMethod("asCraftMirror", Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".inventory.CraftItemStack"), 1).invoke(this, itemStack);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void openInventory(Player p) {
		try {
			Object entityPlayer = ReflectionUtil.getMethod("getHandle", p.getClass(), 0).invoke(p);
			ReflectionUtil.getMethod("f", entityHorse.getClass(), 1).invoke(entityHorse, entityPlayer);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public LivingEntity getHorse() {
		try {
			return (LivingEntity) ReflectionUtil.getMethod("getBukkitEntity", entityHorse.getClass(), 0).invoke(entityHorse);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/** Changes a value in the NBTTagCompound and updates it to the horse. */
	private void setHorseValue(String key, Object value) {
		NBTUtil.setValue(nbtTagCompound, key, value);
		NBTUtil.updateNBTTagCompound(entityHorse, nbtTagCompound);
	}

	private static class NBTUtil {
		public static Object getNBTTagCompound(Object entity) {
			try {
				Object nbtTagCompound = ReflectionUtil.getClass("NBTTagCompound");
				for (Method m : entity.getClass().getMethods()) {
					Class<?>[] pt = m.getParameterTypes();
					if (m.getName().equals("b") && pt.length == 1 && pt[0].getName().contains("NBTTagCompound")) {
						m.invoke(entity, nbtTagCompound);
					}
				}
				return nbtTagCompound;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		public static void updateNBTTagCompound(Object entity, Object nbtTagCompound) {
			try {
				for (Method m : entity.getClass().getMethods()) {
					Class<?>[] pt = m.getParameterTypes();
					if (m.getName().equals("a") && pt.length == 1 && pt[0].getName().contains("NBTTagCompound")) {
						m.invoke(entity, nbtTagCompound);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public static void setValue(Object nbtTagCompound, String key, Object value) {
			try {
				if (value instanceof Integer) {
					ReflectionUtil.getMethod("setInt", nbtTagCompound.getClass(), 2).invoke(nbtTagCompound, key, (Integer) value);
					return;
				} else if (value instanceof Boolean) {
					ReflectionUtil.getMethod("setBoolean", nbtTagCompound.getClass(), 2).invoke(nbtTagCompound, key, (Boolean) value);
					return;
				} else {
					ReflectionUtil.getMethod("set", nbtTagCompound.getClass(), 2).invoke(nbtTagCompound, key, value);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public static Object getValue(Object nbtTagCompound, Class<?> c, String key) {
			try {
				if (c == Integer.class) {
					return ReflectionUtil.getMethod("getInt", nbtTagCompound.getClass(), 1).invoke(nbtTagCompound, key);
				} else if (c == Boolean.class) {
					return ReflectionUtil.getMethod("getBoolean", nbtTagCompound.getClass(), 1).invoke(nbtTagCompound, key);
				} else {
					return ReflectionUtil.getMethod("getCompound", nbtTagCompound.getClass(), 1).invoke(nbtTagCompound, key);
				}
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		public static boolean hasKey(Object nbtTagCompound, String key) {
			try {
				return (boolean) ReflectionUtil.getMethod("hasKey", nbtTagCompound.getClass(), 1).invoke(nbtTagCompound, key);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}

		public static boolean hasKeys(Object nbtTagCompound, String[] keys) {
			for (String key : keys) {
				if (!hasKey(nbtTagCompound, key)) {
					return false;
				}
			}
			return true;
		}
	}

	private static class ReflectionUtil {
		public static Object getClass(String name, Object... args) throws Exception {
			Class<?> c = Class.forName(ReflectionUtil.getPackageName() + "." + name);
			int params = 0;
			if (args != null) {
				params = args.length;
			}
			for (Constructor<?> co : c.getConstructors()) {
				if (co.getParameterTypes().length == params) {
					return co.newInstance(args);
				}
			}
			return null;
		}

		public static Method getMethod(String name, Class<?> c, int params) {
			for (Method m : c.getMethods()) {
				if (m.getName().equals(name) && m.getParameterTypes().length == params) {
					return m;
				}
			}
			return null;
		}

		public static String getPackageName() {
			return "net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
		}
	}
}
