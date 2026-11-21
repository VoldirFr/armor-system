package fr.voldir.armorsystem.core;

public enum ArmorTier {
    ULTRA_LIGHT(0, "ultra_light"),
    VERY_LIGHT(1, "very_light"),
    LIGHT(2, "light"),
    LIGHT_MEDIUM(3, "light_medium"),
    MEDIUM(4, "medium"),
    HEAVY_MEDIUM(5, "heavy_medium"),
    HEAVY(6, "heavy"),
    VERY_HEAVY(7, "very_heavy"),
    ULTRA_HEAVY(8, "ultra_heavy");

    private final int index;
    private final String id;

    ArmorTier(int index, String id) {
        this.index = index;
        this.id = id;
    }

    public int index() {
        return index;
    }

    public String id() {
        return id;
    }

    public static ArmorTier byIndex(int index) {
        for (ArmorTier tier : values()) {
            if (tier.index == index) return tier;
        }
        return null;
    }
}
