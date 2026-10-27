# Cogitor UI reference

Inspiration sources (structure only):

- https://muz.li/inspiration/mobile-app-design-inspiration/
- https://scrn.gallery/apps
- https://puzinas.ru/novosti-dizajna/primery-dizajna-ui-ux-dlja-mobilnyh-prilozhenij-derzost-i-minimalizm/

## Steal from Muzli

| Pattern | Where it shows | Cogitor translation |
|---------|----------------|---------------------|
| Immersive dark + one glow | AI Chatbot, crypto wallets | Ink `#0B0E11`, amber CTA, optional indigo wash |
| Huge wordmark / greeting | Chat, onboarding | `displayLarge` on login; `displaySmall` on lists |
| Fat rounded cards, big numbers | Finance dashboards | `NkCard` 22dp, count as header subtitle |
| Floating tab / capsule nav | iOS-style 2026 kits | Pill `NavigationBar` inset 16dp, radius 28 |
| Soft section air, no boxes everywhere | Zenvest cream apps | Use ink+surface lift, not extra frames |
| Color-coded status, not rainbow UI | Calendars, tasks | PriorityDot + overdue error wash only |

## Do not steal

- Neon purple/pink meshes (second-row Muzli skins)
- Food photography as the whole UI
- Illustration mascots / 3D toys as empty states
- Glass-everywhere + 6 accent hues
- Stock Material 2 lists with 8-dp density

## Semantic roles (cogitor-go names)

Keep these names if you touch CSS on cogitor-go:

- `logic` = amber `#E8A33D` (CTA, code, “reason”)
- `assoc` = mint `#7EE0C0` (links, related)
- `mark` = indigo `#6E8BFF` (identity, inner hex)

NoteKeeper eight-token map:

`primary=logic`, `secondary=assoc`, `background=ink`, `surface=ink-surface`, tertiary in Compose = `mark`.

## Type scale (mobile)

| Style | Size | Weight | Use |
|-------|------|--------|-----|
| displayLarge | 44sp | Bold | Login wordmark |
| displaySmall | 32sp | Bold | Screen hero title |
| titleLarge | 20sp | SemiBold | Card titles |
| bodyMedium | 15sp | Regular | Previews |
| labelSmall | 11sp | Medium | tags, sync glyphs |

No random 13sp / 18sp one-offs.

## Compose shapes

`extraSmall 10`, `small 14`, `medium 22`, `large 32`.
