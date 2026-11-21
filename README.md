# Armor System

Mod NeoForge 1.21.1 (`armorsystem`) : système automatique d'armuresfantasy.

## Principe

- **27 sets d'armures** = 9 paliers de poids × 3 sous-classes (3×3×3 = 27), soit **108 items**
  (casque / plastron / jambières / bottes pour chaque set).
- **Paliers de poids** (index 0 → 8) : ultra léger, très léger, léger, moyen léger, moyen,
  moyen lourd, lourd, très lourd, ultra lourd.
  - Bonus d'attaque par pièce : `+10%` pour ultra léger, `-1.25%` par écart → ultra lourd `+0%`.
  - Bonus de défense : `+6%` d'armure et `+3%` de toughness pour ultra lourd,
    `-0.75%` / `-0.375%` par écart → ultra léger `+0%`.
- **Sous-classes** (bonus de full set uniquement, configurable) :
  - magique : +200 mana (attribut `irons_spellbooks:max_mana` d'Iron's Spells 'n Spellbooks),
  - mixte : +100 mana et +15% vitesse d'attaque,
  - physique : +30% vitesse d'attaque.
- **Matériaux** : tout minerai vanilla (`quartz`, `redstone`, `lapis_lazuli`, `emerald`,
  `copper_ingot`, `gold_ingot`, `iron_ingot`, `diamond`, `netherite_ingot`), chacun associé à un
  palier de poids. La **couleur hex de l'armure est celle de l'item ingot source** (moyenne des
  pixels de sa texture, overridable en config) et est appliquée à la couche teintable
  (`layer.dyeable()`) du rendu 3D porté + à l'icône d'inventaire. C'est cette couche "color layer"
  qui sera remplacée par les futurs modèles 3D.
- **Craft** : `armure de base` (armure vanilla, tag `armorsystem:base_armors`) + N ingots
  (tag `armorsystem:armor_ingots`, N configurable) → la pièce du set correspondant.
  - l'ingot choisit le **palier** (config `craft.ingotTier`),
  - la matière de l'armure de base choisit la **sous-classe** (config `craft.baseArmorSubclass`),
  - la **puissance finale** (armure, toughness, durabilité, attaque, knockback, enchantabilité)
    vient de la valeur du minerai, configurable par minerai (`minerals.<id>.*`).

## Config

`config/armorsystem-common.toml` : bonus de chaque palier (`tiers.*`), valeurs de chaque minerai
(`minerals.*` dont `colorHex`), nombre d'ingots par craft et mappings
(`craft.*`), bonus de set (`setBonus.*`).

## Build

```bash
JAVA_HOME=/workspace/tools/jdk-21 ./gradlew build -Dorg.gradle.jvmargs=-Xmx1g --console=plain
```

Dépendances (Modrinth maven) : `irons-spells-n-spellbooks` 1.21.1-3.16.4 (+ geckolib, curios,
playeranimator). Dépendance runtime déclarée requise : `irons_spellbooks`.
