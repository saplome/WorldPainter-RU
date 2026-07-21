# Windows release tools

Каталог содержит developer-only сценарии и ресурсы сборки Portable, Inno Setup и WiX/MSI. Он не входит в runtime JAR или установленное приложение.

## Основные переключатели

- `-SkipMaven` — не запускать `mvn clean package`, использовать уже собранные JAR;
- `-BuildPortable` — создать app-image и Portable ZIP;
- `-BuildInnoInstaller` — создать фирменный Setup.exe через Inno Setup 6;
- `-BuildInstaller` — создать классический jpackage/WiX installer;
- `-CreateDraftRelease` — создать или обновить **черновик** GitHub Release;
- `-OpenDraftRelease` — открыть черновик в браузере после загрузки;
- `-AdditionalDraftAsset <путь>` — добавить к черновику исходники или другой файл;
- `-ReleaseTag`, `-ReleaseTitle`, `-ReleaseNotesFile` — переопределить данные черновика.

## Сборка и загрузка черновика

```powershell
gh auth login
mvn clean install -DskipTests

powershell -ExecutionPolicy Bypass -File .\tools\windows-packaging\build-windows-installer.ps1 `
  -SkipMaven -BuildPortable -BuildInnoInstaller `
  -CreateDraftRelease -OpenDraftRelease
```

Скрипт загружает найденные Setup.exe, Portable ZIP и дополнительные assets. Повторный запуск заменяет одноимённые файлы. Публикация никогда не выполняется автоматически: после проверки страницы GitHub нажмите **Publish release** вручную.
