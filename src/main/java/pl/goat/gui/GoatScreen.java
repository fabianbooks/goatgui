package pl.goat.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GoatScreen extends Screen {
    // Kolory (ARGB)
    private static final int BG_DIM      = 0x88000000;
    private static final int PANEL       = 0xF0121218;
    private static final int PANEL_BORDER= 0xFF22222C;
    private static final int CARD        = 0xFF191920;
    private static final int CARD_HOVER  = 0xFF22222B;
    private static final int ACCENT      = 0xFFF2607A;
    private static final int ACCENT_HOVER= 0xFFFF7A92;
    private static final int TEXT        = 0xFFFFFFFF;
    private static final int TEXT_DIM    = 0xFF7C7C8A;

    private static final int PANEL_W = 330;
    private static final int PANEL_H = 250;
    private static final int HEADER_H = 32;
    private static final int FOOTER_H = 36;
    private static final int CARD_W = 150;
    private static final int CARD_H = 28;
    private static final int GAP = 6;

    private enum Tab {
        GRACZ("Gracz"), VISUALS("Visuals"), USTAWIENIA("Ustawienia");
        final String label;
        Tab(String label) { this.label = label; }
    }

    private Tab tab = Tab.GRACZ;
    private int scroll = 0;
    private String searchedPlayer = null;

    // Przykladowe przelaczniki (zakladki Visuals / Ustawienia)
    private boolean showCoords = true;
    private boolean showPing = true;
    private boolean compactList = false;
    private boolean sortByName = false;

    public GoatScreen() {
        super(Text.literal("GoatGUI"));
    }

    private int px() { return (width - PANEL_W) / 2; }
    private int py() { return (height - PANEL_H) / 2; }

    private List<PlayerListEntry> players() {
        List<PlayerListEntry> list = new ArrayList<>();
        var handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler != null) list.addAll(handler.getPlayerList());
        if (sortByName) {
            list.sort(Comparator.comparing(e -> e.getProfile().getName().toLowerCase()));
        }
        return list;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, BG_DIM);

        int x = px(), y = py();
        // Panel
        ctx.fill(x - 1, y - 1, x + PANEL_W + 1, y + PANEL_H + 1, PANEL_BORDER);
        ctx.fill(x, y, x + PANEL_W, y + PANEL_H, PANEL);

        drawHeader(ctx, x, y, mouseX, mouseY);
        switch (tab) {
            case GRACZ -> drawPlayers(ctx, x, y, mouseX, mouseY);
            case VISUALS -> drawToggles(ctx, x, y, mouseX, mouseY, true);
            case USTAWIENIA -> drawToggles(ctx, x, y, mouseX, mouseY, false);
        }
        drawFooter(ctx, x, y, mouseX, mouseY);
    }

    private void drawHeader(DrawContext ctx, int x, int y, int mx, int my) {
        var mc = MinecraftClient.getInstance();
        String name = mc.getSession().getUsername();
        ctx.fill(x + 10, y + 7, x + 28, y + 25, ACCENT);
        ctx.drawText(textRenderer, "G", x + 16, y + 12, TEXT, false);
        ctx.drawText(textRenderer, name, x + 36, y + 8, TEXT, false);
        ctx.drawText(textRenderer, tab.label, x + 36, y + 18, TEXT_DIM, false);

        // Przycisk zamkniecia
        int cx = x + PANEL_W - 26, cy = y + 7;
        boolean hover = inside(mx, my, cx, cy, 18, 18);
        ctx.fill(cx, cy, cx + 18, cy + 18, hover ? CARD_HOVER : CARD);
        ctx.drawText(textRenderer, "x", cx + 6, cy + 5, TEXT, false);
        ctx.fill(x, y + HEADER_H, x + PANEL_W, y + HEADER_H + 1, PANEL_BORDER);
    }

    private void drawPlayers(DrawContext ctx, int x, int y, int mx, int my) {
        int top = y + HEADER_H + 6;
        int bottom = y + PANEL_H - FOOTER_H - 4;
        ctx.enableScissor(x, top, x + PANEL_W, bottom);

        List<PlayerListEntry> list = players();
        for (int i = 0; i < list.size(); i++) {
            int col = i % 2, row = i / 2;
            int cx = x + 10 + col * (CARD_W + GAP);
            int cy = top + row * (CARD_H + GAP) - scroll;
            if (cy + CARD_H < top || cy > bottom) continue;

            PlayerListEntry e = list.get(i);
            String pname = e.getProfile().getName();
            boolean hover = inside(mx, my, cx, cy, CARD_W, CARD_H) && my >= top && my <= bottom;
            ctx.fill(cx, cy, cx + CARD_W, cy + CARD_H, hover ? CARD_HOVER : CARD);

            // Glowa gracza
            var skin = e.getSkinTextures().texture();
            ctx.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured,
                    skin, cx + 4, cy + 4, 8.0f, 8.0f, 20, 20, 8, 8, 64, 64);

            ctx.drawText(textRenderer, trim(pname, 62), cx + 28, cy + 6, TEXT, false);
            String sub = showPing ? e.getLatency() + " ms" : "online";
            ctx.drawText(textRenderer, sub, cx + 28, cy + 16, TEXT_DIM, false);

            // Przycisk Search
            int bx = cx + CARD_W - 50, by = cy + 5;
            boolean bh = inside(mx, my, bx, by, 44, 18);
            ctx.fill(bx, by, bx + 44, by + 18, bh ? ACCENT_HOVER : ACCENT);
            ctx.drawText(textRenderer, "Search", bx + 7, by + 5, TEXT, false);
        }
        ctx.disableScissor();
    }

    private void drawToggles(DrawContext ctx, int x, int y, int mx, int my, boolean visuals) {
        String[] labels = visuals
                ? new String[]{"Pokaz ping", "Pokaz koordynaty"}
                : new String[]{"Sortuj po nicku", "Kompaktowa lista"};
        boolean[] values = visuals
                ? new boolean[]{showPing, showCoords}
                : new boolean[]{sortByName, compactList};

        for (int i = 0; i < labels.length; i++) {
            int cx = x + 10, cy = y + HEADER_H + 10 + i * (CARD_H + GAP);
            int w = PANEL_W - 20;
            boolean hover = inside(mx, my, cx, cy, w, CARD_H);
            ctx.fill(cx, cy, cx + w, cy + CARD_H, hover ? CARD_HOVER : CARD);
            ctx.drawText(textRenderer, labels[i], cx + 10, cy + 10, TEXT, false);

            int tx = cx + w - 34, ty = cy + 8;
            ctx.fill(tx, ty, tx + 24, ty + 12, values[i] ? ACCENT : 0xFF33333D);
            int knob = values[i] ? tx + 13 : tx + 1;
            ctx.fill(knob, ty + 1, knob + 10, ty + 11, TEXT);
        }
    }

    private void drawFooter(DrawContext ctx, int x, int y, int mx, int my) {
        int fy = y + PANEL_H - FOOTER_H;
        ctx.fill(x, fy, x + PANEL_W, fy + 1, PANEL_BORDER);

        int tabW = 62;
        int total = Tab.values().length * tabW;
        int sx = x + (PANEL_W - total) / 2;
        for (int i = 0; i < Tab.values().length; i++) {
            Tab t = Tab.values()[i];
            int tx = sx + i * tabW;
            boolean active = t == tab;
            boolean hover = inside(mx, my, tx, fy + 5, tabW - 4, 26);
            if (active) ctx.fill(tx, fy + 5, tx + tabW - 4, fy + 31, 0xFF2A1C22);
            else if (hover) ctx.fill(tx, fy + 5, tx + tabW - 4, fy + 31, CARD);
            int color = active ? ACCENT : TEXT_DIM;
            int w = textRenderer.getWidth(t.label);
            ctx.drawText(textRenderer, t.label, tx + (tabW - 4 - w) / 2, fy + 14, color, false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        int x = px(), y = py();

        // Zamknij
        if (inside(mx, my, x + PANEL_W - 26, y + 7, 18, 18)) { close(); return true; }

        // Zakladki
        int fy = y + PANEL_H - FOOTER_H;
        int tabW = 62;
        int sx = x + (PANEL_W - Tab.values().length * tabW) / 2;
        for (int i = 0; i < Tab.values().length; i++) {
            if (inside(mx, my, sx + i * tabW, fy + 5, tabW - 4, 26)) {
                tab = Tab.values()[i];
                scroll = 0;
                return true;
            }
        }

        if (tab == Tab.GRACZ) {
            int top = y + HEADER_H + 6;
            int bottom = y + PANEL_H - FOOTER_H - 4;
            List<PlayerListEntry> list = players();
            for (int i = 0; i < list.size(); i++) {
                int cx = x + 10 + (i % 2) * (CARD_W + GAP);
                int cy = top + (i / 2) * (CARD_H + GAP) - scroll;
                if (my < top || my > bottom) continue;
                if (inside(mx, my, cx + CARD_W - 50, cy + 5, 44, 18)) {
                    onSearch(list.get(i));
                    return true;
                }
            }
        } else {
            for (int i = 0; i < 2; i++) {
                int cx = x + 10, cy = y + HEADER_H + 10 + i * (CARD_H + GAP);
                if (inside(mx, my, cx, cy, PANEL_W - 20, CARD_H)) {
                    toggle(tab == Tab.VISUALS, i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void toggle(boolean visuals, int i) {
        if (visuals) { if (i == 0) showPing = !showPing; else showCoords = !showCoords; }
        else { if (i == 0) sortByName = !sortByName; else compactList = !compactList; }
    }

    /** Akcja przycisku Search - pokazuje w czacie informacje o graczu z listy. */
    private void onSearch(PlayerListEntry e) {
        searchedPlayer = e.getProfile().getName();
        var mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("[GoatGUI] ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal(searchedPlayer + " | ping: " + e.getLatency() + " ms")
                            .formatted(Formatting.WHITE)), false);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (tab == Tab.GRACZ) {
            int rows = (players().size() + 1) / 2;
            int content = rows * (CARD_H + GAP);
            int view = PANEL_H - HEADER_H - FOOTER_H - 10;
            int max = Math.max(0, content - view);
            scroll = Math.max(0, Math.min(max, scroll - (int) (v * 14)));
            return true;
        }
        return super.mouseScrolled(mx, my, h, v);
    }

    private String trim(String s, int maxPx) {
        return textRenderer.getWidth(s) <= maxPx ? s : textRenderer.trimToWidth(s, maxPx - 6) + "..";
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean shouldPause() { return false; }
}
