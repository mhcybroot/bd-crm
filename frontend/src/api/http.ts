import axios from 'axios'
import router from '@/router'
import type { ApiErrorResponse } from '@/types/api'
import { getStoredToken, setStoredToken, setStoredUser } from '@/utils/storage'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
})

http.interceptors.request.use((config) => {
  const token = getStoredToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      setStoredToken(null)
      setStoredUser(null)
      if (router.currentRoute.value.path !== '/login') {
        await router.push('/login')
      }
    }

    const data = error.response?.data as ApiErrorResponse | undefined
    const message = data?.details?.length ? data.details.join(', ') : data?.message || error.message
    return Promise.reject(new Error(message))
  },
)
