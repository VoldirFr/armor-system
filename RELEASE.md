# Releases

Toutes les versions publiées du mod sont présentes dans ce dossier. **Les anciennes versions ne sont jamais supprimées.**

| Version | Fichier | Compatibilité |
|---------|---------|---------------|
| 1.0.0 | [armorsystem-1.0.0.jar](releases/armorsystem-1.0.0.jar) | Minecraft 1.21.1, NeoForge 21.1.265, Iron's Spells 'n Spellbooks 3.16+ |

## 1.0.0

Première release : système automatique d'armures.

- 27 sets (9 paliers de poids × 3 sous-classes : magique / mixte / physique), 108 items.
- Couleur hex de l'armure = couleur de l'item ingot source (config `minerals.<id>.colorHex` pour override),
  appliquée à l'icône et à la couche teintable de l'armure portée (futur "color layer" 3D).
- Bonus par pièce selon le palier (+10% atk ultra léger → +0% ultra lourd ; +6% armure / +3% toughness
  ultra lourd → 0%), bonus de full set par sous-classe (mana via Iron's Spells, vitesse d'attaque).
- Craft `armorsystem:armor_forge` : armure de base vanilla + N ingots ; puissance finale = valeur du minerai.
- Config générée triée et commentée : `config/armorsystem-common.toml`.
- Build testé : `./gradlew build` (voir releases/).
