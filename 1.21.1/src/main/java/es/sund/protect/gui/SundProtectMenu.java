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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu tipo "cofre de una fila" (9 huecos, vanilla, sin necesitar mod en
 * el cliente) que muestra un item por flag: lana roja=true (regla activa,
 * bloquea), lana verde=false (regla inactiva, permite). Click en
 * cualquier boton del raton invierte el flag y refresca el item en el
 * sitio -- nunca se mueve ni se saca ningun item de verdad, es solo una
 * interfaz.
 *
 * Version 1.21.1: Mojang sustituyo NBT "display"/setHoverName por el
 * sistema de Data Components (ItemStack#set(DataComponentType, T)) --
 * ya no hace falta serializar Component a JSON a mano (Component.Serializer
 * .toJson paso a exigir un HolderLookup.Provider en esta version), se pasa
 * la lista de Component directamente via DataComponents.LORE/ItemLore.
 */
public class SundProtectMenu extends ChestMenu {

    private static final int SIZE = 9;

    private final ProtectRegion region;
    private final Container display;

    public SundProtectMenu(int syncId, Inventory playerInventory, ProtectRegion region) {
        super(net.minecraft.world.inventory.MenuType.GENERIC_9x1, syncId, playerInventory,
                buildContainer(region), 1);
        this.region = region;
        this.display = this.getContainer();
    }

    private static Container buildContainer(ProtectRegion region) {
        SimpleContainer container = new SimpleContainer(SIZE);
        List<FlagInfo> flags = Flags.ALL_INFO;
        for (int i = 0; i < SIZE; i++) {
            if (i < flags.size()) {
                container.setItem(i, itemFor(region, flags.get(i)));
            } else {
                ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
                filler.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
                container.setItem(i, filler);
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

    private static ItemStack itemFor(ProtectRegion region, FlagInfo flag) {
        boolean denied = region.isFlagDenied(flag.id());
        Item base = denied ? Items.RED_WOOL : Items.LIME_WOOL;
        ItemStack stack = new ItemStack(base);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal((denied ? "§c" : "§a") + flag.displayName())
                .withStyle(s -> s.withItalic(false)));

        List<Component> lore = new ArrayList<>();
        for (String line : wrap(flag.description())) {
            lore.add(Component.literal("§7" + line));
        }
        lore.add(Component.literal(""));
        lore.add(Component.literal("§etrue"));
        lore.add(Component.literal("§7  regla activa,"));
        lore.add(Component.literal("§7  impide la accion"));
        lore.add(Component.literal("§afalse"));
        lore.add(Component.literal("§7  regla inactiva,"));
        lore.add(Component.literal("§7  se permite"));
        lore.add(Component.literal(""));
        lore.add(Component.literal("§fEstado actual: " + (denied ? "§ctrue" : "§afalse")));
        lore.add(Component.literal("§8Click para invertir"));

        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0 || slotId >= Flags.ALL_INFO.size()) {
            return; // huecos de relleno o clicks fuera del menu (inventario del jugador) -- ignorar
        }
        FlagInfo flag = Flags.ALL_INFO.get(slotId);
        boolean currentlyDenied = region.isFlagDenied(flag.id());
        region.setFlag(flag.id(), !currentlyDenied); // invierte true<->false
        RegionManager.save();
        this.display.setItem(slotId, itemFor(region, flag));
        this.broadcastChanges();
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
