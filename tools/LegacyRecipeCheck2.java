import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.*;
import com.avrgaming.civcraft.util.ItemManager;

public class LegacyRecipeCheck2 {
	public static void main(String[] args) throws Exception {
		Class.forName("net.minecraft.server.v1_13_R2.DispenserRegistry").getMethod("c").invoke(null);
		final Object unsafe = Class.forName("org.bukkit.craftbukkit.v1_13_R2.util.CraftMagicNumbers").getField("INSTANCE").get(null);
		Bukkit.setServer((Server) Proxy.newProxyInstance(LegacyRecipeCheck2.class.getClassLoader(), new Class[] { Server.class }, new InvocationHandler() {
			public Object invoke(Object p, Method m, Object[] a) throws Throwable {
				switch (m.getName()) {
				case "getUnsafe": return unsafe;
				case "getItemFactory": return Class.forName("org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemFactory").getMethod("instance").invoke(null);
				case "getLogger": return java.util.logging.Logger.getLogger("check");
				case "getName": case "getVersion": case "getBukkitVersion": return "check";
				default: return null;
				}
			}
		}));
		YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(args[0]));
		Map<String, Mat> materials = load(cfg);
		ShapelessRecipe dummy = new ShapelessRecipe(new NamespacedKey("civ", "dummy"), new ItemStack(Material.STONE));
		dummy.addIngredient(Material.STONE);
		Object craft = Class.forName("org.bukkit.craftbukkit.v1_13_R2.inventory.CraftShapelessRecipe").getMethod("fromBukkitRecipe", ShapelessRecipe.class).invoke(null, dummy);
		Method toNms = Class.forName("org.bukkit.craftbukkit.v1_13_R2.inventory.CraftRecipe").getMethod("toNMS", RecipeChoice.class, boolean.class);
		int n = 0, bad = 0;
		try { toNms.invoke(craft, new RecipeChoice.MaterialChoice(Material.AIR), true); System.out.println("SANITY_FAIL: AIR accepted"); } catch (InvocationTargetException e) { System.out.println("SANITY_OK: AIR rejected: " + e.getCause().getMessage()); }
		try { toNms.invoke(craft, new RecipeChoice.MaterialChoice(Material.LEGACY_STONE), true); System.out.println("SANITY_FAIL: LEGACY accepted"); } catch (InvocationTargetException e) { System.out.println("SANITY_OK: LEGACY rejected: " + e.getCause().getMessage()); }
		for (Mat m : materials.values()) {
			if (!m.craftable) continue;
			n++;
			if (n <= 3) System.out.println("FIRST " + n + " " + m.id + " shaped=" + m.shaped + " ing=" + (m.ingredients == null ? null : m.ingredients.keySet()));
			int count = 0;
			if (m.ingredients == null) { System.out.println("NO_INGREDIENTS " + m.id); bad++; continue; }
			ShapelessRecipe real = new ShapelessRecipe(new NamespacedKey("civ", m.id.toLowerCase()), new ItemStack(Material.STONE));
			for (Ing ing : m.ingredients.values()) {
				int t = ing.type_id, d = ing.data;
				if (ing.custom_id != null) {
					Mat c = materials.get(ing.custom_id);
					if (c == null) { System.out.println("MISSING_CUSTOM " + m.id + " " + ing.custom_id); bad++; continue; }
					t = c.item_id; d = c.item_data;
				}
				RecipeChoice choice = ItemManager.getRecipeChoice(t, d);
				try { for (int k = 0; k < ing.count; k++) real.addIngredient(choice); } catch (Exception e) { System.out.println("ADD_FAIL " + m.id + " " + e); bad++; }
				count++;
				try { toNms.invoke(craft, choice, true); } catch (InvocationTargetException e) { bad++; System.out.println("BAD " + m.id + " shaped=" + m.shaped + " " + t + ":" + d + " custom=" + ing.custom_id + " " + choice); }
			}
			if (!m.shaped) {
				try {
					Object c2 = Class.forName("org.bukkit.craftbukkit.v1_13_R2.inventory.CraftShapelessRecipe").getMethod("fromBukkitRecipe", ShapelessRecipe.class).invoke(null, real);
					for (RecipeChoice rc : real.getChoiceList()) { toNms.invoke(c2, rc, true); }
				} catch (Throwable e) { bad++; System.out.println("REAL_FAIL " + m.id + " " + (e.getCause() != null ? e.getCause() : e) + " " + real.getChoiceList()); }
			}
			if (count == 0) { System.out.println("EMPTY " + m.id); bad++; }
		}
		System.out.println("recipes=" + n + " bad=" + bad);
	}

	static class Ing { int type_id; int data; String custom_id; int count = 1; }
	static class Mat { String id; int item_id; int item_data; boolean craftable; boolean shaped; HashMap<String, Ing> ingredients; }

	/** Same parsing as ConfigMaterial.loadConfig (ingredients keyed by custom id or "mc_"+type_id). */
	static Map<String, Mat> load(YamlConfiguration cfg) {
		Map<String, Mat> materials = new HashMap<String, Mat>();
		for (Map<?, ?> b : cfg.getMapList("materials")) {
			Mat mat = new Mat();
			mat.id = (String) b.get("id");
			mat.item_id = (Integer) b.get("item_id");
			mat.item_data = (Integer) b.get("item_data");
			Boolean craftable = (Boolean) b.get("craftable");
			mat.craftable = craftable != null && craftable;
			Boolean shaped = (Boolean) b.get("shaped");
			mat.shaped = shaped != null && shaped;
			List<Map<?, ?>> list = (List<Map<?, ?>>) b.get("ingredients");
			if (list != null) {
				mat.ingredients = new HashMap<String, Ing>();
				for (Map<?, ?> ingred : list) {
					Ing i = new Ing();
					i.type_id = (Integer) ingred.get("type_id");
					i.data = (Integer) ingred.get("data");
					i.custom_id = (String) ingred.get("custom_id");
					String key = i.custom_id != null ? i.custom_id : "mc_" + i.type_id;
					Integer count = (Integer) ingred.get("count");
					if (count != null) i.count = count;
					mat.ingredients.put(key, i);
				}
			}
			materials.put(mat.id, mat);
		}
		return materials;
	}
}
