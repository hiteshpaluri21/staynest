import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'
import LoginPage from './pages/LoginPage'
import UserListPage from './pages/iam/UserListPage'
import RoomTypePage from './pages/ric/RoomTypePage'
import RoomListPage from './pages/ric/RoomListPage'
import RatePlanPage from './pages/ric/RatePlanPage'
import AnalyticsDashboardPage from './pages/ric/AnalyticsDashboardPage'
import BookingSearchPage from './pages/rbm/BookingSearchPage'
import MyReservationsPage from './pages/rbm/MyReservationsPage'
import GuestProfilePage from './pages/rbm/GuestProfilePage'
import FrontDeskPage from './pages/fds/FrontDeskPage'
import StayDetailPage from './pages/fds/StayDetailPage'
import HousekeepingPage from './pages/hkm/HousekeepingPage'
import MaintenancePage from './pages/hkm/MaintenancePage'
import MenuPage from './pages/fbm/MenuPage'
import FBOrderPage from './pages/fbm/FBOrderPage'
import DiningReservationPage from './pages/fbm/DiningReservationPage'
import NotificationsPage from './pages/nal/NotificationsPage'

const withLayout = (roles, el) => (
  <ProtectedRoute roles={roles}>
    <Layout>{el}</Layout>
  </ProtectedRoute>
)

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/unauthorized" element={<div className="p-5 text-center"><h3>403 — Unauthorized</h3></div>} />

      <Route path="/users" element={withLayout(['ADMIN'], <UserListPage />)} />
      <Route path="/room-types" element={withLayout(['ADMIN'], <RoomTypePage />)} />
      <Route path="/rooms" element={withLayout(['ADMIN', 'FRONTDESK'], <RoomListPage />)} />
      <Route path="/rate-plans" element={withLayout(['ADMIN', 'REVENUEMANAGER'], <RatePlanPage />)} />
      <Route path="/analytics" element={withLayout(['ADMIN', 'REVENUEMANAGER'], <AnalyticsDashboardPage />)} />

      <Route path="/book" element={withLayout(['GUEST', 'FRONTDESK', 'ADMIN'], <BookingSearchPage />)} />
      <Route path="/my-reservations" element={withLayout(['GUEST', 'FRONTDESK', 'ADMIN'], <MyReservationsPage />)} />
      <Route path="/profile" element={withLayout(['GUEST', 'FRONTDESK', 'ADMIN'], <GuestProfilePage />)} />

      <Route path="/front-desk" element={withLayout(['FRONTDESK', 'ADMIN'], <FrontDeskPage />)} />
      <Route path="/stays/:stayId" element={withLayout(['FRONTDESK', 'ADMIN'], <StayDetailPage />)} />

      <Route path="/housekeeping" element={withLayout(['HOUSEKEEPING', 'ADMIN'], <HousekeepingPage />)} />
      <Route path="/maintenance" element={withLayout(['HOUSEKEEPING', 'ADMIN'], <MaintenancePage />)} />

      <Route path="/menu" element={withLayout(['FBMANAGER', 'ADMIN'], <MenuPage />)} />
      <Route path="/orders" element={withLayout(['FBMANAGER', 'ADMIN', 'FRONTDESK'], <FBOrderPage />)} />
      <Route path="/dining-reservations" element={withLayout(['FBMANAGER', 'ADMIN'], <DiningReservationPage />)} />

      <Route path="/notifications" element={withLayout(['GUEST', 'FRONTDESK', 'HOUSEKEEPING', 'FBMANAGER', 'REVENUEMANAGER', 'ADMIN'], <NotificationsPage />)} />

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}