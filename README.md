# SpacePal

**Find, reserve and manage parking — in one app.**

SpacePal is an Android app that connects drivers looking for parking with the people who have parking to offer: private lot owners, businesses and event organizers.

---

## The problem

Finding parking in busy cities is slow, stressful and wasteful:

- **Drivers can't see what's free.** People circle the block or drive from lot to lot hoping for a space, burning fuel and time and adding to traffic.
- **No way to hold a space.** Even when a driver knows a lot exists, there's no guarantee a space will still be there on arrival.
- **Private parking goes unused.** Many owners have empty spaces (driveways, office lots, land near busy areas) but no simple way to list them or manage who uses them.
- **Events make it worse.** Concerts, exhibitions and fairs bring sudden crowds, and visitors don't know where the organizer's official parking is.
- **Trust is missing.** Drivers have no way to tell whether a listed "parking spot" is real, or whether the person listing it actually owns or controls the land.
- **Manual check-in.** Lot operators track arrivals and departures on paper or by eye, so occupancy is guesswork.

## How SpacePal solves it

| Gap | SpacePal's answer |
|---|---|
| Can't see free spaces | A **live map** of parking spots, color-coded by availability (blue = spaces free, red = full), with distance from your current location and place search. |
| Can't hold a space | **One-tap reservations.** A driver reserves a spot and gets a QR code. Unused reservations expire automatically so spaces aren't blocked forever. |
| Unused private parking | Any **manager** can list spots by picking the location on a map and setting the number of spaces. A switch hides or shows a spot whenever they like. |
| Event parking confusion | Managers can post **event promotions** (banner, description, dates). Drivers see them on their dashboard, and tapping one shows only that organizer's parking on the map. |
| Lack of trust | **Two-tier verification:** managers verify their identity (ID + selfie), and each spot is verified with a property deed / title document. **Only verified spots appear to drivers.** |
| Manual check-in | Managers **scan the driver's QR code** to check them in and again to check them out. Available-space counts update in real time, and the manager's dashboard shows occupancy and current bookings. |

---

## Who uses it

### Drivers
1. **Dashboard:** quick access to your most-used spots and current event promotions.
2. **Map:** browse verified spots, see availability and distance, search places, reserve.
3. **My Reservations:** your active booking's QR code with a countdown, plus booking history.
4. **Account:** profile and logout.

### Managers (lot owners / organizers)
1. **Dashboard:** once verified, shows occupancy (e.g. `3 / 10`), live list of incoming and parked drivers, and a QR scanner for check-in/out.
2. **My Spots:** add/edit spots, upload a deed to verify each spot, toggle visibility.
3. **My Events:** create event posts (submitted for approval before going live).
4. **Profile & Settings.**

---

## How a booking works

```
Driver reserves  ──►  active        (QR code issued, short expiry window)
Manager scans    ──►  in-progress   (driver checked in, one space taken)
Manager scans    ──►  completed     (driver checked out, space freed)

Driver cancels or reservation expires  ──►  cancelled
```

Check-in uses a database transaction, so two drivers can't take the last space at the same time.

## Verification flow

```
Manager:  UNVERIFIED ──(upload ID + selfie)──► PENDING_ID ──(approved)──► ID_VERIFIED
Spot:     UNVERIFIED ──(upload deed)────────► PENDING_DEED ──(approved)──► TITLE_VERIFIED
Event:    PENDING_APPROVAL ──(approved)──► ACTIVE
```

Approval is done by an administrator outside the app (directly in Firebase).

---

## Tech stack

- **Android** (Java), min SDK 27, target SDK 34
- **Firebase Authentication:** email/password login
- **Firebase Realtime Database:** users, parking spots, reservations, events
- **Firebase Storage:** ID/selfie photos, deed documents, event banners
- **Google Maps SDK, Places API, Directions API:** map, search, routes
- **ZXing:** QR code generation and scanning
- **Glide:** image loading
- **Material Components:** UI

## Project structure

```
app/src/main/java/com/example/sp_v1/
├── auth/       Login and registration
├── models/     User, ParkingSpot, Reservation, EventPost
├── ui/         Driver screens, map, verification, spot editing
│   └── manager/  Manager dashboard, spots, events, profile, settings
├── adapters/   RecyclerView adapters for lists
└── utils/      DatabaseHelper (all Firebase reads/writes), DateUtils
```

## Getting started

1. Open the project in **Android Studio**.
2. Add your Firebase config file at `app/google-services.json` (from your Firebase project with Auth, Realtime Database and Storage enabled).
3. Add your Google Maps key to `local.properties` in the project root:
   ```
   MAPS_API_KEY=your_maps_api_key
   ```
4. Set `directions_api_key` in `app/src/main/res/values/strings.xml`.
5. Build and run on a device or emulator with Google Play services.

> Keep API keys and `google-services.json` out of public repositories.

