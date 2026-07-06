# CHANGELOG

## [2.27.0-r1] - 2026-07-06

Первый публичный релиз WorldPainter RU на базе WorldPainter 2.27.0.

### Добавлено

- Русская локализация интерфейса.
- Русские resource bundles для основных строк и enum-значений.
- Локализация стандартных Swing/JOptionPane/JFileChooser строк.
- Документация для публикации на GitHub.
- Release checklist и packaging notes.
- Скрипт `scripts/build-windows-installer.ps1` для сборки Windows installer через `jpackage`.
- Portable layout и release assets для GitHub Releases.

### Изменено

- Пользовательские строки во многих UI-классах переведены на существующую систему ResourceBundle.
- Диалоги, progress-сообщения, import/export/merge/startup/recovery тексты подготовлены для русской локализации.
- Доработаны installer metadata, иконка, portable layout и структура release assets.
- Диалог поддержки приведён к более компактному размеру и дополнен ссылкой на проект перевода.

### Не изменялось

- Алгоритмы генерации миров.
- Форматы файлов.
- Maven-структура проекта.
- Бизнес-логика WorldPainter.
- UI-архитектура и внешний вид.

### Известные ограничения

- Не все редкие аварийные окна можно подтвердить без ручного GUI-тестирования.
- Стороннее предупреждение JIDE о лицензии не относится к русификации и требует отдельного решения.
