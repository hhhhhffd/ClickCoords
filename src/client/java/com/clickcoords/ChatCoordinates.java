package com.clickcoords;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class ChatCoordinates {
    private ChatCoordinates() {}

    private record Span(int start, int end, Style style) {}

    public static Component decorate(Component message) {
        StringBuilder plain = new StringBuilder();
        List<Span> spans = new ArrayList<>();
        message.<Void>visit((style, text) -> {
            if (!text.isEmpty()) {
                int start = plain.length();
                plain.append(text);
                spans.add(new Span(start, plain.length(), style));
            }
            return Optional.empty();
        }, Style.EMPTY);

        String text = plain.toString();
        List<CoordinateParser.Match> hits = CoordinateParser.find(text);
        if (hits.isEmpty()) return message;

        // If any part already has a server/plugin click action, preserve the entire match as-is.
        // A transformed label never starts a command or changes a pre-existing link.
        List<CoordinateParser.Match> edits = new ArrayList<>();
        for (var hit : hits) {
            boolean occupied = false;
            for (Span span : spans) {
                if (span.start >= hit.end()) break;
                if (span.end > hit.start() && span.style.getClickEvent() != null) {
                    occupied = true;
                    break;
                }
            }
            if (!occupied) edits.add(hit);
        }
        if (edits.isEmpty()) return message;

        // Loader alone does not register assets from mods as vanilla resource packs.
        String lang = Minecraft.getInstance().getLanguageManager().getSelected();
        String tooltip = "ru_ru".equals(lang) ? "Нажми, чтобы скопировать: %s" : "Click to copy: %s";
        MutableComponent output = Component.empty();
        int editIndex = 0;
        for (Span span : spans) {
            int cursor = span.start;
            while (cursor < span.end) {
                while (editIndex < edits.size() && edits.get(editIndex).end() <= cursor) editIndex++;
                var hit = editIndex == edits.size() ? null : edits.get(editIndex);
                if (hit == null || hit.start() >= span.end) {
                    append(output, text, cursor, span.end, span.style);
                    break;
                }
                if (cursor < hit.start()) {
                    append(output, text, cursor, hit.start(), span.style);
                    cursor = hit.start();
                }
                Style styled = span.style.withColor(ChatFormatting.AQUA).withUnderlined(true)
                        .withClickEvent(new ClickEvent.CopyToClipboard(hit.copy()));
                if (span.style.getHoverEvent() == null) {
                    styled = styled.withHoverEvent(new HoverEvent.ShowText(
                            Component.translatableWithFallback("clickcoords.tooltip.copy", tooltip, hit.copy())));
                }
                int end = Math.min(span.end, hit.end());
                append(output, text, cursor, end, styled);
                cursor = end;
            }
        }
        return output;
    }

    private static void append(MutableComponent parent, String text, int from, int to, Style style) {
        if (from < to) parent.append(Component.literal(text.substring(from, to)).setStyle(style));
    }
}
