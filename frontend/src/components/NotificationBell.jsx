import { useEffect, useState } from 'react'
import { FaBell } from 'react-icons/fa'
import { useNavigate } from 'react-router-dom'
import { getUnreadCount } from '../services/nal/notificationService'
import { useAuth } from '../context/AuthContext'

export default function NotificationBell() {
  const { user } = useAuth()
  const [count, setCount] = useState(0)
  const navigate = useNavigate()

  useEffect(() => {
    if (!user?.userId) return
    let active = true
    const tick = async () => {
      try {
        const res = await getUnreadCount(user.userId)
        if (active) setCount(res?.unreadCount || 0)
      } catch { /* ignore */ }
    }
    tick()
    const id = setInterval(tick, 30000)
    return () => { active = false; clearInterval(id) }
  }, [user?.userId])

  return (
    // The unread pill sits half outside the icon, so the box needs padding of its own
    // rather than a margin — a margin let the pill overlap the neighbouring control.
    <div className="notification-bell" onClick={() => navigate('/notifications')} title="Notifications">
      <FaBell size={20} />
      {count > 0 && (
        <span className="notification-bell-count badge rounded-pill bg-danger">
          {count > 9 ? '9+' : count}
        </span>
      )}
    </div>
  )
}