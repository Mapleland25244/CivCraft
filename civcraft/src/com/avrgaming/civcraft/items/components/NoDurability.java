package com.avrgaming.civcraft.items.components;

import com.avrgaming.civcraft.util.ItemManager;
import gpl.AttributeUtil;

import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

public class NoDurability extends ItemComponent {

	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {		
	}
	
	@Override
	public void onInventoryOpen(InventoryOpenEvent event, ItemStack stack) {
		ItemManager.setDamage(stack, (short) 0);		
	}

}
