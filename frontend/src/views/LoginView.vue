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
  <v-container fluid class="login-shell fill-height">
    <v-row align="center" class="fill-height">
      <v-col cols="12" lg="7" class="d-none d-lg-flex">
        <div class="login-hero">
          <div class="section-kicker text-white mb-3">Business Development CRM</div>
          <h1 class="login-title">A command center for momentum, not just record keeping.</h1>
          <p class="login-copy">
            Manage pipeline flow, follow-up discipline, team ownership, and company context in one polished workspace.
          </p>
          <div class="login-feature-grid crm-stagger">
            <div class="login-feature-card">
              <div class="metric-label text-white">Visibility</div>
              <div class="text-h5 font-weight-bold mt-2">Live pipeline clarity</div>
            </div>
            <div class="login-feature-card">
              <div class="metric-label text-white">Execution</div>
              <div class="text-h5 font-weight-bold mt-2">Follow-up rhythm that holds</div>
            </div>
            <div class="login-feature-card">
              <div class="metric-label text-white">Control</div>
              <div class="text-h5 font-weight-bold mt-2">Organization-aware operations</div>
            </div>
          </div>
        </div>
      </v-col>
      <v-col cols="12" md="8" lg="5" class="mx-auto">
        <v-card class="login-card crm-card">
          <div class="text-overline text-primary mb-2">BD CRM</div>
          <h1 class="section-heading login-card-title">Welcome back</h1>
          <p class="text-medium-emphasis mb-6">Sign in to review leads, unblock next steps, and move revenue forward.</p>
          <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }}</v-alert>
          <v-form @submit.prevent="submit">
            <v-text-field v-model="form.username" label="Username or email" prepend-inner-icon="mdi-account-outline" required />
            <v-text-field
              v-model="form.password"
              label="Password"
              type="password"
              prepend-inner-icon="mdi-lock-outline"
              required
            />
            <v-btn type="submit" color="primary" block size="x-large" :loading="loading">Enter workspace</v-btn>
          </v-form>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped>
.login-shell {
  padding: 24px;
}

.login-hero {
  min-height: 86vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 42px;
  border-radius: 36px;
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.9), rgba(15, 118, 110, 0.84)),
    radial-gradient(circle at top left, rgba(234, 88, 12, 0.28), transparent 30%);
  color: white;
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.26);
}

.login-title {
  max-width: 9ch;
  margin: 0;
  font-family: 'Fraunces', serif;
  font-size: clamp(3rem, 5vw, 5.4rem);
  line-height: 0.95;
}

.login-copy {
  max-width: 54ch;
  margin: 22px 0 34px;
  color: rgba(248, 250, 252, 0.82);
  font-size: 1.06rem;
}

.login-feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.login-feature-card {
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(14px);
}

.login-card {
  padding: 38px;
  max-width: 520px;
  margin: 0 auto;
}

.login-card-title {
  font-size: 2.1rem;
}

@media (max-width: 1264px) {
  .login-shell {
    padding: 18px;
  }
}
</style>
