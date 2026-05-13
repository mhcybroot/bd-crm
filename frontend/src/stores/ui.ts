import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUiStore = defineStore('ui', () => {
  const loading = ref(false)
  const snackbar = ref({
    open: false,
    color: 'success',
    message: '',
  })

  function showSuccess(message: string) {
    snackbar.value = { open: true, color: 'success', message }
  }

  function showError(message: string) {
    snackbar.value = { open: true, color: 'error', message }
  }

  return { loading, snackbar, showSuccess, showError }
})
