import java.nio.file.Files;
import java.nio.file.Paths;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.data.BlockData;

import com.avrgaming.civcraft.compat.LegacyBridge;

/**
 * Offline check of compat/LegacyBridge against the real 1.13.2 server tables (no server needed).
 * Reads tools/template-blocks-baseline.txt (every id:data pair the blueprints use) and verifies that each pair
 * converts to a 1.13 block, and that converting back gives the same pair.
 *
 * Run: see docs/testing/06-u1a-1.13.2.md (classpath: spigot-1.13.2.jar, compiled civcraft classes).
 */
public class LegacyBridgeCheck {

	public static void main(String[] args) throws Exception {
		Class.forName("net.minecraft.server.v1_13_R2.DispenserRegistry").getMethod("c").invoke(null);
		final Object unsafe = Class.forName("org.bukkit.craftbukkit.v1_13_R2.util.CraftMagicNumbers").getField("INSTANCE").get(null);
		Bukkit.setServer((Server) Proxy.newProxyInstance(LegacyBridgeCheck.class.getClassLoader(), new Class[] { Server.class },
				new InvocationHandler() {
					public Object invoke(Object p, Method m, Object[] a) {
						switch (m.getName()) {
						case "getUnsafe": return unsafe;
						case "createBlockData":
							try {
								return Class.forName("org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData").getMethod("newData", Material.class, String.class).invoke(null, a[0], a.length > 1 ? a[1] : null);
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						case "getLogger": return java.util.logging.Logger.getLogger("check");
						case "getName": case "getVersion": case "getBukkitVersion": return "check";
						default: return null;
						}
					}
				}));

		int pairs = 0, toAir = 0, idMismatch = 0, dataMismatch = 0;
		for (String line : Files.readAllLines(Paths.get(args[0]))) {
			if (line.startsWith("#") || line.trim().isEmpty()) {
				continue;
			}
			String[] parts = line.split(" ");
			String[] pair = parts[0].split(":");
			int id = Integer.parseInt(pair[0]);
			int data = Integer.parseInt(pair[1]);
			pairs++;

			BlockData bd = LegacyBridge.blockData(id, data);
			Material m = LegacyBridge.blockMaterial(id, data);
			if (id != 0 && (bd.getMaterial() == Material.AIR || m == Material.AIR)) {
				toAir++;
				System.out.println("TO_AIR   " + parts[0] + " " + parts[parts.length - 1]);
				continue;
			}
			int backId = LegacyBridge.idOf(bd);
			int backData = LegacyBridge.dataOf(bd, -1);
			if (backId != id) {
				idMismatch++;
				System.out.println("ID       " + parts[0] + " " + parts[parts.length - 1] + " -> " + bd.getAsString() + " -> id " + backId);
			} else if (backData != data) {
				dataMismatch++;
				System.out.println("DATA     " + parts[0] + " " + parts[parts.length - 1] + " -> " + bd.getAsString() + " -> data " + backData);
			}
		}
		if (args.length > 1) {
			checkItems(args[1]);
		}
		System.out.println("pairs=" + pairs + " toAir=" + toAir + " idMismatch=" + idMismatch + " dataMismatch=" + dataMismatch);
	}

	/** Every item and ingredient pair in materials.yml must convert to a real item. */
	private static void checkItems(String file) throws Exception {
		java.util.regex.Pattern idLine = java.util.regex.Pattern.compile("^[ ]*-?[ ]*(item_id|type_id):[ ]*([0-9]+)");
		java.util.regex.Pattern dataLine = java.util.regex.Pattern.compile("^[ ]*(item_data|data):[ ]*(-?[0-9]+)");
		int pairs = 0, toAir = 0, id = -1;
		for (String line : Files.readAllLines(Paths.get(file))) {
			java.util.regex.Matcher m = idLine.matcher(line);
			if (m.find()) {
				id = Integer.parseInt(m.group(2));
				continue;
			}
			m = dataLine.matcher(line);
			if (m.find() && id >= 0) {
				int data = Integer.parseInt(m.group(2));
				pairs++;
				Material item = data < 0 ? (LegacyBridge.itemVariants(id).isEmpty() ? Material.AIR : LegacyBridge.itemVariants(id).get(0)) : LegacyBridge.itemMaterial(id, data);
				if (data < 0) {
					System.out.println("WILDCARD " + id + ":-1 -> " + LegacyBridge.itemVariants(id));
				}
				if (id != 0 && (item == Material.AIR || item.isLegacy() || !item.isItem())) {
					toAir++;
					System.out.println("ITEM_AIR " + id + ":" + data);
				}
				id = -1;
			}
		}
		System.out.println("materials.yml pairs=" + pairs + " toAir=" + toAir);
	}
}
