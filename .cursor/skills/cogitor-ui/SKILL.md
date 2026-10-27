---
name: cogitor-ui
description: >-
  Applies Cogitor visual language to mobile and web UI. Use when designing,
  restyling, or reviewing Android Compose, React/Tailwind, or any Cogitor /
  NoteKeeper screen; when the user mentions Cogitor theme, brand, hex-C mark,
  muz.li, scrn.gallery, Puzinas, or says the UI looks cheap/ugly.
---

# Cogitor UI

Steal **structure** from 2026 mobile galleries (Muzli, scrn.gallery, Puzinas). Never steal **palette**. Brand is ink + amber CTA + mint + indigo mark.

Read this skill before changing layout, color, type, nav, empty states, or login.

## Tokens (locked)

| Role | Hex | Use |
|------|-----|-----|
| Ink canvas | `#0B0E11` | page / scaffold background |
| Ink surface | `#141A21` | cards, bars, sheets |
| Hover | `#1C252E` | pressed / selected wash |
| Border | `#2C3540` | hairline only, ~28–40% opacity on Compose |
| Text | `#E9EDEF` | titles, body |
| Text muted | `#9AA3AB` | meta, captions |
| Logic / CTA | `#E8A33D` | primary buttons, FAB, selected tab, tags |
| Assoc | `#7EE0C0` | secondary, links, success-ish |
| Mark | `#6E8BFF` | hex-C inner, tertiary, structure accents |
| Danger | `#FF6B6B` | errors, overdue |

Mark paths (viewBox `0 0 100 100`):

- Outer C: `M93.3,33V25L50,0 6.7,25v50L50,100l43.3,-25v-8H75.1L50,81.5 22.7,65.75v-31.5L50,18.5 75.1,33z` fill `#EDEFF2` (or `currentColor` on dark)
- Inner hex: `M50,30l17.3,10v20L50,70 32.7,60V40z` fill `#6E8BFF`
- Lockup sits on `#0B0E11` rounded square. Never replace with letter "N" / "M".

Default theme id: `cogitor`. Other palettes (web 12) are optional skins. They must keep the **layout rules** below.

## Layout (from Muzli, filtered)

2026 apps on Muzli: large type, air, one hero, floating chrome, round cards, dark premium **or** cream finance. Cogitor is the **dark premium** lane (AI Chatbot / crypto / dashboard), not food photos, not neon purple, not pastel rainbow.

1. **Hero title in content**, not a dense Material `TopAppBar` as the only title. Use `displaySmall` / `displayLarge` (32–44sp, bold, tight tracking).
2. **Whitespace.** List padding ≥20dp. Card inner ≥16dp. Row gap 10–14dp. Radius 22dp cards, 28–32dp FAB / pill nav.
3. **One accent per screen.** Amber for action. Mint or indigo as supporting, not both screaming.
4. **Cards over ListItem.** Custom column: big title, muted 2-line preview, tiny `#tags`. No dense three-line M3 ListItem chrome.
5. **Pill / floating tab bar.** Surface on ink, selected = amber, indicator wash of primary ~16% alpha. Avoid stock grey bar flush to edges when possible.
6. **Empty states:** one icon (outline, muted), short title, one-line hint. No illustrations of mascots.
7. **Login:** mark lockup + huge wordmark + fields. Server URL is a field, not a banner.
8. **Motion:** short, opacity/translation only. No confetti, no gradient mesh backgrounds.

Detail screens may keep a thin top bar for Back. List/root screens should not.

## Do not

- Generic green `#2ecc71` as Cogitor primary
- Rainbow theme tokens on default Cogitor surfaces
- Stock "N" logo
- Nested competing Scaffolds with two titles
- Hairline-busy borders; prefer soft outline or none + surface lift
- Copy Muzli neon purple/pink finance skins onto Cogitor
- Light cream Zenvest look as the default (allowed as the separate `light` theme)

## Mapping

**Android (`android-v2`):** `AppThemeId.Cogitor` in `ui/theme/AppThemes.kt`. Shared chrome: `NkScreenHeader`, `NkCard`, `NkFab`, `nkTopBarColors` in `ui/components/NkUi.kt`. Hex-C: `res/drawable/ic_cogitor_mark.xml`. Persist `theme_id` in `SettingsDataStore`.

**Web:** `themes.cogitor` in `note-keeper-web/src/utils/themes.ts`. CSS vars `--color-*`. Tailwind `text-primary` / `bg-surface` / `border-border`. Mark from Cogitor SVG, not Font Awesome letter.

**Sister site (cogitor-go):** `--seed-bg/fg/surface/primary/accent` in `web/src/index.css`. Amber = logic/CTA. Mint = assoc/links. Do not invent a fourth brand hue.

## Workflow

1. Identify surface (list, editor, login, settings, vault).
2. Apply tokens + layout rules. Prefer existing `Nk*` / CSS vars over new one-off colors.
3. If adding a theme, clone eight semantic tokens; do not fork a new naming scheme.
4. Compile Android (`:app:compileDebugKotlin`) or check web routes that share the chrome.

Details and anti-patterns: [reference.md](reference.md)
