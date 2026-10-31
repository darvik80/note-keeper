# NoteKeeper

[English](README.md) · **Русский**

Заметки, задачи и сейф паролей, которые вы поднимаете у себя. Веб-интерфейс упакован вместе с сервером (JAR на JVM или native-бинарник GraalVM). Android-клиент лежит в `android/` и ходит в тот же API.

Текущая версия: **2.0.2**. Что менялось: [CHANGELOG.md](CHANGELOG.md). Как пользоваться: [docs/user-guide.ru.md](docs/user-guide.ru.md) · [in English](docs/user-guide.md).

**Попробовать:** публичный тестовый стенд — [https://note.cogitor.dev](https://note.cogitor.dev). Там можно кликать по интерфейсу. Настоящие пароли и личные заметки туда лучше не класть: это чужая площадка, данные с неё не ваши.

![Home](docs/screenshots/home.png)

<p align="center">
  <img src="docs/screenshots/notes.png" width="32%" alt="Заметки" />
  <img src="docs/screenshots/todos.png" width="32%" alt="Задачи" />
  <img src="docs/screenshots/vault.png" width="32%" alt="Сейф" />
</p>

## Что внутри

Заметки пишутся в Markdown с привычными расширениями GitHub: таблицы, списки дел, блоки кода, диаграммы Mermaid, картинки. Их можно раскладывать по папкам, помечать тегами и избранным, шифровать AES-256-GCM, смотреть историю версий, создавать из шаблона, импортировать `.txt` / `.md` и делиться с другими пользователями того же сервера.

У задач тоже Markdown. Есть теги, приоритет, срок и напоминание. Повтор — каждый день, неделю, месяц, по будням или в выбранные дни недели. «Готово» у повторяющейся задачи значит «сделано в этом периоде»; в следующем она снова открыта. Напоминания и необязательный ежедневный отчёт уходят в Telegram и/или DingTalk.

Сейф хранит папки с секретами (пароли, SSH-ключи, API-токены, защищённые заметки) на сервере. Веб и Android синхронизируются через `/api/v1/vault`.

Рядом: полнотекстовый поиск с сохранёнными запросами, календарь, аналитика, архив, корзина и избранное. В интерфейсе тринадцать тем (по умолчанию **cogitor**), боковая панель, которая на телефоне становится выезжающим меню, командная палитра и тосты.

Вход — почта и пароль. Google и Yandex необязательны: пустой client id — и кнопок нет. Любой из провайдеров можно привязать к уже существующему аккаунту. Сессия — JWT на 24 часа. Персональные API-токены (`nk_…`) ограничиваются областями `notes`, `todos`, `vault` или `mcp`.

Клиенты: встроенный веб, Android (`android/`, minSdk 31), REST `/api/v1` и MCP на `/mcp` (область `mcp`). Плагины FAR Manager — отдельный репозиторий, см. `far-vault/README.md`.

Эксплуатация: по умолчанию SQLite, при необходимости PostgreSQL, ручные и по расписанию бэкапы, Docker (JVM и native) и Debian-пакет с native-бинарником.

## Стек

| Слой | |
|------|--|
| Backend | Java 25, Spring Boot 4.1, MyBatis, SQLite / PostgreSQL, JWT, GraalVM 25 |
| Web | React 18, TypeScript, Tailwind CSS, Webpack 5 (dev-сервер **:3001**) |
| Android | Kotlin, Jetpack Compose (`android/`) |

## Что нужно

- Java 25+ (GraalVM CE 25, если собираете native)
- Node.js 18+ (только для разработки фронтенда)
- Maven 3.9+

## Быстрый старт

### Полная сборка (backend + frontend)

```bash
mvn clean install
java -jar note-keeper-service/target/note-keeper-service-*.jar
# → http://localhost:8080
```

### Только backend

```bash
cd note-keeper-service
mvn spring-boot:run
# API: http://localhost:8080/api/v1
# Swagger: http://localhost:8080/swagger-ui.html
```

### Dev-сервер фронтенда

```bash
cd note-keeper-web
npm install
npm run dev
# → http://localhost:3001  (проксирует /api на localhost:8080)
```

### Native-образ GraalVM

Нужен GraalVM 25+ с `native-image` в `PATH`. На Windows — x64 Native Tools Command Prompt.

```bash
mvn -Pnative native:compile
./note-keeper-service/target/note-keeper -Djavax.xml.accessExternalDTD=all
```

Один файл — и React UI, и API. JVM в рантайме нет.

### Docker

```bash
docker build -t note-keeper .
docker run -d -p 8080:8080 -v ./var:/app/.data note-keeper

docker build -f Dockerfile.native -t note-keeper-native .
docker run -d -p 8080:8080 -v ./var:/app/.data note-keeper-native
```

### Пакет Debian

`.deb` содержит native-бинарник GraalVM, JVM на машине не нужен. CI собирает его через `Dockerfile.native` (`-march=x86-64-v2`). Нужен Docker и `dpkg-deb` (или снова Docker, чтобы упаковать архив).

```bash
bash deb/build.sh          # dist/note-keeper_<version>_amd64.deb
sudo dpkg -i dist/note-keeper_*.deb
```

Установщик спрашивает публичный URL (со схемой `https://`), порт (по умолчанию 8080), базу (`sqlite` или `postgresql`) и по желанию Google / Yandex: client id, secret и callback. Пустые поля OAuth выключают этого провайдера. Позже ответы можно сменить:

```bash
sudo dpkg-reconfigure note-keeper
```

Тихая установка (`DEBIAN_FRONTEND=noninteractive`) оставляет значения по умолчанию: SQLite, порт 8080, без OAuth. Ответы пишутся в `/opt/note-keeper/etc/application.yaml` (режим 0640).

Gitea Actions (`.gitea/workflows/build-deb.yml`, раннер `devops.local`) публикует пакет в `Cogitor/packages` как generic `cogitor-products`.

## Настройка

Базовый файл — `note-keeper-service/src/main/resources/application.yml`. Его перекрывают переменные окружения, профили Spring или overlay YAML, который пишет Debian-пакет.

### База данных

По умолчанию SQLite. Файл создаётся в `.data/notekeeper.db` (в Debian-раскладке — `/opt/note-keeper/var`).

PostgreSQL:

```bash
SPRING_PROFILES_ACTIVE=postgresql \
DB_HOST=localhost DB_PORT=5432 DB_NAME=notekeeper \
DB_USER=postgres DB_PASSWORD=secret \
java -jar note-keeper-service.jar
```

Схема накатывается при старте.

### Шифрование

```bash
cd note-keeper-service
mvn compile exec:java -Dexec.mainClass="dev.cogitor.note.keeper.service.EncryptionService" -Dexec.classpathScope=compile
```

```yaml
app:
  encryption:
    key: "your-base64-key"   # или APP_ENCRYPTION_KEY
```

Если ключ не задать, при каждом старте генерируется новый — и зашифрованные заметки после перезапуска уже не прочитать.

### Вход

| Способ | Настройка |
|--------|-----------|
| Локальный | `POST /api/v1/auth/register` |
| Google | необязательные `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI` |
| Yandex | необязательные `YANDEX_CLIENT_ID`, `YANDEX_CLIENT_SECRET`, `YANDEX_REDIRECT_URI` |
| JWT | `JWT_SECRET` (если пусто — генерируется) |
| Публичный URL | `APP_PUBLIC_BASE_URL` — обязательно со схемой `https://` или `http://`. Нужен для OAuth-редиректов и ссылок в отчётах |

Кнопки OAuth появляются, когда `/api/v1/config` говорит, что провайдер включён. После входа в **Настройки → Аккаунт** можно привязать или отвязать Google и Yandex.

### Интеграции

У каждого пользователя в Настройках: Telegram (токен бота + chat ID, по желанию webhook) и DingTalk (URL webhook + необязательный HMAC-секрет). У ежедневного отчёта есть время, каналы и шаблоны.

### Резервные копии

Из Настроек или через API: cron (по умолчанию 02:00), срок хранения, ручной экспорт/импорт, список/скачивание/удаление файлов.

### Хранилище

```yaml
app:
  storage:
    base-dir: ${user.dir}/.data
    attachments-dir: ${app.storage.base-dir}/attachments
    backups-dir: ${app.storage.base-dir}/backups
    db-path: ${app.storage.base-dir}/notekeeper.db
```

## API

Базовый путь: `/api/v1`. Везде, кроме `/auth/**` и публичного `/config`, нужен заголовок `Authorization: Bearer <jwt или nk_ токен>`.

Интерактивная документация: Swagger на `/swagger-ui.html`. Письменный (неполный) справочник — [API_DOCUMENTATION.md](API_DOCUMENTATION.md); полный — Swagger.

| Область | |
|---------|--|
| Auth | `POST /auth/register`, `/auth/login`; OAuth `GET /auth/oauth/{google\|yandex}`; привязка `POST/DELETE /auth/link/{provider}`; `GET /auth/me` |
| Notes | CRUD, импорт, архив, восстановление, история, шаринг |
| Todos | CRUD, архив, восстановление, шаринг, `POST /todos/{id}/toggle-complete`, журнал выполнений |
| Vault | `/vault/folders`, `/vault/items`, `/vault/sync` |
| Вложения | загрузка, пакет, скачивание, удаление |
| Поиск | `/search`, сохранённые запросы |
| Шаблоны | CRUD |
| Бэкап | экспорт, импорт, список, настройки |
| Токены | `GET/POST /tokens`, `DELETE /tokens/{id}` |
| Интеграции | Telegram, DingTalk |
| Аналитика | `GET /analytics?timeRange=week\|month\|year` |
| Настройки / теги / пользователи | как в Swagger |
| MCP | `POST /mcp` |
| Метрики | публичные `GET /actuator/health`, `GET /actuator/prometheus` (скрап Prometheus). JVM/HTTP/Tomcat/Hikari плюс бизнес-счётчики (`notekeeper_notes_added_total`, `notekeeper_notes_archived_total`, `notekeeper_auth_logins_total`, `notekeeper_auth_rejections_total`, `notekeeper_telegram_webhook_errors_total`, `notekeeper_reminders_sent_total`, `notekeeper_backups_total`, `notekeeper_backup_duration_seconds`) и gauge `notekeeper_records_*` |

Фильтры заметок: `folder`, `tag`, `priority`, `isFavorite`, `isEncrypted`, `isArchived`, `isDeleted`. Фильтры задач: `completed`, `tag`, `priority`, `isFavorite`, `isArchived`, `isDeleted`. Мягкое удаление (`DELETE /notes/{id}`) отправляет в корзину; `?permanent=true` удаляет насовсем.

## Android

Откройте `android/` в Android Studio. `applicationId` — `dev.cogitor.notekeeper.v2`.

В приложении есть заметки, задачи, сейф и поиск. Вход — почта/пароль и по желанию Google и Yandex (`notekeeper://oauth`). Свайп заметки — в архив; свайп задачи — «готово» или архив.

Клиент можно направить на свой сервер или на тестовый стенд [https://note.cogitor.dev](https://note.cogitor.dev). Подробности: [android/README.md](android/README.md).

## Выкат

| Workflow | Когда | |
|----------|-------|--|
| `tests.yml` | PR/push | Тесты backend |
| `deploy.yml` | push в master | JVM-образ → registry → NAS **:9081** |
| `deploy-native.yml` | тег/dispatch | Native-образ → NAS **:9082** |
| `deploy-k3s.yml` | вручную | K3s |

Реестр: `devops.local:5000` (HTTP, insecure). Секреты: `REGISTRY_USER`, `REGISTRY_PASSWORD`.

Перед приложением стоит reverse proxy (`nginx.conf`). Он должен пробрасывать `Host`, `X-Forwarded-*` и WebSocket `Upgrade` / `Connection`. В `APP_PUBLIC_BASE_URL` укажите публичный адрес **со схемой**, например `https://note.cogitor.dev`.

Скрап Prometheus (без авторизации):

```yaml
scrape_configs:
  - job_name: note-keeper
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ["localhost:8080"]
```

## Дерево репозитория

```
note-keeper/
├── note-keeper-service/     Spring Boot API + статическая вёрстка
├── note-keeper-web/         React UI (копируется в JAR)
├── android/                 Android-приложение (Compose)
├── far-vault/               Ссылка на плагины FAR (другой репозиторий)
├── docs/                    Руководство, PRD, скриншоты
├── Dockerfile / Dockerfile.native
├── nginx.conf
├── CHANGELOG.md
└── API_DOCUMENTATION.md
```

## Договорённости

Контроллеры вызывают сервисы, сервисы — MyBatis XML. Из контроллера в маппер ходить нельзя. У моделей Lombok `@Data`, DTO — обычные классы. `GlobalExceptionHandler` отвечает JSON `ErrorResponse`.

На вебе страницы лежат в `src/pages/`. Глобального стора нет — только props и React context. Темы — Tailwind и CSS-переменные.

## Если что-то сломалось

| Проблема | Что сделать |
|----------|-------------|
| После рестарта не читаются зашифрованные заметки | Задайте постоянный `app.encryption.key` / `APP_ENCRYPTION_KEY` |
| Не пишутся БД / вложения / бэкапы | Каталог `.data/` (и подкаталоги) должен существовать и быть доступен на запись |
| SQLite при нескольких писателях | Пул — одно соединение; для нескольких инстансов берите PostgreSQL |
| Native: ошибки XML/SQLite | Запуск с `-Djavax.xml.accessExternalDTD=all`; в native-аргументах есть `SqliteJdbcFeature` |
| Native: `CPU ISA level is lower than required` | Собирайте образом `25-ol9` и `-march=x86-64-v2` (у Synology уровень v2) |
| Native: Jackson на Telegram/DingTalk | Вложенные DTO ответа должны быть public и перечислены в `NativeRuntimeHints` |
| OAuth уводит на относительный путь | Задайте `APP_PUBLIC_BASE_URL` вместе с `https://` |

## Лицензия

MIT
