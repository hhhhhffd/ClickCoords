package com.clickcoords;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
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
import net.minecraft.util.StringDecomposer;

public final class ChatCoordinates {
    private static final ArrayDeque<WeakReference<ClickEvent.CopyToClipboard>> LOCAL_CLICKS = new ArrayDeque<>();

    private ChatCoordinates() {}

    /** Reference identity distinguishes our decoration from an identical server-provided copy action. */
    public static String recognizedCopy(ClickEvent event) {
        if (!(event instanceof ClickEvent.CopyToClipboard copy)) return null;
        for (var reference : LOCAL_CLICKS) {
            if (reference.get() == copy) return copy.value();
        }
        return null;
    }

    private static ClickEvent.CopyToClipboard createLocalClick(String coordinates) {
        var event = new ClickEvent.CopyToClipboard(coordinates);
        if (LOCAL_CLICKS.size() >= 4096) {
            LOCAL_CLICKS.removeIf(reference -> reference.get() == null);
            if (LOCAL_CLICKS.size() >= 4096) LOCAL_CLICKS.removeFirst();
        }
        LOCAL_CLICKS.addLast(new WeakReference<>(event));
        return event;
    }

    private record Span(int start, int end, Style style) {}
    private record Edit(CoordinateParser.Match match, ClickEvent.CopyToClipboard event) {
        int start() { return match.start(); }
        int end() { return match.end(); }
        String copy() { return match.copy(); }
    }

    public static Component decorate(Component message) {
        return decorate(message, "ru_ru".equals(Minecraft.getInstance().getLanguageManager().getSelected()));
    }

    static Component decorate(Component message, boolean russian) {
        StringBuilder plain = new StringBuilder();
        List<Span> spans = new ArrayList<>();
        message.<Void>visit((style, text) -> {
            // Vanilla removes embedded legacy formatting before rendering. Match that visible text.
            if (text.indexOf('§') < 0) {
                appendSpan(plain, spans, text, style);
            } else {
                StringDecomposer.iterateFormatted(text, style, (index, appliedStyle, codePoint) -> {
                    appendSpan(plain, spans, Character.toString(codePoint), appliedStyle);
                    return true;
                });
            }
            return Optional.empty();
        }, Style.EMPTY);

        String text = plain.toString();
        List<CoordinateParser.Match> hits = CoordinateParser.find(text);
        if (hits.isEmpty()) return message;

        // Preserve server/plugin click actions by default. On explicitly labeled coordinates,
        // only the coordinate span itself gets the local copy action; other links are unchanged.
        List<Edit> edits = new ArrayList<>();
        for (var hit : hits) {
            boolean occupied = false;
            for (Span span : spans) {
                if (span.start >= hit.end()) break;
                if (span.end > hit.start() && span.style.getClickEvent() != null) {
                    occupied = true;
                    break;
                }
            }
            if (!occupied || CoordinateParser.explicitCoordinates(text, hit)) {
                edits.add(new Edit(hit, createLocalClick(hit.copy())));
            }
        }
        if (edits.isEmpty()) return message;

        // Loader alone does not register assets from mods as vanilla resource packs.
        String tooltip = russian ? "Нажми, чтобы скопировать: %s" : "Click to copy: %s";
        String waypointTooltip = russian ? "Shift+клик: поставить или снять метку" : "Shift+click: toggle waypoint";
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
                        .withClickEvent(hit.event());
                if (span.style.getHoverEvent() == null || span.style.getClickEvent() != null) {
                    styled = styled.withHoverEvent(new HoverEvent.ShowText(
                            Component.translatableWithFallback("clickcoords.tooltip.copy", tooltip, hit.copy())
                                    .append("\n").append(Component.translatableWithFallback(
                                            "clickcoords.tooltip.waypoint", waypointTooltip))));
                }
                int end = Math.min(span.end, hit.end());
                append(output, text, cursor, end, styled);
                cursor = end;
            }
        }
        return output;
    }

    private static void appendSpan(StringBuilder plain, List<Span> spans, String text, Style style) {
        if (text.isEmpty()) return;
        int start = plain.length();
        plain.append(text);
        if (!spans.isEmpty() && spans.getLast().style.equals(style)) {
            Span last = spans.removeLast();
            start = last.start;
        }
        spans.add(new Span(start, plain.length(), style));
    }

    private static void append(MutableComponent parent, String text, int from, int to, Style style) {
        if (from < to) parent.append(Component.literal(text.substring(from, to)).setStyle(style));
    }
}
