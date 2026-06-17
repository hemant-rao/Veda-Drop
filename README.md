# GlamGo - Premium On-Demand Doorstep Beauty & Grooming Platform

GlamGo is a modern, production-grade Android application built with **Jetpack Compose**, **Kotlin Coroutines / Flow**, and a robust hybrid data layer utilizing **Firebase Firestore** alongside local **Room Database** persistence. It delivers a seamless, high-trust experience for beauty services on-demand, styled in a luxury dark cosmic theme.

---

## 🌌 The Vision & Design Philosophy

GlamGo is engineered to look and feel premium, stepping away from generic interfaces to establish a luxury beauty salon atmosphere:
*   **Theming**: High-contrast, elegant colors utilizing `DeepPlum` (#2A0845), `GlamRose` (#FF4A70), and `GlamGold` (#D4AF37) over Slate backgrounds.
*   **Edge-to-Edge Experience**: Complete edge-to-edge rendering with proper safe bounds padding via `WindowInsets` handling.
*   **Intuitive Hierarchy**: Tactile card borders, elegant verified stickers, custom typography pairings, and modern responsive micro-actions.

---

## 🚀 Key App Modules & Features

### 1. Hybrid Persistence: FireStore Sync & Room Local Cache
*   **Live Firestore Integration**: Persists user profile updates, partner listing parameters, and chat message history.
*   **Offline Fallback Capability**: Uses a graceful configuration check (`GlamGoFirestoreManager.isEnabled`). If firebase configurations are absent, the application elegantly defaults to a local-first **Room database** pipeline to keep the app fully functional and crash-free.
*   **Real-Time Syncing**: Multi-role support where profile adjustments and services offered are pushed instantly to the cloud.

### 2. Live Booking Tracker & Status System
*   **State-Driven Appointment Pipeline**: Direct lifecycle handling from **Pending** to **Confirmed**, **In Progress**, and **Completed**.
*   **Dual Viewpoint Control Nodes**:
    *   *Customer Perspective*: Transparent progress bars on upcoming appointments, direct provider chats, and an **Instant Refund Cancellation** trigger.
    *   *Provider Panel*: Dynamic status toggles (Accept, Mark in progress, Mark completed) allowing absolute direct service execution control.
*   **Automated Wallet Escrow**: Safely deposits funds to provider wallets upon completion, minus platform commission, and completely automates instant partial/full refunds to customer wallets on booking cancellation.

### 3. Zomato/Swiggy-Style Safe Pre-Booking Discussions
*   **Pre-Booking Inquiry Line**: Before scheduling a slot, clients can initiate pre-service questions on products used or visual safe-seals.
*   **Brand Integrity Verification**: Direct question suggestions (e.g., *Is the kit 100% brand-new & sealed?*, *Which premium brands will you bring?*, *Are there hidden travel charges?*).
*   **Dynamic Auto-Replies**: Context-aware automated expert answers responding instantly to provide assurance regarding hygienic standards and certified organic brands.

### 4. Interactive Ratings & Customer Reviews
*   **Verified Experience Circle**: Allows authenticated customers who completed appointments to directly submit ratings (1-5 stars) and reviews on product brand authenticity and partner hospitality.
*   **Static & Live Aggregations**: Provider profile pages load both pre-loaded community trust reviews and actual dynamic customer reviews, preserving overall score calculations.

### 5. Preferred Beauty Experts ("Favorite" Toggle)
*   **Taggable Favorites**: Customers can tap the heart icon on any beauty partner during service search comparison.
*   **Horizontal Favorites Shelf**: Beautifully aggregated horizontal scroll tray within the `Customer Profile Screen` for rapid appointment-booking access.

### 6. Built-In Gemini AI Beauty Stylist Assistant
*   **Conversational Guidance**: Integrated Gemini LLM agent customized as a beauty stylist guiding haircut, makeup, facial, massage, and spa questions.
*   **Personalization**: Generates step-by-step grooming advice tailored strictly to user concerns, directly in the chat panel.

---

## 🛠 Tech Stack & Architecture

```
                                              [ Jetpack Compose UI ]
                                                         │
                                               [ GlamGoViewModel ]
                                                         │
                                             [ GlamGo Repository ]
                                           /                       \
                 (Offline Cache / Local db)                         (Cloud Sync Layer)
              [ Room AppDatabase / DAOs ]                 [ GlamGoFirestoreManager ]
```

*   **Jetpack Compose**: Declarative Material 3 UI.
*   **Room Database**: Offline persistence holding tables for `UserEntity`, `BookingEntity`, `PartnerServiceEntity`, `ChatMessageEntity`, `FavoritePartnerEntity`, and `ComplaintEntity`.
*   **Firebase Firestore**: Document-collection remote synchronizer supporting scalable document queries for remote databases.
*   **Kotlin Flow & Coroutines**: Reactive stream management maintaining absolute synchronization during UI transitions.

---

## 🎨 Resource Codes & UI Touchpoints

*   **TestTags (Snake Case)**: Automated tests are equipped with standard test targets (e.g., `fav_toggle_<id>`, `empty_favorites_card`, `favorites_scroll_row`, `pre_chat_input`).
*   **Accessibility First**: Interactivity triggers strictly adhere to the minimum visual touch standard constraint (minimum **48dp x 48dp** density size).

---

Developed with ❤️ using modern Android practices. For inquiries or live setups, configure a valid `google-services.json` onto the gradle bundle.
