# Changelog

## 1.4

- **Robust Math Parser & Expression Evaluation**:
  - Fixed unary vs. binary subtraction and negative number parsing in bill amount calculations.
  - Added arithmetic overflow safeguards preventing invalid or negative statement totals.
- **Total Owed & Summary Partitioning**:
  - Fixed active vs. archived service partitioning in `computeTotalOwed` to ensure accurate summary totals, overdue calculations, and bill counts.
- **Asynchronous App Widget Updates**:
  - Switched `LunasWidgetProvider` updates to `goAsync()` on background dispatchers to eliminate UI thread blocking and ANR risks.
  - Added visual status badges and iconography for overdue statements and up-to-date accounts.
- **Smooth Month Pager Performance**:
  - Hoisted month calculations and optimized Compose state subscriptions across `HorizontalPager` pages to eliminate frame drops during month swiping.
- **Notification Navigation & Backstack Routing**:
  - Improved reminder notifications with proper deep linking and synthetic backstack so tapping notifications navigates smoothly into the target statement without dropping app state.
- **Backup & Restore Data Integrity**:
  - Preserved original record creation timestamps during JSON restore.
  - Fixed `merge=false` full restore behavior to cleanly wipe existing records before restoring.
- **Validation & State Consistency**:
  - Enforced strict tenor validation (`tenor > 0`) preventing invalid installment schedules.
  - Fixed Compose `remember` keys in `AddBillBottomSheet` and services management to prevent stale dialog state when switching items.
  - Isolated background coroutine operations under `SupervisorJob` to prevent child cancellation cascading.
- **Comprehensive Unit Testing**:
  - Added unit test suites covering math parsing edge cases, month arithmetic, and summary computation partitioning.

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
