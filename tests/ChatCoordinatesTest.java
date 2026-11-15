package com.clickcoords;

import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/** Uses Minecraft's real components and legacy-format decoder, without a game window. */
public class ChatCoordinatesTest {
    private static void each(Component c, BiConsumer<Style, String> fn) {
        c.<Void>visit((style, text) -> {
            if (!text.isEmpty()) fn.accept(style, text);
            return Optional.empty();
        }, Style.EMPTY);
    }
    private static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }
    private static void coords(Component c, String visible, String copy) {
        Component d = ChatCoordinates.decorate(c, true);
        var clickable = new StringBuilder();
        each(d, (style, text) -> {
            if (copy.equals(ChatCoordinates.recognizedCopy(style.getClickEvent()))) {
                check(style.isUnderlined(), "coordinate wasn't underlined");
                check(style.getColor().getValue() == 0x55FFFF, "coordinate wasn't aqua");
                clickable.append(text);
            }
        });
        check(clickable.toString().equals(visible), "wrong clickable text: " + clickable);
    }
    public static void main(String[] args) {
        String header = "════ « Загадочный маяк » ════\nПризван игроком Test\nна координатах ";
        coords(Component.literal(header + "§6[1752 129 2393]§r\n════"), "[1752 129 2393]", "1752 129 2393");
        coords(Component.literal(header + "§6[§e1752 §c129 §62393]§r"), "[1752 129 2393]", "1752 129 2393");
        coords(Component.literal("Аирдроп\nПризван игроком Test\nна координатах §6[-1494 113 -2134]"),
            "[-1494 113 -2134]", "-1494 113 -2134");
        coords(Component.literal("незер починка на кордах ").append(
                Component.literal("780 ").withStyle(ChatFormatting.RED)).append(
                Component.literal("2500").withStyle(ChatFormatting.BOLD)), "780 2500", "780 2500");

        var serverAction = new ClickEvent.SuggestCommand("/event beacon");
        Component event = Component.literal(header + "§6[1752 129 2393]§r\nКнопка").setStyle(
                Style.EMPTY.withClickEvent(serverAction));
        coords(event, "[1752 129 2393]", "1752 129 2393");
        each(ChatCoordinates.decorate(event, true), (style, text) -> {
            if (text.contains("Кнопка") || text.contains("Загадочный маяк")) {
                check(style.getClickEvent() == serverAction, "unrelated server action changed");
            }
        });

        Component generic = Component.literal("Help: [1 2 3]").setStyle(Style.EMPTY.withClickEvent(serverAction));
        check(ChatCoordinates.decorate(generic, true) == generic, "generic server action wasn't preserved");
        Component unrelatedCopy = Component.literal("click").setStyle(
                Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard("780 2500")));
        each(unrelatedCopy, (style, text) ->
                check(ChatCoordinates.recognizedCopy(style.getClickEvent()) == null, "foreign copy accepted"));
        System.out.println("7 Minecraft component regressions passed");
    }
}
