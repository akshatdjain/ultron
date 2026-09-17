package com.akshatdjain.ultron.ui.components

import com.akshatdjain.ultron.ble.LightProtocol

enum class ModeCategory { STATIC, FLOW, EXTENDED }

data class LightMode(val label: String, val command: String, val category: ModeCategory)

// Labels below are taken directly from the original vendor app's decompiled resources
// (ModeAdapter.java's type->label table, ColorFragment.java/SymphonyFragment.java's
// senders, and strings.xml) — confirmed, not guessed. Command byte = family base
// ("04" flow / "1E" extended) + index; each index belongs to one of the vendor's
// fixed category tabs (Single/Breathe/Gradient/... for flow, Illusion/Streamer/...
// for extended), so the label is "Category · Variant".
//
// Extended indices 0x19 and 0x1A-0x1E only exist on the vendor app's alternate
// "device mode 2" UI variant and aren't confirmed reachable on this hardware, so
// they're omitted here.
val LIGHT_MODES: List<LightMode> = buildList {
    add(LightMode("Static", "0400", ModeCategory.STATIC))

    fun flow(index: Int, label: String) =
        add(LightMode(label, LightProtocol.Commands.WATER_MODE_BASE + "%02X".format(index), ModeCategory.FLOW))

    flow(0x00, "Single · Solid Color")
    flow(0x01, "Single · Colorful")
    flow(0x02, "Single · Random")
    flow(0x03, "Breathe · Single")
    flow(0x04, "Breathe · Colorful")
    flow(0x05, "Breathe · Random")
    flow(0x06, "Gradient · Colorful")
    flow(0x07, "Gradient · C&Y&Pe")
    flow(0x08, "Gradient · R&Y&P")
    flow(0x09, "Gradient · R&B")
    flow(0x0A, "Gradient · G&B")
    flow(0x0B, "Gradient · Random")
    flow(0x0C, "Strobe · Colorful")
    flow(0x0D, "Strobe · R&G&B")
    flow(0x0E, "Strobe · C&Y&Pe")
    flow(0x0F, "Rhythm · Colorful")
    flow(0x10, "Rhythm · R&Y&P")
    flow(0x11, "Rhythm · C&Y&Pe")
    flow(0x12, "Motion · B&R")
    flow(0x13, "Motion · R&G")
    flow(0x14, "Motion · R&Cyan")

    fun extended(index: Int, label: String) =
        add(LightMode(label, LightProtocol.Commands.EXTENDED_MODE_BASE + "%02X".format(index), ModeCategory.EXTENDED))

    extended(0x00, "Illusion · Illusion")
    extended(0x01, "Illusion · Light")
    extended(0x02, "Illusion · Breathe")
    extended(0x03, "Streamer · Streamer")
    extended(0x04, "Streamer · Open/Close")
    extended(0x05, "Streamer · Optical")
    extended(0x06, "Streamer · Pull Curtain")
    extended(0x07, "Streamer · Curtain Close")
    extended(0x08, "Streamer · Piled Up")
    extended(0x09, "Streamer · Chasing Light")
    extended(0x0A, "Streamer · Magic")
    extended(0x0B, "Streamer · Meteor")
    extended(0x0C, "Tailing · Colorful")
    extended(0x0D, "Tailing · Multicolor")
    extended(0x0E, "Tailing · Single")
    extended(0x0F, "Rhythm · Spectrum")
    extended(0x10, "Rhythm · Stretch")
    extended(0x11, "Rhythm · Optical")
    extended(0x12, "Rhythm · Snap-in")
    extended(0x13, "Rhythm · Beam")
    extended(0x14, "Rhythm · Single")
    extended(0x15, "Motion · Stretch")
    extended(0x16, "Motion · Snap-in")
    extended(0x17, "Motion · R&B")
    extended(0x18, "Sync RGB")
}
