<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const error = ref('')

async function submit() {
  loading.value = true
  error.value = ''
  try {
    await authStore.login(form)
    await router.push('/dashboard')
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Login failed'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <v-container class="fill-height">
    <v-row align="center" justify="center">
      <v-col cols="12" md="4">
        <v-card class="pa-8">
          <div class="text-overline text-primary mb-2">BD CRM</div>
          <h1 class="text-h4 mb-2">Welcome back</h1>
          <p class="text-medium-emphasis mb-6">Track leads, follow-ups, ownership, and team momentum.</p>
          <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>
          <v-form @submit.prevent="submit">
            <v-text-field v-model="form.username" label="Username" prepend-inner-icon="mdi-account-outline" required />
            <v-text-field
              v-model="form.password"
              label="Password"
              type="password"
              prepend-inner-icon="mdi-lock-outline"
              required
            />
            <v-btn type="submit" color="primary" block size="large" :loading="loading">Sign in</v-btn>
          </v-form>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
