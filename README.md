# ClickCoords

Клиентский мод для **Minecraft Java 1.21.11 / Fabric**. Делает распознанные координаты в чате голубыми и кликабельными. Когда чат открыт, нажми левой кнопкой по координатам: в буфер попадёт только тройка `x y z`. Мод не выполняет команды, не отправляет сообщение и не трогает уже существующие действия по клику.

Примеры: `Ивент заспавнился на координатах -120 70 345`, `Event spawned at X: -120, Y: 70, Z: 345`, `(-12, 64, 245)`. Форматы с пробелами без скобок требуют контекста координат, чтобы обычные числа не стали ссылками.

Кинь JAR в `mods`. Fabric API и установка на сервер не нужны. Сборка: Java 21 и `./gradlew build` (Windows: `gradlew.bat build`).

Автопроверка распознавания без запуска Minecraft (из корня проекта, на JDK 21):

```sh
mkdir -p build/parser-test
javac -d build/parser-test src/client/java/com/clickcoords/CoordinateParser.java tests/CoordinateParserTest.java
java -cp build/parser-test CoordinateParserTest
```
