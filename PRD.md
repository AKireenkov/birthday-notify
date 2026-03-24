# PRD: Birthday Notification Email Service (MVP)

---

## 1. Executive Summary

### Problem Statement
HR-менеджеры и руководители не имеют надежного автоматизированного механизма отслеживания дней рождения сотрудников. Текущий процесс ручной, подвержен ошибкам и зависит от памяти. Проблема усугубляется российским производственным календарем: выходные и праздники создают многодневные пробелы, в которых рассылка пятницы должна охватывать события субботы-воскресенья или целой праздничной недели.

### Solution Overview
Standalone Python 3.11+ скрипт, который:
1. Запускается ежедневно в **08:00 МСК** через cron/systemd timer
2. Определяет, является ли сегодня **рабочим днем** (по производственному календарю РФ в JSON)
3. Если нерабочий — завершается (без письма)
4. Рассчитывает **окно уведомления**: сегодня (T) + завтра (T+1) + все последующие нерабочие дни до первого рабочего (не включая его)
5. Загружает **CSV с сотрудниками**, дедуплицирует, сопоставляет по дню+месяцу рождения
6. Формирует **HTML-письмо** с секциями по датам
7. Отправляет через **Yandex SMTP**
8. Логирует все события (без ПД)
9. При сбое: 1 retry через 30 сек, потом ошибка

### Business Impact
- Устраняет пропуски уведомлений о днях рождения в праздничные периоды
- Снижает ручную нагрузку на HR
- Нулевая инфраструктурная стоимость для MVP

### Scope

**IN Scope (MVP):**
- Ежедневный расчет дней рождения с учетом производственного календаря
- CSV-источник данных (UTF-8, разделитель `;`)
- HTML-письмо через Yandex SMTP
- Настраиваемые получатели через `.env`
- Обработка 29 февраля (показ 1 марта в невисокосный год)
- Проверка свежести файла (warning + skip если >24ч)
- 1 retry при сбое SMTP, затем ошибка
- Console + file logging (без ПД)
- Дедупликация по ФИО + Дата рождения

**OUT of Scope (MVP):**
- Web UI / админ-панель
- База данных
- REST/GraphQL API
- Ручная переотправка
- Редактирование сотрудников в системе
- Telegram / Slack / SMS
- История отправок
- Мульти-тенантность
- Интернационализация
- Динамический календарь через API

---

## 2. Users & Roles

### Получатель рассылки (HR-менеджер / Руководитель)
- Получает HTML-письмо в ~08:00 МСК каждый рабочий день
- Читает структурированный список дней рождения по датам
- Не взаимодействует с системой напрямую

### Администратор (DevOps / HR IT)
- Устанавливает зависимости Python
- Создает и заполняет `.env`
- Размещает CSV-файл
- Настраивает cron
- Обновляет производственный календарь ежегодно
- Мониторит логи

**Пользовательского интерфейса нет.** Вся конфигурация через файлы/переменные окружения.

---

## 3. Functional Requirements

### FR-1: Проверка рабочего дня

**Описание:** Скрипт определяет, является ли текущая дата (МСК, UTC+3) рабочим днем.

**Бизнес-правило:** Письмо отправляется только в рабочие дни. Нерабочий день = выход с кодом 0.

**User Story:** Как администратор, я хочу, чтобы скрипт пропускал выполнение в выходные и праздники.

**Acceptance Criteria:**
- Given: сегодня рабочий день → When: скрипт запущен → Then: выполнение продолжается
- Given: сегодня суббота → When: скрипт запущен → Then: лог "Today (DD.MM.YYYY) is a non-working day. Skipping.", exit 0
- Given: сегодня 1 января → When: скрипт запущен → Then: лог skip, exit 0
- Given: файл календаря отсутствует → When: скрипт запущен → Then: "ERROR: Production calendar file not found at <path>", exit 1

**Priority:** Must Have
**Dependencies:** FR-2

---

### FR-2: Загрузка производственного календаря

**Описание:** Скрипт загружает JSON-файл с нерабочими датами. Путь настраивается через `.env`.

**Формат:**
```json
{
  "non_working_dates": [
    "2025-01-01",
    "2025-01-02"
  ]
}
```

**Логика:** Дата нерабочая если `weekday() >= 5` (Сб/Вс) ИЛИ дата в списке `non_working_dates`. В JSON хранятся только праздники и переносы — выходные вычисляются программно.

**Acceptance Criteria:**
- Given: валидный JSON → When: загрузка → Then: корректный lookup рабочий/нерабочий
- Given: файл отсутствует → Then: "ERROR: ... not found", exit 1
- Given: невалидный JSON → Then: "ERROR: ... malformed", exit 1
- Given: нет ключа `non_working_dates` → Then: "ERROR: ... missing key", exit 1

**Priority:** Must Have

---

### FR-3: Расчет окна уведомления

**Описание:** Скрипт рассчитывает набор дат для рассылки.

**Алгоритм:**
- Окно = {T} ∪ {T+1} ∪ {все последующие нерабочие дни подряд до первого рабочего (не включая)}

**Примеры:**

| Сегодня (T) | Окно |
|---|---|
| Среда (рабочая) | Ср, Чт |
| Пятница (рабочая) | Пт, Сб, Вс |
| Пт 7 марта 2025 | 7, 8, 9, 10 марта |
| Пт 30 апреля 2025 | 30 апр, 1-4 мая |
| Ср 31 декабря 2025 | 31 дек, 1-8 янв 2026 |

**Acceptance Criteria:**
- Given: среда → Then: окно = [Ср, Чт]
- Given: пятница (обычная) → Then: окно = [Пт, Сб, Вс]
- Given: пт перед майскими → Then: окно покрывает все нерабочие дни
- Given: ср 31 декабря → Then: окно включает 31 дек + 1-8 янв

**Priority:** Must Have
**Dependencies:** FR-1, FR-2

---

### FR-4: Загрузка и валидация CSV

**Описание:** Скрипт читает CSV с фиксированной схемой.

**Спецификация файла:**
- Encoding: UTF-8 (с BOM или без — оба варианта)
- Delimiter: `;`
- Header row обязателен
- Колонки: `ФИО;Дата рождения;Должность;Подразделение`
- Формат даты: `DD.MM.YYYY`
- Файл старше 24ч → WARNING + skip (exit 0)
- Невалидные даты → skip строки + WARNING (номер строки, без ПД)
- Пустые ФИО → skip
- Дедупликация: по ФИО + Дата рождения, первый wins

**Acceptance Criteria:**
- Given: валидный CSV → Then: все строки распарсены, count залогирован
- Given: файл не найден → Then: "ERROR: Employee file not found", exit 1
- Given: нет колонки `Должность` → Then: "ERROR: ... missing columns: Должность", exit 1
- Given: файл старше 24ч → Then: "WARNING: ... stale", exit 0
- Given: дубликаты → Then: первый сохраняется, лог "Removed N duplicate records"
- Given: невалидная дата в строке 5 → Then: строка пропущена, лог "WARNING: Skipped row 5"

**Priority:** Must Have

---

### FR-5: Сопоставление дней рождения

**Описание:** Для каждой даты окна скрипт находит сотрудников с совпадающими днем+месяцем рождения. Год не учитывается.

**Правило 29 февраля:** В невисокосный год → показывать 1 марта. В високосный → 29 февраля.

**Acceptance Criteria:**
- Given: сотрудник род. 15.06.1985, 15 июня в окне → Then: сотрудник в секции 15 июня
- Given: сотрудник род. 29.02.1992, год 2025 (невисокосный), 1 марта в окне → Then: сотрудник в секции 1 марта
- Given: сотрудник род. 29.02.1992, год 2028 (високосный), 29 февраля в окне → Then: сотрудник в секции 29 февраля
- Given: окно Dec 31 + Jan 1-8 → Then: сотрудники с ДР в эти дни корректно найдены
- Given: нет совпадений → Then: все секции "Нет именинников"

**Priority:** Must Have
**Dependencies:** FR-3, FR-4

---

### FR-6: Формирование HTML-письма

**Описание:** Скрипт формирует письмо с секциями по датам окна.

**Спецификация:**
- Subject: `🎂 Дни рождения сотрудников — DD.MM.YYYY` (дата запуска)
- Секции в хронологическом порядке
- Заголовок секции: дата в формате `DD MMMM YYYY` по-русски
- Каждый сотрудник: ФИО, Должность, Подразделение
- Пустая дата: "Нет именинников" (секция не скрывается)
- HTML с inline CSS (без внешних стилей)
- Plain-text fallback (multipart/alternative)
- From: настраиваемый через `.env`
- To: настраиваемый список через `.env`
- Письмо отправляется ВСЕГДА, даже если нет именинников
- Все данные из CSV — через `html.escape()`

**Acceptance Criteria:**
- Given: окно Пт-Вс, 2 сотрудника в Пт, 0 в Сб, 1 в Вс → Then: 3 секции, Сб = "Нет именинников"
- Given: нет ДР вообще → Then: все секции "Нет именинников", письмо отправлено
- Given: имя содержит `<script>` → Then: HTML-escaped
- Given: 50 сотрудников на одну дату → Then: все 50 показаны

**Priority:** Must Have
**Dependencies:** FR-5

---

### FR-7: Отправка через Yandex SMTP

**Описание:** Отправка через Yandex SMTP с credentials из `.env`.

**Спецификация:**
- Host: `smtp.yandex.ru`, port `465`, SSL
- Auth: логин + пароль из `.env`
- Timeout: 30 секунд
- При сбое: ожидание 30 сек → 1 retry
- Retry тоже неудачен: "ERROR: Failed to send after 2 attempts", exit 1
- Auth failure: "ERROR: SMTP authentication failed", exit 1 (без retry)
- Успех: "Email sent successfully to N recipients at HH:MM:SS"

**Acceptance Criteria:**
- Given: валидные credentials → Then: письмо доставлено, лог с count и timestamp
- Given: первая попытка timeout, retry ok → Then: письмо доставлено, лог обоих попыток
- Given: обе попытки fail → Then: "ERROR: Failed after 2 attempts", exit 1
- Given: неверный пароль → Then: "ERROR: SMTP auth failed", exit 1 (без retry)

**Priority:** Must Have
**Dependencies:** FR-6

---

### FR-8: Логирование

**Описание:** Скрипт логирует все значимые события в stdout/stderr и rotating file.

**Спецификация:**
- Уровни: INFO, WARNING, ERROR
- Формат: `YYYY-MM-DD HH:MM:SS [LEVEL] message`
- Путь файла: настраиваемый через `.env` (default: `./birthday_notify.log`)
- **Никаких ПД в логах**: ФИО, email, телефоны — никогда
- Логируется: старт, статус загрузки календаря, count строк CSV, даты окна, count совпадений, статус отправки, ошибки
- Rotating: max 5 MB, 5 backup файлов

**Priority:** Must Have

---

## 4. Non-Functional Requirements

| ID | Категория | Требование |
|---|---|---|
| NFR-1 | Performance | Выполнение (без SMTP) < 5 сек для 10,000 строк CSV |
| NFR-2 | Reliability | Транзиентные сбои SMTP < 30 сек покрываются retry |
| NFR-3 | Correctness | Расчет окна корректен для всех стандартных праздничных паттернов РФ |
| NFR-4 | Security | SMTP credentials только в `.env`, `.env` в `.gitignore`, без ПД в логах |
| NFR-5 | Maintainability | Только `calendar.json` требует ежегодного обновления |
| NFR-6 | Compatibility | Python 3.11+ на Linux (Ubuntu 20.04+) |
| NFR-7 | Observability | Exit codes: 0 = success/skip, 1 = error |
| NFR-8 | Email | Корректный рендер в Gmail, Apple Mail, Outlook Web. Inline CSS. |
| NFR-9 | Timezone | Все даты в МСК (UTC+3), без зависимости от timezone сервера |
| NFR-10 | Idempotency | Повторный запуск в тот же день = идентичное письмо |

---

## 5. Technical Specification

### 5.1 CSV File Format

- **Encoding:** UTF-8 (с BOM или без)
- **Delimiter:** `;`
- **Line ending:** LF или CRLF
- **Header row:** обязателен, первая строка

| Column | Type | Format | Required |
|---|---|---|---|
| ФИО | string | free text | yes |
| Дата рождения | date | DD.MM.YYYY | yes |
| Должность | string | free text | yes |
| Подразделение | string | free text | yes |

**Пример:**
```
ФИО;Дата рождения;Должность;Подразделение
Иванов Иван Иванович;15.06.1985;Разработчик;IT
Петрова Мария Сергеевна;29.02.1992;HR-менеджер;HR
```

### 5.2 Production Calendar JSON

```json
{
  "non_working_dates": [
    "2025-01-01", "2025-01-02", "2025-01-03",
    "2025-01-04", "2025-01-05", "2025-01-06",
    "2025-01-07", "2025-01-08",
    "2025-02-24",
    "2025-03-10",
    "2025-05-01", "2025-05-02",
    "2025-06-12",
    "2025-11-04",
    "2025-12-31"
  ]
}
```

Логика: `is_non_working(date) = date.weekday() in {5,6} OR date in non_working_dates`

### 5.3 Configuration (.env)

| Parameter | Required | Default | Description |
|---|---|---|---|
| `SMTP_HOST` | No | `smtp.yandex.ru` | SMTP hostname |
| `SMTP_PORT` | No | `465` | SMTP port (SSL) |
| `SMTP_USER` | Yes | — | SMTP login |
| `SMTP_PASSWORD` | Yes | — | SMTP password |
| `EMAIL_FROM` | Yes | — | Sender address |
| `EMAIL_RECIPIENTS` | Yes | — | Comma-separated recipients |
| `EMPLOYEES_CSV_PATH` | No | `./employees.csv` | Path to CSV |
| `CALENDAR_PATH` | No | `./calendar.json` | Path to calendar JSON |
| `LOG_FILE_PATH` | No | `./birthday_notify.log` | Log file path |
| `STALE_FILE_HOURS` | No | `24` | Max CSV age in hours |
| `SMTP_TIMEOUT_SECONDS` | No | `30` | SMTP timeout |
| `SMTP_RETRY_DELAY_SECONDS` | No | `30` | Retry delay |

### 5.4 Script Execution Flow

```
Step 1:  Load .env, validate required params
Step 2:  Init logger (console + rotating file)
Step 3:  Load production calendar JSON, validate
Step 4:  Get current date in Moscow time (UTC+3)
Step 5:  Check working day → non-working: log + EXIT 0
Step 6:  Calculate notification window
Step 7:  Check CSV exists → missing: ERROR + EXIT 1
Step 8:  Check CSV mtime → stale: WARNING + EXIT 0
Step 9:  Load CSV, validate headers → invalid: ERROR + EXIT 1
Step 10: Deduplicate, log count
Step 11: Match birthdays per window date (Feb-29 rule)
Step 12: Compose HTML + plain-text email
Step 13: Send via SMTP → success: EXIT 0 / retry fail: EXIT 1
```

### 5.5 Project Structure

```
birthday_notify/
├── main.py
├── calendar_loader.py
├── csv_loader.py
├── window_calculator.py
├── birthday_matcher.py
├── email_composer.py
├── email_sender.py
├── logger_setup.py
├── config.py
├── employees.csv          # not in VCS
├── calendar.json          # in VCS, updated annually
├── .env                   # not in VCS
├── .env.example           # in VCS
├── .gitignore
├── requirements.txt       # python-dotenv
└── tests/
    ├── test_calendar_loader.py
    ├── test_window_calculator.py
    ├── test_csv_loader.py
    ├── test_birthday_matcher.py
    ├── test_email_composer.py
    ├── test_email_sender.py
    ├── test_config.py
    ├── test_integration.py
    └── fixtures/
```

---

## 6. Error Handling

| # | Trigger | Behavior | Log | Exit |
|---|---|---|---|---|
| E-01 | Missing required .env param | Immediate exit | `ERROR: Missing required config: <PARAM>` | 1 |
| E-02 | Calendar file not found | Immediate exit | `ERROR: Calendar not found at <path>` | 1 |
| E-03 | Calendar invalid JSON | Immediate exit | `ERROR: Calendar malformed: <error>` | 1 |
| E-04 | Calendar missing key | Immediate exit | `ERROR: Calendar missing key 'non_working_dates'` | 1 |
| E-05 | CSV not found | Immediate exit | `ERROR: Employee file not found at <path>` | 1 |
| E-06 | CSV wrong headers | Immediate exit | `ERROR: Missing columns: <cols>` | 1 |
| E-07 | CSV stale (>24h) | Graceful skip | `WARNING: File stale (modified: <ts>). Skipping.` | 0 |
| E-08 | Non-working day | Graceful skip | `INFO: Today (<date>) is non-working. Skipping.` | 0 |
| E-09 | Bad date in CSV row | Skip row | `WARNING: Skipped row <N> — invalid date` | 0 |
| E-10 | SMTP fail (1st) | Retry in 30s | `WARNING: SMTP attempt 1 failed: <code>. Retrying.` | — |
| E-11 | SMTP fail (2nd) | Exit error | `ERROR: Failed after 2 attempts. Last: <code>` | 1 |
| E-12 | SMTP auth fail | No retry, exit | `ERROR: SMTP auth failed` | 1 |
| E-13 | SMTP timeout | → E-10/E-11 | `WARNING: SMTP timed out after <N>s` | per E-10/11 |
| E-14 | Log dir not writable | Console only | `WARNING: Cannot write log to <path>. Console only.` | 0 |

---

## 7. Test Requirements

### Test Cases

| TC | Test Case | Expected |
|---|---|---|
| TC-01 | Working day midweek | Window = Wed+Thu, email sent |
| TC-02 | Non-working day | Exit 0, no email |
| TC-03 | Friday before weekend | Window = Fri+Sat+Sun |
| TC-04 | Before May holidays | Window covers holiday period |
| TC-05 | Dec 31 before New Year | Window = Dec 31 + Jan 1-8 |
| TC-06 | Feb 29 birthday, non-leap | Employee under Mar 1 |
| TC-07 | Feb 29 birthday, leap | Employee under Feb 29 |
| TC-08 | No birthdays | Email sent, all "Нет именинников" |
| TC-09 | Duplicates | 1 kept, log "Removed 1 duplicate" |
| TC-10 | Stale CSV | Exit 0, warning, no email |
| TC-11 | Missing CSV | Exit 1, error |
| TC-12 | Missing calendar | Exit 1, error |
| TC-13 | SMTP retry success | Email sent, retry logged |
| TC-14 | SMTP double fail | Exit 1, error |
| TC-15 | HTML escaping | `<script>` escaped |
| TC-16 | 10k rows CSV | < 5 seconds |
| TC-17 | Year boundary | Dec→Jan matching correct |
| TC-18 | Invalid date in CSV | Row skipped, warning |
| TC-19 | Missing .env param | Exit 1, config error |
| TC-20 | Auth failure | Exit 1, no retry |

---

## 8. Decisions Log

| # | Question | Decision | Rationale |
|---|---|---|---|
| D-01 | File format | CSV, UTF-8, `;` | Simple, Excel-editable |
| D-02 | Email gateway | Yandex SMTP :465 | MVP test: a.qa.cloud@yandex.ru |
| D-03 | Error handling | Fail-fast structural, graceful operational | Predictable exit codes |
| D-04 | Duplicates | Dedup by ФИО+ДатаРождения | Prevent double notifications |
| D-05 | Schedule | 08:00 MSK daily, script checks working day | Single cron entry |
| D-06 | Feb 29 | Show on Mar 1 non-leap | Standard convention |
| D-07 | Retry | 1 retry after 30s, then error | Handles transient failures |
| D-08 | Stale file | Warn + skip, exit 0 | Prevents outdated data |
| D-09 | Send history | No | MVP — inbox is history |
| D-10 | Runtime | Python 3.11+, cron | Universal Linux setup |
| D-11 | Calendar | Static JSON, annual update | No internet dependency |
| D-12 | Subject | `🎂 ... — DD.MM.YYYY` (run date) | Consistent format |
| D-13 | Recipients | `.env` list, MVP: a.qa.cloud@yandex.ru | Flexible |
| D-14 | From address | `.env` configurable | Custom display name |
| D-15 | Logging | Console + rotating file, no PII | Audit-friendly |

---

## 9. Out of Scope

- Web UI / admin panel
- Database (PostgreSQL, SQLite)
- REST or GraphQL API
- Manual resend / on-demand
- Employee record editing
- Telegram / Slack / SMS
- Send history / delivery receipts
- Multi-tenant support
- Internationalization
- Dynamic calendar via API
- Email tracking
- Attachments
