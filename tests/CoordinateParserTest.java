import com.clickcoords.CoordinateParser;
import java.util.List;

public class CoordinateParserTest {
    private static int cases;

    private static void expect(String text, String... pairs) {
        if (pairs.length % 2 != 0) throw new IllegalArgumentException();
        var matches = CoordinateParser.find(text);
        if (matches.size() * 2 != pairs.length) {
            throw new AssertionError("wrong match count of " + text + ": " + matches + " expected " + List.of(pairs));
        }
        for (int i = 0; i < matches.size(); i++) {
            var match = matches.get(i);
            String raw = text.substring(match.start(), match.end());
            if (!pairs[i * 2].equals(raw) || !pairs[i * 2 + 1].equals(match.copy())) {
                throw new AssertionError("wrong match of " + text + ": " + raw + " → " + match.copy());
            }
        }
        cases++;
    }

    public static void main(String[] args) {
        expect("💫 Метеорит заспавнился на координатах -125 72 345!",
                "-125 72 345", "-125 72 345");
        expect("Метеорит заспавнился на -125 72 345!", "-125 72 345", "-125 72 345");
        expect("Метеорит появился в точке -125 72 345!", "-125 72 345", "-125 72 345");
        expect("Meteor spawned at 120 72 -345!", "120 72 -345", "120 72 -345");
        expect("Coords: +134.5 −22 0.5", "+134.5 −22 0.5", "134.5 -22 0.5");
        expect("Coordinates: 1\u00A02\u20083", "1\u00A02\u20083", "1 2 3");
        expect("Signal: [99 -3 100]; next: (12.5, 80, -33).",
                "[99 -3 100]", "99 -3 100", "(12.5, 80, -33)", "12.5 80 -33");
        expect("A x: -100, Y=65; z +300.33.", "x: -100, Y=65; z +300.33", "-100 65 300.33");
        expect("X12 Y-7 Z70", "X12 Y-7 Z70", "12 -7 70");
        expect("X: 12, Y: 71, Z: -4", "X: 12, Y: 71, Z: -4", "12 71 -4");
        expect("Signals: 120, 64, 337 or -12;-22;-43", "120, 64, 337", "120 64 337", "-12;-22;-43", "-12 -22 -43");
        expect("Found marker at (120,151,123)", "(120,151,123)", "120 151 123");
        expect("Coords: 120,151,123", "120,151,123", "120 151 123");
        expect("Total: 120,151,123 coins");
        expect("Play time: 12:34:56");
        expect("Version v1.21.11; address 123.45.67.89");
        expect("Dates: 2026-11-14, then 2026/11/15");
        expect("Kill count: 17 34 56");
        expect("Spawned 12 34 56 mobs");
        expect("Stats: 1,2,3,4");
        expect("Stats: 11, 21, 31");
        expect("Статы: [12 22 32]");
        expect("Date: 14, 11, 2026");
        expect("Price: 12; 34; 56");
        expect("Coords: 1 2 3 4");
        expect("No: a17,22,31; then abc17 22 31");
        expect("No: -1,2,3.4.5 and IP: 1.2.3.4");
        expect("x: 1 y: 2 and z: 3");
        expect("truex1 y2 z3");
        expect("No decimals separated by commas: [1,123,2,345,3,567]");
        expect("A [1,2,3], B [4 5 6].", "[1,2,3]", "1 2 3", "[4 5 6]", "4 5 6");
        expect("At coords: 1 2.0 -3." , "1 2.0 -3", "1 2.0 -3");
        expect("════ « Загадочный маяк » ════\nПризван игроком Игрок\nна координатах [1752 129 2393]\n════",
                "[1752 129 2393]", "1752 129 2393");
        expect("незер починка на кордах 780 2500", "780 2500", "780 2500");
        expect("Аирдроп\nПризван игроком Игрок\nна координатах [-1494 113 -2134]",
                "[-1494 113 -2134]", "-1494 113 -2134");
        expect("[780 2500]", "[780 2500]", "780 2500");
        expect("Coords: (-1.5; +2)", "(-1.5; +2)", "-1.5 2");
        expect("X: -35, Z: +66", "X: -35, Z: +66", "-35 66");
        expect("780 2500", "780 2500", "780 2500");
        expect("780 60 2500", "780 60 2500", "780 60 2500");
        expect("Kills: 780 2500");
        expect("Куплю за 780 2500 монет");
        expect("Price: [1,234]; remaining [1,234]");
        expect("1,234");
        expect("Coords: 1,234", "1,234", "1 234");
        expect("Coords: 1 2 3 4");
        expect("Coords: 1,2,3,4");
        expect("Time: 12:30");
        expect("Coords: 780 2500 and XYZ: 1 20 30", "780 2500", "780 2500", "1 20 30", "1 20 30");
        expect("☉ [Князь] _k0t1k ➜ 100 400", "100 400", "100 400");
        expect("[Князь] Player » 100 400", "100 400", "100 400");
        expect("⚡[Игрок] Player ⇒ -120 64 912", "-120 64 912", "-120 64 912");
        expect("<Player> 100 400", "100 400", "100 400");
        expect("<Player> 100 60 400", "100 60 400", "100 60 400");
        expect("Player: 100, 400", "100, 400", "100 400");
        expect("Time: 12 30");
        expect("<Player> продам за 100 400 монет");
        expect("[Князь] Player ➜ 100 60 400 500");
        expect("[Игрок] Player ➜ где 100 400 в чате");
        expect("Invalid[/warp] 100 400");
        System.out.println(cases + " Java parser test cases passed");
    }
}
