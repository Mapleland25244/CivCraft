package com.avrgaming.civcraft.nms.v1_13_R2;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.nms.AttributeData;
import com.avrgaming.civcraft.nms.ItemNbt;

import org.bukkit.craftbukkit.v1_13_R2.util.CraftChatMessage;
import net.minecraft.server.v1_13_R2.IChatBaseComponent;
import net.minecraft.server.v1_13_R2.NBTBase;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.NBTTagInt;
import net.minecraft.server.v1_13_R2.NBTTagList;
import net.minecraft.server.v1_13_R2.NBTTagString;

/**
 * The NBT half of gpl.AttributeUtil (originally ProtocolLib's AttributeStorage example), moved here unchanged in
 * behavior, including the NBT keys. Existing items store their data under these keys, so they must not change.
 */
public class ItemNbt_v1_13_R2 implements ItemNbt {

	// NBT type ids used by getList
	private static final int TAG_STRING = 8;
	private static final int TAG_COMPOUND = 10;

	private net.minecraft.server.v1_13_R2.ItemStack nmsStack;
	private NBTTagCompound parent;
	private NBTTagList attributes;

	public ItemNbt_v1_13_R2(ItemStack stack) {
		// Create a CraftItemStack (under the hood)
		this.nmsStack = CraftItemStack.asNMSCopy(stack);

		if (this.nmsStack == null) {
			return;
		}

		// Load NBT
		if (nmsStack.getTag() == null) {
			parent = new NBTTagCompound();
			nmsStack.setTag(parent);
		} else {
			parent = nmsStack.getTag();
		}

		// Load attribute list
		if (parent.hasKey("AttributeModifiers")) {
			attributes = parent.getList("AttributeModifiers", TAG_COMPOUND);
		} else {
			/* No attributes on this item detected. */
			attributes = new NBTTagList();
			parent.set("AttributeModifiers", attributes);
		}
	}

	@Override
	public boolean hasStack() {
		return nmsStack != null;
	}

	@Override
	public ItemStack getStack() {
		if (nmsStack.getTag() != null) {
			if (attributes.size() == 0) {
				parent.remove("AttributeModifiers");
			}
		}

		return CraftItemStack.asCraftMirror(nmsStack);
	}

	// ---- AttributeModifiers ----

	@Override
	public int attributeCount() {
		return attributes.size();
	}

	@Override
	public void addAttribute(AttributeData a) {
		NBTTagCompound data = new NBTTagCompound();
		data.setDouble("Amount", a.amount);
		data.setInt("Operation", a.operation);
		data.setString("AttributeName", a.typeId);
		data.setString("Name", a.name);
		data.setLong("UUIDLeast", a.uuid.getLeastSignificantBits());
		data.setLong("UUIDMost", a.uuid.getMostSignificantBits());
		attributes.add(data);
	}

	private static AttributeData toData(NBTTagCompound data) {
		return new AttributeData(data.getDouble("Amount"), data.getInt("Operation"), data.getString("AttributeName"),
				data.getString("Name"), new UUID(data.getLong("UUIDMost"), data.getLong("UUIDLeast")));
	}

	@Override
	public AttributeData getAttribute(int index) {
		return toData((NBTTagCompound) attributes.get(index));
	}

	@Override
	public List<AttributeData> getAttributes() {
		List<AttributeData> list = new ArrayList<AttributeData>();
		for (int i = 0; i < attributes.size(); i++) {
			list.add(getAttribute(i));
		}
		return list;
	}

	@Override
	public boolean removeAttribute(UUID uuid) {
		for (int i = 0; i < attributes.size(); i++) {
			if (getAttribute(i).uuid.equals(uuid)) {
				attributes.remove(i);
				return true;
			}
		}
		return false;
	}

	@Override
	public void removeAllAttributes() {
		attributes = new NBTTagList();
		if (parent != null) {
			parent.set("AttributeModifiers", attributes);
		}
	}

	// ---- display ----

	/*
	 * 1.13 stores the display name as a JSON text component, but lore is still a list of plain section-sign strings
	 * (lore became JSON in 1.14). CivCraft works with section-sign strings, so only the name is converted.
	 */
	private static String encodeName(String legacyText) {
		if (legacyText == null) {
			return "";
		}
		if (isJsonComponent(legacyText)) {
			// already a stored component (read straight from NBT or ItemMeta): wrapping it again would show the JSON as text
			return legacyText;
		}
		IChatBaseComponent component = CraftChatMessage.fromStringOrNull(legacyText);
		if (component == null) {
			return "";
		}
		// 1.12 showed names upright (a color code clears italics); 1.13 JSON text is italic unless told otherwise
		component.getChatModifier().setItalic(false);
		return IChatBaseComponent.ChatSerializer.a(component);
	}

	private static boolean isJsonComponent(String text) {
		if (!text.startsWith("{\"")) {
			return false;
		}
		try {
			return IChatBaseComponent.ChatSerializer.a(text) != null;
		} catch (Exception e) {
			return false;
		}
	}

	/** Reads a stored name (JSON) or lore line (plain; JSON only on items made by a few early 1.13 test builds). */
	private static String decodeText(String stored) {
		if (stored == null) {
			return "";
		}
		if (stored.startsWith("{") || stored.startsWith("[") || stored.startsWith("\"")) {
			try {
				IChatBaseComponent component = IChatBaseComponent.ChatSerializer.a(stored);
				if (component != null) {
					return CraftChatMessage.fromComponent(component);
				}
			} catch (Exception e) {
				// not JSON after all: return it as written
			}
		}
		return stored;
	}

	@Override
	public void addLore(String str) {
		if (nmsStack == null) {
			return;
		}

		if (nmsStack.getTag() == null) {
			nmsStack.setTag(new NBTTagCompound());
		}
		NBTTagCompound displayCompound = nmsStack.getTag().getCompound("display");

		if (displayCompound == null) {
			displayCompound = new NBTTagCompound();
		}

		NBTTagList loreList = displayCompound.getList("Lore", TAG_STRING);
		if (loreList == null) {
			loreList = new NBTTagList();
		}

		loreList.add(new NBTTagString(str == null ? "" : str));
		displayCompound.set("Lore", loreList);
		nmsStack.getTag().set("display", displayCompound);
	}

	@Override
	public String[] getLore() {
		if (nmsStack == null) {
			return null;
		}

		if (nmsStack.getTag() == null) {
			return null;
		}

		NBTTagCompound displayCompound = nmsStack.getTag().getCompound("display");

		if (displayCompound == null) {
			return null;
		}

		NBTTagList loreList = displayCompound.getList("Lore", TAG_STRING);
		if (loreList == null) {
			return null;
		}

		if (loreList.size() < 1) {
			return null;
		}

		String[] lore = new String[loreList.size()];
		for (int i = 0; i < loreList.size(); i++) {
			lore[i] = decodeText(loreList.getString(i));
		}

		return lore;
	}

	@Override
	public void setLore(String[] strings) {
		NBTTagCompound displayCompound = nmsStack.getTag().getCompound("display");

		if (displayCompound == null) {
			displayCompound = new NBTTagCompound();
		}

		NBTTagList loreList = new NBTTagList();

		for (String str : strings) {
			loreList.add(new NBTTagString(str == null ? "" : str));
		}

		displayCompound.set("Lore", loreList);
		nmsStack.getTag().set("display", displayCompound);
	}

	@Override
	public void setName(String name) {
		if (nmsStack == null) {
			return;
		}

		if (nmsStack.getTag() == null) {
			nmsStack.setTag(new NBTTagCompound());
		}

		NBTTagCompound displayCompound = nmsStack.getTag().getCompound("display");

		if (displayCompound == null) {
			displayCompound = new NBTTagCompound();
		}

		displayCompound.set("Name", new NBTTagString(encodeName(ChatColor.RESET + name)));
		nmsStack.getTag().set("display", displayCompound);
	}

	@Override
	public String getName() {
		NBTTagCompound displayCompound = nmsStack.getTag().getCompound("display");

		if (displayCompound == null) {
			displayCompound = new NBTTagCompound();
		}

		return decodeText(displayCompound.getString("Name"));
	}

	@Override
	public void setColor(long color) {
		NBTTagCompound displayCompound = nmsStack.getTag().getCompound("display");

		if (displayCompound == null) {
			displayCompound = new NBTTagCompound();
		}

		displayCompound.set("color", new NBTTagInt((int) color));
		nmsStack.getTag().set("display", displayCompound);
	}

	@Override
	public int getColor() {
		NBTTagCompound displayCompound = nmsStack.getTag().getCompound("display");
		if (displayCompound == null) {
			return 0;
		}

		return displayCompound.getInt("color");
	}

	@Override
	public boolean hasColor() {
		if (nmsStack == null) {
			return false;
		}

		if (nmsStack.getTag() == null) {
			return false;
		}

		NBTTagCompound displayCompound = nmsStack.getTag().getCompound("display");
		if (displayCompound == null) {
			return false;
		}

		return displayCompound.hasKey("color");
	}

	@Override
	public void setSkullOwner(String string) {
		if (nmsStack == null) {
			return;
		}

		NBTTagCompound skullCompound = nmsStack.getTag().getCompound("SkullOwner");
		if (skullCompound == null) {
			skullCompound = new NBTTagCompound();
		}

		skullCompound.set("Name", new NBTTagString(string));
		nmsStack.getTag().set("SkullOwner", skullCompound);
	}

	@Override
	public void setHideFlag(int flags) {
		if (nmsStack == null) {
			return;
		}

		nmsStack.getTag().setInt("HideFlags", flags);
	}

	// ---- item_enhancements ----

	@Override
	public void addEnhancement(String enhancementName, String key, String value) {
		NBTTagCompound compound = nmsStack.getTag().getCompound("item_enhancements");

		if (compound == null) {
			compound = new NBTTagCompound();
		}

		NBTTagCompound enhCompound = compound.getCompound(enhancementName);
		if (enhCompound == null) {
			enhCompound = new NBTTagCompound();
		}

		if (key != null) {
			enhCompound.set(key, new NBTTagString(value));
		}
		enhCompound.set("name", new NBTTagString(enhancementName));

		compound.set(enhancementName, enhCompound);
		nmsStack.getTag().set("item_enhancements", compound);
	}

	@Override
	public String getEnhancementData(String enhName, String key) {
		if (!hasEnhancement(enhName)) {
			return null;
		}

		NBTTagCompound compound = nmsStack.getTag().getCompound("item_enhancements");
		NBTTagCompound enhCompound = compound.getCompound(enhName);

		if (!enhCompound.hasKey(key)) {
			return null;
		}

		return enhCompound.getString(key);
	}

	@Override
	public List<String> getEnhancementNames() {
		List<String> names = new ArrayList<String>();

		if (!hasEnhancements()) {
			return names;
		}

		NBTTagCompound compound = nmsStack.getTag().getCompound("item_enhancements");

		for (String key : compound.getKeys()) {
			Object obj = compound.get(key);

			if (obj instanceof NBTTagCompound) {
				NBTTagCompound enhCompound = (NBTTagCompound) obj;
				names.add(enhCompound.getString("name").replace("\"", ""));
			}
		}

		return names;
	}

	@Override
	public boolean hasEnhancement(String enhName) {
		NBTTagCompound compound = nmsStack.getTag().getCompound("item_enhancements");
		if (compound == null) {
			return false;
		}

		return compound.hasKey(enhName);
	}

	@Override
	public boolean hasEnhancements() {
		if (nmsStack == null) {
			return false;
		}

		if (nmsStack.getTag() == null) {
			return false;
		}

		return nmsStack.getTag().hasKey("item_enhancements");
	}

	@Override
	public boolean hasLegacyEnhancements() {
		if (nmsStack == null) {
			return false;
		}

		if (nmsStack.getTag() == null) {
			return false;
		}

		return nmsStack.getTag().hasKey("civ_enhancements");
	}

	// ---- civcraft ----

	@Override
	public void setCivCraftProperty(String key, String value) {
		if (nmsStack == null) {
			return;
		}

		if (nmsStack.getTag() == null) {
			nmsStack.setTag(new NBTTagCompound());
		}

		NBTTagCompound civcraftCompound = nmsStack.getTag().getCompound("civcraft");

		if (civcraftCompound == null) {
			civcraftCompound = new NBTTagCompound();
		}

		civcraftCompound.set(key, new NBTTagString(value));
		nmsStack.getTag().set("civcraft", civcraftCompound);
	}

	@Override
	public String getCivCraftProperty(String key) {
		if (nmsStack == null) {
			return null;
		}
		NBTTagCompound civcraftCompound = nmsStack.getTag().getCompound("civcraft");

		if (civcraftCompound == null) {
			return null;
		}

		NBTBase strTag = civcraftCompound.get(key);
		if (strTag == null) {
			return null;
		}

		return ((NBTTagString) strTag).toString().replace("\"", "");
	}

	@Override
	public void removeCivCraftProperty(String string) {
		if (nmsStack == null) {
			return;
		}

		NBTTagCompound civcraftCompound = nmsStack.getTag().getCompound("civcraft");
		if (civcraftCompound == null) {
			return;
		}

		civcraftCompound.remove(string);

		if (civcraftCompound.isEmpty()) {
			removeCivCraftCompound();
		}
	}

	@Override
	public void removeCivCraftCompound() {
		if (nmsStack == null) {
			return;
		}

		NBTTagCompound civcraftCompound = nmsStack.getTag().getCompound("civcraft");
		if (civcraftCompound == null) {
			return;
		}

		nmsStack.getTag().remove("civcraft");
	}
}
