#!/usr/bin/env python3
"""
Genera los iconos (16x16 PNG), los modelos de item, y el override de
`paper.json` del resourcepack de SunDProtect.

Cada flag tiene dos texturas (estado true/false) que son identicas salvo
por un punto de 4x4 en la esquina superior izquierda: verde si el flag
esta en true, rojo si esta en false (pedido explicito del usuario,
2026-08-18 -- literal, aunque invierte el convenio antiguo de lana roja/
verde del menu original).

El indice de icono de cada flag (usado para calcular el custom_model_data)
esta fijado en es.sund.protect.flag.FlagInfo -- si se anade un flag nuevo,
usa el siguiente indice libre aqui Y alli, nunca reutilices uno ya
publicado en un resourcepack distribuido.

custom_model_data = 2000 + iconIndex*2         (estado false/permitido)
custom_model_data = 2000 + iconIndex*2 + 1     (estado true/bloqueado)

Requiere Pillow (`pip install Pillow`).
"""
import json
import os

from PIL import Image

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PACK_ASSETS = os.path.join(REPO_ROOT, "resourcepack", "assets")
TEXTURES_DIR = os.path.join(PACK_ASSETS, "sundprotect", "textures", "item")
MODELS_DIR = os.path.join(PACK_ASSETS, "sundprotect", "models", "item")
PAPER_OVERRIDE = os.path.join(PACK_ASSETS, "minecraft", "models", "item", "paper.json")

SIZE = 16
BASE_CMD = 2000

# (slug, iconIndex) -- debe coincidir EXACTAMENTE con Flags.ALL_INFO
# (mismo orden, mismo iconIndex) en el codigo Java.
FLAGS = [
    ("mob_spawn", 0),
    ("break", 1),
    ("place", 2),
    ("mobgrief", 3),
    ("pvp", 4),
    ("animal_spawn", 5),
    ("all_spawn", 6),
    ("use", 7),
    ("container", 8),
    ("item_drop", 9),
    ("item_pickup", 10),
    ("leash", 11),
]

TRANSPARENT = (0, 0, 0, 0)


def new_canvas():
    return Image.new("RGBA", (SIZE, SIZE), TRANSPARENT)


def put(img, x, y, color):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        img.putpixel((x, y), color)


def rect(img, x0, y0, x1, y1, color):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(img, x, y, color)


def rect_outline(img, x0, y0, x1, y1, color):
    for x in range(x0, x1 + 1):
        put(img, x, y0, color)
        put(img, x, y1, color)
    for y in range(y0, y1 + 1):
        put(img, x0, y, color)
        put(img, x1, y, color)


def diag_line(img, x0, y0, x1, y1, color, thickness=1):
    """Solo para diagonales de 45 grados exactos (todas las usadas aqui)."""
    steps = max(abs(x1 - x0), abs(y1 - y0))
    dx = 0 if x1 == x0 else (1 if x1 > x0 else -1)
    dy = 0 if y1 == y0 else (1 if y1 > y0 else -1)
    x, y = x0, y0
    for _ in range(steps + 1):
        for t in range(thickness):
            put(img, x + t, y, color)
        x += dx
        y += dy


# ---------------------------------------------------------------------
# Un dibujante por flag. Formas simples, en bloques (estilo Minecraft),
# pensadas para distinguirse a golpe de vista en un slot de 16x16 -- no
# se puede verificar el resultado real en un cliente desde este entorno
# (sin pantalla/cliente de juego disponible), solo que el PNG es valido.
# ---------------------------------------------------------------------

def draw_mob_spawn(img):
    # cabeza de zombi: piel verde, ojos negros, boca oscura
    rect(img, 3, 3, 12, 12, (58, 122, 74, 255))
    rect(img, 3, 3, 12, 4, (44, 97, 58, 255))  # sombra superior (pelo)
    rect(img, 5, 7, 6, 8, (20, 20, 20, 255))   # ojo izq
    rect(img, 9, 7, 10, 8, (20, 20, 20, 255))  # ojo der
    rect(img, 5, 10, 10, 11, (30, 40, 30, 255))  # boca


def draw_break(img):
    # pico: mango marron diagonal + cabeza gris
    diag_line(img, 4, 13, 11, 6, (122, 75, 42, 255), thickness=2)
    rect(img, 8, 2, 14, 4, (168, 168, 168, 255))
    rect(img, 9, 5, 13, 6, (120, 120, 120, 255))


def draw_place(img):
    # bloque de cesped: cara superior verde, laterales marrones
    rect(img, 3, 5, 12, 12, (107, 68, 35, 255))
    rect(img, 3, 3, 12, 5, (91, 140, 62, 255))
    rect_outline(img, 3, 3, 12, 12, (40, 30, 20, 255))


def draw_mobgrief(img):
    # TNT: cuerpo rojo, franja blanca, mecha negra
    rect(img, 3, 4, 12, 13, (176, 46, 38, 255))
    rect(img, 3, 7, 12, 9, (238, 238, 238, 255))
    rect(img, 5, 8, 5, 8, (30, 30, 30, 255))
    rect(img, 7, 8, 7, 8, (30, 30, 30, 255))
    rect(img, 9, 8, 9, 8, (30, 30, 30, 255))
    rect(img, 7, 2, 8, 3, (40, 40, 40, 255))  # mecha


def draw_pvp(img):
    # espadas cruzadas: hojas grises, empunaduras marrones
    diag_line(img, 3, 3, 12, 12, (200, 200, 205, 255), thickness=2)
    diag_line(img, 12, 3, 3, 12, (200, 200, 205, 255), thickness=2)
    rect(img, 2, 2, 3, 3, (122, 75, 42, 255))
    rect(img, 12, 2, 13, 3, (122, 75, 42, 255))
    rect(img, 2, 12, 3, 13, (122, 75, 42, 255))
    rect(img, 12, 12, 13, 13, (122, 75, 42, 255))


def draw_animal_spawn(img):
    # huevo: cuerpo crema, un par de pecas marrones
    rect(img, 6, 3, 9, 4, (240, 230, 200, 255))
    rect(img, 5, 5, 10, 11, (240, 230, 200, 255))
    rect(img, 6, 12, 9, 12, (240, 230, 200, 255))
    put(img, 7, 6, (150, 110, 70, 255))
    put(img, 9, 8, (150, 110, 70, 255))
    put(img, 6, 9, (150, 110, 70, 255))


def draw_all_spawn(img):
    # silueta gris generica + senal de prohibido (anillo rojo + barra)
    rect(img, 7, 3, 8, 5, (140, 140, 140, 255))   # cabeza
    rect(img, 6, 6, 9, 11, (140, 140, 140, 255))  # cuerpo
    rect(img, 4, 7, 5, 10, (140, 140, 140, 255))  # brazo izq
    rect(img, 10, 7, 11, 10, (140, 140, 140, 255))  # brazo der
    ring = (204, 40, 40, 255)
    for (x, y) in [
        (2, 4), (3, 4), (12, 4), (13, 4),
        (1, 5), (14, 5), (1, 6), (14, 6),
        (0, 7), (15, 7), (0, 8), (15, 8),
        (1, 9), (14, 9), (1, 10), (14, 10),
        (2, 11), (3, 11), (12, 11), (13, 11),
    ]:
        put(img, x, y, ring)
    diag_line(img, 2, 2, 13, 13, ring, thickness=2)


def draw_use(img):
    # palanca: base de piedra + brazo diagonal
    rect(img, 4, 11, 11, 13, (130, 130, 130, 255))
    diag_line(img, 6, 11, 11, 4, (70, 50, 35, 255), thickness=2)


def draw_container(img):
    # cofre: caja marron con bandas oscuras y cerrojo dorado
    rect(img, 3, 5, 12, 13, (133, 94, 51, 255))
    rect(img, 3, 5, 12, 6, (90, 62, 32, 255))
    rect(img, 3, 8, 12, 9, (90, 62, 32, 255))
    rect(img, 7, 7, 8, 9, (210, 175, 55, 255))


def draw_item_drop(img):
    # flecha hacia abajo sobre un item pequeno
    rect(img, 7, 2, 8, 8, (235, 200, 60, 255))
    for i, w in enumerate(range(5, 0, -1)):
        y = 9 + i
        put(img, 7 - w // 2 + 1, y, (235, 200, 60, 255))
        put(img, 8 + w // 2 - 1, y, (235, 200, 60, 255))
    rect(img, 5, 6, 6, 8, (235, 200, 60, 255))
    rect(img, 9, 6, 10, 8, (235, 200, 60, 255))
    rect(img, 6, 12, 9, 14, (150, 100, 60, 255))  # item en el suelo


def draw_item_pickup(img):
    # iman en herradura: gris con puntas rojas
    rect(img, 4, 3, 6, 10, (150, 150, 150, 255))
    rect(img, 9, 3, 11, 10, (150, 150, 150, 255))
    rect(img, 4, 9, 11, 11, (150, 150, 150, 255))
    rect(img, 4, 11, 6, 13, (190, 50, 40, 255))
    rect(img, 9, 11, 11, 13, (190, 50, 40, 255))


def draw_leash(img):
    # cuerda marron en zigzag con un pequeno lazo
    path = [(3, 3), (4, 4), (5, 5), (4, 6), (5, 7), (6, 8), (7, 9), (8, 10),
            (9, 11), (10, 12), (11, 13)]
    for (x, y) in path:
        put(img, x, y, (110, 75, 40, 255))
        put(img, x + 1, y, (110, 75, 40, 255))
    rect_outline(img, 2, 2, 4, 4, (80, 55, 30, 255))


DRAWERS = {
    "mob_spawn": draw_mob_spawn,
    "break": draw_break,
    "place": draw_place,
    "mobgrief": draw_mobgrief,
    "pvp": draw_pvp,
    "animal_spawn": draw_animal_spawn,
    "all_spawn": draw_all_spawn,
    "use": draw_use,
    "container": draw_container,
    "item_drop": draw_item_drop,
    "item_pickup": draw_item_pickup,
    "leash": draw_leash,
}

GREEN_DOT = (40, 200, 60, 255)
RED_DOT = (210, 30, 30, 255)
DOT_BORDER = (20, 20, 20, 255)


def add_dot(img, denied):
    """denied=True (flag en true) -> punto rojo. denied=False -> verde.
    Pedido literal del usuario: verde=true, rojo=false; pero el estado
    'true' de un flag ES 'denied' (bloquea) -- así que el color depende
    del VALOR del flag, no de si la accion se permite. true=verde,
    false=rojo, tal cual se pidio."""
    color = RED_DOT if not denied else GREEN_DOT
    rect(img, 0, 0, 3, 3, DOT_BORDER)
    rect(img, 1, 1, 2, 2, color)


def main():
    os.makedirs(TEXTURES_DIR, exist_ok=True)
    os.makedirs(MODELS_DIR, exist_ok=True)
    os.makedirs(os.path.dirname(PAPER_OVERRIDE), exist_ok=True)

    overrides = []
    for slug, icon_index in FLAGS:
        drawer = DRAWERS[slug]
        for state, denied in (("off", False), ("on", True)):
            img = new_canvas()
            drawer(img)
            add_dot(img, denied)
            texture_path = os.path.join(TEXTURES_DIR, f"{slug}_{state}.png")
            img.save(texture_path)

            model = {
                "parent": "item/generated",
                "textures": {"layer0": f"sundprotect:item/{slug}_{state}"},
            }
            model_path = os.path.join(MODELS_DIR, f"{slug}_{state}.json")
            with open(model_path, "w", encoding="utf-8") as f:
                json.dump(model, f, indent=2)
                f.write("\n")

            cmd = BASE_CMD + icon_index * 2 + (1 if denied else 0)
            overrides.append({
                "predicate": {"custom_model_data": cmd},
                "model": f"sundprotect:item/{slug}_{state}",
            })

    overrides.sort(key=lambda o: o["predicate"]["custom_model_data"])
    paper_model = {
        "parent": "item/generated",
        "textures": {"layer0": "item/paper"},
        "overrides": overrides,
    }
    with open(PAPER_OVERRIDE, "w", encoding="utf-8") as f:
        json.dump(paper_model, f, indent=2)
        f.write("\n")

    print(f"Generados {len(FLAGS) * 2} iconos + modelos, y {PAPER_OVERRIDE}")
    for o in overrides:
        print(f"  custom_model_data={o['predicate']['custom_model_data']:>4}  ->  {o['model']}")


if __name__ == "__main__":
    main()
