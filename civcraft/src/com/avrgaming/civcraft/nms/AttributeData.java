package com.avrgaming.civcraft.nms;

import java.util.UUID;

/** One entry of an item's AttributeModifiers list, without any server types. */
public class AttributeData {
	public double amount;
	public int operation;
	public String typeId;
	public String name;
	public UUID uuid;

	public AttributeData() {
	}

	public AttributeData(double amount, int operation, String typeId, String name, UUID uuid) {
		this.amount = amount;
		this.operation = operation;
		this.typeId = typeId;
		this.name = name;
		this.uuid = uuid;
	}
}
