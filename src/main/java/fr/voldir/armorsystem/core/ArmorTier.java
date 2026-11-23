package fr.voldir.armorsystem.core;

public enum ArmorTier {
    ULTRA_LIGHT(0, "ultra_light", ModelClass.LIGHT),
    VERY_LIGHT(1, "very_light", ModelClass.LIGHT),
    LIGHT(2, "light", ModelClass.LIGHT),
    LIGHT_MEDIUM(3, "light_medium", ModelClass.MEDIUM),
    MEDIUM(4, "medium", ModelClass.MEDIUM),
    HEAVY_MEDIUM(5, "heavy_medium", ModelClass.MEDIUM),
    HEAVY(6, "heavy", ModelClass.HEAVY),
    VERY_HEAVY(7, "very_heavy", ModelClass.HEAVY),
    ULTRA_HEAVY(8, "ultra_heavy", ModelClass.HEAVY);

    /** Model class family: each has a custom Bedrock geometry + 2 texture layers (shift + layer1). */
    public enum ModelClass {
        LIGHT("light", "tempest_robe"),
        MEDIUM("medium", "archer_armor"),
        HEAVY("heavy", "paladin_armor");

        private final String id;
        private final String geometryName;

        ModelClass(String id, String geometryName) {
            this.id = id;
            this.geometryName = geometryName;
        }

        public String id() {
            return id;
        }

        /** e.g. {@code models/armor/light/tempest_robe.geo.json} */
        public String geometryPath() {
            return "models/armor/" + id + "/" + geometryName + ".geo.json";
        }
    }

    private final int index;
    private final String id;
    private final ModelClass modelClass;

    ArmorTier(int index, String id, ModelClass modelClass) {
        this.index = index;
        this.id = id;
        this.modelClass = modelClass;
    }

    public int index() {
        return index;
    }

    public String id() {
        return id;
    }

    /** Custom 3D armor model family for this tier (non-null for every current tier). */
    public ModelClass modelClass() {
        return modelClass;
    }

    public boolean usesCustomModel() {
        return modelClass != null;
    }

    public static ArmorTier byIndex(int index) {
        for (ArmorTier tier : values()) {
            if (tier.index == index) return tier;
        }
        return null;
    }
}
