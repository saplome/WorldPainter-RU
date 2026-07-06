# CHANGELOG

## [2.27.0-r1] - 2026-07-05

Первый подготовительный релиз WorldPainter RU на базе WorldPainter 2.27.0.

### Добавлено

- Русская локализация интерфейса.
- Русские resource bundles для основных строк и enum-значений.
- Локализация стандартных Swing/JOptionPane/JFileChooser строк.
- Документация для публикации на GitHub.
- Release checklist.
- Packaging notes для будущей сборки installer.exe.
- Заготовка `scripts/build-windows-installer.ps1` для jpackage.

### Изменено

- Пользовательские строки во многих UI-классах переведены на существующую систему ResourceBundle.
- Диалоги, progress-сообщения, import/export/merge/startup/recovery тексты подготовлены для русской локализации.

### Не изменялось

- Алгоритмы генерации миров.
- Форматы файлов.
- Maven-структура проекта.
- Бизнес-логика WorldPainter.
- UI-архитектура и внешний вид.

### Известно

- Installer pipeline ещё требует финальной проверки runtime layout и classpath.
- Не все редкие аварийные окна можно подтвердить без ручного GUI-тестирования.
- `target/` и другие build artifacts не должны попадать в GitHub-репозиторий.
