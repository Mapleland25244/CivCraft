import com.avrgaming.civcraft.nms.v1_13_R2.ItemNbt_v1_13_R2;
import net.minecraft.server.v1_13_R2.DispenserRegistry;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Offline check: item_enhancements written by ItemNbt must be found again after the stack is reopened and cloned. */
public class EnhancementCheck {
	public static void main(String[] a) throws Exception {
		DispenserRegistry.c();
		org.bukkit.Server server = (org.bukkit.Server) java.lang.reflect.Proxy.newProxyInstance(
				EnhancementCheck.class.getClassLoader(), new Class<?>[] { org.bukkit.Server.class },
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

		ItemStack stack = new ItemStack(Material.IRON_LEGGINGS);
		ItemNbt_v1_13_R2 nbt = new ItemNbt_v1_13_R2(stack);
		nbt.addEnhancement("LoreEnhancementSoulBound", null, null);
		nbt.addEnhancement("LoreEnhancementDefense", "level", "2.0");
		ItemStack made = nbt.getStack();

		for (ItemStack s : new ItemStack[] { made, made.clone(), new ItemStack(made) }) {
			ItemNbt_v1_13_R2 again = new ItemNbt_v1_13_R2(s);
			System.out.println("hasEnhancements=" + again.hasEnhancements() + " names=" + again.getEnhancementNames()
					+ " hasSoulbound=" + again.hasEnhancement("LoreEnhancementSoulBound")
					+ " defenseLevel=" + again.getEnhancementData("LoreEnhancementDefense", "level"));
		}
	}
}
