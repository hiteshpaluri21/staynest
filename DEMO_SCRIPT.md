# StayNest — 10-Minute Live Demo Script

> **Format:** Live walkthrough of the running application, narrated.
> **Goal:** Show the full guest lifecycle across microservices + the role-based back office.
> **Total time:** 10:00

## Before You Start (pre-flight checklist — do NOT do this on stage)

Have all of this already running and warmed up:

1. **MySQL** up on `localhost:3306` (user `root` / `root`).
2. **Eureka** (`:8761`) — open its dashboard in a spare tab to prove all 8 services registered.
3. All backend services started (iam, room, reservation, frontdesk, housekeeping, fb, revenue, notification) + **api-gateway** (`:8090`).
4. **Frontend**: `npm run dev` in `frontend/` → open `http://localhost:5173`.
5. Pre-create demo logins for each role (or know the seeded IAM credentials), and have **two browser windows** side by side:
   - **Window A** = Guest
   - **Window B** = Staff (Front Desk / Admin)
6. Have at least one **Room Type**, **Rate Plan**, and a couple of **Rooms** already seeded so the availability search returns results.

---

## 0:00 – 1:00 — Opening & the Big Picture (1 min)

**Say:**
> "StayNest is a full hotel-management platform. It's not one app — it's **nine Spring Boot microservices** behind an API gateway, with a React front end. Today I'll take a real guest from **booking all the way to check-out**, and show how each service plays its part."

**Show:** The **Eureka dashboard** (`:8761`).

> "Every service registers itself here with Netflix Eureka. The gateway on port 8090 is the single front door — the React app only ever talks to the gateway, which routes by path to the right service. Each service owns its own MySQL database."

*(One sentence on the stack: Java 17, Spring Boot 4, Spring Cloud, JWT security, React + Vite.)*

---

## 1:00 – 2:00 — Login, Identity & Roles (IAM Service) (1 min)

**Show:** The **Login page** (`/login`). Log in as **Admin** in Window B.

**Say:**
> "Authentication is handled by the **IAM service**. On login it issues a **JWT**, and that same token is validated independently by every other service — so security is consistent across the whole system."

> "StayNest has **six roles** — Guest, Front Desk, Housekeeping, F&B Manager, Revenue Manager, and Admin. What you can see and do is driven by your role, both in the UI and on the backend."

**Show:** As Admin, open the **User List** page to prove the role/user management exists.

---

## 2:00 – 3:30 — Room Inventory (Room Service) (1.5 min)

**Show (as Admin/Staff):** Navigate the **Room Inventory** module:
- **Room Types** page — show a room type (e.g. Deluxe).
- **Rate Plans** page — show a rate plan and its price.
- **Room List** page — show physical rooms and their status (AVAILABLE / OCCUPIED).

**Say:**
> "Before anyone can book, we need inventory. The **Room service** manages three things: **room types** (the category and its base attributes), **rate plans** (the pricing), and the **physical rooms** themselves with a live status. This separation is what lets the booking engine check real availability in a moment."

---

## 3:30 – 5:30 — Booking a Stay (Reservation Service) (2 min)

**Switch to Window A — log in as a Guest.**

**Show:** **Booking Search** page. Enter check-in / check-out dates, pick a room type + rate plan, search.

**Say:**
> "Now the guest experience. When I search, the request goes through the gateway to the **Reservation service** — and this is where the microservices start talking to each other. Reservation calls the **Room service** over a **Feign client** to validate the room type and rate plan, then checks **real availability** by counting overlapping reservations against the number of physical rooms of that type. No overbooking of the inventory we set up."

**Show:** Confirm the booking (booking confirm modal).

**Say:**
> "On confirm, it auto-resolves the guest's profile — pulling their real identity from IAM — saves the reservation as **CONFIRMED**, and fires notifications: one to the guest, and a fan-out to **all front-desk staff**."

**Show:** Open the **notification bell** (guest) → the confirmation notification. Then flip to Window B → front desk also got notified.

**Show:** Guest's **My Reservations** page — the new CONFIRMED booking.

---

## 5:30 – 7:00 — Check-In & Folio (Front Desk Service) (1.5 min)

**Switch to Window B — log in / switch to Front Desk.**

**Show:** **Front Desk** page → find the reservation → **Check In**.

**Say:**
> "The guest arrives. Front desk checks them in — and watch how many services this single action coordinates. The **Front Desk service** validates the reservation, then creates a **Stay Record** marked ACTIVE, tells the **Room service** to flip the room to **OCCUPIED**, tells the **Reservation service** to mark it **CHECKED-IN**, and notifies the guest. One click, four services, all consistent."

**Show:** **Stay Detail** page → **Add a charge** (folio item) — e.g. a room-service charge or a discount.

**Say:**
> "During the stay, everything the guest consumes accrues to their **folio** — a running balance. Charges, discounts, F&B — it all lands here."

---

## 7:00 – 8:15 — Back Office: Housekeeping & F&B (1.25 min)

**Show (Housekeeping role or Admin):** **Housekeeping** page — create/assign a task; **Maintenance** page — a maintenance request.

**Say:**
> "The **Housekeeping service** manages cleaning tasks and maintenance requests tied to rooms — the operational side that keeps the property running."

**Show (F&B Manager or Admin):** **Menu** page → **F&B Order** page — place an order for the staying guest.

**Say:**
> "The **F&B service** runs the restaurant side — menu, orders, and dining reservations. An in-room order here posts a charge straight back to the guest's folio through the Front Desk service."

*(Flip to the folio to show the charge landed — if wired end-to-end.)*

---

## 8:15 – 9:00 — Check-Out (Front Desk Service) (0.75 min)

**Show (Front Desk):** Stay Detail → **Check Out**.

**Say:**
> "At check-out, the Front Desk service totals every folio item into the final bill, closes the **Stay Record** as CHECKED-OUT, releases the room back to **AVAILABLE**, marks the reservation **CHECKED-OUT**, and sends the guest their final total. The full lifecycle — book, stay, pay, leave — is complete."

---

## 9:00 – 9:45 — Analytics & Wrap (Revenue Service) (0.75 min)

**Show (Admin / Revenue Manager):** **Analytics Dashboard** page.

**Say:**
> "Finally, the **Revenue service** aggregates all this activity into KPIs and hospitality reports — occupancy, revenue, and operational metrics for management."

---

## 9:45 – 10:00 — Close (0.15 min)

**Say:**
> "So that's StayNest: **nine independently deployable services**, each owning its own domain and data, coordinating through a gateway, Eureka discovery, Feign calls, and a shared JWT — delivering one seamless guest journey from search to check-out. The architecture is built to scale each capability independently. Happy to dive into any service in the questions."

---

## Recovery / If Something Breaks

- **A service is down:** point to the Eureka dashboard, say "in a microservices world we degrade gracefully" — and note that **notifications are fire-and-forget**, so a notification outage never blocks a booking or check-in.
- **Availability search returns nothing:** you forgot to seed rooms/rate plans — fall back to showing an existing reservation in *My Reservations* and continue from check-in.
- **Login token expired:** just re-login; JWTs expire after 24h.

## Timing Cheat-Sheet
| Segment | Ends at |
|---|---|
| Intro + Eureka | 1:00 |
| Login / roles | 2:00 |
| Room inventory | 3:30 |
| Booking | 5:30 |
| Check-in + folio | 7:00 |
| Housekeeping + F&B | 8:15 |
| Check-out | 9:00 |
| Analytics | 9:45 |
| Close | 10:00 |
