<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as leadApi from '@/api/leads'
import * as templateApi from '@/api/templates'
import * as userApi from '@/api/users'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import type { FollowupTemplateResponse, LeadRequest, UserResponse } from '@/types/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUiStore()
const isEdit = computed(() => !!route.params.id)
const canManageAssignee = computed(() => authStore.hasRole('ADMIN', 'MANAGER'))
const loading = ref(false)
const users = ref<UserResponse[]>([])
const templates = ref<FollowupTemplateResponse[]>([])
const form = reactive<LeadRequest>({
  companyName: '',
  contactName: '',
  email: null,
  phone: null,
  source: null,
  description: null,
  priority: 'MEDIUM',
  assignedUserId: null,
  templateId: null,
})

async function loadReferenceData() {
  const [usersResult, templatesResult] = await Promise.allSettled([userApi.listUsers(), templateApi.listTemplates()])

  if (usersResult.status === 'fulfilled') {
    users.value = usersResult.value
  } else {
    users.value = authStore.user
      ? [{
          id: authStore.user.id,
          username: authStore.user.username,
          fullName: authStore.user.fullName,
          email: authStore.user.email,
          active: true,
          managerId: null,
          roles: authStore.user.roles,
        }]
      : []
  }

  if (templatesResult.status === 'fulfilled') {
    templates.value = templatesResult.value
  } else {
    templates.value = []
    uiStore.showError('Unable to load follow-up templates')
  }

  if (!form.assignedUserId) {
    form.assignedUserId = authStore.user?.id ?? users.value[0]?.id ?? null
  }
  if (!form.templateId) {
    form.templateId = templates.value[0]?.id ?? null
  }
}

async function loadLead() {
  if (!isEdit.value) return
  const data = await leadApi.getLead(Number(route.params.id))
  Object.assign(form, {
    companyName: data.lead.companyName,
    contactName: data.lead.contactName,
    email: data.lead.email,
    phone: data.lead.phone,
    source: data.lead.source,
    description: '',
    priority: data.lead.priority,
    assignedUserId: data.lead.assignedUserId,
    templateId: data.lead.templateId,
  })
}

async function submit() {
  loading.value = true
  try {
    const response = isEdit.value
      ? await leadApi.updateLead(Number(route.params.id), form)
      : await leadApi.createLead(form)
    uiStore.showSuccess(`Lead ${isEdit.value ? 'updated' : 'created'} successfully`)
    await router.push(`/leads/${response.lead.id}`)
  } catch (error) {
    uiStore.showError(error instanceof Error ? error.message : 'Unable to save lead')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadReferenceData()
  await loadLead()
})
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ isEdit ? 'Edit Lead' : 'New Lead' }}</h1>
        <p class="page-subtitle">Capture ownership, cadence, and contact details in one place.</p>
      </div>
    </div>

    <v-card>
      <v-card-text>
        <v-form @submit.prevent="submit">
          <v-row>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.companyName" label="Company name" required />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.contactName" label="Contact name" required />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.email" label="Email" />
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field v-model="form.phone" label="Phone" />
            </v-col>
            <v-col cols="12" md="4">
              <v-text-field v-model="form.source" label="Lead source" />
            </v-col>
            <v-col cols="12" md="4">
              <v-select v-model="form.priority" label="Priority" :items="['LOW', 'MEDIUM', 'HIGH']" />
            </v-col>
            <v-col cols="12" md="4">
              <v-select
                v-model="form.assignedUserId"
                label="Assignee"
                :items="users"
                item-title="fullName"
                item-value="id"
                :disabled="!canManageAssignee"
              />
            </v-col>
            <v-col cols="12">
              <v-select
                v-model="form.templateId"
                label="Follow-up template"
                :items="templates"
                item-title="name"
                item-value="id"
              />
            </v-col>
            <v-col cols="12">
              <v-textarea v-model="form.description" label="Description" rows="4" />
            </v-col>
          </v-row>
          <div class="d-flex justify-end ga-3">
            <v-btn variant="text" @click="router.push('/leads')">Cancel</v-btn>
            <v-btn color="primary" type="submit" :loading="loading">Save lead</v-btn>
          </div>
        </v-form>
      </v-card-text>
    </v-card>
  </div>
</template>
