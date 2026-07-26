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
    <div className="position-relative me-3" style={{ cursor: 'pointer' }} onClick={() => navigate('/notifications')}>
      <FaBell size={20} />
      {count > 0 && (
        <span className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style={{ fontSize: '.65rem' }}>
          {count > 9 ? '9+' : count}
        </span>
      )}
    </div>
  )
}