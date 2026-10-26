package com.sheridan.gcr.client.screen;

/**
 * Independent GCR theme using the color values from the supplied Luxium
 * Aftermatch-style menu (AftermatchTitleMenu1711 / TitlePauseHoverStripe1712).
 * Luxium is a visual reference only, never a dependency.
 *
 * The original menu has a translucent #080D0D sidebar, #FFD46B hover stripe,
 * warm-white hovered lettering, gray labels, and muted gray borders.
 */
public final class GcrUITheme {
    // Exact source colors: 0xB9080D0D, 0xFFFFD46B, 0xFFF8F6E8,
    // 0xFFB7B9BE, 0x607A8586.
    public static final int SIDEBAR = 0xB9080D0D;
    public static final int PANEL = 0xD0080D0D;
    public static final int SCRIM = 0x10080D0D;
    public static final int BUTTON = 0xD2121717;
    public static final int BUTTON_HOVER = 0xDA373023;
    public static final int BUTTON_INSTALLED = 0xD31C2323;
    public static final int INPUT = 0xEE0A0D0E;
    public static final int BORDER = 0x607A8586;
    public static final int BORDER_HOVER = 0xB0FFD46B;
    public static final int AMBER = 0xFFFFD46B;
    public static final int WARM_WHITE = 0xFFF8F6E8;
    public static final int GRAY_TEXT = 0xFFB7B9BE;
    public static final int MUTED_TEXT = 0xFF7E898B;
    public static final int CONNECTOR = 0x887A8586;
    public static final int DISABLED = 0x99555C60;

    private GcrUITheme() {}
}
