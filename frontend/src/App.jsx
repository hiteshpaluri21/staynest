import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'
import LandingPage from './pages/LandingPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import UserListPage from './pages/iam/UserListPage'
import AuditLogPage from './pages/iam/AuditLogPage'
import RoomTypePage from './pages/ric/RoomTypePage'
import RoomListPage from './pages/ric/RoomListPage'
import RatePlanPage from './pages/ric/RatePlanPage'
import BookingSearchPage from './pages/rbm/BookingSearchPage'
import MyReservationsPage from './pages/rbm/MyReservationsPage'
import MyStayPage from './pages/rbm/MyStayPage'
import ReservationsPage from './pages/rbm/ReservationsPage'
import GuestProfilePage from './pages/rbm/GuestProfilePage'
import FrontDeskPage from './pages/fds/FrontDeskPage'
import StayDetailPage from './pages/fds/StayDetailPage'
import StayRecordsPage from './pages/fds/StayRecordsPage'
import HousekeepingPage from './pages/hkm/HousekeepingPage'
import MaintenancePage from './pages/hkm/MaintenancePage'
import MenuPage from './pages/fbm/MenuPage'
import FBOrderPage from './pages/fbm/FBOrderPage'
import DiningReservationPage from './pages/fbm/DiningReservationPage'
import NotificationsPage from './pages/nal/NotificationsPage'

const withLayout = (roles, el, opts = {}) => (
  <ProtectedRoute roles={roles} strict={opts.strict}>
    <Layout>{el}</Layout>
  </ProtectedRoute>
)

export default function App() {
  return (
    <Routes>
      {/* Public landing page. Stays reachable when signed in, but swaps its CTAs
          for a link into the app. */}
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/unauthorized" element={<div className="p-5 text-center"><h3>403 — Unauthorized</h3></div>} />

      <Route path="/users" element={withLayout(['ADMIN'], <UserListPage />)} />
      <Route path="/audit-logs" element={withLayout(['ADMIN'], <AuditLogPage />)} />
      <Route path="/room-types" element={withLayout(['ADMIN'], <RoomTypePage />)} />
      <Route path="/rooms" element={withLayout(['ADMIN'], <RoomListPage />)} />
      <Route path="/rate-plans" element={withLayout(['ADMIN', 'GUEST'], <RatePlanPage />)} />

      {/* Guest-only, strict: an admin administers the hotel, they do not book rooms in it.
          Admins see every booking on /reservations instead. */}
      <Route path="/book" element={withLayout(['GUEST'], <BookingSearchPage />, { strict: true })} />
      {/* Guest-only, strict: admins use /reservations, which shows the same data for everyone. */}
      <Route path="/my-reservations" element={withLayout(['GUEST'], <MyReservationsPage />, { strict: true })} />
      <Route path="/my-stay" element={withLayout(['GUEST'], <MyStayPage />)} />
      <Route path="/reservations" element={withLayout(['FRONTDESK', 'ADMIN'], <ReservationsPage />)} />
      <Route path="/profile" element={withLayout(['GUEST', 'FRONTDESK', 'ADMIN'], <GuestProfilePage />)} />

      <Route path="/front-desk" element={withLayout(['FRONTDESK', 'ADMIN'], <FrontDeskPage />)} />
      <Route path="/stay-records" element={withLayout(['FRONTDESK', 'ADMIN'], <StayRecordsPage />)} />
      <Route path="/stays/:stayId" element={withLayout(['FRONTDESK', 'ADMIN'], <StayDetailPage />)} />

      {/* Front desk raises tasks here; housekeeping processes them. */}
      <Route path="/housekeeping" element={withLayout(['HOUSEKEEPING', 'FRONTDESK', 'ADMIN'], <HousekeepingPage />)} />
      <Route path="/maintenance" element={withLayout(['HOUSEKEEPING', 'ADMIN', 'GUEST'], <MaintenancePage />)} />

      <Route path="/menu" element={withLayout(['FBMANAGER', 'ADMIN', 'GUEST'], <MenuPage />)} />
      <Route path="/orders" element={withLayout(['FBMANAGER', 'ADMIN'], <FBOrderPage />)} />
      {/* Guests book a table here; F&B staff seat and complete what has been booked. */}
      <Route path="/dining-reservations" element={withLayout(['FBMANAGER', 'ADMIN', 'GUEST'], <DiningReservationPage />)} />

      <Route path="/notifications" element={withLayout(['GUEST', 'FRONTDESK', 'HOUSEKEEPING', 'FBMANAGER', 'ADMIN'], <NotificationsPage />)} />

      {/* Unknown URLs land on the public site, not the sign-in form. */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}