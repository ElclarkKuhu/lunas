# Changelog

## 1.3

- **Automatic Sort by Unpaid**: Paylater statements on the main screen are now sorted with active unpaid bills at the top (ordered by urgency: overdue first, then closest due date), followed by paid bills, and inactive services at the bottom, with smooth list transition animations.
- **Notification Reliability & Android 13+ Support**:
  - Added runtime `POST_NOTIFICATIONS` permission handling for Android 13+ (API 33+).
  - Fixed billing month offset evaluation in reminders for services with cutoff in one month and due date in the next (e.g. SPayLater, GoPayLater).
  - Switched reminder scheduling to `setExactAndAllowWhileIdle` so daily alarms wake the phone reliably during Doze mode.
  - Added dedicated monochrome notification icon (`ic_stat_lunas`) and set channel importance to high.
- **Flexible Notification Preferences**:
  - Added precise minute-level time selection for daily reminders using a native time picker.
  - Added granular toggles for specific notification events: bill printed (cutoff date), approaching due date, on due date, and overdue.
  - Added customizable lead days before due date (H-x).
- **Reordered Settings Layout**: Restructured settings hierarchy to prioritize Manage Services and Notification settings at the top.
- **Display Currency Selection**: Added support for selectable currency display (IDR, USD, SGD, MYR, AUD, EUR) with Indonesian Rupiah (Rp) as default.
- **Release Build Optimization**: Enabled R8 minification and resource shrinking for faster, smaller release APKs.

## 1.2

- Added English translations for the app, including the main screen, settings, Manage Services, and home-screen widget.
- Added a Privacy Policy entry to app settings.
- Changed service removal to archive services so their bills and payment history are preserved and included in summaries. Archived services can be restored.
- Made JSON imports transactional so failed imports do not leave partially imported data.
- Rejects invalid billing months in imported bills and backups.
- Raised the minimum supported Android version to Android 8.0 (API 26).
- Displays the app version from package metadata in settings.
