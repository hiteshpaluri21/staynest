// Accepted guest ID documents. Shared so the profile editor and the booking-time
// "complete your profile" dialog can never drift apart.
export const ID_DOCUMENT_TYPES = ['PASSPORT', 'AADHAAR', 'DRIVING_LICENSE', 'NATIONAL_ID', 'VOTER_ID']

/** "DRIVING_LICENSE" -> "DRIVING LICENSE" for display. */
export const idTypeLabel = (type) => String(type ?? '').replace(/_/g, ' ')
