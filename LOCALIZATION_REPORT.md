# Исправлено

- Локализованы подтверждённые пользовательские строки в progress/dialog-области: импорт карты высот, импорт маски, анализ карты, масштабирование, поворот, сдвиг, создание измерений, автосохранение, удаление пользовательского биома.
- Локализованы сообщения и заголовки Merge: окно выполнения, название операции, progress-текст, отчёт о результате, путь резервной копии, сообщение отмены.
- Локализованы предупреждения результата Export о нестандартной высоте, высоте больше 384 блоков и data pack для нестандартной высоты. Названия и диапазоны Minecraft сохранены без перевода.
- Локализованы startup/recovery сообщения: второй экземпляр WorldPainter, восстановление из autosave, ошибка чтения конфигурации, заголовки Startup Error/Warning, предупреждение snapshot-релиза.
- Локализованы заголовки ошибок progress dialog `Invalid Map` и `Incompatible Material`, а также сообщение MaterialSelector о несовместимом материале.
- Локализованы оставшиеся английские вставки `optimising/scaling` в пользовательском сообщении ошибки обработки изображения.

Изменённые файлы:

- `WPGUI/src/main/java/org/pepsoft/worldpainter/App.java`
- `WPGUI/src/main/java/org/pepsoft/worldpainter/ExportProgressDialog.java`
- `WPGUI/src/main/java/org/pepsoft/worldpainter/ImportHeightMapDialog.java`
- `WPGUI/src/main/java/org/pepsoft/worldpainter/Main.java`
- `WPGUI/src/main/java/org/pepsoft/worldpainter/MaterialSelector.java`
- `WPGUI/src/main/java/org/pepsoft/worldpainter/MergeProgressDialog.java`
- `WPGUI/src/main/java/org/pepsoft/worldpainter/MultiProgressDialog.java`
- `WPGUI/src/main/java/org/pepsoft/worldpainter/RotateWorldDialog.java`
- `WPGUI/src/main/java/org/pepsoft/worldpainter/ScaleWorldDialog.java`
- `WPGUI/src/main/java/org/pepsoft/worldpainter/ShiftWorldDialog.java`
- `WPGUI/src/main/java/org/pepsoft/worldpainter/WorldPainter.java`
- `WPGUI/src/main/java/org/pepsoft/worldpainter/importing/ImportMaskDialog.java`
- `WPGUI/src/main/java/org/pepsoft/worldpainter/importing/MapImportDialog.java`
- `WPGUI/src/main/java/org/pepsoft/worldpainter/tools/BiomesViewerFrame.java`
- `WPGUI/src/main/resources/org/pepsoft/worldpainter/resources/strings.properties`
- `WPGUI/src/main/resources/org/pepsoft/worldpainter/resources/strings_ru.properties`

# Проверено

- Выполнен статический аудит по `JOptionPane`, startup/recovery, import/export/merge, progress dialog/task, MaterialSelector и связанным progress-текстам.
- Проверены добавленные ключи в `strings.properties` и `strings_ru.properties`.
- Выполнено `mvn clean package`: сборка успешна, WPCore tests: 9 run, 0 failures, 0 errors, 1 skipped; WPGUI tests: 0 run.
- Выполнено `mvn -pl WPGUI exec:exec`: команда стартовала GUI-процесс и не завершилась за 60 секунд, после таймаута оставшиеся Java-процессы остановлены.

# Требует ручной проверки

- Визуальная проверка GUI не подтверждена автоматически: окно WorldPainter не было надёжно зафиксировано средствами текущей среды.
- Требуется ручной проход по главному окну, меню, toolbar/status bar, Preferences, About, Import, Export, Merge, Recovery, Startup, Plugins, JFileChooser/JOptionPane/JDialog и редким ошибочным сценариям.
- Отдельно проверить предупреждения Export для платформ с data pack и нестандартной высотой, потому что они зависят от выбранного формата карты.

# Осталось непереведённым

- Подтверждённых новых hardcoded UI-строк в исправленной области после патча не осталось.
- Не переводились названия платформ Minecraft, диапазоны версий Minecraft, идентификаторы форматов, расширения и имена файлов.
- Английский `strings.properties` оставлен как базовый fallback bundle.

# Ограничения текущей архитектуры

- В проекте одновременно используются старые ключи `strings.*` и сгенерированные `WPI18n`-ключи вида `ui.h.*`; архитектура локализации не менялась.
- `strings_ru.properties` уже содержит смешанные участки escaped Unicode и UTF-8/повреждённо отображаемого текста; новые русские ключи добавлялись в безопасном escaped Unicode.
- Часть строк приходит из внешних источников: имена пользовательских слоёв, материалов, файлов, путей, платформ, модулей и плагинов.
- Системные подписи Swing/OS Look&Feel и сторонних диалогов могут зависеть от окружения и требуют ручной проверки на целевой машине.
