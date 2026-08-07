import { useEffect, useState } from 'react'
import { Container, Button, Dropdown, Carousel } from 'react-bootstrap'
import { Link, useNavigate } from 'react-router-dom'
import {
  FaSwimmingPool, FaDumbbell, FaWifi, FaCarSide, FaConciergeBell, FaBroom,
  FaUtensils, FaBriefcase, FaUserCircle, FaSignOutAlt, FaThLarge, FaCheck,
} from 'react-icons/fa'
import ThemeToggle from '../components/ThemeToggle'
import { useAuth } from '../context/AuthContext'
import { getRoomTypes } from '../services/ric/roomTypeService'
import { homeFor, canBook, canViewProfile } from '../utils/home'
import heroHotel from '../assets/photos/hero-hotel.jpg'
import roomStandard from '../assets/photos/room-standard.jpg'
import roomDeluxe from '../assets/photos/room-deluxe.jpg'
import roomSuite from '../assets/photos/room-suite.jpg'
import roomVilla from '../assets/photos/room-villa.jpg'
import diningBistro from '../assets/photos/dining-bistro.jpg'
import diningSkyLounge from '../assets/photos/dining-sky-lounge.jpg'
import diningGrill from '../assets/photos/dining-grill.jpg'
import diningPoolside from '../assets/photos/dining-poolside.jpg'
import slideAbout from '../assets/photos/slide-about.jpg'
import slidePolicies from '../assets/photos/slide-policies.jpg'

/*
 * StayNest — public hotel site, served at "/".
 *
 * This is the hotel's own front page: it sells the property to a visitor. Nothing on it needs
 * an account. Every booking button goes through <BookCta> below, which sends signed-in guests
 * to the booking search and everyone else to sign-in.
 *
 * ON THE COPY — please keep to this rule when editing:
 *
 * Nothing here states a fact the project cannot stand behind. There is no founding year, no
 * room count, no street address and no phone number, because inventing them makes the whole
 * page read as filler. What is left is either a genuine property description or a promise the
 * software actually keeps — the policies below, for instance, are the rules the services
 * really enforce (see ReservationServiceImpl#cancelReservation, StayRecordServiceImpl and
 * DiningReservationServiceImpl#rejectClashingBooking). If you change a rule in the backend,
 * change it here too.
 */

/** One line, used as the eyebrow in the hero and again in the footer. */
const TAGLINE = 'Rest, well kept.'

/*
 * Rates, beds, occupancy and amenities are read live from room-service, so the public site and
 * the booking screens can never quote different numbers. Only the sales copy and the
 * photograph are held here, keyed by the RoomTypeName enum — the backend has no field for
 * either, and neither belongs in a database row.
 *
 * ORDER is the order the four cards appear in, cheapest first.
 */
const ORDER = ['STANDARD', 'DELUXE', 'SUITE', 'VILLA']

const ROOM_COPY = {
  STANDARD: {
    label: 'Standard',
    blurb: 'Everything a short stay needs, and nothing you would not use.',
    image: roomStandard,
  },
  DELUXE: {
    label: 'Deluxe',
    blurb: 'More floor, a proper armchair, and a window worth opening the blinds for.',
    image: roomDeluxe,
  },
  SUITE: {
    label: 'Suite',
    blurb: 'A living room of its own — for longer stays, or when the family comes along.',
    image: roomSuite,
  },
  VILLA: {
    label: 'Villa',
    blurb: 'Our largest room, with a private plunge pool and a garden nobody walks through.',
    image: roomVilla,
  },
}

/** "3,500.00" reads better as "3,500" on a price tag. */
const formatRate = (rate) => {
  const n = Number(rate)
  return Number.isFinite(n) ? n.toLocaleString('en-IN', { maximumFractionDigits: 0 }) : rate
}

/** amenitiesList is a free-text field; staff have used commas and pipes interchangeably. */
const splitAmenities = (list) => String(list || '')
  .split(/[|,]/)
  .map(a => a.trim())
  .filter(Boolean)
  .slice(0, 4)

/*
 * The four outlets the F&B module books tables for.
 *
 * No opening hours: the booking form accepts any time, so printing hours would state a rule
 * the system does not enforce.
 */
const DINING = [
  {
    name: 'The Garden Bistro',
    kind: 'All day',
    text: 'Breakfast that runs late and a kitchen that stays open until the last table leaves.',
    image: diningBistro,
  },
  {
    name: 'Sky Lounge',
    kind: 'Bar & small plates',
    text: 'The top floor, a short cocktail list, and the city going quiet underneath you.',
    image: diningSkyLounge,
  },
  {
    name: 'Rooftop Grill',
    kind: 'Fine dining',
    text: 'Cooking over open fire, on a menu that changes when the produce does.',
    image: diningGrill,
  },
  {
    name: 'Poolside Bar',
    kind: 'Casual',
    text: 'Cold drinks and something small to eat, without leaving your lounger.',
    image: diningPoolside,
  },
]

const FACILITIES = [
  { icon: <FaSwimmingPool />, name: 'Rooftop pool', text: 'A quiet length of water above the traffic.' },
  { icon: <FaDumbbell />, name: 'Gym & spa', text: 'Weights, cardio, sauna and treatment rooms.' },
  { icon: <FaConciergeBell />, name: 'Front desk', text: 'Staffed around the clock, in person or from your room.' },
  { icon: <FaUtensils />, name: 'In-room dining', text: 'Order the full menu; it lands on your folio.' },
  { icon: <FaBroom />, name: 'Housekeeping', text: 'Serviced daily, turndown whenever you ask.' },
  { icon: <FaWifi />, name: 'Wi-Fi', text: 'Included everywhere, with no sign-in codes.' },
  { icon: <FaCarSide />, name: 'Valet parking', text: 'Hand the keys over at the door.' },
  { icon: <FaBriefcase />, name: 'Meeting rooms', text: 'Quiet space to work, arranged at the desk.' },
]

/*
 * The two slides in the carousel. Every line in POLICIES is a rule the backend genuinely
 * applies — see the note at the top of this file before adding to it.
 */
const POLICIES = [
  'Check in from 2:00 pm. Check out by 11:00 am.',
  'Cancel free while your booking still reads Confirmed. Once you have checked in, the desk handles it.',
  'You book a room type, not a room number — your room is assigned when you arrive.',
  'Charges reach your folio as they happen, so there is one bill and no surprises at the end.',
  'A table booking holds that outlet for your whole sitting. Cancel it if plans change and the slot frees up at once.',
  'Your folio and your notifications are yours alone. No other account can open them.',
]

const NAV_LINKS = [
  { href: '#rooms', label: 'Rooms' },
  { href: '#dining', label: 'Dining' },
  { href: '#facilities', label: 'Facilities' },
  { href: '#about', label: 'About' },
  { href: '#contact', label: 'Contact' },
]

export default function LandingPage() {
  // Wait for the session check, otherwise a signed-in guest sees "Log in" flash first.
  const { user, isAuthenticated, loading, logout } = useAuth()
  const navigate = useNavigate()
  const signedIn = isAuthenticated && !loading

  // Signing out leaves you on the public site rather than at a login prompt you did not ask for.
  const handleLogout = () => {
    logout()
    navigate('/')
  }

  /*
   * Room types come from room-service so the rates here and in the booking screens can never
   * disagree. GET /api/room-types is open to visitors for exactly this.
   *
   * A failure leaves the cards out rather than showing invented prices — quoting a rate the
   * hotel might not honour is worse than showing nothing.
   */
  const [roomTypes, setRoomTypes] = useState(null)
  useEffect(() => {
    getRoomTypes()
      .then(types => setRoomTypes(types || []))
      .catch(() => setRoomTypes([]))
  }, [])

  // The four headline types, cheapest first, skipping any that have been withdrawn.
  const rooms = (roomTypes || [])
    .filter(t => ROOM_COPY[t.name] && (!t.status || t.status === 'ACTIVE'))
    .sort((a, b) => ORDER.indexOf(a.name) - ORDER.indexOf(b.name))

  /*
   * Booking is only open to guests — the /book route rejects every staff account, admins
   * included, which used to dump them on /unauthorized from these very buttons. Staff get a
   * link to their own console instead, so no button on this page can lead to a dead end.
   */
  const staffViewer = signedIn && !canBook(user?.role)

  /** Every booking call-to-action on the page. */
  const BookCta = ({ size, variant }) => staffViewer
    ? <Button as={Link} to={homeFor(user?.role)} size={size} variant={variant}>Open my dashboard</Button>
    : <Button as={Link} to={signedIn ? '/book' : '/login'} size={size} variant={variant}>Book a room</Button>

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

          {/* Signed out: a way in. Signed in: the same account menu as the app's own top bar. */}
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
          {/* Two columns from lg up; the photograph drops below the copy on narrower screens
              rather than squeezing the headline into a column too thin to read. */}
          <div className="hero-layout">
            <div className="hero-copy">
              <span className="section-label">{TAGLINE}</span>
              <h1>A room that's ready before you are.</h1>
              <p className="hero-sub">
                Four kinds of room, four kitchens and a desk that answers at any hour. Book in a
                minute; we will have the rest in order before you arrive.
              </p>

              <div className="hero-actions">
                <BookCta size="lg" />
                <Button href="#rooms" variant="outline-primary" size="lg">See the rooms</Button>
              </div>
            </div>

            {/* Decorative: the copy already says what the picture shows. */}
            <div className="hero-art">
              <img src={heroHotel} alt="" fetchPriority="high" />
            </div>
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
              <dd>Around the clock</dd>
            </div>
            <div>
              <dt>Booking</dt>
              <dd>Confirmed at once</dd>
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
            <p>
              Serviced daily, with fast Wi-Fi, air conditioning and blackout blinds as standard.
              Every rate below is read live from the system we run the hotel on, so the price
              here is the price you pay.
            </p>
          </div>

          {/* All four across on one row from lg up. */}
          <div className="room-row">
            {rooms.map(room => {
              const copy = ROOM_COPY[room.name]
              const amenities = splitAmenities(room.amenitiesList)
              return (
                <article className="room-card" key={room.roomTypeId}>
                  <div className="room-photo">
                    <img src={copy.image} alt={`${copy.label} room`} loading="lazy" />
                    <span className="room-photo-label">{copy.label}</span>
                  </div>
                  <div className="room-body">
                    <p className="mb-0 small">{copy.blurb}</p>

                    <div className="room-meta">
                      {room.bedConfiguration && <span>{room.bedConfiguration}</span>}
                      {room.maxOccupancy && <span>Sleeps {room.maxOccupancy}</span>}
                    </div>

                    {amenities.length > 0 && (
                      <ul className="room-amenities">
                        {amenities.map(a => <li key={a}>{a}</li>)}
                      </ul>
                    )}

                    <div className="room-rate">
                      <div>
                        <span className="amount">₹{formatRate(room.baseRate)}</span>
                        <span className="per"> / night</span>
                      </div>
                      <BookCta size="sm" />
                    </div>
                  </div>
                </article>
              )
            })}
          </div>

          {/* Only while the fetch is in flight, or if room-service could not be reached. */}
          {roomTypes === null && <p className="text-muted text-center mb-0">Loading our rooms…</p>}
          {roomTypes !== null && rooms.length === 0 && (
            <p className="text-muted text-center mb-0">
              Room details are unavailable right now. Please try again in a moment.
            </p>
          )}
        </Container>
      </section>

      {/* ========================================================== dining == */}
      <section className="section section-alt" id="dining">
        <Container>
          <div className="section-head center">
            <span className="section-label">Eating &amp; drinking</span>
            <h2>Four kitchens, one address</h2>
            <p>
              Reserve a table from your account and the room is held for your whole sitting —
              nobody can book over the top of you.
            </p>
          </div>

          {/* Uniform tiles: the description is revealed on hover, and on focus so that a tap
              or the keyboard works too. */}
          <div className="dining-row">
            {DINING.map(outlet => (
              <article className="dining-tile" key={outlet.name} tabIndex={0}>
                <img src={outlet.image} alt={outlet.name} loading="lazy" />
                <div className="dining-veil">
                  <span className="kind">{outlet.kind}</span>
                  <h3>{outlet.name}</h3>
                  <p>{outlet.text}</p>
                </div>
              </article>
            ))}
          </div>
        </Container>
      </section>

      {/* =========================================== about / policies slides == */}
      <section className="section" id="about">
        <Container>
          <Carousel
            fade
            interval={9000}
            className="story-carousel"
            prevLabel="Previous"
            nextLabel="Next"
          >
            <Carousel.Item>
              <div className="story">
                <div className="story-art">
                  <img src={slideAbout} alt="The StayNest lobby lounge" loading="lazy" />
                </div>
                <div className="story-copy">
                  <span className="section-label">About us</span>
                  <h2>One hotel, run on one system</h2>
                  <p>
                    StayNest is a hotel and the software that runs it. The desk that checks you
                    in, the team that services your room, the kitchens that send breakfast up and
                    the bill you settle on the way out all sit on the same platform — the one you
                    just booked through.
                  </p>
                  <p>
                    Nothing is copied by hand between systems, so your folio is right to the
                    minute and your room is assigned before you reach the counter. It is a small
                    thing that you only notice when it is missing.
                  </p>
                </div>
              </div>
            </Carousel.Item>

            <Carousel.Item>
              <div className="story">
                <div className="story-art">
                  <img src={slidePolicies} alt="The StayNest front desk" loading="lazy" />
                </div>
                <div className="story-copy">
                  <span className="section-label">Policies &amp; house rules</span>
                  <h2>The short version, in plain words</h2>
                  <ul className="policy-list">
                    {POLICIES.map(p => (
                      <li key={p}><FaCheck aria-hidden="true" /><span>{p}</span></li>
                    ))}
                  </ul>
                </div>
              </div>
            </Carousel.Item>
          </Carousel>
        </Container>
      </section>

      {/* ====================================================== facilities == */}
      <section className="section section-alt" id="facilities">
        <Container>
          <div className="section-head">
            <span className="section-label">Facilities</span>
            <h2>What's included</h2>
            <p>No resort fees, and no charge for anything on this list. It comes with the room.</p>
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
      <section className="section" id="contact">
        <Container>
          <div className="section-head">
            <span className="section-label">Getting in touch</span>
            <h2>However you would rather ask</h2>
          </div>

          <div className="auto-grid" style={{ '--min': '18rem' }}>
            <div className="contact-card">
              <dl className="mb-0">
                <dt>Reservations</dt>
                <dd>Book, change or cancel from your account at any hour — no queue and no call.</dd>
                <dt>Front desk</dt>
                <dd>Staffed around the clock. Raise a request from your room and it reaches the right team directly.</dd>
                <dt>Housekeeping &amp; maintenance</dt>
                <dd>Ask from your account, and you will get a notification the moment it is done.</dd>
              </dl>
            </div>

            <div className="contact-card">
              <dl className="mb-0">
                <dt>Your stay, in one place</dt>
                <dd>Your folio updates as charges land, so you can see the running total before you check out.</dd>
                <dt>Dining</dt>
                <dd>Book any of the four outlets from your account, and cancel just as easily.</dd>
                <dt>Getting here</dt>
                <dd>Airport transfers and taxis are arranged at the desk. Valet parking is on site.</dd>
              </dl>
            </div>
          </div>
        </Container>
      </section>

      {/* ===================================================== closing cta == */}
      <Container>
        <section className="section pt-0">
          <div className="closing-cta">
            <span className="section-label">{TAGLINE}</span>
            <h2>Come and stay with us</h2>
            <p>Pick your dates, and you will be booked inside a couple of minutes.</p>
            <BookCta size="lg" />
          </div>
        </section>
      </Container>

      {/* ========================================================== footer == */}
      <footer className="site-footer">
        <Container>
          <div className="footer-grid">
            <div className="footer-brand">
              <span className="brand-wordmark fs-5">
                Stay<span style={{ color: 'var(--sn-accent)' }}>Nest</span>
              </span>
              <p className="footer-tagline">{TAGLINE}</p>
              <p>
                A hotel and the platform that runs it — front desk, housekeeping, kitchens and
                billing on one system.
              </p>
            </div>

            <div className="footer-col">
              <h4>The hotel</h4>
              <a href="#rooms">Rooms &amp; suites</a>
              <a href="#dining">Dining</a>
              <a href="#facilities">Facilities</a>
            </div>

            <div className="footer-col">
              <h4>Good to know</h4>
              <a href="#about">About us</a>
              <a href="#about">Policies &amp; house rules</a>
              <a href="#contact">Getting in touch</a>
            </div>

            <div className="footer-col">
              <h4>Your account</h4>
              {/* Guests and visitors get the booking flow; staff get their own console. */}
              {signedIn
                ? <Link to={homeFor(user?.role)}>My dashboard</Link>
                : <Link to="/login">Log in</Link>}
              {!signedIn && <Link to="/register">Register as a guest</Link>}
              {/* Staff reach the app the same way guests do. */}
              <Link to="/login">Staff sign-in</Link>
            </div>
          </div>

          <div className="footer-bottom">
            <span>&copy; {new Date().getFullYear()} StayNest</span>
            <span>Built as one system, end to end.</span>
          </div>
        </Container>
      </footer>
    </div>
  )
}
