import { api, unwrap } from '../api'
export const getReports = () => api.get('/api/reports').then(unwrap)
export const getReportById = (id) => api.get(`/api/reports/${id}`).then(unwrap)
export const generateReport = (data) => api.post('/api/reports', data).then(unwrap)
export const getSummary = () => api.get('/api/reports/summary').then(unwrap)