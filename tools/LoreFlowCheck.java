import com.avrgaming.civcraft.nms.v1_13_R2.ItemNbt_v1_13_R2;
import net.minecraft.server.v1_13_R2.DispenserRegistry;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Offline check of the 1.13 name/lore storage (nms/v1_13_R2/ItemNbt_v1_13_R2): a string written must read back
 * identical (CivCraft compares lore strings), JSON given back must not be wrapped twice, and nothing is italic unless asked.
 */
public class LoreFlowCheck {
	static int failures = 0;

	static String esc(String s) {
		return s.replace("§", "&s");
	}

	static void expect(String what, boolean ok, String detail) {
		System.out.println((ok ? "ok    " : "FAIL  ") + what + (ok ? "" : "  " + detail));
		if (!ok) {
			failures++;
		}
	}

	public static void main(String[] a) throws Exception {
		DispenserRegistry.c();
		org.bukkit.Server server = (org.bukkit.Server) java.lang.reflect.Proxy.newProxyInstance(
				LoreFlowCheck.class.getClassLoader(), new Class<?>[] { org.bukkit.Server.class },
				(proxy, method, args) -> {
					switch (method.getName()) {
					case "getItemFactory":
						return org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemFactory.instance();
					case "getLogger":
						return java.util.logging.Logger.getAnonymousLogger();
					case "getName":
					case "getVersion":
					case "getBukkitVersion":
						return "check";
					default:
						return null;
					}
				});
		org.bukkit.Bukkit.setServer(server);

		// (a leading reset or italic code before a color code is a no-op in legacy rendering, so it is not a round-trip sample)
		String[] samples = { "§dGear Tier 3", "§3" + "4.0 Defense Bonus", "§6Soulbound", "§oItalic on purpose",
				"§9+3.0 Defense", "Plain text" };

		ItemStack stack = new ItemStack(Material.IRON_LEGGINGS);
		ItemNbt_v1_13_R2 nbt = new ItemNbt_v1_13_R2(stack);
		nbt.setName("Carbide Steel leggings");
		for (String s : samples) {
			nbt.addLore(s);
		}
		ItemStack made = nbt.getStack();

		ItemNbt_v1_13_R2 again = new ItemNbt_v1_13_R2(made);
		String[] lore = again.getLore();
		for (int i = 0; i < samples.length; i++) {
			expect("lore " + i + " reads back unchanged", samples[i].equals(lore[i]), "wrote " + esc(samples[i]) + " read " + esc(lore[i]));
		}
		expect("name reads back", "§rCarbide Steel leggings".equals(again.getName()) || "Carbide Steel leggings".equals(again.getName()),
				"read " + esc(again.getName()));

		// setLore(getLore()) must not change anything
		again.setLore(lore);
		String[] lore2 = new ItemNbt_v1_13_R2(again.getStack()).getLore();
		for (int i = 0; i < samples.length; i++) {
			expect("lore " + i + " survives setLore(getLore())", samples[i].equals(lore2[i]), "read " + esc(lore2[i]));
		}

		// JSON handed back (what ItemMeta.getLore() returns on 1.13) must not be wrapped again
		java.util.List<String> metaLore = made.getItemMeta().getLore();
		ItemNbt_v1_13_R2 viaMeta = new ItemNbt_v1_13_R2(new ItemStack(Material.IRON_LEGGINGS));
		for (String json : metaLore) {
			viaMeta.addLore(json);
		}
		String[] fromMeta = new ItemNbt_v1_13_R2(viaMeta.getStack()).getLore();
		for (int i = 0; i < samples.length; i++) {
			expect("lore " + i + " not double-wrapped via ItemMeta text", samples[i].equals(fromMeta[i]), "read " + esc(fromMeta[i]));
		}

		// 1.13 lore is stored as plain section-sign strings (JSON lore is 1.14): ItemMeta must see exactly what was written
		for (int i = 0; i < samples.length; i++) {
			expect("lore " + i + " stored as a plain string", samples[i].equals(metaLore.get(i)), "stored " + esc(metaLore.get(i)));
		}
		// the name is JSON and upright
		net.minecraft.server.v1_13_R2.ItemStack nms = org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack.asNMSCopy(made);
		String nameNbt = nms.getTag().getCompound("display").getString("Name");
		expect("name stored as upright JSON", nameNbt.startsWith("{") && nameNbt.contains("\"italic\":false"), nameNbt);
		System.out.println("name json : " + nameNbt);
		System.out.println(failures == 0 ? "ALL OK" : failures + " FAILURES");
	}
}
