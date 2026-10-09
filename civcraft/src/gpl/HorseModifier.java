package gpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivLog;

/**
* HorseModifier v1.1 (DarkBlade12), rewritten on the Bukkit API: since 1.11 horses, donkeys and mules are
* real entity types with API methods, so no server internals are needed any more.
*/
public class HorseModifier {
    private AbstractHorse horse;

    public static String HORSE_META = "civcrafthorse";
    private static final UUID movementSpeedUID = UUID.fromString("206a89dc-ae78-4c4d-b42c-3b31db3f5a7c");

    /**
    * Wraps an existing horse, donkey or mule.
    */
    public HorseModifier(LivingEntity entity) {
        if (!HorseModifier.isHorse(entity)) {
            throw new IllegalArgumentException("Entity has to be a horse!");
        }
        this.horse = (AbstractHorse) entity;
    }

    private HorseModifier(AbstractHorse horse) {
        this.horse = horse;
    }

    /**
    * Spawns a normal horse at a given location
    */
    public static HorseModifier spawn(Location loc) {
        return spawn(loc, HorseType.NORMAL);
    }

    /**
    * Spawns a horse of the given type at a given location. Null if the world refused the entity.
    */
    public static HorseModifier spawn(Location loc, HorseType type) {
        try {
            AbstractHorse spawned = (AbstractHorse) loc.getWorld().spawnEntity(loc, type.getEntityType());
            return new HorseModifier(spawned);
        } catch (Exception e) {
            CivLog.warning("Could not spawn a "+type.getName()+" at "+loc+": "+e.getMessage());
            return null;
        }
    }

    /**
    * Checks if an entity is a horse, donkey or mule
    */
    public static boolean isHorse(LivingEntity le) {
        return le instanceof AbstractHorse;
    }


    public static void setHorseSpeed(LivingEntity entity, double amount) {
    	if (!isHorse(entity)) {
    		return;
    	}

    	AttributeInstance speed = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
    	removeSpeedModifier(speed);
    	speed.addModifier(new AttributeModifier(movementSpeedUID, "civcraft horse movement speed", amount, AttributeModifier.Operation.ADD_NUMBER));
    }

    private static void removeSpeedModifier(AttributeInstance speed) {
    	for (AttributeModifier modifier : speed.getModifiers()) {
    		if (modifier.getUniqueId().equals(movementSpeedUID)) {
    			speed.removeModifier(modifier);
    		}
    	}
    }

    private static boolean hasSpeedModifier(LivingEntity entity) {
    	for (AttributeModifier modifier : entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getModifiers()) {
    		if (modifier.getUniqueId().equals(movementSpeedUID)) {
    			return true;
    		}
    	}
    	return false;
    }

    public static void setCivCraftHorse(LivingEntity entity) {
    	entity.setMetadata(HorseModifier.HORSE_META, new FixedMetadataValue(CivCraft.getPlugin(), HorseModifier.HORSE_META));
    }

    public static boolean isCivCraftHorse(LivingEntity entity) {
    	/* The speed modifier Stable gives every horse it sells is saved with the horse; the metadata flag is not saved
    	   (and nothing ever set it), so requiring it would refuse every horse after a restart. */

    	if (!isHorse(entity)) {
    		CivLog.debug("Player tried using Horse that isn't a Horse? Error in HorseModifier.java.");
    		return false;
    	}

    	return hasSpeedModifier(entity);
    }

    /**
    * Changes the color variant of the horse (only for normal horses; mules and donkeys have none)
    */
    public void setVariant(HorseVariant variant) {
        if (!(horse instanceof Horse)) {
            return;
        }
        // old variant id: low byte = color, next byte = style (marking)
        int color = variant.getId() & 0xFF;
        int style = variant.getId() >> 8;
        Horse.Color[] colors = Horse.Color.values();
        Horse.Style[] styles = Horse.Style.values();
        ((Horse) horse).setColor(color < colors.length ? colors[color] : Horse.Color.WHITE);
        ((Horse) horse).setStyle(style < styles.length ? styles[style] : Horse.Style.NONE);
    }

    /**
    * Changes the temper of the horse
    */
    public void setTemper(int temper) {
        horse.setDomestication(temper);
    }

    /**
    * Changes whether the horse is tamed or not
    */
    public void setTamed(boolean tamed) {
        horse.setTamed(tamed);
    }

    /**
    * Changes whether the horse is saddled or not
    */
    public void setSaddled(boolean saddled) {
        horse.getInventory().setSaddle(saddled ? new ItemStack(Material.SADDLE) : null);
    }

    /**
    * Sets the armor item of the horse (only for normal horses)
    */
    public void setArmorItem(ItemStack i) {
        if (horse instanceof Horse) {
            ((Horse) horse).getInventory().setArmor(i);
        }
    }

    /**
    * Returns whether the horse is tamed or not
    */
    public boolean isTamed() {
        return horse.isTamed();
    }

    /**
    * Returns whether the horse is saddled or not
    */
    public boolean isSaddled() {
        return horse.getInventory().getSaddle() != null;
    }

    /**
    * Opens the inventory of the horse for a player (only for tamed horses)
    */
    public void openInventory(Player p) {
        p.openInventory(horse.getInventory());
    }

    /**
    * Returns the horse entity
    */
    public AbstractHorse getHorse() {
        return horse;
    }

    public enum HorseType {

        NORMAL("normal", 0, EntityType.HORSE), DONKEY("donkey", 1, EntityType.DONKEY), MULE("mule", 2, EntityType.MULE),
        UNDEAD("undead", 3, EntityType.ZOMBIE_HORSE), SKELETAL("skeletal", 4, EntityType.SKELETON_HORSE);

        private String name;
        private int id;
        private EntityType entityType;

        HorseType(String name, int id, EntityType entityType) {
            this.name = name;
            this.id = id;
            this.entityType = entityType;
        }

        public String getName() {
            return name;
        }

        public int getId() {
            return id;
        }

        public EntityType getEntityType() {
            return entityType;
        }

        private static final Map<String, HorseType> NAME_MAP = new HashMap<String, HorseType>();
        private static final Map<Integer, HorseType> ID_MAP = new HashMap<Integer, HorseType>();
        static {
            for (HorseType effect : values()) {
                NAME_MAP.put(effect.name, effect);
                ID_MAP.put(effect.id, effect);
            }
        }

        public static HorseType fromName(String name) {
            if (name == null) {
                return null;
            }
            for (Entry<String, HorseType> e : NAME_MAP.entrySet()) {
                if (e.getKey().equalsIgnoreCase(name)) {
                    return e.getValue();
                }
            }
            return null;
        }

        public static HorseType fromId(int id) {
            return ID_MAP.get(id);
        }
    }

    public enum HorseVariant {
        WHITE("white", 0), CREAMY("creamy", 1), CHESTNUT("chestnut", 2), BROWN("brown", 3), BLACK("black", 4), GRAY("gray", 5), DARK_BROWN("dark brown", 6), INVISIBLE("invisible", 7), WHITE_WHITE(
                "white-white", 256), CREAMY_WHITE("creamy-white", 257), CHESTNUT_WHITE("chestnut-white", 258), BROWN_WHITE("brown-white", 259), BLACK_WHITE("black-white", 260), GRAY_WHITE("gray-white", 261), DARK_BROWN_WHITE(
                "dark brown-white", 262), WHITE_WHITE_FIELD("white-white field", 512), CREAMY_WHITE_FIELD("creamy-white field", 513), CHESTNUT_WHITE_FIELD("chestnut-white field", 514), BROWN_WHITE_FIELD(
                "brown-white field", 515), BLACK_WHITE_FIELD("black-white field", 516), GRAY_WHITE_FIELD("gray-white field", 517), DARK_BROWN_WHITE_FIELD("dark brown-white field", 518), WHITE_WHITE_DOTS(
                "white-white dots", 768), CREAMY_WHITE_DOTS("creamy-white dots", 769), CHESTNUT_WHITE_DOTS("chestnut-white dots", 770), BROWN_WHITE_DOTS("brown-white dots", 771), BLACK_WHITE_DOTS(
                "black-white dots", 772), GRAY_WHITE_DOTS("gray-white dots", 773), DARK_BROWN_WHITE_DOTS("dark brown-white dots", 774), WHITE_BLACK_DOTS("white-black dots", 1024), CREAMY_BLACK_DOTS(
                "creamy-black dots", 1025), CHESTNUT_BLACK_DOTS("chestnut-black dots", 1026), BROWN_BLACK_DOTS("brown-black dots", 1027), BLACK_BLACK_DOTS("black-black dots", 1028), GRAY_BLACK_DOTS(
                "gray-black dots", 1029), DARK_BROWN_BLACK_DOTS("dark brown-black dots", 1030);

        private String name;
        private int id;

        HorseVariant(String name, int id) {
            this.name = name;
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public int getId() {
            return id;
        }

        private static final Map<String, HorseVariant> NAME_MAP = new HashMap<String, HorseVariant>();
        private static final Map<Integer, HorseVariant> ID_MAP = new HashMap<Integer, HorseVariant>();
        static {
            for (HorseVariant effect : values()) {
                NAME_MAP.put(effect.name, effect);
                ID_MAP.put(effect.id, effect);
            }
        }

        public static HorseVariant fromName(String name) {
            if (name == null) {
                return null;
            }
            for (Entry<String, HorseVariant> e : NAME_MAP.entrySet()) {
                if (e.getKey().equalsIgnoreCase(name)) {
                    return e.getValue();
                }
            }
            return null;
        }

        public static HorseVariant fromId(int id) {
            return ID_MAP.get(id);
        }
    }
}
