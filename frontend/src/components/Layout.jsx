import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import Navbar from './Navbar'
import Sidebar from './Sidebar'

/** True when the viewport is wide enough for the sidebar to sit beside the content. */
const isWide = () => window.matchMedia('(min-width: 992px)').matches

/**
 * Holds the sidebar's open/closed state, which the burger button in Navbar drives.
 *
 * The sidebar behaves differently by width, and the CSS handles most of it:
 *   wide   — docked beside the content, open by default, burger collapses it
 *   narrow — slides in over the content with a backdrop, closed by default
 */
export default function Layout({ children }) {
  const [open, setOpen] = useState(isWide)
  const location = useLocation()

  // On narrow screens the sidebar covers the page, so navigating should close it.
  // A docked sidebar on a wide screen stays as the user left it.
  useEffect(() => {
    if (!isWide()) setOpen(false)
  }, [location.pathname])

  // Escape closes the overlay, which is the usual expectation for one.
  useEffect(() => {
    const onKeyDown = (e) => {
      if (e.key === 'Escape' && !isWide()) setOpen(false)
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [])

  // Returning to a wide viewport should reveal the sidebar again, otherwise it
  // stays hidden with no obvious way back other than the burger.
  useEffect(() => {
    const mq = window.matchMedia('(min-width: 992px)')
    const onChange = (e) => setOpen(e.matches)
    mq.addEventListener('change', onChange)
    return () => mq.removeEventListener('change', onChange)
  }, [])

  return (
    <div className={`app-layout ${open ? 'sidebar-open' : 'sidebar-closed'}`}>
      <Sidebar />

      {/* Dismisses the overlay. Only visible on narrow screens — see index.css. */}
      <div className="sidebar-backdrop" onClick={() => setOpen(false)} aria-hidden="true" />

      <div className="main-content">
        <Navbar onToggleSidebar={() => setOpen(o => !o)} />
        <div className="page-body">{children}</div>
      </div>
    </div>
  )
}
