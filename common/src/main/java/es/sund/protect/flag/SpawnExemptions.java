package es.sund.protect.flag;

import net.minecraft.world.entity.EntityType;

/**
 * Tipos de entidad que ningun flag de spawn puede bloquear nunca, sea cual
 * sea la region: items sueltos, orbes de experiencia y NPCs de CustomNPCs
 * (namespace de registro "customnpcs" -- ese mod registra sus NPCs con
 * tipos de entidad propios bajo ese namespace). Los jugadores tambien
 * estan exentos pero no tienen EntityType propio (no estan en el registro),
 * asi que ese caso se comprueba aparte con "instanceof Player" en el
 * llamante en vez de aqui.
 *
 * Logica compartida por NaturalSpawnMixin (que bloquea de verdad) y el
 * menu de seleccion de entidades del flag CUSTOM_SPAWN (que no debe
 * siquiera ofrecer estos tipos como opcion -- ver ProtectRegion.java,
 * mismo motivo por el que en su dia se encontro el bug de deny-all-spawn
 * bloqueando items).
 */
public final class SpawnExemptions {

    private static final String CUSTOMNPCS_NAMESPACE = "customnpcs";

    public static boolean isExempt(EntityType<?> type) {
        if (type == EntityType.ITEM || type == EntityType.EXPERIENCE_ORB) {
            return true;
        }
        return CUSTOMNPCS_NAMESPACE.equals(EntityType.getKey(type).getNamespace());
    }

    private SpawnExemptions() {
    }
}
