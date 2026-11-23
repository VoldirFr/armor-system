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
- **Couleur** : le mod calcule en code la **couleur HEX de la texture de l'ingot** (moyenne de tous ses
  pixels, `IngotColorResolver`) et applique ce **color shift** sur les layers teintables de l'armure —
  portée (`IClientItemExtensions#getArmorLayerTintColor` sur `layer.dyeable()`, dont le layer nommé
  `color_layer` = `ArmorSetItem.COLOR_LAYER_NAME`) et icône d'inventaire (`ItemColor`, tint index 0).
  Aucune texture générée par matériau ; la HEX calculée est overridable via `minerals.<id>.colorHex`.
  Base visuelle (version test) : armure vanilla **en fer** (`minecraft:iron` layer, teinté par la HEX du
  matériau) + layer nommé `color_layer` (placeholder transparent,
  `assets/armorsystem/textures/models/armor/color_layer_layer_*.png`). Pour des modèles 3D multi-layers :
  ajoutez vos layers, les layers `dyeable` (ex. nommés `color_layer`) sont teintés.
- **Craft simple** (`armorsystem:armor_forge`, **table de craft 3×3 / grille 2×2 de l'inventaire**) :
  1 **pièce d'armure de base** (armure vanilla OU template
  `armorsystem:base_<set>_<slot>`, tag `armorsystem:base_armors`) + **1 minerai**
  (les 9 minerais des défauts `minerals.<id>.ingot`, tag `armorsystem:armor_ingots`,
  N = `craft.ingotsPerArmor`, défaut 1) → la pièce d'archétype avec le nom du minerai
  (ex : « Plastron ultra léger (physique) Diamant ») et le matériau de l'ingot stocké dans le composant
  `armorsystem:material`. Les minerais modifiés dans la config (`minerals.<id>.ingot`) sont reconnus par
  la recette même hors tag. Les armures vanilla servent de base via le mapping `craft.vanillaArmorToSet`.
  **Seules les bases sont visibles dans l'onglet créatif** ; les armures s'obtiennent uniquement par craft.
  ⚠️ **Migration 1.0.0 → >= 1.0.2** : si `craft.ingotsPerArmor` vaut `4` dans votre `config/armorsystem-common.toml`
  (ancien défaut de la 1.0.0 persisté), remettez-le à `1` — sinon la recette exige 4 minerais et « ne crafts rien »
  avec 1 seul.
- **Vérification en jeu** : GameTests serveur (`fr.voldir.armorsystem.gametest.ArmorForgeGameTests`,
  `gradlew runGameTestServer`) couvrant le craft base+minerai pour les 9 minerais, les bases vanilla,
  les cas négatifs et le nom affiché. Le template vide requis vit dans `gameteststructures/`.

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

## Modèle 3D des armures (1.0.7 → 1.0.8)

Les 9 tiers utilisent un modèle 3D custom, rangé en 3 familles (_Blockbench Bedrock
geometry 1.12.0, 128x128_) chargées dynamiquement depuis
`assets/armorsystem/models/armor/<famille>/<nom>.geo.json` et converties en `HumanoidModel`
(pivots Y up → Y down, rotations négées, cubes rotés éclatés en sous-parts) :

| Famille | Tier | Géométrie |
|---|---|---|
| `light` | ultra_light, very_light, light | `tempest_robe.geo.json` |
| `medium` | light_medium, medium, heavy_medium | `archer_armor.geo.json` |
| `heavy` | heavy, very_heavy, ultra_heavy | `paladin_armor.geo.json` |

Le rendu superpose deux passes de texture (mêmes UV) :
1. `<famille>_shift_layer_1/2.png` — texture grayscale (le vêtement), re-colorée **runtime** par minerai
   via un vrai hue-shift (luminance du gris préservée : zones sombres = couleur profonde et saturée,
   zones claires = couleur pâle), générée en `DynamicTexture` dans
   `armorsystem:dynamic/<famille>_shift/<minerai>`.
2. `<famille>_layer1_layer_1/2.png` — layer de décorations, rendu **au-dessus**, jamais coloré.
