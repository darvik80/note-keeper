# NoteKeeper Android

Compose client, version **2.0.2**. Open this folder in Android Studio.

- `applicationId`: `dev.cogitor.notekeeper.v2`
- minSdk 31
- Bottom nav: Notes | Todos | Vault | Search
- Note editor: markdown toolbar, 800ms autosave; IME resizes the editor
- Todo editor: stays on screen after save
- Lists: pull-to-sync; swipe note → archive; swipe todo → done / archive
- Vault: `/api/v1/vault` (JWT), copy password
- Room + WorkManager sync for notes/todos
- Not yet: Hilt, SQLCipher local vault cache

Sign-in: email/password, Google, Yandex (Custom Tabs → `notekeeper://oauth`). Buttons follow `/api/v1/config` (`GOOGLE_CLIENT_ID` / `YANDEX_CLIENT_ID`). Prefer Chrome so Edge does not steal the custom-scheme redirect.

User guide: [../docs/user-guide.md](../docs/user-guide.md). Product docs: [../README.md](../README.md), [../CHANGELOG.md](../CHANGELOG.md).
