# NoteKeeper Android

Compose client, version **2.0.3**. Open this folder in Android Studio.

You can point the app at your own server or at the public test instance [https://note.cogitor.dev](https://note.cogitor.dev). The stand is for trying the product, not for real secrets.

- `applicationId`: `dev.cogitor.notekeeper.v2`
- minSdk 31
- Bottom nav: Notes | Todos | Vault | Search
- Note editor: markdown toolbar, autosave after 800 ms; the IME resizes the editor
- Todo editor: stays on screen after save
- Lists: pull to sync; swipe a note to archive; swipe a todo to mark it done or archive it
- Vault: `/api/v1/vault` (JWT), copy password
- Room + WorkManager sync for notes and todos
- Not yet: Hilt, SQLCipher for the local vault cache

Sign-in is email and password, plus Google and Yandex when the server has them (Custom Tabs → `notekeeper://oauth`). Buttons follow `/api/v1/config`. Prefer Chrome so Edge does not steal the custom-scheme redirect.

User guide: [../docs/user-guide.md](../docs/user-guide.md) · [на русском](../docs/user-guide.ru.md). Product docs: [../README.md](../README.md) · [README.ru.md](../README.ru.md). Releases: [../CHANGELOG.md](../CHANGELOG.md).
