# FAR vault plugin lives elsewhere

Код плагина: [`D:\Projects\cpp\far-manager\plugins\secret-vault`](file:///D:/Projects/cpp/far-manager/plugins/secret-vault)

Локальный DPAPI-vault + NoteKeeper REST (`nk_` PAT, scope `vault`).

- Настройки плагина в Far: URL + PAT
- Alt+F9 — push/pull `/api/v1/vault/sync`
- Панель открылась — pull
- Сохранение записи — push (`If-Match: revision`, 409 → last-write-wins)
