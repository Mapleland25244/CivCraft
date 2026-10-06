package gpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.nms.HorseAccess;
import com.avrgaming.civcraft.nms.Nms;
 
/**
* HorseModifier v1.1
*
* You are free to use it, modify it and redistribute it under the condition to give credit to me
*
* @author DarkBlade12
*
* The server-specific part (NBT through reflection, attribute modifiers) now lives behind HorseAccess in the nms package.
*/
public class HorseModifier {
    private HorseAccess access;
    
    public static String HORSE_META = "civcrafthorse";
    private static final UUID movementSpeedUID = UUID.fromString("206a89dc-ae78-4c4d-b42c-3b31db3f5a7c");
 
    /**
    * Creates a new instance of the HorseModifier, which allows you to change/get values of horses which aren't accessible with the bukkit api atm
    */
    public HorseModifier(LivingEntity horse) {
        if (!HorseModifier.isHorse(horse)) {
            throw new IllegalArgumentException("Entity has to be a horse!");
        }
        this.access = Nms.get().wrapHorse(horse);
    }
 
    /**
    * Creates a new instance of the HorseModifier; This constructor is only used for the static spawn method
    */
    private HorseModifier(HorseAccess access) {
        this.access = access;
    }
 
    /**
    * Spawns a horse at a given location
    */
    public static HorseModifier spawn(Location loc) {
        HorseAccess access = Nms.get().spawnHorse(loc);
        if (access == null) {
            return null;
        }
        return new HorseModifier(access);
    }
 
    /**
    * Checks if an entity is a horse
    */
    public static boolean isHorse(LivingEntity le) {
        return Nms.get().isHorse(le);
    }
 
    
    public static void setHorseSpeed(LivingEntity entity, double amount) {
    	if (!isHorse(entity)) {
    		return;
    	}
    	
    	Nms.get().setHorseSpeedModifier(entity, movementSpeedUID, "civcraft horse movement speed", amount);
    }
    
    public static void setCivCraftHorse(LivingEntity entity) {
    	entity.setMetadata(HorseModifier.HORSE_META, new FixedMetadataValue(CivCraft.getPlugin(), HorseModifier.HORSE_META));
    }
    
    public static boolean isCivCraftHorse(LivingEntity entity) {
    	if (!entity.hasMetadata(HORSE_META)) {
    		CivLog.debug("Player tried using Horse without meta: "+HORSE_META);
    		return false;
    	}
    	
    	if (!isHorse(entity)) {
    		CivLog.debug("Player tried using Horse that isn't a Horse? Error in HorseModifier.java.");
    		return false;
    	}
    	
    	return Nms.get().hasHorseSpeedModifier(entity, movementSpeedUID);
    }
    
    /**
    * Changes the type of the horse
    */
    public void setType(HorseType type) {
        access.setInt("Type", type.getId());
    }
 
    /**
    * Changes whether the horse is chested or not (only for donkeys and mules)
    */
    public void setChested(boolean chested) {
        access.setBoolean("ChestedHorse", chested);
    }
 
    /**
    * Changes whether the horse is eating or not
    */
    public void setEating(boolean eating) {
        access.setBoolean("EatingHaystack", eating);
    }
 
    /**
    * Changes whether the horse was bred or not
    */
    public void setBred(boolean bred) {
        access.setBoolean("Bred", bred);
    }
 
    /**
    * Changes the color variant of the horse (only for normal horses)
    */
    public void setVariant(HorseVariant variant) {
        access.setInt("Variant", variant.getId());
    }
 
    /**
    * Changes the temper of the horse
    */
    public void setTemper(int temper) {
        access.setInt("Temper", temper);
    }
 
    /**
    * Changes whether the horse is tamed or not
    */
    public void setTamed(boolean tamed) {
        access.setBoolean("Tame", tamed);
    }
 
    /**
    * Changes whether the horse is saddled or not
    */
    public void setSaddled(boolean saddled) {
        access.setBoolean("Saddle", saddled);
    }
 
    /**
    * Sets the armor item of the horse (only for normal horses)
    */
    public void setArmorItem(ItemStack i) {
        access.setArmorItem(i);
    }
 
    /**
    * Returns the type of the horse
    */
    public HorseType getType() {
        return HorseType.fromId(access.getInt("Type"));
    }
 
    /**
    * Returns whether the horse is chested or not
    */
    public boolean isChested() {
        return access.getBoolean("ChestedHorse");
    }
 
    /**
    * Returns whether the horse is eating or not
    */
    public boolean isEating() {
        return access.getBoolean("EatingHaystack");
    }
 
    /**
    * Returns whether the horse was bred or not
    */
    public boolean isBred() {
        return access.getBoolean("Bred");
    }
 
    /**
    * Returns the variant of the horse
    */
    public HorseVariant getVariant() {
        return HorseVariant.fromId(access.getInt("Variant"));
    }
 
    /**
    * Returns the temper of the horse
    */
    public int getTemper() {
        return access.getInt("Temper");
    }
 
    /**
    * Returns whether the horse is tamed or not
    */
    public boolean isTamed() {
        return access.getBoolean("Tame");
    }
 
    /**
    * Returns whether the horse is saddled or not
    */
    public boolean isSaddled() {
        return access.getBoolean("Saddle");
    }
 
    /**
    * Returns the armor item of the horse
    */
    public ItemStack getArmorItem() {
        return access.getArmorItem();
    }
 
    /**
    * Opens the inventory of the horse for a player (only for tamed horses)
    */
    public void openInventory(Player p) {
        access.openInventory(p);
    }
 
    /**
    * Returns the horse entity
    */
    public LivingEntity getHorse() {
        return access.getHorse();
    }
 
    public enum HorseType {
 
        NORMAL("normal", 0), DONKEY("donkey", 1), MULE("mule", 2), UNDEAD("undead", 3), SKELETAL("skeletal", 4);
 
        private String name;
        private int id;
 
        HorseType(String name, int id) {
            this.name = name;
            this.id = id;
        }
 
        public String getName() {
            return name;
        }
 
        public int getId() {
            return id;
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
