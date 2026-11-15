package com.clickcoords;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Recognizes coordinate triples without interpreting any chat as a command. */
public final class CoordinateParser {
    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final int MAX_MESSAGE_LENGTH = 16_384;
    private static final int MAX_MATCHES = 32;
    private static final String WS = "[\\p{Zs}\\t]";
    private static final String H = WS + "*";
    private static final String NUM = "[+\\-−]?[0-9]{1,8}(?:\\.[0-9]{1,12})?";
    private static final String LEFT = "(?<![\\p{L}\\p{N}_.:/+−\\-])";
    private static final String RIGHT = "(?![\\p{L}\\p{N}_:/+−\\-]|\\.[0-9])";
    private static final String DIV = "(?:" + H + "[,;]" + H + "|" + WS + "+)";
    private static final String PUNCT = "[:,;=：]";
    private static final String CUE = "(?iu)(?:\\b(?:coordinates?|coords?|xyz)"
            + "|(?:координат(?:ы|ах|ам|ами|у|е|ов)?|корд(?:ы|ах|ам|ами|у|ов)?|точк(?:а|е|и|у)))"
            + H + "(?:[=:#：–—]" + H + ")?$";
    private static final Pattern COORD_CUE = Pattern.compile(CUE);
    private static final Pattern NUMBER_LIST_CUE = Pattern.compile("(?iu)(?:"
            + "\\b(?:stats?|scores?|totals?|time|date|ip|ping|levels?|coins|kills|deaths|assists|damage|price|counts?)"
            + "|(?:статистик[аи]|статы|очки|сч[её]т|время|дата|пинг|уровни|монет[ы]?|урон|цен[аы]))"
            + H + "(?:[=:#：–—]" + H + ")?$");
    private static final Pattern SPAWN_CUE = Pattern.compile("(?iu)(?:"
            + "\\b(?:spawn(?:ed|s)?|appeared|generated|located|event)"+ WS + "+(?:at|near|on|position)"
            + "|(?:заспавнил[\\p{L}]*|спавн[\\p{L}]*|появил[\\p{L}]*|возник[\\p{L}]*|событие)"
            + WS + "+(?:на|в|по|около))" + H + "(?:[=:#：–—]" + H + ")?$");
    private static final Pattern LABELED = Pattern.compile(
            "(?<![\\p{L}\\p{N}_])x" + H + "(?:" + PUNCT + H + ")?(" + NUM + ")"
            + DIV + "y" + H + "(?:" + PUNCT + H + ")?(" + NUM + ")"
            + DIV + "z" + H + "(?:" + PUNCT + H + ")?(" + NUM + ")" + RIGHT, FLAGS);
    private static final Pattern BRACKETED = Pattern.compile(
            "([\\[(])" + H + "(" + NUM + ")" + DIV + "(" + NUM + ")" + DIV
            + "(" + NUM + ")" + H + "([\\])])", FLAGS);
    private static final Pattern DELIMITED = Pattern.compile(
            LEFT + "(" + NUM + ")" + H + "([,;])" + H + "(" + NUM + ")" + H
            + "\\2" + H + "(" + NUM + ")" + RIGHT, FLAGS);
    private static final Pattern UNLABELED = Pattern.compile(
            LEFT + "(" + NUM + ")" + WS + "+(" + NUM + ")" + WS + "+(" + NUM + ")" + RIGHT, FLAGS);

    private CoordinateParser() {}

    /** start is inclusive and end exclusive, both UTF-16 offsets into the original line. */
    public record Match(int start, int end, String copy) {}

    public static List<Match> find(String text) {
        if (text == null || text.length() < 5 || text.length() > MAX_MESSAGE_LENGTH) return List.of();
        ArrayList<Match> out = new ArrayList<>();
        Matcher labels = LABELED.matcher(text);
        while (out.size() < MAX_MATCHES && labels.find()) add(out, labels, 1, 2, 3);
        Matcher brackets = BRACKETED.matcher(text);
        while (out.size() < MAX_MATCHES && brackets.find()) {
            boolean square = "[".equals(brackets.group(1));
            if (square != "]".equals(brackets.group(5))) continue;
            if (looksLikeListOfGroups(text, brackets.start(), brackets.end()) || hasNumberListCue(text, brackets.start())) continue;
            add(out, brackets, 2, 3, 4);
        }
        Matcher delimited = DELIMITED.matcher(text);
        while (out.size() < MAX_MATCHES && delimited.find()) {
            if (adjacentDelimiterValue(text, delimited.start(), delimited.end(), delimited.group(2).charAt(0))
                    || hasNumberListCue(text, delimited.start())) continue;
            if (looksLikeThousands(delimited) && !hasCoordinateCue(text, delimited.start())) continue;
            add(out, delimited, 1, 3, 4);
        }
        Matcher plain = UNLABELED.matcher(text);
        while (out.size() < MAX_MATCHES && plain.find()) {
            if (!hasCoordinateCue(text, plain.start()) || adjacentWhitespaceValue(text, plain.end())) continue;
            add(out, plain, 1, 2, 3);
        }
        out.sort(Comparator.comparingInt(Match::start));
        return List.copyOf(out);
    }

    private static void add(List<Match> out, Matcher m, int x, int y, int z) {
        int start = m.start(), end = m.end();
        for (Match existing : out) if (existing.start < end && start < existing.end) return;
        out.add(new Match(start, end, normalize(m.group(x)) + " " + normalize(m.group(y)) + " " + normalize(m.group(z))));
    }

    private static String normalize(String number) {
        String n = number.replace('−', '-');
        return n.charAt(0) == '+' ? n.substring(1) : n;
    }

    private static boolean hasCoordinateCue(String line, int before) {
        String prefix = line.substring(Math.max(0, before - 96), before);
        return COORD_CUE.matcher(prefix).find() || SPAWN_CUE.matcher(prefix).find();
    }

    private static boolean hasNumberListCue(String line, int before) {
        String prefix = line.substring(Math.max(0, before - 64), before);
        return NUMBER_LIST_CUE.matcher(prefix).find();
    }

    private static boolean adjacentDelimiterValue(String line, int start, int end, char sep) {
        int prev = skipBack(line, start - 1);
        int next = skipForward(line, end);
        if (prev >= 0 && line.charAt(prev) == sep) {
            prev = skipBack(line, prev - 1);
            if (prev >= 0 && asciiDigit(line.charAt(prev))) return true;
        }
        return next < line.length() && line.charAt(next) == sep && startsNumber(line, skipForward(line, next + 1));
    }

    private static boolean adjacentWhitespaceValue(String line, int end) {
        int next = skipForward(line, end);
        return next > end && startsNumber(line, next);
    }

    private static boolean looksLikeListOfGroups(String line, int start, int end) {
        // A bracketed section must be on its own, not part of a glued identifier.
        return start > 0 && asciiDigit(line.charAt(start - 1))
                || end < line.length() && asciiDigit(line.charAt(end));
    }

    private static boolean looksLikeThousands(Matcher m) {
        return ",".equals(m.group(2)) && m.group().indexOf(' ') < 0
                && m.group(1).matches("[+\\-−]?[0-9]{1,3}")
                && m.group(3).matches("[0-9]{3}") && m.group(4).matches("[0-9]{3}");
    }

    private static boolean startsNumber(String line, int p) {
        if (p >= line.length()) return false;
        char c = line.charAt(p);
        if (c == '-' || c == '−' || c == '+') return p + 1 < line.length() && asciiDigit(line.charAt(p + 1));
        return asciiDigit(c);
    }

    private static int skipForward(String line, int p) {
        while (p < line.length() && horizontalWhitespace(line.charAt(p))) p++;
        return p;
    }

    private static int skipBack(String line, int p) {
        while (p >= 0 && horizontalWhitespace(line.charAt(p))) p--;
        return p;
    }

    private static boolean asciiDigit(char c) { return c >= '0' && c <= '9'; }

    private static boolean horizontalWhitespace(char c) {
        return c == '\t' || Character.getType(c) == Character.SPACE_SEPARATOR;
    }
}
