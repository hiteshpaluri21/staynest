export const roleBadge = (role) => {
  const map = {
    ADMIN: 'primary', FRONTDESK: 'info', HOUSEKEEPING: 'success',
    FBMANAGER: 'warning', GUEST: 'secondary',
  }
  return map[role] || 'secondary'
}

export const statusBadge = (status) => {
  if (!status) return 'secondary'
  const s = String(status).toUpperCase()
  if (['ACTIVE', 'CONFIRMED', 'AVAILABLE', 'DONE', 'RESOLVED', 'READ', 'BILLED'].includes(s)) return 'success'
  if (['INPROGRESS', 'PREPARING', 'SERVED', 'CLEANING'].includes(s)) return 'warning'
  if (['CANCELLED', 'NOSHOW', 'BLACKLISTED', 'BLOCKED', 'INACTIVE'].includes(s)) return 'danger'
  if (['PENDING', 'OPEN', 'UNREAD', 'PLACED'].includes(s)) return 'info'
  if (['MAINTENANCE', 'CHECKEDIN'].includes(s)) return 'primary'
  return 'secondary'
}

/*
 * Colour for an audit-trail action. Actions are free-form strings written by each
 * service's AuditRecorder (CREATE, UPDATE_STATUS, POST_CHARGE, …), so this matches
 * on the leading verb rather than listing every combination.
 */
export const actionBadge = (action) => {
  if (!action) return 'secondary'
  const a = String(action).toUpperCase()
  if (a.startsWith('CREATE')) return 'success'
  if (a.startsWith('DELETE') || a.startsWith('SOFT_DELETE') || a === 'CANCEL' || a === 'BLACKLIST') return 'danger'
  if (a.startsWith('UPDATE')) return 'info'
  if (a === 'CHECKIN' || a === 'CHECKOUT' || a === 'ASSIGN') return 'primary'
  // LOGIN, MARK_READ and friends are routine traffic — they fall through to grey.
  if (a === 'POST_CHARGE') return 'warning'
  return 'secondary'
}

export const loyaltyBadge = (tier) => {
  const map = { NONE: 'secondary', SILVER: 'light', GOLD: 'warning', PLATINUM: 'purple' }
  return map[tier] || 'secondary'
}