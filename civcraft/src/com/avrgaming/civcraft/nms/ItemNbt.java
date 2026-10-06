package com.avrgaming.civcraft.nms;

import java.util.List;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

/**
 * NBT view of one item stack copy. The NBT keys (display, AttributeModifiers, civcraft, item_enhancements,
 * civ_enhancements, HideFlags, SkullOwner) are part of the stored format of every existing player item,
 * so implementations must keep reading and writing exactly these keys.
 */
public interface ItemNbt {

	/** False when the server could not make an NMS copy of the stack (e.g. air); most operations are then no-ops or fail. */
	boolean hasStack();

	/** The modified stack. Removes an empty AttributeModifiers list first. */
	ItemStack getStack();

	// AttributeModifiers
	int attributeCount();

	void addAttribute(AttributeData attribute);

	AttributeData getAttribute(int index);

	List<AttributeData> getAttributes();

	/** Removes the first entry with this UUID. */
	boolean removeAttribute(UUID uuid);

	void removeAllAttributes();

	// display
	void addLore(String line);

	/** Null when the item has no lore. */
	String[] getLore();

	void setLore(String[] lines);

	void setName(String name);

	String getName();

	void setColor(long color);

	int getColor();

	boolean hasColor();

	void setSkullOwner(String name);

	void setHideFlag(int flags);

	// item_enhancements
	void addEnhancement(String enhancementName, String key, String value);

	boolean hasEnhancement(String enhancementName);

	boolean hasEnhancements();

	String getEnhancementData(String enhancementName, String key);

	/** The "name" value of every entry under item_enhancements. */
	List<String> getEnhancementNames();

	boolean hasLegacyEnhancements();

	// civcraft
	void setCivCraftProperty(String key, String value);

	String getCivCraftProperty(String key);

	void removeCivCraftProperty(String key);

	void removeCivCraftCompound();
}
