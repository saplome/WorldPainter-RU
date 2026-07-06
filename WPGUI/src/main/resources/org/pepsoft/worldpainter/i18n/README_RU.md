# Локализация WorldPainter RU

В этом форке уже используется единая Java-система локализации `ResourceBundle`.
Основные файлы переводов находятся здесь:

```text
WPGUI/src/main/resources/org/pepsoft/worldpainter/resources/strings.properties
WPGUI/src/main/resources/org/pepsoft/worldpainter/resources/strings_ru.properties
WPGUI/src/main/resources/org/pepsoft/worldpainter/resources/strings_nl.properties
```

Принцип такой:

```java
WPI18n.s("some.key")
```

Для нового языка достаточно добавить файл:

```text
strings_de.properties
strings_uk.properties
strings_es.properties
```

и положить туда те же ключи с переводом. Если ключ отсутствует, приложение покажет сам ключ или английский текст, поэтому неполный перевод не ломает запуск.

Рекомендуемый стиль ключей:

```properties
menu.file.open=Открыть...
operation.Height.name=Высота
operation.Height.description=Повышать или понижать рельеф
layer.Resources.name=Ресурсы
layer.Resources.desc=Подземные залежи угля, руд, гравия, земли, лавы и воды
biome.Plains=Равнины
plant.Oak=Дуб
colour.Red=Красный
```

Не стоит хранить переводы в отдельных случайных папках по модулям. Лучше держать один общий bundle `resources/strings_*.properties`, потому что `WPI18n` доступен и из WPCore, и из WPGUI.
