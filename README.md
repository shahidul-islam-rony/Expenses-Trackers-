# 💸 Expense Tracker - Smart Offline Fund & Expense Manager

A modern, privacy-first Android application built with **Kotlin** and **Jetpack Compose** that helps you effortlessly manage your wallet cash, online banking funds, and everyday expenses with automated SMS tracking, monthly category reports, dynamic search filters, and bulk management.

---

## 🌟 Key Features

* 📌 **Sticky Search & Timeframe Filters**: The live search bar and timeframe filter chips (*All Time*, *Today*, *This Week*, *This Month*, *Last Month*) stay pinned at the top while scrolling through transaction history for quick, uninterrupted access.
* 🔍 **Comprehensive Multi-Filters**: Filter transactions simultaneously by **Timeframe**, **Type** (*Expenses*, *Income*, *Dues*), **Account Source** (*Wallet / Cash*, *Online Banking*), and **Category**, alongside real-time search across titles, notes, and amounts.
* 🔘 **Multi-Select & Bulk Deletion**: Enter selection mode via the "Select" button or by long-pressing any transaction card. Select multiple items (or all) with visual checkboxes and delete them in bulk with safety confirmation.
* ⬆️ **"Back to Top" Quick Navigation**: A smooth animated floating button appears as you scroll down, allowing you to return to the top with a single tap.
* 📊 **Monthly Expense Reports**: Dedicated category-wise spending report with visual progress bars, percentage breakdowns, net cash flow, top expenses, month-by-month navigation, and one-tap report sharing.
* 🏷️ **Custom Expense Categories**: Create, customize, and manage custom expense categories with unique emoji icons.
* 🏦 **Multi-Account Balance Tracking**: Maintain distinct balances for **Wallet / Cash** and **Online Banking / Cards** with instant adjustment tools and live net worth calculations.
* 📱 **Automated SMS & Email Alert Sync**: Automatically detect and extract financial debit/credit alerts from bank SMS messages and email push notifications.
* 🔀 **Strict Currency Guard**: Prevents mixed currency pollution by strictly filtering transactions against your chosen primary currency (`$`, `৳`, `₹`, `€`, `£`, `Dhs`, etc.).
* 🔒 **100% Offline & Privacy-First**: Operates completely on-device using a local **Room Database**. No personal financial records or messages are uploaded to external cloud servers or AI APIs.

---

## 🛠️ How Automatic Expense Detection Works

1. **Incoming Alert Capture**:
   * **SMS**: `SmsReceiver` detects incoming bank debit and credit SMS alerts via broadcast intents.
   * **Email & Notifications**: `FinancialNotificationListener` captures transaction alerts from email clients (Gmail, Outlook, Yahoo) and banking apps.
2. **Regex & Keyword Engine**:
   * Analyzes notification text for financial triggers (e.g., `debited`, `credited`, `paid`, `spent`, `received`, `deposited`, `txn of`, `withdrawn`).
   * Extracts exact numerical amounts, merchant names, dates, and payment methods.
3. **Strict Currency Filtering**:
   * Cross-references detected currency codes against the user's configured currency.
   * Only matching transactions are imported.
4. **Deduplication & Local Persistence**:
   * Prevents double-counting by checking title similarity, amount, and timestamp proximity before saving to the local **Room Database**.

---

## 🔒 Permissions & Access Requirements

| Permission / Access | Purpose | How to Enable |
| :--- | :--- | :--- |
| **`READ_SMS`** | Allows scanning inbox SMS messages to import past bank transaction alerts. | Prompted automatically on launch or via the SMS Sync dialog. |
| **`RECEIVE_SMS`** | Captures incoming bank SMS alerts in real-time. | Prompted along with `READ_SMS`. |
| **Notification Access** (`BIND_NOTIFICATION_LISTENER_SERVICE`) | Captures bank push notifications from Gmail, Outlook, and banking apps. | Enable via **SMS Auto Sync & Scan** in the side menu. |

---

## 💱 Supported Currencies

The app supports custom symbol configuration and automatic detection for global and regional currencies, including:

* **BDT** (`৳`, Taka, Tk)
* **USD** (`$`, Dollar)
* **AED** (`Dhs`, Dirham)
* **EUR** (`€`, Euro)
* **INR** (`₹`, Rupee, Rs)
* **GBP** (`£`, Pound)
* **SAR** (Riyal)
* **QAR** (Riyal)

> **Tip:** You can update your currency symbol at any time under **Initial Funds & Balances** in the side menu.

---

## 🏗️ Technical Stack & Architecture

* **UI**: 100% Jetpack Compose with Material Design 3
* **Language**: Kotlin
* **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture
* **Database**: Room Database with Kotlin Symbol Processing (KSP)
* **Reactive State**: Kotlin Coroutines & `StateFlow`
* **Background Processing**: Android Broadcast Receivers & `NotificationListenerService`
* **Storage**: 100% Local SQLite / Room

---

## 👨‍💻 Developer & Credits

* **Developer**: Shahidul Islam rony
* **Facebook Profile**: [https://www.facebook.com/Sirony15/](https://www.facebook.com/Sirony15/)
* **Facebook Page**: [https://www.facebook.com/FB.SIRONYBD/](https://www.facebook.com/FB.SIRONYBD/)
* **YouTube Channel**: [https://www.youtube.com/sironybd](https://www.youtube.com/sironybd)
* **Contact Email**: [sirony15@gmail.com](mailto:sirony15@gmail.com)

---

## 📄 Privacy Policy

**Effective Date:** August 18, 2026

This Privacy Policy explains how **Expense Tracker** handles your data. The application is built with an **Offline & Private-First** architecture.

1. **Local Data Processing**: All bank message parsing, notification reading, and database operations happen strictly on your local device.
2. **No Remote Servers or AI Token Usage**: We do not store, transmit, or process your financial logs on external servers or third-party AI APIs.
3. **App Permissions**:
   - `READ_SMS` / `RECEIVE_SMS`: Used exclusively on-device to detect and log bank transaction alerts.
   - Notification Listener Service: Used strictly on-device to read financial debit/credit push notifications.
4. **Data Ownership**: You have full control over your financial records with the ability to edit, select, and bulk delete any entry at any time.
