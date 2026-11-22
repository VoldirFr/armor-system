# Armor System

Mod NeoForge 1.21.1 (`armorsystem`) : système automatique d'armures.

## Principe

- **27 archétypes** = 9 paliers de poids × 3 sous-classes (magique / mixte / physique), 4 pièces chacun
  → **108 items d'armure**, chacun disponible **pour chaque matériau** (le matériau est un composant
  de l'item, posé au craft : tous les archétypes × tous les minerais = 27 × 9 combinaisons).
- **Paliers de poids** (index 0 → 8 : ultra léger → ultra lourd), bonus **en %** par pièce :
  - attaque : `+10%` (ultra léger) `-1.25%`/écart → `+0%` (ultra lourd) — `ADD_MULTIPLIED_TOTAL` sur `attack_damage` ;
  - armure : `+6%` (ultra lourd) `-0.75%`/écart → `0%` ; toughness : `+3%` `-0.375%`/écart.
- **Sous-classes** : bonus de **full set** (4 pièces du même archétype équipées), affiché **en bas du
  tooltip** (« Full set bonus: ... ») : magique +200 mana (`irons_spellbooks:max_mana`), mixte +100 mana
  & +15% attack speed, physique +30% attack speed — le tout configurable (`setBonus.*`).
- **Stats par pièce** façon table d'attributs (config `minerals.<id>.*`) : `armor`, `armorToughness`,
  `knockbackResistance`, `movementSpeed`, `maxHealth`, `attackSpeed`, `luck`, `attackDamagePct` (%),
  `durability`, `enchantability` — valeurs du matériau choisi, + bonus % du palier.
- **Couleur** : à la pose, le mod calcule en code la **couleur HEX de la texture de l'ingot** (moyenne de
  tous ses pixels, `IngotColorResolver`) et applique ce color shift uniquement sur le **layer nommé
  `color_layer`** (`ArmorSetItem.COLOR_LAYER_NAME`) — à la fois sur l'armure portée
  (`IClientItemExtensions#getArmorLayerTintColor`) et sur l'icône (layer 1 du modèle, `ItemColor`).
  Aucune texture générée par matériau ; la HEX calculée est overridable via `minerals.<id>.colorHex`.
  Base visuelle (version test) : armure vanilla **en fer** (`minecraft:iron` layer) + layer `color_layer`
  blanc (`assets/armorsystem/textures/models/armor/color_layer_layer_*.png`). Pour des modèles 3D :
  ajoutez vos layers, seul celui dont le nom contient `color_layer` est teinté.
- **Craft** (`armorsystem:armor_forge`) : 1 **armure de base** (template `armorsystem:base_<set>_<slot>`,
  tag `armorsystem:base_armors` — choisit l'archétype et la pièce) + N **ingots**
  (tag `armorsystem:armor_ingots`, N = `craft.ingotsPerArmor`) → la pièce d'archétype avec le matériau
  de l'ingot stocké dans le composant `armorsystem:material`.

## Config

`config/armorsystem-common.toml` généré **trié et commenté** (vérifié via runServer) :
`[tiers]` (9 × 3 bonus %) > `[minerals]` (9 × 12 valeurs) > `[craft]` > `[setBonus]`.

## Build / releases

```bash
JAVA_HOME=/workspace/tools/jdk-21 ./gradlew build -Dorg.gradle.jvmargs=-Xmx1g --console=plain
cp build/libs/armorsystem-<ver>.jar releases/
```

Les jar publiées vivent dans `releases/` (1.0.0, 1.0.1, ...).

Dépendances (Modrinth maven + `libs/`) : `irons-spells-n-spellbooks` 1.21.1-3.16.4, `irons-lib`
1.21.1-2.4.0, `geckolib-neoforge` 1.21.1-4.8.2 (jar dans `libs/`), `curios` 9.5.1+1.21.1,
`playeranimator` 2.0.1+1.21.1-forge. `irons_spellbooks` requis dans `neoforge.mods.toml`.
