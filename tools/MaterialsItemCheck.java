import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;

import com.avrgaming.civcraft.compat.LegacyBridge;

/**
 * Offline check: every item_id/item_data (materials) and type_id/data (ingredients) in materials.yml must convert to a
 * real 1.13 item, and the recipe choice for it must not be empty.
 *
 * Run: java -cp ... MaterialsItemCheck civcraft/data/materials.yml
 */
public class MaterialsItemCheck {

	public static void main(String[] args) throws Exception {
		Class.forName("net.minecraft.server.v1_13_R2.DispenserRegistry").getMethod("c").invoke(null);
		final Object unsafe = Class.forName("org.bukkit.craftbukkit.v1_13_R2.util.CraftMagicNumbers").getField("INSTANCE").get(null);
		Bukkit.setServer((Server) Proxy.newProxyInstance(MaterialsItemCheck.class.getClassLoader(), new Class[] { Server.class },
				new InvocationHandler() {
					public Object invoke(Object p, Method m, Object[] a) {
						switch (m.getName()) {
						case "getUnsafe": return unsafe;
						case "getLogger": return java.util.logging.Logger.getLogger("check");
						case "getName": case "getVersion": case "getBukkitVersion": return "check";
						default: return null;
						}
					}
				}));

		List<String> lines = Files.readAllLines(Paths.get(args[0]));
		Pattern idPat = Pattern.compile("^\\s*-?\\s*(item_id|type_id):\\s*(-?\\d+)");
		Pattern dataPat = Pattern.compile("^\\s*(item_data|data):\\s*(-?\\d+)");
		String currentId = "?";
		int checked = 0, bad = 0;
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			Matcher idm = Pattern.compile("^\\s*-\\s*id:\\s*'([^']+)'").matcher(line);
			if (idm.find()) {
				currentId = idm.group(1);
			}
			Matcher m = idPat.matcher(line);
			if (!m.find()) {
				continue;
			}
			int id = Integer.parseInt(m.group(2));
			int data = 0;
			String customId = null;
			// data and custom_id follow within the next few lines
			for (int j = i + 1; j < Math.min(lines.size(), i + 6); j++) {
				Matcher dm = dataPat.matcher(lines.get(j));
				if (dm.find()) {
					data = Integer.parseInt(dm.group(2));
				}
				Matcher cm = Pattern.compile("custom_id:\\s*'([^']+)'").matcher(lines.get(j));
				if (cm.find()) {
					customId = cm.group(1);
				}
				if (lines.get(j).matches("^\\s*-\\s+.*")) {
					break;
				}
			}
			if (id == 0 && customId != null) {
				continue; // custom ingredient: its own item_id is checked where it is defined
			}
			if (id == 0) {
				continue;
			}
			checked++;
			Material item = LegacyBridge.itemMaterial(id, data);
			boolean variantsEmpty = data < 0 && LegacyBridge.itemVariants(id).isEmpty();
			if (item == Material.AIR || !item.isItem() || variantsEmpty) {
				bad++;
				System.out.println("BAD  " + currentId + "  " + id + ":" + data + " -> " + item + (variantsEmpty ? " (no variants)" : ""));
			}
		}
		System.out.println("checked " + checked + " ids, " + bad + " bad");
	}
}
