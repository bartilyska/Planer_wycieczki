import { AlertCircle, SearchX } from 'lucide-react'

export function ErrorMessage({ children, retry }) {
  return (
    <div className="notice error" role="alert">
      <AlertCircle size={19} aria-hidden="true" />
      <div>{children}{retry && <button type="button" className="text-button" onClick={retry}>Spróbuj ponownie</button>}</div>
    </div>
  )
}

export function LoadingState({ label, count = 3 }) {
  return (
    <div className="skeleton-list" role="status" aria-label={label}>
      <span className="sr-only">{label}</span>
      {Array.from({ length: count }, (_, index) => <div className="skeleton" key={index} aria-hidden="true" />)}
    </div>
  )
}

export function EmptyState({ children }) {
  return <div className="empty"><SearchX size={24} aria-hidden="true" /><p>{children}</p></div>
}
