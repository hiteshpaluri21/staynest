import { Container, Button, Dropdown } from 'react-bootstrap'
import { Link, useNavigate } from 'react-router-dom'
import { FaUserCircle, FaSignOutAlt, FaThLarge } from 'react-icons/fa'
import ThemeToggle from '../components/ThemeToggle'
import { useAuth } from '../context/AuthContext'
import { homeFor, canBook, canViewProfile } from '../utils/home'

/*
 * StayNest — public hotel site, served at "/".
 *
 * This is the hotel's own front page: it sells the property to a visitor. Nothing
 * on it needs an account. The one call-to-action sends signed-in guests straight
 * to the booking search and everyone else to sign-in (which itself offers
 * registration, and drops guests on /book once they are in).
 */

export default function LandingPage() {
  // Wait for the session check, otherwise a signed-in guest sees "Log in" flash first.
  const { user, isAuthenticated, loading, logout } = useAuth()
  const navigate = useNavigate()
  const signedIn = isAuthenticated && !loading

  // Already on the landing page, so this just clears the session and lets the header
  // swap the account chip back to "Log in".
  const handleLogout = () => {
    logout()
    navigate('/')
  }

  /*
   * Booking is only open to guests — the /book route rejects every staff account,
   * admins included, which used to dump them on /unauthorized from this very
   * button. Staff get a link to their own console instead, so nothing on this
   * page can lead to a dead end.
   */
  const cta = signedIn && !canBook(user?.role)
    ? { to: homeFor(user?.role), label: 'Open my dashboard' }
    : { to: signedIn ? '/book' : '/login', label: 'Book a room' }

  return (
    <div className="site">
      {/* ========================================================== header == */}
      <header className="site-header">
        <Container className="d-flex align-items-center gap-3 py-3">
          <span className="brand-wordmark fs-5 me-auto">
            Stay<span style={{ color: 'var(--sn-accent)' }}>Nest</span>
          </span>

          <ThemeToggle />

          {/*
           * Signed out: a way in. Signed in: the same account menu as the app's own top
           * bar. This used to be a bare link to the dashboard, so clicking it navigated
           * away with no way to sign out without first entering the console.
           */}
          {signedIn ? (
            <Dropdown align="end">
              <Dropdown.Toggle variant="link" id="viewer-dd" className="viewer-chip">
                <FaUserCircle aria-hidden="true" />
                <span className="name d-none d-sm-inline">{user?.name || 'Signed in'}</span>
                <span className="badge bg-secondary">{user?.role}</span>
              </Dropdown.Toggle>
              <Dropdown.Menu>
                <Dropdown.Item as={Link} to={homeFor(user?.role)}>
                  <FaThLarge className="me-2" /> My Dashboard
                </Dropdown.Item>
                {canViewProfile(user?.role) &&
                  <Dropdown.Item as={Link} to="/profile">My Profile</Dropdown.Item>}
                <Dropdown.Item as={Link} to="/notifications">Notifications</Dropdown.Item>
                <Dropdown.Divider />
                <Dropdown.Item onClick={handleLogout}>
                  <FaSignOutAlt className="me-2" /> Logout
                </Dropdown.Item>
              </Dropdown.Menu>
            </Dropdown>
          ) : (
            <Button as={Link} to="/login" size="sm">Log in</Button>
          )}
        </Container>
      </header>

      {/* ============================================================ hero == */}
      <section className="hero">
        <Container>
          <span className="section-label">Downtown · Since 1974</span>
          <h1>A calm room in the middle of everything.</h1>
          <p className="hero-sub">
            Four places to eat, a rooftop pool, and a front desk that never closes —
            five minutes from the old quarter.
          </p>

          <Button as={Link} to={cta.to} size="lg">{cta.label}</Button>

          <dl className="hero-facts">
            <div>
              <dt>Check-in</dt>
              <dd>From 2:00 pm</dd>
            </div>
            <div>
              <dt>Check-out</dt>
              <dd>Until 11:00 am</dd>
            </div>
            <div>
              <dt>Front desk</dt>
              <dd>24 hours</dd>
            </div>
          </dl>
        </Container>
      </section>
    </div>
  )
}
