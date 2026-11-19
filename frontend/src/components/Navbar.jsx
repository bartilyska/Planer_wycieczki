import { Compass, CalendarDays, FlaskConical } from 'lucide-react'

export default function Navbar({ demo, onHome, onCreate }) {
  return (
    <header className="site-header">
      <div className="shell nav">
        <a className="brand" href="#" onClick={(event) => { event.preventDefault(); onHome() }}>
          <span className="brand-icon"><Compass size={23} strokeWidth={2.1} aria-hidden="true" /></span>
          Trip<span>Planner</span>
        </a>
        <nav aria-label="Główna nawigacja">
          {demo && <span className="demo-chip"><FlaskConical size={15} /> <span>Tryb demo</span></span>}
          <button className="button nav-action" onClick={onCreate}><CalendarDays size={17} /> <span>Zaplanuj dzień</span></button>
        </nav>
      </div>
    </header>
  )
}
