# HomeServe Android MVP App

HomeServe is a native Android application built in Kotlin using Jetpack Compose, targeting service bookings in Tamil Nadu (Chennai, Coimbatore, Madurai, Trichy). 

This project incorporates the MVVM architecture and is integrated with the **RevenueCat SDK** for managing subscription entitlements.

## Features Built
1. **Splash Screen**: Animation and automated session check.
2. **Mock Authentication**: Fast, credential-free login/signup storing sessions locally in `SharedPreferences`.
3. **Home Dashboard**: Grid of service categories and city filters dropdown.
4. **Provider Directory**: Provider lists filtered by category and city, containing ratings, price badges, and photos.
5. **Provider Details**: Detailed overview, professional biographies, and user reviews.
6. **Booking Flow**: Pickers for scheduling dates and times.
7. **Monthly Booking Limits & Reset**: Free-tier users can book only once per month. Reset is calculated on calendar month transitions.
8. **RevenueCat Gating & Paywall**: Automatic redirection to a sleek Paywall featuring Premium (₹399/mo) and Elite (₹799/mo) plans when free booking limits are exceeded.
9. **Order History**: History logs showing statuses of upcoming bookings.
10. **Profile Control**: Subscriptions restoration, subscription upgrading, and billing simulation buttons.

## Project Structure
```
app/src/main/java/com/homeserve/app/
├── HomeServeApplication.kt   # App configuration & RevenueCat initialization
├── MainActivity.kt           # Main entry hosting NavHost
├── billing/
│   └── RevenueCatManager.kt  # RevenueCat SDK wrapper (with Simulation Mode fallback)
├── data/
│   ├── model/
│   │   └── Models.kt         # Data model representations
│   └── repository/
│       └── MockDataRepository.kt # SharedPref-backed repositories for state management
├── navigation/
│   ├── Screen.kt             # App destinations configurations
│   └── NavGraph.kt           # Controller-based routing definitions
└── ui/
    ├── components/
    │   └── CommonUI.kt       # Loading, Empty, Error, and Star rating widgets
    ├── theme/
    │   ├── Color.kt          # Professional Material 3 palettes
    │   ├── Type.kt           # Font typographies configuration
    │   └── Theme.kt          # M3 light/dark style wrapper
    └── screens/
        ├── splash/           # Splash screen visual and VM
        ├── auth/             # Combined Login / Signup UI and VM
        ├── home/             # Main dashboard grid and VM
        ├── provider/         # Directory lists, profile details, and VMs
        ├── booking/          # Date pickers booking and VM
        ├── paywall/          # Offering lists purchase forms and VM
        ├── orderhistory/     # Bookings lists status tags and VM
        └── profile/          # User subscription restore tools and VM
```

## Adding Your RevenueCat API Key
1. Open the [local.properties](file:///v:/andriod%20demo/local.properties) file at the project root.
2. Locate or add `revenuecat.apiKey`:
   ```properties
   revenuecat.apiKey="your_actual_revenuecat_api_key"
   ```
3. Sync and build the Gradle project in Android Studio. The key will be read from this file and injected securely as `BuildConfig.REVENUECAT_API_KEY` at build time.

## Running & Verification
- **Simulating Paywall**: Go to the **Profile** screen and tap **"Simulate Free booking limit"**. This will manually increase your booking count. When you go back and attempt to book another service, you will be redirected to the Paywall.
- **Simulating Subscriptions**: The app operates in a fully functional **Simulation Mode** if the default placeholder key is used. Tapping **"Subscribe Now"** or **"Restore Purchases"** will immediately update your tier to premium/elite, unlocking unlimited bookings.
- **Persistence Test**: Upgrade your subscription in the Paywall, close the app from recent tasks, and restart it. The subscription tier will be cached and restored instantly on launch.
