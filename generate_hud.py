import json

margin = "${margin}"
bottom = "${bottom}"
right = "${right}"
screen_height = "${screen_height}"

# Colors
bg_dark = -2147483648 # 0x80000000 (More opaque for joystick)
bg_button = 1291845632 # 0x4D000000
stroke_white = -1 # White
stroke_green = -11141291 # 0xFF55FF55
stroke_red = -43691 # 0xFFFF5555
stroke_teal = -11141121 # 0xFF55FFFF
bg_green_solid = -11141291 # 0xFF55FF55

def get_scaled(target_dp, current_dp, var_name="${width}"):
    if target_dp == current_dp:
        return var_name
    return f"({var_name} / {current_dp} * {target_dp})"

def create_btn(name, keycode, dx, dy, w=80, h=30, bg=bg_button, stroke=-1, stroke_width=0, radius=0, toggle=False):
    return {
        "bgColor": bg,
        "cornerRadius": radius,
        "dynamicX": dx,
        "dynamicY": dy,
        "height": h,
        "isDynamicBtn": False,
        "isHideable": False,
        "isSwipeable": False,
        "isToggle": toggle,
        "keycodes": [keycode, 0, 0, 0],
        "name": name,
        "opacity": 1,
        "passThruEnabled": False,
        "strokeColor": stroke,
        "strokeWidth": stroke_width,
        "width": w
    }

buttons = []

# Top left group (rounded rectangles)
spacing = 15
buttons.append(create_btn("DEBUG", 292, f"{margin}", f"{margin}", w=70, h=35, radius=12, stroke_width=2))

chat_x = 70 + spacing
buttons.append(create_btn("CHAT", 84, f"{margin} + {get_scaled(chat_x, 70)}", f"{margin}", w=70, h=35, radius=12, stroke_width=2))

kbd_x = chat_x + 70 + spacing
buttons.append(create_btn("KEYBOARD", -1, f"{margin} + {get_scaled(kbd_x, 90)}", f"{margin}", w=90, h=35, radius=12, stroke_width=2))

tab_x = kbd_x + 90 + spacing
buttons.append(create_btn("TAB", 258, f"{margin} + {get_scaled(tab_x, 70)}", f"{margin}", w=70, h=35, radius=12, stroke_width=2))

buttons.append(create_btn("3RD", 294, f"{margin}", f"{margin} * 2 + {get_scaled(35 + spacing, 35, '${height}')}", w=70, h=35, radius=12, stroke_width=2))

# Bottom left group
buttons.append(create_btn("GUI", -2, f"{margin}", f"{bottom} - {margin}", w=60, h=35, radius=12, stroke_width=2))

# Right middle/bottom group
# Spacing between circles
c_spacing = 25

# PRI
pri_w, pri_h = 80, 80
pri_x_off = pri_w
pri_y_off = 40
buttons.append(create_btn("PRI", -3, f"{right} - {margin} - {get_scaled(pri_x_off, pri_w)}", f"{bottom} - {margin} - {get_scaled(pri_y_off, pri_h, '${height}')}", w=pri_w, h=pri_h, radius=100, stroke=stroke_red, stroke_width=3))

# SEC
sec_w, sec_h = 60, 60
sec_x_off = 20 + sec_w
sec_y_off = pri_y_off + c_spacing + sec_h
buttons.append(create_btn("SEC", -4, f"{right} - {margin} - {get_scaled(sec_x_off, sec_w)}", f"{bottom} - {margin} - {get_scaled(sec_y_off, sec_h, '${height}')}", w=sec_w, h=sec_h, radius=100, stroke=stroke_teal, stroke_width=3))

# JUMP
jump_w, jump_h = 60, 60
jump_x_off = sec_x_off + c_spacing + jump_w
jump_y_off = sec_y_off
buttons.append(create_btn("JUMP", 32, f"{right} - {margin} - {get_scaled(jump_x_off, jump_w)}", f"{bottom} - {margin} - {get_scaled(jump_y_off, jump_h, '${height}')}", w=jump_w, h=jump_h, radius=100, stroke=stroke_green, stroke_width=3))

# SNEAK
sneak_w, sneak_h = 60, 60
sneak_x_off = pri_x_off + c_spacing + sneak_w
sneak_y_off = pri_y_off - 10
buttons.append(create_btn("SNEAK", 340, f"{right} - {margin} - {get_scaled(sneak_x_off, sneak_w)}", f"{bottom} - {margin} - {get_scaled(sneak_y_off, sneak_h, '${height}')}", w=sneak_w, h=sneak_h, radius=20, bg=bg_green_solid, stroke_width=0, toggle=True))

# INV
inv_w, inv_h = 50, 50
inv_x_off = sneak_x_off + c_spacing + inv_w
inv_y_off = sneak_y_off - 5
buttons.append(create_btn("INV", 69, f"{right} - {margin} - {get_scaled(inv_x_off, inv_w)}", f"{bottom} - {margin} - {get_scaled(inv_y_off, inv_h, '${height}')}", w=inv_w, h=inv_h, radius=100, stroke=stroke_white, stroke_width=2))

joysticks = [{
    "forwardLock": True,
    "absolute": False,
    "bgColor": bg_dark,
    "cornerRadius": 100,
    "dynamicX": f"{margin} * 4",
    "dynamicY": f"{bottom} - {margin} * 4 - {get_scaled(160, 160, '${height}')}",
    "height": 160,
    "width": 160,
    "isDynamicBtn": False,
    "isHideable": False,
    "isSwipeable": False,
    "isToggle": False,
    "keycodes": [0, 0, 0, 0],
    "name": "Joystick",
    "opacity": 1,
    "passThruEnabled": False,
    "strokeColor": stroke_white,
    "strokeWidth": 2
}]

layout = {
    "mControlDataList": buttons,
    "mDrawerDataList": [],
    "mJoystickDataList": joysticks,
    "scaledAt": 100,
    "version": 8
}

with open("default.json", "w") as f:
    json.dump(layout, f)

print("default.json generated.")
