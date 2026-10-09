import net.minecraft.server.v1_13_R2.*;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftChatMessage;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;

/** Offline check: how name and lore written as JSON NBT survive a Bukkit ItemMeta round trip on 1.13.2. */
public class LoreRoundTripCheck {
	static String enc(String s) {
		IChatBaseComponent c = CraftChatMessage.fromStringOrNull(s);
		return c == null ? "" : IChatBaseComponent.ChatSerializer.a(c);
	}

	static String raw(ItemStack st) {
		net.minecraft.server.v1_13_R2.ItemStack n = CraftItemStack.asNMSCopy(st);
		return String.valueOf(n.getTag());
	}

	public static void main(String[] a) throws Exception {
		DispenserRegistry.c();
		org.bukkit.Server server = (org.bukkit.Server) java.lang.reflect.Proxy.newProxyInstance(
				LoreRoundTripCheck.class.getClassLoader(), new Class<?>[] { org.bukkit.Server.class },
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
		ItemStack st = new ItemStack(Material.IRON_LEGGINGS);
		net.minecraft.server.v1_13_R2.ItemStack n = CraftItemStack.asNMSCopy(st);
		NBTTagCompound tag = new NBTTagCompound();
		NBTTagCompound disp = new NBTTagCompound();
		NBTTagList lore = new NBTTagList();
		lore.add(new NBTTagString(enc("§dGear Tier 3")));
		lore.add(new NBTTagString(enc("§3" + "4.0 Defense Bonus")));
		disp.set("Lore", lore);
		disp.set("Name", new NBTTagString(enc("§r" + "Carbide Steel leggings")));
		tag.set("display", disp);
		n.setTag(tag);
		ItemStack bukkit = CraftItemStack.asBukkitCopy(n);
		System.out.println("1 NBT written : " + raw(bukkit));
		ItemMeta meta = bukkit.getItemMeta();
		System.out.println("2 meta lore   : " + meta.getLore());
		System.out.println("2 meta name   : " + meta.getDisplayName());
		bukkit.setItemMeta(meta);
		System.out.println("3 after meta  : " + raw(bukkit));

		ItemMeta m2 = bukkit.getItemMeta();
		m2.setLore(m2.getLore());
		ItemStack s2 = bukkit.clone();
		s2.setItemMeta(m2);
		System.out.println("4 setLore(getLore()) : " + raw(s2));

		ItemMeta m3 = bukkit.getItemMeta();
		m3.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, false);
		m3.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
		ItemStack s3 = bukkit.clone();
		s3.setItemMeta(m3);
		System.out.println("5 addGlow     : " + raw(s3));

		ItemMeta m4 = bukkit.getItemMeta();
		m4.setLore(java.util.Arrays.asList("§dplain lore"));
		ItemStack s4 = bukkit.clone();
		s4.setItemMeta(m4);
		System.out.println("6 plain setLore: " + raw(s4));
	}
}
