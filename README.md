# SunDProtect

Mod **server-side** para servidores **Fabric**, inspirado en
[RedProtect](https://github.com/FabioZumbi12/RedProtect): protección de
regiones cuboides con flags por región (spawn de mobs/animales, romper y
colocar bloques o líquidos, griefing de explosiones, PVP). No es un port
del código de RedProtect — es una implementación propia de SunD Studios,
pensada para cubrir justo lo que usan los servidores de SunD (SunD Origins
y CobbleSpain) sin arrastrar el resto de funciones de un plugin genérico
tipo Bukkit/Spigot.

No requiere nada en el cliente (`"environment": "server"` en
`fabric.mod.json`) — solo se instala en el servidor.

## Comandos

Todo bajo `/sundprotect`, requiere permiso de operador (nivel 2):

```
/sundprotect create <nombre> <x1> <y1> <z1> <x2> <y2> <z2>
/sundprotect remove <nombre>
/sundprotect list
/sundprotect info <nombre>
/sundprotect flag <nombre>                    -- abre un menú (cofre de 4 filas)
/sundprotect flag <nombre> <flag> <true|false>
```

Una región nueva nace con **todos los flags activos** (todo bloqueado) —
se abre a mano lo que se quiera permitir, ya sea con el menú o por
comando.

### Flags

| Flag | Bloquea |
|---|---|
| `deny-mob-spawn` | Spawn natural de mobs hostiles |
| `deny-animal-spawn` | Spawn natural de animales |
| `deny-all-spawn` | Spawn natural de cualquier entidad. Al activarla (ponerla a `true`) también activa `deny-mob-spawn` y `deny-animal-spawn` automáticamente, por coherencia — **solo en ese sentido**: desactivarla después no las toca, para no deshacer un ajuste fino hecho a mano |
| `deny-break` | Romper bloques |
| `deny-place` | Colocar bloques o líquidos (verter/recoger con cubo) |
| `deny-mobgrief` | Destrucción de bloques por cualquier explosión (creeper, TNT, carga de viento...) |
| `deny-pvp` | Daño entre jugadores |
| `deny-use` | Usar puertas, trampillas, verjas, palancas, botones y placas de presión |
| `deny-container` | Abrir cofres, barriles, hornos, shulkers y otros contenedores |
| `deny-item-drop` | Tirar items (tecla Q) estando dentro de la región |
| `deny-item-pickup` | Recoger items del suelo dentro de la región |
| `deny-leash` | Atar entidades con correa |

Los operadores (permiso nivel 2) se saltan siempre todos los flags.

## Compatibilidad con CustomNPCs-Unofficial

Las entidades de [CustomNPCs-Unofficial](https://modrinth.com/mod/customnpcs-unofficial)
(namespace de registro `customnpcs`) y los jugadores **nunca** se ven
afectados por ningún flag de spawn, ni siquiera por `deny-all-spawn`. Es
intencional (mirar `NaturalSpawnMixin`), no un fallo — un NPC colocado a
mano dentro de una región protegida debe seguir apareciendo con
normalidad.

## Iconos

Solo staff abre este menú (hace falta permiso de operador), así que no
tiene sentido distribuir un resourcepack para ello — son items 100%
vanilla, todos los clientes ya los conocen de fábrica. Cada flag ocupa dos
slots seguidos: el item que la representa (cabeza de zombi para spawn de
mobs, TNT para griefing, cofre para contenedores... ver la tabla de arriba)
y, justo al lado, un bloque de lana que marca el estado — **lima = `true`**
(regla activa, bloquea), **roja = `false`** (regla inactiva, permite). Un
click en cualquiera de los dos slots invierte la flag.

## Estructura

Mismo patrón que otros mods de SunD Studios como
[SunDScoreSync](https://github.com/IvanFreireDacoba/SunDScoreSync):
código compartido en `common/`, un subproyecto Gradle independiente por
versión de Minecraft.

- `common/src/main/java/...` — casi todo el mod (comando, persistencia de
  regiones en JSON, flags, y los mixins/eventos de protección). Sin
  cambios entre versiones.
- `1.20.1/src/main/java/.../gui/SundProtectMenu.java` y
  `1.21.1/src/main/java/.../gui/SundProtectMenu.java` — **el único fichero
  que diverge**: Mojang sustituyó el NBT `display`/`setHoverName` de
  `ItemStack` por el sistema de Data Components entre estas dos versiones
  (`ItemStack#set(DataComponentType, T)`, `DataComponents.LORE` con
  `ItemLore`), así que el menú de flags tiene una implementación por
  versión con el mismo resultado visual.

## Compilar

Requiere JDK 21+ (el runtime del propio servidor puede ser otro, esto es
solo para compilar).

```bash
cd 1.20.1 && ./gradlew build   # jar en 1.20.1/build/libs/sundprotect-<version>.jar
cd ../1.21.1 && ./gradlew build   # jar en 1.21.1/build/libs/sundprotect-<version>.jar
```

## Instalación

Copia el jar correspondiente a la versión de tu servidor (sin el
`-sources.jar`) a `mods/`, junto a Fabric API. Al crear la primera región
se genera `<mundo>/sundprotect_regions.json` — no depende de que el chunk
esté cargado, se puede consultar y editar aunque nadie haya visitado la
zona.

## Releases

- `v1.1.0-mc1.20.1` / `v1.1.0-mc1.21.1` — icono + indicador de estado por
  flag (ver "Iconos"), 5 flags nuevas (`deny-use`, `deny-container`,
  `deny-item-drop`, `deny-item-pickup`, `deny-leash`) y la cascada de
  `deny-all-spawn`.
- `v1.0.0-mc1.20.1` (SunD Origins) / `v1.0.0-mc1.21.1` (CobbleSpain) —
  primera versión pública, solo jar.

## Contribuciones

Se aceptan sugerencias y peticiones de funciones — abre un
[issue](https://github.com/IvanFreireDacoba/SunDProtect/issues).

También se aceptan **pull requests**, siempre que vengan **en una rama
nueva** (nunca directamente contra `main`).

## Licencia

Licencia propia de SunD Studios ([`LICENSE`](LICENSE)), inspirada en el
principio ShareAlike de Creative Commons: cualquier derivado debe
distribuirse con esta misma licencia y siempre de forma gratuita — nadie
puede cobrar por el mod en sí (descarga, copia o instalación). Se puede
usar libremente en servidores de pago (rangos, cosméticos, suscripción...)
siempre que no se cobre por distribuir el mod en sí. Dar crédito se
agradece pero no es obligatorio; lo que sí está prohibido es apropiarse de
la autoría.
