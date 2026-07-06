# PACKAGING NOTES

Заметки по упаковке WorldPainter RU 2.27.0-r1 в portable layout и Windows installer через `jpackage`.

## Версии релиза и Windows Installer

Публичное имя релиза:

```text
WorldPainter RU 2.27.0-r1
```

Windows Installer `ProductVersion` не поддерживает суффиксы вроде `-r1`. Версия `2.27.0-r1` падает в `jpackage`/WiX с ошибкой о недопустимом компоненте версии.

Поэтому в скрипте используются две версии:

- публичная версия релиза: `2.27.0-r1`;
- техническая Windows Installer версия для `--app-version`: `2.27.1`.

После успешного `jpackage` итоговый exe переименовывается в:

```text
WorldPainter RU 2.27.0-r1 Setup.exe
```

## Метаданные jpackage и кодировка WiX

Скрипт передаёт в `jpackage` продуктовые метаданные:

```powershell
--name "WorldPainter RU"
--app-version "2.27.1"
--vendor "WorldPainter RU"
--description "Русифицированная версия WorldPainter 2.27.0"
```

`--app-version` остаётся технической Windows Installer-версией `2.27.1`, потому что MSI `ProductVersion` не поддерживает суффиксы вроде `-r1`. Публичная версия релиза остаётся `2.27.0-r1` и используется в имени итогового установщика.

Для русскоязычного `--description` используется installer resource:

```text
scripts/jpackage-resources/windows/MsiInstallerStrings_ru.wxl
```

Файл не меняет UI установщика и не дублирует строки WiX. Он только добавляет `ru-ru` localization с `Codepage="1251"`, чтобы WiX мог собрать MSI с кириллическим описанием продукта.

## Почему `WPGUI-2.27.0.jar` не запускается через `java -jar`

`WPGUI/target/WPGUI-2.27.0.jar` - обычный Maven jar модуля `WPGUI`.

Он не является самодостаточным приложением:

- внутри нет всех runtime-зависимостей;
- `WPCore` и `WPDynmapPreviewer` собираются отдельными jar;
- Maven dev-запуск использует classpath, который формирует `exec-maven-plugin`;
- jar не собирается как fat jar;
- исходный manifest не содержит полный `Class-Path`.

Рабочий dev-запуск остаётся таким:

```bash
mvn -pl WPGUI exec:exec
```

## Runtime layout

Скрипт `scripts/build-windows-installer.ps1` собирает layout:

```text
release/
  logs/
  staging/
    app/
      WPGUI-2.27.0.jar
      WPCore-2.27.0.jar
      WPDynmapPreviewer-2.27.0.jar
      lib/
        runtime-dependency-1.jar
        runtime-dependency-2.jar
        ...
  installer/
    WorldPainter RU 2.27.0-r1 Setup.exe
```

Порядок действий:

1. Проверяются `java`, `jar`, `mvn`, `jpackage`, `candle.exe`, `light.exe`.
2. Выполняется `mvn clean package`, если не указан `-SkipMaven`.
3. Выполняется Maven dependency plugin:

```powershell
mvn -pl WPGUI dependency:copy-dependencies `
  -DincludeScope=runtime `
  -DexcludeArtifactIds=WPCore,WPGUI,WPDynmapPreviewer `
  -DoutputDirectory=release\staging\app\lib
```

4. Jar-файлы модулей проекта копируются в `release/staging/app`.
5. Runtime-зависимости, включая `WPValueObjects-1.2.0.jar`, остаются в `release/staging/app/lib`.
6. В staged-копию `WPGUI-2.27.0.jar` добавляются `Main-Class` и `Class-Path`.
7. Без `-BuildInstaller` скрипт останавливается после подготовки layout.
8. С `-BuildInstaller` скрипт вызывает `jpackage`.

## Classpath и jpackage на JDK 17

Проверено по локальной справке `jpackage --help` JDK 17: отдельной опции `--class-path` у `jpackage` нет.

Для non-modular приложения используются:

- `--input`
- `--main-jar`
- `--main-class`

Поэтому classpath задаётся через manifest staged-копии `WPGUI-2.27.0.jar`:

```text
Main-Class: org.pepsoft.worldpainter.Main
Class-Path: WPCore-2.27.0.jar WPDynmapPreviewer-2.27.0.jar lib/...
```

Исходный jar в `WPGUI/target` не изменяется. Изменяется только копия в `release/staging/app`.

## Preparation-only режим

По умолчанию:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-windows-installer.ps1
```

Скрипт собирает Maven-проект, готовит `release/staging/app`, копирует зависимости и показывает содержимое staging layout.

Installer.exe в этом режиме не создаётся.

## Сборка installer.exe

Для реального вызова `jpackage`:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-windows-installer.ps1 -BuildInstaller
```

Команда `jpackage` использует:

```powershell
jpackage `
  --type exe `
  --name "WorldPainter RU" `
  --app-version "2.27.1" `
  --vendor "WorldPainter RU" `
  --description "Русифицированная версия WorldPainter 2.27.0" `
  --input "release\staging\app" `
--main-jar "WPGUI-2.27.0.jar" `
--main-class "org.pepsoft.worldpainter.Main" `
--dest "release\installer-work" `
--icon "assets\icon.ico" `
--resource-dir "scripts\jpackage-resources\windows" `
--win-menu `
  --win-menu-group "WorldPainter RU" `
  --win-shortcut `
  --win-shortcut-prompt `
  --win-dir-chooser `
  --win-upgrade-uuid "d5984a7f-cb32-48c8-b6f1-97a3c4c0da44"
```

После сборки файл переименовывается в `WorldPainter RU 2.27.0-r1 Setup.exe`.

`--win-upgrade-uuid` должен оставаться стабильным между релизами WorldPainter RU. Он связывает будущие Windows Installer-сборки как обновления одного продукта, а не как разные приложения с одинаковым названием.

## Ярлыки и запуск после установки

Installer использует `--win-menu`, `--win-menu-group "WorldPainter RU"` и `--win-shortcut`, чтобы создавать стабильный ярлык в меню «Пуск» и ярлык на рабочем столе.

`--win-shortcut-prompt` добавляет отдельный шаг установщика с выбором ярлыков. Русский текст этого шага задаётся через `scripts/jpackage-resources/windows/ShortcutPromptDlg.wxs`.

Запуск приложения на финальном экране установки добавлен через `scripts/jpackage-resources/windows/main.wxs`: на `ExitDialog` показывается чекбокс `Запустить WorldPainter RU`.

Закрепление на панели задач не добавляется: у `jpackage` нет опции для taskbar pin, а современные версии Windows не рекомендуют программам закреплять себя на панели задач без действия пользователя.

## Зависимости

Нужны:

- JDK 17 с `java`, `jar`, `jpackage`;
- Maven;
- WiX Toolset для Windows installer (`candle.exe` и `light.exe`);
- доступ к Maven Central;
- доступ к `https://www.worldpainter.net/maven-repo/`;
- runtime-зависимости `WPGUI`.

## Portable

Подготовленный `release/staging/app` можно использовать как основу portable-сборки.

Для ручной проверки можно запускать:

```cmd
java -jar WPGUI-2.27.0.jar
```

из каталога `release/staging/app`, потому что staged-копия jar получает `Main-Class` и `Class-Path`.

## Риски

- WiX должен быть установлен и доступен в `PATH`.
- Публичная версия релиза и техническая Windows Installer версия отличаются: `2.27.0-r1` против `2.27.1`.
- Неполный runtime classpath приведёт к ошибкам запуска.
- Installer нужно проверять на чистой Windows-системе.
- Лицензии runtime-зависимостей нужно проверить перед публикацией binary release.
