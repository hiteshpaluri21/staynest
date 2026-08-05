// Field validators shared by the registration / user / guest-profile forms.
// Each returns an error message string, or '' when the value is valid — so a form can do:
//   const errors = { email: validateEmail(form.email), phone: validatePhone(form.phone) }
// These mirror the backend constraints in ValidationPatterns so a valid form never gets
// bounced by the server (and vice versa).

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[A-Za-z]{2,}$/

// Exactly 10 national digits, optionally preceded by a + country code of 1-3 digits.
const PHONE_RE = /^(?:\+\d{1,3})?\d{10}$/

/** Removes the separators people naturally type so they don't count as invalid characters. */
export const normalizePhone = (value) => String(value ?? '').replace(/[\s()\-.]/g, '')

export const validateEmail = (value) => {
  const email = String(value ?? '').trim()
  if (!email) return 'Email address is required'
  if (!EMAIL_RE.test(email)) return 'Enter a valid email address (e.g. john@example.com)'
  if (email.length > 150) return 'Email address must be 150 characters or fewer'
  return ''
}

export const validatePhone = (value) => {
  const raw = String(value ?? '').trim()
  if (!raw) return 'Phone number is required'

  const compact = normalizePhone(raw)
  if (!/^\+?\d+$/.test(compact)) {
    return 'Phone number can only contain digits, with an optional leading + country code'
  }
  if (!PHONE_RE.test(compact)) {
    if (compact.startsWith('+')) {
      return 'Enter exactly 10 digits after the country code (e.g. +91 9876543210)'
    }
    return `Phone number must be exactly 10 digits (you entered ${compact.length})`
  }
  return ''
}

export const validateName = (value) => (String(value ?? '').trim() ? '' : 'Full name is required')

// A guest must supply these before they can hold a reservation — front desk needs them at
// check-in. Name and email already come from registration; nationality stays optional.
export const REQUIRED_PROFILE_FIELDS = [
  { key: 'phone', label: 'Phone number' },
  { key: 'idDocumentType', label: 'ID document type' },
  { key: 'idNumber', label: 'ID number' },
]

export const missingProfileFields = (guest) =>
  REQUIRED_PROFILE_FIELDS.filter(f => !String(guest?.[f.key] ?? '').trim())

export const isProfileComplete = (guest) => Boolean(guest) && missingProfileFields(guest).length === 0

/** True when every entry of a validator result map is an empty string. */
export const isClean = (errors) => Object.values(errors).every(msg => !msg)
