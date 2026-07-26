export const roleBadge = (role) => {
  const map = {
    ADMIN: 'primary', FRONTDESK: 'info', HOUSEKEEPING: 'success',
    FBMANAGER: 'warning', REVENUEMANAGER: 'danger', GUEST: 'secondary',
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

export const loyaltyBadge = (tier) => {
  const map = { NONE: 'secondary', SILVER: 'light', GOLD: 'warning', PLATINUM: 'purple' }
  return map[tier] || 'secondary'
}