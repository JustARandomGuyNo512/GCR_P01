package com.sheridan.gcr.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.sheridan.gcr.client.PhysicalMagazineInput;
import com.sheridan.gcr.client.KeyBinds;
import com.sheridan.gcr.items.MagazineItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adaptive radial magazine picker: equal angular petals for 1..8 distinct
 * loaded magazine variants, grouping physically equivalent magazines by
 * module type, round count, capacity and canonical cartridge composition.
 * One original inventory slot / ItemStack snapshot is retained per petal
 * for the existing server-authoritative selected reload validation.
 */
public final class MagazineWheelScreen extends Screen {
    private static final int PAGE_SIZE = 8;
    private static final double TWO_PI = Math.PI * 2.0;
    private static final double CANCEL_RADIUS = 43.0;
    private static final double PETAL_INNER_RADIUS = 50.0;

    private record GroupKey(String moduleType, String itemId, int rounds, int capacity,
                            String cartridges, String displayName) {}

    private record MagazineGroup(PhysicalMagazineInput.MagazineChoice selected, int quantity) {}

    private final List<MagazineGroup> groups;
    private final String gunIdentity;
    private final String preferredType;
    private int page;
    private int hovered = -1;
    private boolean cancelHover = true;
    private boolean completed;

    public MagazineWheelScreen(List<PhysicalMagazineInput.MagazineChoice> choices,
                                         String gunIdentity, String preferredType) {
        super(Component.translatable("gcr.wheel.title"));
        this.groups = groupChoices(choices, preferredType);
        this.gunIdentity = gunIdentity;
        this.preferredType = preferredType;
    }

    private static List<MagazineGroup> groupChoices(List<PhysicalMagazineInput.MagazineChoice> choices,
                                                     String preferredType) {
        Map<GroupKey, List<PhysicalMagazineInput.MagazineChoice>> groups = new LinkedHashMap<>();
        for (var choice : choices) {
            ItemStack stack = choice.snapshot();
            if (!(stack.getItem() instanceof MagazineItem item)) continue;
            var roundTypes = item.getRoundTypes(stack);
            ArrayList<String> cartridgeKeys = new ArrayList<>(roundTypes.getAllKeys());
            cartridgeKeys.sort(String::compareTo);
            StringBuilder composition = new StringBuilder();
            for (String key : cartridgeKeys) {
                composition.append(key).append('=').append(roundTypes.getInt(key)).append(';');
            }
            GroupKey key = new GroupKey(choice.magazineType(),
                    String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())),
                    choice.rounds(), choice.capacity(), composition.toString(),
                    stack.getHoverName().getString());
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(choice);
        }

        List<Map.Entry<GroupKey, List<PhysicalMagazineInput.MagazineChoice>>> ordered =
                new ArrayList<>(groups.entrySet());
        ordered.sort(Comparator
                .comparing((Map.Entry<GroupKey, List<PhysicalMagazineInput.MagazineChoice>> e)
                        -> !e.getKey().moduleType().equals(preferredType))
                .thenComparing(e -> e.getKey().displayName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(e -> e.getKey().itemId())
                .thenComparing(Comparator.comparingInt(
                        (Map.Entry<GroupKey, List<PhysicalMagazineInput.MagazineChoice>> e)
                                -> e.getKey().rounds()).reversed())
                .thenComparing(e -> e.getKey().cartridges()));

        ArrayList<MagazineGroup> result = new ArrayList<>();
        for (var entry : ordered) {
            int amount = 0;
            for (var choice : entry.getValue()) amount += Math.max(1, choice.snapshot().getCount());
            result.add(new MagazineGroup(entry.getValue().get(0), amount));
        }
        return List.copyOf(result);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * The open Screen releases mouse capture, so looking around stays blocked.
     * Minecraft also releases KeyMappings on opening a GUI: re-synchronize
     * only movement keys with the physical keyboard every GUI tick.
     * Firing, item use and other gameplay actions remain unavailable.
     */
    private static void mirrorMovementKey(KeyMapping mapping, long window) {
        InputConstants.Key key = mapping.getKey();
        if (key.getType() == InputConstants.Type.KEYSYM && key.getValue() >= 0) {
            mapping.setDown(InputConstants.isKeyDown(window, key.getValue()));
        }
    }

    private void clearMovementKeys() {
        if (minecraft == null) return;
        minecraft.options.keyUp.setDown(false);
        minecraft.options.keyDown.setDown(false);
        minecraft.options.keyLeft.setDown(false);
        minecraft.options.keyRight.setDown(false);
        minecraft.options.keyJump.setDown(false);
        minecraft.options.keyShift.setDown(false);
        minecraft.options.keySprint.setDown(false);
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.player == null || !minecraft.isWindowActive()) {
            clearMovementKeys();
            return;
        }
        long window = minecraft.getWindow().getWindow();
        mirrorMovementKey(minecraft.options.keyUp, window);
        mirrorMovementKey(minecraft.options.keyDown, window);
        mirrorMovementKey(minecraft.options.keyLeft, window);
        mirrorMovementKey(minecraft.options.keyRight, window);
        mirrorMovementKey(minecraft.options.keyJump, window);
        mirrorMovementKey(minecraft.options.keyShift, window);
        mirrorMovementKey(minecraft.options.keySprint, window);
    }

    private int displayed() {
        return Math.min(PAGE_SIZE, Math.max(0, groups.size() - page * PAGE_SIZE));
    }

    private double outerRadius() {
        return Math.max(90.0, Math.min(166.0, Math.min(width * 0.35, (height - 90.0) * 0.46)));
    }

    private void updateHovered(double x, double y) {
        double dx = x - width / 2.0;
        double dy = y - height / 2.0;
        cancelHover = Math.hypot(dx, dy) <= CANCEL_RADIUS;
        if (cancelHover || displayed() == 0) {
            hovered = -1;
            return;
        }
        // The nearest sector is selected even beyond the wheel's edge, making
        // long mouse gestures reliable. Only the center represents cancellation.
        double angle = Math.atan2(dx, -dy);
        if (angle < 0) angle += TWO_PI;
        int n = displayed();
        int index = (int) Math.floor((angle + Math.PI / n) * n / TWO_PI) % n;
        hovered = page * PAGE_SIZE + index;
    }

    @Override
    public void mouseMoved(double x, double y) {
        updateHovered(x, y);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        int pages = Math.max(1, (groups.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (pages <= 1) return true;
        page = Math.floorMod(page + (delta < 0 ? 1 : -1), pages);
        updateHovered(x, y);
        return true;
    }

    private void finish(boolean choose) {
        if (completed) return;
        completed = true;
        PhysicalMagazineInput.MagazineChoice selected = choose && hovered >= 0 && hovered < groups.size()
                ? groups.get(hovered).selected() : null;
        PhysicalMagazineInput.onWheelClosed(gunIdentity, selected);
        if (minecraft != null && minecraft.screen == this) minecraft.setScreen(null);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (KeyBinds.RELOAD.getKey().getType() == InputConstants.Type.KEYSYM
                && keyCode == KeyBinds.RELOAD.getKey().getValue()) {
            finish(true);
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (KeyBinds.RELOAD.getKey().getType() == InputConstants.Type.MOUSE
                && button == KeyBinds.RELOAD.getKey().getValue()) {
            updateHovered(x, y);
            finish(true);
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        finish(false);
    }

    @Override
    public void removed() {
        clearMovementKeys();
        if (!completed) finish(false);
    }

    private static void vertex(BufferBuilder b, double x, double y, int color) {
        b.vertex(x, y, 0).color((color >>> 16) & 255, (color >>> 8) & 255,
                color & 255, (color >>> 24) & 255).endVertex();
    }

    private static void triangle(BufferBuilder b, double x1, double y1,
                                 double x2, double y2, double x3, double y3, int color) {
        vertex(b, x1, y1, color);
        vertex(b, x2, y2, color);
        vertex(b, x3, y3, color);
    }

    private static void ringSection(BufferBuilder b, double cx, double cy,
                                    double inner, double outer, double start, double stop, int color) {
        int steps = Math.max(8, (int) Math.ceil((stop - start) / Math.toRadians(4)));
        for (int i = 0; i < steps; i++) {
            double a = start + (stop - start) * i / steps;
            double z = start + (stop - start) * (i + 1) / steps;
            double aiX = cx + Math.sin(a) * inner, aiY = cy - Math.cos(a) * inner;
            double biX = cx + Math.sin(z) * inner, biY = cy - Math.cos(z) * inner;
            double aoX = cx + Math.sin(a) * outer, aoY = cy - Math.cos(a) * outer;
            double boX = cx + Math.sin(z) * outer, boY = cy - Math.cos(z) * outer;
            triangle(b, aiX, aiY, aoX, aoY, biX, biY, color);
            triangle(b, biX, biY, aoX, aoY, boX, boY, color);
        }
    }

    private void renderSectors(int centerX, int centerY, double outer) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        int n = displayed();
        for (int i = 0; i < n; i++) {
            MagazineGroup group = groups.get(page * PAGE_SIZE + i);
            boolean favorite = group.selected().magazineType().equals(preferredType);
            int color = hovered == page * PAGE_SIZE + i ? GcrUITheme.BUTTON_HOVER
                    : favorite ? GcrUITheme.BUTTON_INSTALLED : GcrUITheme.BUTTON;
            double center = TWO_PI * i / n;
            double halfAngle = Math.PI / n;
            double gap = Math.min(Math.toRadians(2), halfAngle * 0.12);
            ringSection(b, centerX, centerY, PETAL_INNER_RADIUS, outer,
                    center - halfAngle + gap, center + halfAngle - gap, color);
        }
        ringSection(b, centerX, centerY, 0, CANCEL_RADIUS - 1, -Math.PI, Math.PI,
                cancelHover ? GcrUITheme.BUTTON_HOVER : GcrUITheme.INPUT);
        BufferUploader.drawWithShader(b.end());
        RenderSystem.disableBlend();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        updateHovered(mouseX, mouseY);
        gfx.fill(0, 0, width, height, 0xA9080D0D);

        int cx = width / 2, cy = height / 2;
        double r = outerRadius();
        renderSectors(cx, cy, r);
        gfx.drawCenteredString(font, title, cx, (int)(cy - r - 24), GcrUITheme.AMBER);

        int n = displayed();
        for (int i = 0; i < n; i++) {
            MagazineGroup group = groups.get(page * PAGE_SIZE + i);
            var choice = group.selected();
            double a = TWO_PI * i / n;
            double textRadius = (PETAL_INNER_RADIUS + r) * 0.54;
            int tx = cx + (int)(Math.sin(a) * textRadius);
            int ty = cy - (int)(Math.cos(a) * textRadius);

            // For 7-8 petals use compact labels to avoid spilling into adjacent wedges.
            int maxTextWidth = n >= 7 ? 72 : (n >= 5 ? 88 : 125);
            ItemStack display = choice.snapshot();
            gfx.renderItem(display, tx - 8, ty - 23);
            String name = font.plainSubstrByWidth(display.getHoverName().getString(), maxTextWidth);
            gfx.drawCenteredString(font, name, tx, ty - 2, GcrUITheme.WARM_WHITE);
            String count = choice.rounds() + "/" + choice.capacity();
            if (choice.magazineType().equals(preferredType)) count = "★ " + count;
            gfx.drawCenteredString(font, count, tx, ty + 10, GcrUITheme.GRAY_TEXT);
            if (group.quantity() > 1) {
                gfx.drawCenteredString(font, "×" + group.quantity(), tx, ty + 22, GcrUITheme.AMBER);
            }
        }

        // The center uses the SAME golden hover state as magazine petals.
        gfx.drawCenteredString(font, Component.literal("R"), cx, cy - 12, GcrUITheme.AMBER);
        gfx.drawCenteredString(font, Component.translatable("gcr.wheel.cancel"), cx, cy + 3, GcrUITheme.WARM_WHITE);
        if (groups.size() > PAGE_SIZE) {
            gfx.drawCenteredString(font, (page + 1) + "/" + ((groups.size() + PAGE_SIZE - 1) / PAGE_SIZE),
                    cx, cy + (int)r + 9, GcrUITheme.AMBER);
        }
        gfx.drawCenteredString(font, Component.translatable("gcr.wheel.hint"),
                cx, Math.min(height - 12, cy + (int)r + 31), GcrUITheme.GRAY_TEXT);
        super.render(gfx, mouseX, mouseY, partialTick);
    }
}
