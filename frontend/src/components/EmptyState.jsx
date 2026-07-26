export default function EmptyState({ message = 'No records found' }) {
    return <div className="text-center text-muted py-5"><p>{message}</p></div>
}