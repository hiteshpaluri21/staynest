import { Button } from 'react-bootstrap'
import { FaMoon, FaSun } from 'react-icons/fa'
import { useTheme } from '../context/ThemeContext'

/** Sits in the top bar; icon shows the theme you'd switch *to*, which is the usual convention. */
export default function ThemeToggle() {
  const { isDark, toggleTheme } = useTheme()
  const label = isDark ? 'Switch to light theme' : 'Switch to dark theme'

  return (
    <Button
      variant="link"
      className="theme-toggle"
      onClick={toggleTheme}
      title={label}
      aria-label={label}
    >
      {isDark ? <FaSun size={16} /> : <FaMoon size={16} />}
    </Button>
  )
}
