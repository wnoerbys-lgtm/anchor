# Anchor Macro

Selvstændigt Fabric-mod til Minecraft **1.21.11** (Java 21).

Tryk **Z** (kan ændres under Indstillinger → Styring → "Anchor Macro"):
1. Hvis du ikke har en totem i offhand, flyttes en fra din inventory dertil.
2. Et respawn anchor placeres der, hvor du kigger.
3. Anchoret fyldes med glowstone, og din hotbar-slot sættes tilbage.

Kræver et respawn anchor og glowstone i hotbaren. Selve sprængningen gør du selv.

## Config (`config/anchormacro.json`)
| Felt | Standard | Betydning |
|------|----------|-----------|
| charges | 1 | Antal glowstone (1-4) |
| requireTotem | true | Kræv totem før macroen kører |
| autoEquipTotem | true | Flyt selv en totem til offhand |
| restoreSlot | true | Sæt hotbar-slot tilbage |

## Byg
Push til GitHub, og hent jar-filen under Actions -> Artifacts. Eller lokalt:
`gradle wrapper && ./gradlew build` (kræver Java 21 og nyeste Gradle).
