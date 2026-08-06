/*
 * Where each role belongs after signing in, and who is allowed to book.
 *
 * This lived inline in LoginPage, which meant the public site had no way to know
 * it and sent every signed-in user to /book. The /book route only admits GUEST, so
 * staff accounts were bounced to /unauthorized. Both places now read from here.
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
 * the roles on the /book route in App.jsx — which is guests only, as an admin
 * administers the hotel rather than booking a stay in it.
 */
export const canBook = (role) => role === 'GUEST'

/**
 * Whether this role can reach the guest profile page. Must stay in step with the roles
 * on the /profile route in App.jsx — housekeeping and F&B are not admitted, so the
 * account menus must not offer them the link.
 */
export const canViewProfile = (role) => ['GUEST', 'FRONTDESK', 'ADMIN'].includes(role)
