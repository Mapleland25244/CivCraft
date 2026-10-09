import java.io.FileReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.yaml.snakeyaml.Yaml;

import com.avrgaming.civcraft.util.ItemManager;

/**
 * Offline check that every craftable entry of materials.yml converts to recipe choices the 1.13.2 server accepts
 * (the server throws "Recipe requires at least one non-air choice!" otherwise and the plugin does not start).
 * Mirrors the ingredient handling of LoreCraftableMaterial.buildRecipes. Usage: LegacyRecipeCheck materials.yml
 */
public class LegacyRecipeCheck {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		Class.forName("net.minecraft.server.v1_13_R2.DispenserRegistry").getMethod("c").invoke(null);
		final Object unsafe = Class.forName("org.bukkit.craftbukkit.v1_13_R2.util.CraftMagicNumbers").getField("INSTANCE").get(null);
		Bukkit.setServer((Server) Proxy.newProxyInstance(LegacyRecipeCheck.class.getClassLoader(), new Class[] { Server.class },
				new InvocationHandler() {
					public Object invoke(Object p, Method m, Object[] a) {
						switch (m.getName()) {
						case "getUnsafe": return unsafe;
						case "getItemFactory":
							try {
								return Class.forName("org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemFactory").getMethod("instance").invoke(null);
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						case "getLogger": return java.util.logging.Logger.getLogger("check");
						case "getName": case "getVersion": case "getBukkitVersion": return "check";
						default: return null;
						}
					}
				}));

		Map<String, Object> root = (Map<String, Object>) new Yaml().load(new FileReader(args[0]));
		List<Map<String, Object>> materials = (List<Map<String, Object>>) root.get("materials");
		Map<String, Map<String, Object>> byId = new HashMap<String, Map<String, Object>>();
		for (Map<String, Object> m : materials) {
			byId.put((String) m.get("id"), m);
		}

		// any CraftRecipe instance will do: toNMS is a default method of the interface
		Class<?> craftShapeless = Class.forName("org.bukkit.craftbukkit.v1_13_R2.inventory.CraftShapelessRecipe");
		ShapelessRecipe dummy = new ShapelessRecipe(new NamespacedKey("civ", "dummy"), new ItemStack(Material.STONE));
		dummy.addIngredient(Material.STONE);
		Object craft = craftShapeless.getMethod("fromBukkitRecipe", ShapelessRecipe.class).invoke(null, dummy);
		Method toNms = Class.forName("org.bukkit.craftbukkit.v1_13_R2.inventory.CraftRecipe").getMethod("toNMS", RecipeChoice.class, boolean.class);

		int recipes = 0, bad = 0;
		for (Map<String, Object> m : materials) {
			if (!Boolean.TRUE.equals(m.get("craftable"))) {
				continue;
			}
			recipes++;
			List<RecipeChoice> choices = new ArrayList<RecipeChoice>();
			List<Map<String, Object>> ingredients = (List<Map<String, Object>>) m.get("ingredients");
			if (ingredients != null) {
				for (Map<String, Object> ing : ingredients) {
					int typeId = ing.get("type_id") == null ? 0 : ((Number) ing.get("type_id")).intValue();
					int data = ing.get("data") == null ? 0 : ((Number) ing.get("data")).intValue();
					if (ing.get("custom_id") != null) {
						Map<String, Object> custom = byId.get((String) ing.get("custom_id"));
						if (custom == null) {
							System.out.println("MISSING_CUSTOM " + m.get("id") + " -> " + ing.get("custom_id"));
							bad++;
							continue;
						}
						typeId = ((Number) custom.get("item_id")).intValue();
						data = custom.get("item_data") == null ? 0 : ((Number) custom.get("item_data")).intValue();
					}
					choices.add(ItemManager.getRecipeChoice(typeId, data));
				}
			}
			for (RecipeChoice choice : choices) {
				try {
					toNms.invoke(craft, choice, true);
				} catch (java.lang.reflect.InvocationTargetException e) {
					bad++;
					System.out.println("BAD_RECIPE " + m.get("id") + " " + choice + " : " + e.getCause().getMessage());
				}
			}
		}
		System.out.println("recipes=" + recipes + " bad=" + bad);
	}
}
