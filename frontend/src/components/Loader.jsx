import { Spinner } from 'react-bootstrap'
export default function Loader() {
    return <div className="text-center py-5"><Spinner animation="border" style={{ color: '#1e3a5f' }} /></div>
}