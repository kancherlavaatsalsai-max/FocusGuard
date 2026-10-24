# FocusGuard 🎯

Your personal Regain-style study app: all "Pro" features, free, no ads.
Built for CLAT prep.

## Features
- **App blocking** – block Instagram, YouTube, etc. during focus sessions (or all day, or at bedtime). 5‑minute emergency unlock, or Strict Mode with none.
- **Reels / Shorts counter** – counts each swipe in Instagram Reels, YouTube Shorts, TikTok and Snapchat Spotlight. Daily total, per‑platform split, 7‑day chart, daily limit alert.
- **Focus timer** – Pomodoro (25/5 ×4), Deep Work (90/15), Custom. Coins, streaks, session history.
- **BrainPal-style** – "brain health" meter based on how many reels you watched, and Scroll Battles with friends.
- **Screen time** – per-app usage for today.
- **CLAT countdown** – big card in the app + two home-screen widgets (small 2×2, wide 4×2). Exam date is set to **6 Dec 2026**; change it in Settings.
- Dark / light theme.

## Build the APK (pick ONE way)

### A) Online, no installs (GitHub)
1. Create a free GitHub account → New repository → upload all files of this folder.
2. Open the **Actions** tab → "Build FocusGuard APK" → **Run workflow**.
3. When it turns green (~5 min) open the run → **Artifacts → FocusGuard-APK** → download → unzip → `app-release.apk`.
4. Copy it to your phone and tap it to install (allow "install unknown apps" when asked).

### B) Android Studio
1. Install Android Studio, then `File → Open` this folder and wait for Gradle sync.
2. `Build → Build APK(s)` → click "locate" in the popup.
   (or press ▶ with your phone plugged in over USB)

## First-time setup on the phone
1. Open FocusGuard → on the home screen tap **Enable** on the Accessibility card → find *FocusGuard* → switch on.
   (Android 13+: if it is greyed out, go to Settings → Apps → FocusGuard → ⋮ → *Allow restricted settings*, then try again.)
2. Tap **Allow** on the Usage Access card → switch FocusGuard on.
3. Allow notifications.
4. Block Apps → "Block common distractions".
5. Long‑press home screen → Widgets → FocusGuard → add the CLAT widget.

## Notes
- Widgets refresh at least every 30 min and whenever the app syncs (every 5 min), plus on date change.
- Reel counting relies on the layout of other apps' screens, so after big Instagram/YouTube updates the counts may need a code tweak (see `FocusAccessibilityService.kt`, `tracked`).
