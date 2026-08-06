/*
 * Where each role belongs after signing in, and who is allowed to book.
 *
 * This lived inline in LoginPage, which meant the public site had no way to know
 * it and sent every signed-in user to /book. The /book route only admits GUEST and
 * ADMIN, so front desk, housekeeping and F&B staff were bounced to /unauthorized.
 * Both places now read from here.
 */

const HOME_BY_ROLE = {
  ADMIN: '/users',
  FRONTDESK: '/front-desk',
  HOUSEKEEPING: '/housekeeping',
  FBMANAGER: '/orders',
  GUEST: '/book',
}

/** The landing page for a role. Falls back to the guest booking search. */
export const homeFor = (role) => HOME_BY_ROLE[role] || '/book'

/**
 * Whether this role can reach the booking search at all. Must stay in step with
 * the roles on the /book route in App.jsx.
 */
export const canBook = (role) => role === 'GUEST' || role === 'ADMIN'
