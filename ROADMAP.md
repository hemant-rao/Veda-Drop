# 🌸 Nikhat Glow (निखत ग्लो) — App Roadmap & Architecture Blueprint

## 📜 1. OVERVIEW (ऐप क्या है?)
**Nikhat Glow** is a premium, offline-first on-demand hyper-local home beauty salon and grooming marketplace connector app. It connects local customers looking for professional beauty treatments with certified freelance beauticians and grooming experts (known as **Partners**). 

This application bridges the trust and accessibility gap, enabling users to request deluxe salon treatments (bridal makeup, skin cleanup, massage therapy, organic facials, and hair styling) right at their doorstep, while providing independent artists with a enterprise-ready digital partner desk to run a successful, micro-salon business.

---

## ⚙️ 2. HOW IT WORKS (यह कैसे काम करता है?)

The app operates on a dual-interface architecture: **Customer Portal** and **Partner Desk**.

### 🔹 For Customers (ग्राहकों के लिए):
1. **Browse & Discover**: Customers open the feed to view a pristine, high-contrast marketplace curated with available specialists. They can instantly search for partners by name and use premium filter chips (All, Salon, Beauty, Makeup, Massage).
2. **Consult Portfolio**: Click any partner’s profile to view their live portfolio (bridal lookups, hair stylings), commercial certificates, and verified star ratings/reviews.
3. **Configure & Block Slot**: Select standard services to compile a dynamic cart. In the booking checkout, input a delivery address (supported by search-as-you-type autocomplete) and configure a preferred date and time.
4. **Real-time Availability check**: The app triggers an automatic backend availability verification on key choices. If a partner is off-duty or on leave on that date, it blocks the check and prompts the customer to select a free date.
5. **Direct Connect**: Once request is confirmed, customers can chat in real-time with the specialist to discuss exact details, and track their appointment state under **"My Bookings"**. Payouts are made directly to the freelance expert on-duty after the service.

### 🔸 For Partners (ब्यूटी पार्टनर्स के लिए):
1. **Commercial Onboarding**: Create a professional profile, configure their custom rates catalog, and complete Aadhaar/PAN commercial KYC forms for legal operation verification.
2. **Operational Control Engine**: Set live service radii and default working days. 
3. **Interactive Calendar blocker**: Use a visual, calendar grid-view to tap days and block out family vacation leaves or sick days, keeping booking calendars perfectly synchronized in real-time.
4. **Volume & Earnings Analytics**: Track business health with our **recharts-style custom Canvas Line Chart**, showing monthly booking volume trends paired with dynamic payouts progression.
5. **AI Portfolio Showcase**: Upload custom showcase images, or select from high-quality curated AI Beauty Work Presets to instantly decorate their portfolio with pre-filled premium visuals.

---

## 🚀 3. CORE COMPLETED FEATURES (प्रमुख फीचर्स की सूची)

### 🌟 Customer Experience Modules
*   **Aesthetic M3 Interface**: Built with a beautiful high-contrast dark palette featuring Space Black, Deep Plum accents, and Amber Gold highlights.
*   **Advanced Search & Category Routing**: Search bar that instantly queries partner profiles and dynamically filters them by category tags with smooth visual animations.
*   **Star Ratings & Historic Client Reviews**: Real rating averages on partner profiles with scrollable visual review cards detailing direct client feedback.
*   **"My Bookings" Tracker Feed**: Dedicated past & upcoming bookings feed categorized cleanly with individual booking states (Pending, Accepted, Completed, Cancelled).
*   **Real-time Specialist Availability Indicator**: Automatic timing slot checks during checkout. If the chosen professional has a date blocked in her calendar, she's flagged off-duty and bookings are blocked.
*   **Instant Location Autocomplete**: Real search-as-you-type suggestions integrating free OpenStreetMap (OSM) geo capabilities.

### 💼 Partner Business Modules
*   **Recharts-inspired Growth Line Chart**: Custom Canvas-rendered dual trendlines visual charting monthly booking counts (volume) and earnings (₹) with filled gradients, legends, and grid layouts.
*   **Interactive Visual Block-out Calendar**: Full grid calendar allowing partners to tap days to toggle vacation blocks in orange, with automatic client-side synchronization.
*   **Automated 24h Service Attendance Alerts**: Proactive alert loops checking upcoming confirmed appointments 24 hours prior, triggering helpful push alerts for both customer and partner to guarantee perfect attendance.
*   **AI Portfolio Generator Presets**: Beautiful preset selector options (Bridal Glow, Organic Facial, Balayage Hair, Luxe Pedicure) that automatically load high-quality Unsplash beauty photography and pre-populated captions.
*   **Rate Catalog & Service Radius Control**: Partners can set their custom service coverage ranges and introduce custom salon listings.
*   **Enterprise KYC Onboarding**: Custom digital registration form verifying commercial specialists.

---

## 🛠️ 4. TECH-STACK BLUEPRINT (तकनीकी विनिर्देश)

*   **Language**: 100% Kotlin
*   **UI Framework**: Jetpack Compose (Declarative UI)
*   **Design Language**: Material Design 3 (M3)
*   **Core Concurrency**: Kotlin Coroutines & Flow (StateFlow, SharedFlow)
*   **Data Layer**: Clean Repository Pattern with SQLite Local Persistence
*   **Image Loading**: Coil AsyncImage (curated high-resolution Unsplash assets)
*   **Visual Art**: Custom Canvas mathematical graphics

---

## 📈 5. FUTURE ROADMAP (भविष्य की योजनाएं)

```
┌─────────────────────────────────┐
│     Current Stable Build        │   ◄── Completed: Live Calendar, Custom Charts, Presets, 24h Alerts
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│  Phase 1: Deep Local Analytics  │   ◄── Focus: Weekly analytics breakdowns, dynamic micro-surges
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│  Phase 2: Digital Payment Gate   │   ◄── Focus: Escrow-based UPI pre-auth transfers
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│  Phase 3: AI Skin-tone Consult  │   ◄── Focus: AR face-scanning live cosmetic lookup matches
└─────────────────────────────────┘
```

1.  **AI Skin Cosmetics Consultation**: Integrate Gemini AI vision scanner to let customers photograph themselves and receive custom skin shade lipstick/concealer matches.
2.  **Digital Escrow-based UPI**: Provide dynamic pre-authorised customer UPI payments that stay safe in local trust reserves until the client marks the skincare service completed.
3.  **Local Micro-Surge Pricing**: Algorithmic price surges during wedding season or rainy festivals to maximize freelance beauticians' hourly earnings.

---
*Roadmap beautifully detailed, updated, and verified under AI Studio guidelines.*
