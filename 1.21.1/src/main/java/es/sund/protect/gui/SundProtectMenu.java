package es.sund.protect.gui;

import es.sund.protect.data.ProtectRegion;
import es.sund.protect.data.RegionManager;
import es.sund.protect.flag.FlagInfo;
import es.sund.protect.flag.Flags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu tipo "cofre de 4 filas" (36 huecos, vanilla, sin necesitar mod ni
 * resourcepack en el cliente) -- solo lo abre staff (permiso de operador),
 * asi que no hace falta distribuir nada a los jugadores para que se vea
 * bien. Cada flag ocupa 2 slots consecutivos: el item vanilla que la
 * representa (zombie_head, TNT, cofre... ver FlagInfo) y, justo al lado,
 * un bloque de lana que marca el estado -- lima si el flag esta a true,
 * roja si esta a false (deliberado, invertido respecto al viejo esquema
 * de la v1.0.0 donde roja significaba "denied"/true; pedido explicito del
 * usuario el 2026-08-18: verde=true, rojo=false, sin excepciones).
 *
 * Click en cualquiera de los dos slots de una flag (icono o indicador) la
 * invierte y refresca TODOS los slots de flags, no solo los de la
 * pulsada -- porque activar deny-all-spawn puede cambiar otras dos flags
 * en cascada (ver ProtectRegion.setFlag). Nunca se mueve ni se saca
 * ningun item de verdad, es solo una interfaz.
 *
 * Version 1.21.1: Mojang sustituyo NBT "display"/setHoverName por el
 * sistema de Data Components (ItemStack#set(DataComponentType, T)) --
 * mismo comportamiento final que la 1.20.1, wiring distinto.
 */
public class SundProtectMenu extends ChestMenu {

    private static final int ROWS = 4;
    private static final int SIZE = ROWS * 9;

    private final ProtectRegion region;
    private final Container display;

    public SundProtectMenu(int syncId, Inventory playerInventory, ProtectRegion region) {
        super(MenuType.GENERIC_9x4, syncId, playerInventory, buildContainer(region), ROWS);
        this.region = region;
        this.display = this.getContainer();
    }

    private static Container buildContainer(ProtectRegion region) {
        SimpleContainer container = new SimpleContainer(SIZE);
        List<FlagInfo> flags = Flags.ALL_INFO;
        for (int slot = 0; slot < SIZE; slot++) {
            int flagIndex = slot / 2;
            if (flagIndex < flags.size()) {
                FlagInfo flag = flags.get(flagIndex);
                boolean denied = region.isFlagDenied(flag.id());
                container.setItem(slot, slot % 2 == 0 ? iconFor(flag, denied) : indicatorFor(denied));
            } else {
                ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
                filler.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
                container.setItem(slot, filler);
            }
        }
        return container;
    }

    private static final int WRAP_WIDTH = 28;

    /** Corta el texto en lineas cortas (rompe por espacios) para que el
     * tooltip no se estire mucho a lo ancho. */
    private static List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (current.length() > 0 && current.length() + 1 + word.length() > WRAP_WIDTH) {
                lines.add(current.toString());
                current = new StringBuilder();
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(word);
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static ItemStack iconFor(FlagInfo flag, boolean denied) {
        Item base = flag.icon();
        ItemStack stack = new ItemStack(base);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal((denied ? "§c" : "§a") + flag.displayName())
                .withStyle(s -> s.withItalic(false)));

        List<Component> lore = new ArrayList<>();
        for (String line : wrap(flag.description())) {
            lore.add(Component.literal("§7" + line));
        }
        lore.add(Component.literal(""));
        lore.add(Component.literal("§fEstado actual: " + (denied ? "§ctrue" : "§afalse")));
        lore.add(Component.literal("§8Click para invertir"));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private static ItemStack indicatorFor(boolean denied) {
        ItemStack stack = new ItemStack(denied ? Items.LIME_WOOL : Items.RED_WOOL);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(denied ? "§atrue" : "§cfalse")
                .withStyle(s -> s.withItalic(false)));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§etrue"));
        lore.add(Component.literal("§7  regla activa,"));
        lore.add(Component.literal("§7  impide la accion"));
        lore.add(Component.literal("§afalse"));
        lore.add(Component.literal("§7  regla inactiva,"));
        lore.add(Component.literal("§7  se permite"));
        lore.add(Component.literal(""));
        lore.add(Component.literal("§8Click para invertir"));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    /** Repinta todos los slots de flags -- necesario porque un solo click
     * puede cambiar varios flags a la vez (cascada de deny-all-spawn). */
    private void refreshAllFlagSlots() {
        List<FlagInfo> flags = Flags.ALL_INFO;
        for (int i = 0; i < flags.size(); i++) {
            FlagInfo flag = flags.get(i);
            boolean denied = region.isFlagDenied(flag.id());
            this.display.setItem(i * 2, iconFor(flag, denied));
            this.display.setItem(i * 2 + 1, indicatorFor(denied));
        }
        this.broadcastChanges();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        int flagIndex = slotId / 2;
        if (slotId < 0 || flagIndex >= Flags.ALL_INFO.size()) {
            return; // huecos de relleno o clicks fuera del menu (inventario del jugador) -- ignorar
        }
        FlagInfo flag = Flags.ALL_INFO.get(flagIndex);
        boolean currentlyDenied = region.isFlagDenied(flag.id());
        region.setFlag(flag.id(), !currentlyDenied); // invierte true<->false (con cascada si aplica)
        RegionManager.save();
        refreshAllFlagSlots();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // nunca se puede sacar nada del menu con shift-click
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
