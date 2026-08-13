# 💸 Expense Tracker - Automated Email & SMS Fund Manager

A modern, privacy-first Android application built with **Kotlin** and **Jetpack Compose** that automatically tracks your income and expenses by analyzing bank SMS alerts, email notifications, and mobile banking transactions in real-time.

---

## 🌟 Key Features

* 📱 **Automated SMS Transaction Detection**: Scans incoming and inbox SMS messages from financial institutions, mobile wallets, and banks to extract transaction details effortlessly.
* 📧 **Real-Time Email Alert Auto-Sync**: Uses a background Notification Listener Service to capture financial alerts from email clients (Gmail, Outlook, Yahoo) and mobile payment apps as soon as notifications arrive.
* 🔀 **Strict Currency-Specific Guard**: Filters out non-matching currencies. If your active tracker is set to **BDT**, transactions in **AED** or **USD** are strictly ignored—ensuring zero currency pollution in your balance totals.
* 🏦 **Multi-Account Balance Management**: Track separate balances for **Wallet / Cash** and **Online Banking / Cards** with instant account-specific adjustments.
* 🤖 **Gemini AI Smart Assistant**: Optional AI-powered transaction analysis and receipt parser for deep category insights, budget recommendations, and spending breakdowns.
* 📊 **Visual Analytics & Category Breakdown**: Categorizes transactions into Groceries, Dining, Transport, Shopping, Utilities, Income, and more with clean visual charts.
* 🔍 **Manual & Automated Import**: Option to trigger a one-tap SMS inbox scan, paste email notification text manually for instant testing, or rely on fully automatic background tracking.
* 🔐 **100% Offline & Private First**: All bank message parsing and Room database persistence happen directly on your local device. No personal financial data is stored on remote servers.

---

## 🛠️ How Automatic Expense Detection Works

1. **Incoming Alert Capture**:
   * **SMS**: The `SmsReceiver` catches incoming bank SMS alerts via broadcast intents.
   * **Email & App Notifications**: The `FinancialNotificationListener` captures push notifications from email apps (Gmail, Outlook) and banking apps.
2. **Regex & Keyword Engine**:
   * Analyzes text for financial transaction indicators (e.g., `debited`, `credited`, `paid`, `spent`, `received`, `deposited`, `txn of`, `withdrawn`).
   * Extracts exact numerical amounts, merchant names (e.g., *Carrefour*, *Swapno*, *Amazon*, *Uber*, *KFC*), and payment methods.
3. **Strict Currency Filtering**:
   * Cross-references detected currency codes (`AED`, `BDT`, `USD`, `EUR`, `INR`, `GBP`, `৳`, `$`, `₹`, `€`, `Dhs`) against the user's active target currency.
   * Only transactions matching the configured tracker currency are saved.
4. **Deduplication & Local Storage**:
   * Prevents double-counting by checking title similarity, amount, and timestamp proximity before persisting entries to the local **Room Database**.

---

## 🔒 Permissions & Access Requirements

To enable background auto-detection, the app requires specific Android permissions:

| Permission / Access | Purpose | How to Enable |
| :--- | :--- | :--- |
| **`READ_SMS`** | Allows scanning inbox SMS messages to import past bank transaction alerts. | Prompted automatically on screen launch or when tapping "Scan SMS Inbox". |
| **`RECEIVE_SMS`** | Captures new bank SMS notifications instantly in the background as they arrive. | Prompted along with `READ_SMS`. |
| **Notification Access** (`BIND_NOTIFICATION_LISTENER_SERVICE`) | Captures bank debit/credit push notifications from Gmail, Outlook, Yahoo, and mobile banking apps automatically. | Tap **"Enable Email Auto-Sync"** in the SMS & Email Tracker sheet, then toggle permission for **Expense Tracker**. |
| **`INTERNET`** | Optional; used exclusively for Google Gemini AI categorization queries. | Granted automatically by Android. |

---

## 💱 Supported Currencies

The app supports auto-detection and custom symbol configuration for a wide range of global and regional currencies, including:

* **BDT** (`৳`, Taka, Tk)
* **AED** (`Dhs`, Dirham)
* **USD** (`$`, Dollar)
* **EUR** (`€`, Euro)
* **INR** (`₹`, Rs, Rupee)
* **GBP** (`£`, Pound)
* **SAR** (Riyal)
* **QAR** (Riyal)

> **Tip:** You can change your active tracker currency anytime under **Adjust Balances** in the app.

---

## 🏗️ Technical Stack & Architecture

* **UI**: 100% Jetpack Compose with Material Design 3 (Dynamic Light/Dark Themes)
* **Language**: Kotlin
* **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture
* **Database**: Room Database with Kotlin Symbol Processing (KSP)
* **Asynchrony**: Kotlin Coroutines & Flow
* **Background Processing**: Android Broadcast Receivers & `NotificationListenerService`
* **AI Integration**: Google Gemini API via REST / SDK

---

## 🚀 Setup & Installation

### Prerequisites
* **Android Studio** Jellyfish / Koala or newer
* **JDK 17**
* **Android SDK**: Minimum API 26 (Android 8.0), Target API 34 (Android 14)

### Building from Source

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-username/expense-tracker.git
   cd expense-tracker
   ```

2. **Open in Android Studio**:
   * Open Android Studio and select **Open an Existing Project**.
   * Navigate to the cloned folder and click **OK**.

3. **Sync & Build**:
   * Allow Gradle to sync dependencies automatically.
   * Run the app on an Android device or emulator (`Shift + F10`).

---

## 📖 Usage Guide

1. **Set Up Starting Balances**: Tap the **Wallet** or **Online Banking** card at the top to adjust your initial funds and select your primary currency.
2. **Enable Background Auto-Sync**:
   * Grant SMS permissions when prompted.
   * Open the **SMS & Email Tracker** sheet (SMS icon at the top right) and tap **Enable Email Auto-Sync** to allow reading bank emails from Gmail/Outlook notifications.
3. **Automatic Tracking**: Whenever you receive a bank debit/credit alert via SMS or Email in your selected currency, the transaction is automatically added to your log!

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.



**Privacy Policy**

**Effective Date:** August 13, 2026

This Privacy Policy explains how **Expense Tracker** ("we," "our," or "us") handles your information. The app is designed with a **100% Offline & Private First** architecture to ensure your personal financial data remains fully under your control.

---

**1. Information We Collect and Process**

* **SMS Alerts & Notifications:** The app reads incoming and inbox SMS messages, as well as push notifications from bank and email apps (e.g., Gmail, Outlook, Yahoo). This data is processed **locally on your device** solely to extract transaction amounts, merchant names, currencies, and dates.
* **Manual Entries & Balances:** Account details, custom category preferences, and balance adjustments you manually enter are saved directly to your local database.
* **AI Analysis Queries (Optional):** If you choose to use the Google Gemini AI features (such as spending insights or receipt parsing), transaction details required for that specific analysis are processed via Google's API.

**2. How We Use Your Information**

* To automatically log, categorize, and track your income and expenses.
* To maintain separate account balances for Cash/Wallet and Online Banking.
* To filter out non-matching currencies and prevent double-counting of entries.
* To generate visual spending analytics and category breakdowns.

**3. Data Storage & Security**

* **Local Storage Only:** All transaction records, bank message parsing, and database persistence occur on your local device using Android's Room database.
* **No Remote Server Storage:** We do not host, store, or transmit your personal financial data to any remote servers controlled by us.

**4. App Permissions Required**

| Permission / Access | Purpose |
| --- | --- |
| `READ_SMS` & `RECEIVE_SMS` | Used to scan inbox messages and catch incoming bank SMS notifications. |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Used to read push notifications from email and banking apps for financial auto-sync. |
| `INTERNET` | Used strictly for processing optional Google Gemini AI queries. |

**5. Third-Party Services**

The app operates offline by default. If you enable optional AI capabilities, interactions are governed by Google's Privacy Policy via the Google Gemini API. We do not share or sell your data to any third-party advertisers or analytics providers.

**6. Contact & Changes**

We may update this policy to reflect changes in functionality or legal requirements. For questions regarding this policy, please open an issue on the project repository.