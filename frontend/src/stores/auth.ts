import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import type { AuthResponse, LoginRequest, RoleName } from '@/types/api'
import { getStoredToken, getStoredUser, setStoredToken, setStoredUser } from '@/utils/storage'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getStoredToken())
  const user = ref<AuthResponse | null>(getStoredUser())
  const initialized = ref(false)

  const isAuthenticated = computed(() => !!token.value && !!user.value)
  const roles = computed<RoleName[]>(() => user.value?.roles ?? [])

  function hasRole(...required: RoleName[]) {
    return required.some((role) => roles.value.includes(role))
  }

  async function bootstrap() {
    if (!token.value) {
      initialized.value = true
      return
    }

    try {
      user.value = await authApi.getCurrentUser()
      setStoredUser(user.value)
    } catch {
      logout()
    } finally {
      initialized.value = true
    }
  }

  async function login(payload: LoginRequest) {
    const session = await authApi.login(payload)
    token.value = session.token
    user.value = session.user
    setStoredToken(session.token)
    setStoredUser(session.user)
  }

  function logout() {
    token.value = null
    user.value = null
    setStoredToken(null)
    setStoredUser(null)
  }

  return { token, user, initialized, roles, isAuthenticated, hasRole, bootstrap, login, logout }
})
