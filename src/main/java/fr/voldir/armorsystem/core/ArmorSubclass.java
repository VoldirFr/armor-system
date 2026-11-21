package fr.voldir.armorsystem.core;

public enum ArmorSubclass {
    MAGIC("magic"),
    HYBRID("hybrid"),
    PHYSICAL("physical");

    private final String id;

    ArmorSubclass(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ArmorSubclass byId(String id) {
        for (ArmorSubclass subclass : values()) {
            if (subclass.id.equals(id)) return subclass;
        }
        return null;
    }
}
