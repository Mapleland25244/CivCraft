package gpl;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;

import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.nms.AttributeData;
import com.avrgaming.civcraft.nms.ItemNbt;
import com.avrgaming.civcraft.nms.Nms;
import com.avrgaming.civcraft.util.ItemManager;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * Item NBT access for CivCraft's custom items. The NBT work itself lives behind {@link ItemNbt}
 * (see the nms package); this class only keeps the API the rest of the plugin uses.
 */
public class AttributeUtil {
    public enum Operation {
        ADD_NUMBER(0),
        MULTIPLY_PERCENTAGE(1),
        ADD_PERCENTAGE(2);
        private int id;

        private Operation(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static Operation fromId(int id) {
            // Linear scan is very fast for small N
            for (Operation op : values()) {
                if (op.getId() == id) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Corrupt operation ID " + id + " detected.");
        }
    }

    public static class AttributeType {
        private static ConcurrentMap<String, AttributeType> LOOKUP = Maps.newConcurrentMap();
        public static final AttributeType GENERIC_MAX_HEALTH = new AttributeType("generic.maxHealth").register();
        public static final AttributeType GENERIC_FOLLOW_RANGE = new AttributeType("generic.followRange").register();
        public static final AttributeType GENERIC_ATTACK_DAMAGE = new AttributeType("generic.attackDamage").register();
        public static final AttributeType GENERIC_MOVEMENT_SPEED = new AttributeType("generic.movementSpeed").register();
        public static final AttributeType GENERIC_KNOCKBACK_RESISTANCE = new AttributeType("generic.knockbackResistance").register();

        private final String minecraftId;

        /**
         * Construct a new attribute type.
         * <p>
         * Remember to {@link #register()} the type.
         * @param minecraftId - the ID of the type.
         */
        public AttributeType(String minecraftId) {
            this.minecraftId = minecraftId;
        }

        /**
         * Retrieve the associated minecraft ID.
         * @return The associated ID.
         */
        public String getMinecraftId() {
            return minecraftId;
        }

        /**
         * Register the type in the central registry.
         * @return The registered type.
         */
        // Constructors should have no side-effects!
        public AttributeType register() {
            AttributeType old = LOOKUP.putIfAbsent(minecraftId, this);
            return old != null ? old : this;
        }

        /**
         * Retrieve the attribute type associated with a given ID.
         * @param minecraftId The ID to search for.
         * @return The attribute type, or NULL if not found.
         */
        public static AttributeType fromId(String minecraftId) {
            return LOOKUP.get(minecraftId);
        }

        /**
         * Retrieve every registered attribute type.
         * @return Every type.
         */
        public static Iterable<AttributeType> values() {
            return LOOKUP.values();
        }
    }

    public static class Attribute {
        private AttributeData data;

        private Attribute(Builder builder) {
            data = new AttributeData();
            setAmount(builder.amount);
            setOperation(builder.operation);
            setAttributeType(builder.type);
            setName(builder.name);
            setUUID(builder.uuid);
        }

        private Attribute(AttributeData data) {
            this.data = data;
        }

        public double getAmount() {
            return data.amount;
        }

        public void setAmount(double amount) {
            data.amount = amount;
        }

        public Operation getOperation() {
            return Operation.fromId(data.operation);
        }

        public void setOperation(@Nonnull Operation operation) {
            Preconditions.checkNotNull(operation, "operation cannot be NULL.");
            data.operation = operation.getId();
        }

        public AttributeType getAttributeType() {
            return AttributeType.fromId(data.typeId.replace("\"", ""));
        }

        public void setAttributeType(@Nonnull AttributeType type) {
            Preconditions.checkNotNull(type, "type cannot be NULL.");
            data.typeId = type.getMinecraftId();
        }

        public String getName() {
            return data.name.replace("\"", "");
        }

        public void setName(@Nonnull String name) {
            data.name = name;
        }

        public UUID getUUID() {
            return data.uuid;
        }

        public void setUUID(@Nonnull UUID id) {
            Preconditions.checkNotNull("id", "id cannot be NULL.");
            data.uuid = id;
        }

        /**
         * Construct a new attribute builder with a random UUID and default operation of adding numbers.
         * @return The attribute builder.
         */
        public static Builder newBuilder() {
            return new Builder().uuid(UUID.randomUUID()).operation(Operation.ADD_NUMBER);
        }

        // Makes it easier to construct an attribute
        public static class Builder {
            private double amount;
            private Operation operation = Operation.ADD_NUMBER;
            private AttributeType type;
            private String name;
            private UUID uuid;

            private Builder() {
                // Don't make this accessible
            }

            public Builder amount(double amount) {
                this.amount = amount;
                return this;
            }
            public Builder operation(Operation operation) {
                this.operation = operation;
                return this;
            }
            public Builder type(AttributeType type) {
                this.type = type;
                return this;
            }
            public Builder name(String name) {
                this.name = name;
                return this;
            }
            public Builder uuid(UUID uuid) {
                this.uuid = uuid;
                return this;
            }
            public Attribute build() {
                return new Attribute(this);
            }
        }
    }

    private final ItemNbt nbt;

    public AttributeUtil(ItemStack stack) {
        this.nbt = Nms.get().itemNbt(stack);
    }

    /**
     * Retrieve the modified item stack.
     * @return The modified item stack.
     */
    public ItemStack getStack() {
    	if (!nbt.hasStack()) {
    		return ItemManager.createItemStack(CivData.WOOL, 0);
    	}

        return nbt.getStack();
    }

    /**
     * Retrieve the number of attributes.
     * @return Number of attributes.
     */
    public int size() {
        return nbt.attributeCount();
    }

    /**
     * Add a new attribute to the list.
     * @param attribute - the new attribute.
     */
    public void add(Attribute attribute) {
        nbt.addAttribute(attribute.data);
    }

    /**
     * Remove the first instance of the given attribute.
     * <p>
     * The attribute will be removed using its UUID.
     * @param attribute - the attribute to remove.
     * @return TRUE if the attribute was removed, FALSE otherwise.
     */
    public boolean remove(Attribute attribute) {
        return nbt.removeAttribute(attribute.getUUID());
    }

    public void removeAll() {
        nbt.removeAllAttributes();
    }

    public void clear() {
        nbt.removeAllAttributes();
    }

    /**
     * Retrieve the attribute at a given index.
     * @param index - the index to look up.
     * @return The attribute at that index.
     */
    public Attribute get(int index) {
        return new Attribute(nbt.getAttribute(index));
    }

    /**
     * A snapshot of the attributes; changing the returned objects does not change the item.
     */
    public Iterable<Attribute> values() {
        List<Attribute> list = new LinkedList<Attribute>();
        for (AttributeData data : nbt.getAttributes()) {
            list.add(new Attribute(data));
        }
        return list;
    }

    public void addLore(String str) {
    	nbt.addLore(str);
    }

    public String[] getLore() {
    	return nbt.getLore();
    }

    public void setLore(String string) {
    	String[] strings = new String[1];
    	strings[0] = string;
    	setLore(strings);
    }

    public void setLore(String[] strings) {
    	nbt.setLore(strings);
    }

    public void addEnhancement(String enhancementName, String key, String value) {
    	if (enhancementName.equalsIgnoreCase("name")) {
    		throw new IllegalArgumentException();
    	}
    	if (key != null && key.equalsIgnoreCase("name")) {
    		throw new IllegalArgumentException();
    	}

    	nbt.addEnhancement(enhancementName, key, value);
    }

	public void setEnhancementData(String enhancementName, String key, String value) {
		addEnhancement(enhancementName, key, value);

	}

	public String getEnhancementData(String enhName, String key) {
		return nbt.getEnhancementData(enhName, key);
	}

	public LinkedList<LoreEnhancement> getEnhancements() {
		LinkedList<LoreEnhancement> returnList = new LinkedList<LoreEnhancement>();

		for (String name : nbt.getEnhancementNames()) {
			LoreEnhancement enh = LoreEnhancement.enhancements.get(name);
			if (enh != null) {
				returnList.add(enh);
			}
		}

		return returnList;
	}

    public boolean hasEnhancement(String enhName) {
    	return nbt.hasEnhancement(enhName);
	}

	public boolean hasEnhancements() {
		return nbt.hasEnhancements();
	}

    public void setCivCraftProperty(String key, String value) {
    	nbt.setCivCraftProperty(key, value);
    }

    public String getCivCraftProperty(String key) {
    	return nbt.getCivCraftProperty(key);
    }

	public void removeCivCraftProperty(String string) {
		nbt.removeCivCraftProperty(string);
	}

	public void setName(String name) {
		nbt.setName(name);
	}

	public String getName() {
		return nbt.getName();
	}

	public void setColor(Long long1) {
		nbt.setColor(long1);
	}

	public void setSkullOwner(String string) {
		nbt.setSkullOwner(string);
	}

	public void setHideFlag(int flags) {
		nbt.setHideFlag(flags);
	}

	public int getColor() {
		return nbt.getColor();
	}

	public boolean hasColor() {
		return nbt.hasColor();
	}

	public void setLore(LinkedList<String> lore) {
		String[] strs = new String[lore.size()];

		for (int i = 0; i < lore.size(); i++) {
			strs[i] = lore.get(i);
		}

		setLore(strs);
	}

	public void removeCivCraftCompound() {
		nbt.removeCivCraftCompound();
	}

	public boolean hasLegacyEnhancements() {
		return nbt.hasLegacyEnhancements();
	}

	public void addLore(String[] lore) {
		for (String str : lore) {
			addLore(str);
		}
	}


}
