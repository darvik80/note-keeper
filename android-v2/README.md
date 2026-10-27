# NoteKeeper Android v2

Phone-first rewrite. Old `android/` stays until this is stable.

- `applicationId`: `dev.cogitor.notekeeper.v2` (can sit next to v1)
- Bottom nav: Notes | Todos | Vault | Search
- Note editor: markdown toolbar, 800ms autosave, no leave-on-save
- Todo editor: stays on screen after save
- Lists: pull-to-sync; swipe note → archive; swipe todo → done / archive
- Vault: REST `/api/v1/vault` (JWT), copy password
- Room + WorkManager sync for notes/todos as in v1
- Not yet: Hilt, SQLCipher local vault cache

Open in Android Studio from this folder.
