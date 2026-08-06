import { createContext, useContext, useEffect, useState } from 'react'

const ThemeContext = createContext(null)

export const THEME_KEY = 'staynest-theme'

/**
 * Bootstrap 5.3 themes off a `data-bs-theme` attribute, so switching is just a matter of setting it
 * on <html> — every Bootstrap component follows automatically. Our own styles read the same
 * attribute in index.css.
 *
 * The initial value is applied by an inline script in index.html, before first paint, so a dark-mode
 * user doesn't get a white flash on load. This provider has to agree with that script on both the
 * storage key and the fallback.
 */
export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(() => {
    const stored = localStorage.getItem(THEME_KEY)
    if (stored === 'light' || stored === 'dark') return stored
    // No explicit choice yet — follow the OS.
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  })

  useEffect(() => {
    document.documentElement.setAttribute('data-bs-theme', theme)
    localStorage.setItem(THEME_KEY, theme)
  }, [theme])

  // Keep following the OS until the user picks a side themselves.
  useEffect(() => {
    if (localStorage.getItem(THEME_KEY)) return
    const mq = window.matchMedia?.('(prefers-color-scheme: dark)')
    if (!mq) return
    const onChange = (e) => setTheme(e.matches ? 'dark' : 'light')
    mq.addEventListener('change', onChange)
    return () => mq.removeEventListener('change', onChange)
  }, [])

  const toggleTheme = () => setTheme(t => (t === 'dark' ? 'light' : 'dark'))

  return (
    <ThemeContext.Provider value={{ theme, setTheme, toggleTheme, isDark: theme === 'dark' }}>
      {children}
    </ThemeContext.Provider>
  )
}

export const useTheme = () => useContext(ThemeContext)
