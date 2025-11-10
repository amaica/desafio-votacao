import axios from 'axios'
export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080', timeout: 8000 })
api.interceptors.response.use(r=>r,(err)=>{
  const s=err?.response?.status
  if (s===404) err.userMessage='Recurso não encontrado.'
  if (s===403) err.userMessage='Ação não permitida.'
  if (s===409) err.userMessage='Operação conflitante (já existe).'
  if (s===422) err.userMessage='Requisição inválida para o estado atual.'
  return Promise.reject(err)
})
