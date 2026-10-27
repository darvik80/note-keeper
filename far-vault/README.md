# FAR NoteKeeper plugins live elsewhere

Код: [`D:\Projects\cpp\far-manager`](file:///D:/Projects/cpp/far-manager)

- `plugins/secret-vault` — локальный DPAPI-vault + REST `/api/v1/vault` (scope `vault`)
- `plugins/ssh-client` — пароль/ключ из Secret Vault, иначе NoteKeeper vault (по имени или хосту)
- `plugins/note-keeper` — панель notes/todos (scopes `notes`, `todos`)
- Общий клиент: `common/notekeeper` (URL + PAT)

PAT: `%APPDATA%\Far Manager\NoteKeeper\settings.ini`, иначе Secret Vault `sync.ini`.

