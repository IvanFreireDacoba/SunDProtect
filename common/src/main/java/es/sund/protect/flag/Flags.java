package es.sund.protect.flag;

import java.util.List;

public final class Flags {
    public static final String MOB_SPAWN = "deny-mob-spawn";
    public static final String BREAK = "deny-break";
    public static final String PLACE = "deny-place";
    public static final String MOBGRIEF = "deny-mobgrief";
    public static final String PVP = "deny-pvp";
    public static final String ANIMAL_SPAWN = "deny-animal-spawn";
    public static final String ALL_SPAWN = "deny-all-spawn";

    /**
     * Todos los flags van Denied por defecto al crear una region nueva
     * (pedido explicito del usuario, 2026-08-18) -- una region nueva
     * nace totalmente cerrada, hay que abrir a mano lo que se quiera
     * permitir con /sundprotect flag <region> (menu) o por comando.
     */
    public static final List<FlagInfo> ALL_INFO = List.of(
            new FlagInfo(MOB_SPAWN, "Spawn de mobs", "Bloquea el spawn natural de mobs hostiles (creeper, zombie, skeleton, spider...). No afecta a NPCs de CustomNPCs.", true),
            new FlagInfo(BREAK, "Romper bloques", "Nadie (salvo OP) puede romper bloques en la region.", true),
            new FlagInfo(PLACE, "Colocar bloques", "Nadie (salvo OP) puede colocar bloques ni cubos de liquido (verter o recoger) en la region.", true),
            new FlagInfo(MOBGRIEF, "Griefing de explosiones", "Ninguna explosion (creeper, TNT, carga de viento...) destruye bloques en la region.", true),
            new FlagInfo(PVP, "PVP entre jugadores", "Impide que los jugadores se hagan dano entre si dentro de la region.", true),
            new FlagInfo(ANIMAL_SPAWN, "Spawn de animales", "Bloquea el spawn natural de animales (vaca, cerdo, oveja, gallina...).", true),
            new FlagInfo(ALL_SPAWN, "Spawn de TODO", "Bloquea el spawn natural de cualquier entidad (mobs, animales, aldeanos...), salvo jugadores y NPCs de CustomNPCs. Tiene prioridad sobre spawn de mobs/animales si esta activo.", true)
    );

    public static final List<String> ALL = ALL_INFO.stream().map(FlagInfo::id).toList();

    public static FlagInfo info(String id) {
        return ALL_INFO.stream().filter(f -> f.id().equals(id)).findFirst().orElse(null);
    }

    private Flags() {
    }
}
