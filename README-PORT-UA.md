# GCR Forge 1.20.1 — Stage 4

Це вихідний код, не готовий JAR. Порт базується на Stage 3. Весь Iris/Oculus-шлях вимкнений — клієнт орієнтований на Embeddium + Luxium, сумісність ще не протестована.

Додатково перенесено старий API формування вершин, часткове формування VBO моделей зброї та Forge PlayerTickEvent. Для перевірки виконайте `tools/check-prerequisites.sh`, установивши Java 17 та Gradle 8.x і додавши файл `libs/ldlib2-forge-1.20.1-all.jar`, зібраний із https://github.com/weiliangyan/LDLib2-1.20.1-Forge.

У середовищі підготовки доступні лише локальні статичні та синтаксичні перевірки — запуск `compileJava` наразі неможливий. Звіт `PORT-VALIDATION-STAGE4.json` містить результати.
