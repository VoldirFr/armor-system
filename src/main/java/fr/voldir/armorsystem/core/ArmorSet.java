package fr.voldir.armorsystem.core;

import java.util.ArrayList;
import java.util.List;

public record ArmorSet(ArmorTier tier, ArmorSubclass subclass) {

    public String id() {
        return tier.id() + "_" + subclass.id();
    }

    public static List<ArmorSet> all() {
        List<ArmorSet> sets = new ArrayList<>();
        for (ArmorTier tier : ArmorTier.values()) {
            for (ArmorSubclass subclass : ArmorSubclass.values()) {
                sets.add(new ArmorSet(tier, subclass));
            }
        }
        return sets;
    }

    public static ArmorSet of(int tierIndex, String subclassId) {
        ArmorTier tier = ArmorTier.byIndex(tierIndex);
        ArmorSubclass subclass = ArmorSubclass.byId(subclassId);
        if (tier == null || subclass == null) return null;
        return new ArmorSet(tier, subclass);
    }
}
