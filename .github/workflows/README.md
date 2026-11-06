# BrowOS 1.3
Created by Fouz_312 using Claude (Sonnet 5.5 High). Inspired by the BrowOS project by Allysun.
A desktop-style environment inside an Android app. It does not replace Android.

## In this release
- First-time setup (language, account, look, privacy), lock screen with optional password
- Windows-style taskbar, Start menu, widgets board, notification center
- Nothing is pinned by default. Pin from Start (star, or press and hold)
- Uninstall keeps the app in BrowStore; you can also delete its saved data
- BrowStore (Discover, BrowOS Apps, Library, Add a website)
- Browser: Chrome-style, real Chromium window in the Android app, desktop sites, ad blocker, "save password to Vault?" prompts, key button to fill logins, Google search by default
- Per-app settings: notifications, desktop site, pins (Settings > Apps)
- Settings: sidebar layout, window corners, window buttons (Windows or Mac), title bar, transparency, wallpapers (including private photos), language
- Vault (encrypted passwords), Files, Gallery, Music, Videos, PDF, Weather, Terminal, Calendar, Clock, Calculator, Notepad, Paint, Tasks
- Your logo is the app icon

## Files
www/ (the app), icons/ (launcher icons), android-patch/ (two Java files for the Chromium window), package.json, capacitor.config.json, debug.keystore, .github/workflows/build-apk.yml
See ROADMAP_MONDAY.md for what is not included yet.

## Privacy
Everything stays on your phone. Online requests go to: websites you open, Google's icon service (site icons), Open-Meteo (weather), cdnjs (PDF engine). Vault is AES-256-GCM encrypted with your master password; a forgotten master password cannot be recovered. The lock screen password only guards the BrowOS screen.
