import { Container, Button } from 'react-bootstrap'
import { Link } from 'react-router-dom'
import {
  FaSwimmingPool, FaDumbbell, FaWifi, FaCarSide, FaConciergeBell, FaBroom,
  FaUtensils, FaBriefcase, FaMapMarkerAlt, FaPhoneAlt, FaEnvelope, FaClock,
  FaUserCircle,
} from 'react-icons/fa'
import ThemeToggle from '../components/ThemeToggle'
import { useAuth } from '../context/AuthContext'
import { homeFor, canBook } from '../utils/home'

/*
 * StayNest — public hotel site, served at "/".
 *
 * This is the hotel's own front page: it sells the property to a visitor. Nothing
 * on it needs an account. Every booking button goes through <BookCta> below,
 * which sends signed-in guests straight to the booking search and everyone else
 * to sign-in (which itself offers registration, and drops guests on /book once
 * they are in).
 *
 * Content lives in the four arrays below, so editing the site is editing data —
 * no JSX to touch. Room names and the restaurant names match what the backend
 * actually has (RoomTypeName enum, and the outlets in DiningReservationPage).
 *
 * NOTE FOR WHOEVER OWNS THE COPY: rates, phone, email, address and opening hours
 * below are placeholders. Swap them for the real thing before this goes public.
 */

const ROOMS = [
  {
    name: 'Standard',
    blurb: 'A quiet, well-sized room with everything you need for a short stay.',
    bed: '1 queen bed',
    sleeps: 'Sleeps 2',
    size: '28 m²',
    rate: '4,500',
    amenities: ['Free Wi-Fi', 'Air conditioning', 'Work desk', 'Rain shower'],
  },
  {
    name: 'Deluxe',
    blurb: 'More room to spread out, with a seating corner and a city view.',
    bed: '1 king bed',
    sleeps: 'Sleeps 2',
    size: '36 m²',
    rate: '6,800',
    amenities: ['City view', 'Seating area', 'Nespresso', 'Bathtub'],
  },
  {
    name: 'Suite',
    blurb: 'A separate living room, ideal for longer stays or a small family.',
    bed: '1 king + sofa bed',
    sleeps: 'Sleeps 4',
    size: '58 m²',
    rate: '11,200',
    amenities: ['Living room', 'Lounge access', 'Dining table', 'Walk-in robe'],
  },
  {
    name: 'Villa',
    blurb: 'Our largest space — private terrace, garden access and a plunge pool.',
    bed: '2 king beds',
    sleeps: 'Sleeps 5',
    size: '95 m²',
    rate: '18,500',
    amenities: ['Private terrace', 'Plunge pool', 'Garden access', 'Butler service'],
  },
]

// These four outlets are the ones the F&B module books tables for.
const DINING = [
  {
    name: 'The Garden Bistro',
    kind: 'All day dining',
    text: 'Breakfast through to dinner in a bright courtyard room, with a menu that leans on local produce.',
    hours: '6:30 – 23:00 daily',
  },
  {
    name: 'Sky Lounge',
    kind: 'Bar & small plates',
    text: 'Cocktails and sharing plates on the top floor, with the city laid out beneath you.',
    hours: '17:00 – 01:00 daily',
  },
  {
    name: 'Rooftop Grill',
    kind: 'Fine dining',
    text: 'Open-fire cooking and a tasting menu that changes with the season. Reservations recommended.',
    hours: '19:00 – 23:00, Tue – Sun',
  },
  {
    name: 'Poolside Bar',
    kind: 'Casual',
    text: 'Cold drinks, light bites and ice cream, served without you having to leave your lounger.',
    hours: '10:00 – 19:00 daily',
  },
]

const FACILITIES = [
  { icon: <FaSwimmingPool />, name: 'Rooftop pool', text: 'Heated, open from 6am to 10pm.' },
  { icon: <FaDumbbell />, name: 'Gym & spa', text: 'Cardio, weights, sauna and treatment rooms.' },
  { icon: <FaConciergeBell />, name: '24-hour front desk', text: 'Someone is always on the desk.' },
  { icon: <FaUtensils />, name: 'In-room dining', text: 'The full menu, brought to your door.' },
  { icon: <FaBroom />, name: 'Daily housekeeping', text: 'Turndown service on request.' },
  { icon: <FaWifi />, name: 'Fast Wi-Fi', text: 'Free throughout the property.' },
  { icon: <FaCarSide />, name: 'Valet parking', text: 'On-site, with EV charging.' },
  { icon: <FaBriefcase />, name: 'Meeting rooms', text: 'Four rooms, seating 8 to 60.' },
]

const NAV_LINKS = [
  { href: '#rooms', label: 'Rooms' },
  { href: '#dining', label: 'Dining' },
  { href: '#facilities', label: 'Facilities' },
  { href: '#contact', label: 'Contact' },
]

export default function LandingPage() {
  // Wait for the session check, otherwise a signed-in guest sees "Log in" flash first.
  const { user, isAuthenticated, loading } = useAuth()
  const signedIn = isAuthenticated && !loading

  /*
   * Booking is only open to guests (and admins) — the /book route rejects front
   * desk, housekeeping and F&B accounts, which used to dump them on
   * /unauthorized from these very buttons. Staff get a link to their own console
   * instead, so no button on this page can lead to a dead end.
   */
  const staffViewer = signedIn && !canBook(user?.role)

  /**
   * Every booking call-to-action on the page. Guests and visitors get "Book a
   * room"; staff get a link into their own console, because booking would 403.
   */
  const BookCta = ({ size }) => staffViewer
    ? <Button as={Link} to={homeFor(user?.role)} size={size}>Open my dashboard</Button>
    : <Button as={Link} to={signedIn ? '/book' : '/login'} size={size}>Book a room</Button>

  return (
    <div className="site">
      {/* ========================================================== header == */}
      <header className="site-header">
        <Container className="d-flex align-items-center gap-3 py-3">
          <span className="brand-wordmark fs-5 me-auto">
            Stay<span style={{ color: 'var(--sn-accent)' }}>Nest</span>
          </span>

          <nav className="nav-anchors">
            {NAV_LINKS.map(l => <a key={l.href} href={l.href}>{l.label}</a>)}
          </nav>

          <ThemeToggle />

          {/* Signed out: a way in. Signed in: who you are, linking to your console. */}
          {signedIn ? (
            <Link to={homeFor(user?.role)} className="viewer-chip" title="Go to your dashboard">
              <FaUserCircle aria-hidden="true" />
              <span className="name d-none d-sm-inline">{user?.name || 'Signed in'}</span>
              <span className="badge bg-secondary">{user?.role}</span>
            </Link>
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
            One hundred and forty-eight rooms, four places to eat, a rooftop pool, and a front
            desk that never closes — five minutes from the old quarter.
          </p>

          <div className="hero-actions">
            <BookCta size="lg" />
            <Button href="#rooms" variant="outline-primary" size="lg">Explore rooms</Button>
          </div>

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
            <div>
              <dt>Rooms</dt>
              <dd>148</dd>
            </div>
          </dl>
        </Container>
      </section>

      {/* =========================================================== rooms == */}
      <section className="section" id="rooms">
        <Container>
          <div className="section-head">
            <span className="section-label">Rooms &amp; suites</span>
            <h2>Four ways to stay</h2>
            <p>Every room is cleaned daily and comes with fast Wi-Fi, air conditioning and blackout blinds.</p>
          </div>

          <div className="auto-grid" style={{ '--min': '17rem' }}>
            {ROOMS.map(room => (
              <article className="room-card" key={room.name}>
                <div className="room-photo">{room.name}</div>
                <div className="room-body">
                  <p className="mb-0 small">{room.blurb}</p>

                  <div className="room-meta">
                    <span>{room.bed}</span>
                    <span>{room.sleeps}</span>
                    <span>{room.size}</span>
                  </div>

                  <ul className="room-amenities">
                    {room.amenities.map(a => <li key={a}>{a}</li>)}
                  </ul>

                  <div className="room-rate">
                    <div>
                      <span className="amount">₹{room.rate}</span>
                      <span className="per"> / night</span>
                    </div>
                    <BookCta size="sm" />
                  </div>
                </div>
              </article>
            ))}
          </div>
        </Container>
      </section>

      {/* ========================================================== dining == */}
      <section className="section section-alt" id="dining">
        <Container>
          <div className="section-head center">
            <span className="section-label">Eating &amp; drinking</span>
            <h2>Four kitchens, one address</h2>
            <p>Guests can reserve a table from their account, or just walk in.</p>
          </div>

          <div className="auto-grid" style={{ '--min': '15rem' }}>
            {DINING.map(outlet => (
              <article className="dining-card" key={outlet.name}>
                <span className="kind">{outlet.kind}</span>
                <h3>{outlet.name}</h3>
                <p>{outlet.text}</p>
                <p className="hours"><FaClock className="me-2" aria-hidden="true" />{outlet.hours}</p>
              </article>
            ))}
          </div>
        </Container>
      </section>

      {/* ====================================================== facilities == */}
      <section className="section" id="facilities">
        <Container>
          <div className="section-head">
            <span className="section-label">Facilities</span>
            <h2>What's included</h2>
            <p>No resort fees. Everything below is part of the room rate.</p>
          </div>

          <div className="auto-grid" style={{ '--min': '15rem' }}>
            {FACILITIES.map(f => (
              <div className="facility" key={f.name}>
                <span className="facility-icon" aria-hidden="true">{f.icon}</span>
                <div>
                  <h3>{f.name}</h3>
                  <p>{f.text}</p>
                </div>
              </div>
            ))}
          </div>
        </Container>
      </section>

      {/* ========================================================= contact == */}
      <section className="section section-alt" id="contact">
        <Container>
          <div className="section-head">
            <span className="section-label">Finding us</span>
            <h2>Getting here</h2>
          </div>

          <div className="auto-grid" style={{ '--min': '18rem' }}>
            <div className="contact-card">
              <dl className="mb-0">
                <dt><FaMapMarkerAlt className="me-2" aria-hidden="true" />Address</dt>
                <dd>
                  StayNest Downtown<br />
                  14 Harbour Road, Fort District<br />
                  Mumbai 400001
                </dd>
                <dt><FaPhoneAlt className="me-2" aria-hidden="true" />Reservations</dt>
                <dd>+91 22 4000 1974</dd>
                <dt><FaEnvelope className="me-2" aria-hidden="true" />Email</dt>
                <dd>stay@staynest.example</dd>
              </dl>
            </div>

            <div className="contact-card">
              <dl className="mb-0">
                <dt>By air</dt>
                <dd>25 minutes from the international terminal. Airport transfers on request.</dd>
                <dt>By train</dt>
                <dd>A ten-minute walk from Central station, or three minutes by taxi.</dd>
                <dt>Parking</dt>
                <dd>Valet parking on site, with four EV charging bays.</dd>
              </dl>
            </div>
          </div>
        </Container>
      </section>

      {/* ===================================================== closing cta == */}
      <Container>
        <section className="section pt-0">
          <div className="closing-cta">
            <h2>Come and stay with us</h2>
            <p>Check availability for your dates and book in a couple of minutes.</p>
            <BookCta size="lg" />
          </div>
        </section>
      </Container>

      {/* ========================================================== footer == */}
      <footer className="site-footer">
        <Container className="d-flex flex-wrap justify-content-between gap-3">
          <span>&copy; {new Date().getFullYear()} StayNest Downtown</span>
          <span className="d-flex flex-wrap gap-3">
            {NAV_LINKS.map(l => <a key={l.href} href={l.href}>{l.label}</a>)}
            {/* Staff reach the app the same way guests do. */}
            <Link to="/login">Staff sign-in</Link>
          </span>
        </Container>
      </footer>
    </div>
  )
}
