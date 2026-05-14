<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import * as organizationApi from '@/api/organizations'
import { useUiStore } from '@/stores/ui'
import type { OrganizationBootstrapRequest, OrganizationRequest, OrganizationResponse } from '@/types/api'

const uiStore = useUiStore()
const organizations = ref<OrganizationResponse[]>([])
const createDialog = ref(false)
const editDialog = ref(false)
const editingOrganizationId = ref<number | null>(null)

const bootstrapForm = reactive<OrganizationBootstrapRequest>({
  organization: {
    slug: '',
    name: '',
    status: 'ACTIVE',
    timezone: 'UTC',
    locale: 'en-US',
    contactEmail: '',
    planCode: 'standard',
    dataRetentionDays: 365,
  },
  adminUser: {
    username: '',
    password: '',
    fullName: '',
    email: '',
  },
})

const editForm = reactive<OrganizationRequest>({
  slug: '',
  name: '',
  status: 'ACTIVE',
  timezone: 'UTC',
  locale: 'en-US',
  contactEmail: '',
  planCode: 'standard',
  dataRetentionDays: 365,
})

const statusOptions = ['ACTIVE', 'SUSPENDED', 'ARCHIVED'] as const
const canSubmitBootstrap = computed(() =>
  !!bootstrapForm.organization.slug.trim()
  && !!bootstrapForm.organization.name.trim()
  && !!bootstrapForm.organization.contactEmail.trim()
  && !!bootstrapForm.adminUser.username.trim()
  && !!bootstrapForm.adminUser.password.trim()
  && !!bootstrapForm.adminUser.fullName.trim()
  && !!bootstrapForm.adminUser.email.trim(),
)

async function load() {
  organizations.value = await organizationApi.listOrganizations()
}

function resetBootstrapForm() {
  bootstrapForm.organization.slug = ''
  bootstrapForm.organization.name = ''
  bootstrapForm.organization.status = 'ACTIVE'
  bootstrapForm.organization.timezone = 'UTC'
  bootstrapForm.organization.locale = 'en-US'
  bootstrapForm.organization.contactEmail = ''
  bootstrapForm.organization.planCode = 'standard'
  bootstrapForm.organization.dataRetentionDays = 365
  bootstrapForm.adminUser.username = ''
  bootstrapForm.adminUser.password = ''
  bootstrapForm.adminUser.fullName = ''
  bootstrapForm.adminUser.email = ''
}

async function submitBootstrap() {
  try {
    const response = await organizationApi.bootstrapOrganization(bootstrapForm)
    createDialog.value = false
    resetBootstrapForm()
    uiStore.showSuccess(`Created ${response.organization.name} and ${response.firstAdminUser.username}`)
    await load()
  } catch (error) {
    uiStore.showError(error instanceof Error ? error.message : 'Unable to create organization')
  }
}

function openEdit(organization: OrganizationResponse) {
  editingOrganizationId.value = organization.id
  editForm.slug = organization.slug
  editForm.name = organization.name
  editForm.status = organization.status
  editForm.timezone = organization.timezone
  editForm.locale = organization.locale
  editForm.contactEmail = organization.contactEmail
  editForm.planCode = organization.planCode
  editForm.dataRetentionDays = organization.dataRetentionDays
  editDialog.value = true
}

async function saveEdit() {
  if (editingOrganizationId.value == null) return
  try {
    await organizationApi.updateOrganization(editingOrganizationId.value, editForm)
    editDialog.value = false
    editingOrganizationId.value = null
    uiStore.showSuccess('Organization updated')
    await load()
  } catch (error) {
    uiStore.showError(error instanceof Error ? error.message : 'Unable to update organization')
  }
}

async function toggleOrganizationStatus(organization: OrganizationResponse) {
  const nextStatus = organization.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'
  try {
    await organizationApi.updateOrganization(organization.id, {
      slug: organization.slug,
      name: organization.name,
      status: nextStatus,
      timezone: organization.timezone,
      locale: organization.locale,
      contactEmail: organization.contactEmail,
      planCode: organization.planCode,
      dataRetentionDays: organization.dataRetentionDays,
    })
    uiStore.showSuccess(nextStatus === 'SUSPENDED'
      ? `${organization.name} temporarily blocked`
      : `${organization.name} reactivated`)
    await load()
  } catch (error) {
    uiStore.showError(error instanceof Error ? error.message : 'Unable to update organization status')
  }
}

void load()
</script>

<template>
  <div class="page-shell">
    <div class="page-header page-hero">
      <div>
        <h1 class="page-title">Organizations</h1>
        <p class="page-subtitle">Create companies, seed their first admin, and manage lifecycle status.</p>
      </div>
      <v-btn color="primary" prepend-icon="mdi-domain-plus" data-testid="open-create-organization" @click="createDialog = true">New organization</v-btn>
    </div>

    <v-card class="crm-card">
      <v-data-table :items="organizations">
        <template #headers>
          <tr>
            <th class="text-left">Name</th>
            <th class="text-left">Slug</th>
            <th class="text-left">Status</th>
            <th class="text-left">Timezone</th>
            <th class="text-left">Plan</th>
            <th class="text-left">Contact</th>
            <th class="text-left">Actions</th>
          </tr>
        </template>
        <template #item="{ item }">
          <tr class="crm-table-row">
            <td>{{ item.name }}</td>
            <td>{{ item.slug }}</td>
            <td><v-chip size="small" variant="tonal" :color="item.status === 'ACTIVE' ? 'success' : item.status === 'SUSPENDED' ? 'warning' : 'secondary'">{{ item.status }}</v-chip></td>
            <td>{{ item.timezone }}</td>
            <td>{{ item.planCode }}</td>
            <td>{{ item.contactEmail }}</td>
            <td>
              <v-btn
                size="small"
                class="mr-2"
                :color="item.status === 'ACTIVE' ? 'warning' : 'success'"
                variant="tonal"
                :data-testid="`toggle-organization-status-${item.id}`"
                @click="toggleOrganizationStatus(item)"
              >
                {{ item.status === 'ACTIVE' ? 'Block' : 'Activate' }}
              </v-btn>
              <v-btn size="small" variant="tonal" @click="openEdit(item)">Edit</v-btn>
            </td>
          </tr>
        </template>
      </v-data-table>
    </v-card>

    <v-dialog v-model="createDialog" max-width="880">
      <v-card class="glass-dialog-card">
        <v-card-title>Create organization</v-card-title>
        <v-card-text>
          <div class="admin-note mb-4">Bootstrap a new company and its first organization admin in one controlled setup flow.</div>
          <v-row>
            <v-col cols="12"><div class="text-subtitle-2">Organization</div></v-col>
            <v-col cols="12" md="6"><v-text-field v-model="bootstrapForm.organization.name" label="Company name" data-testid="bootstrap-org-name" /></v-col>
            <v-col cols="12" md="6"><v-text-field v-model="bootstrapForm.organization.slug" label="Slug" data-testid="bootstrap-org-slug" /></v-col>
            <v-col cols="12" md="4"><v-select v-model="bootstrapForm.organization.status" label="Status" :items="statusOptions" /></v-col>
            <v-col cols="12" md="4"><v-text-field v-model="bootstrapForm.organization.timezone" label="Timezone" /></v-col>
            <v-col cols="12" md="4"><v-text-field v-model="bootstrapForm.organization.locale" label="Locale" /></v-col>
            <v-col cols="12" md="6"><v-text-field v-model="bootstrapForm.organization.contactEmail" label="Contact email" data-testid="bootstrap-org-contact-email" /></v-col>
            <v-col cols="12" md="3"><v-text-field v-model="bootstrapForm.organization.planCode" label="Plan code" /></v-col>
            <v-col cols="12" md="3"><v-text-field v-model.number="bootstrapForm.organization.dataRetentionDays" type="number" label="Retention days" /></v-col>

            <v-col cols="12"><div class="text-subtitle-2 mt-2">First organization admin</div></v-col>
            <v-col cols="12" md="6"><v-text-field v-model="bootstrapForm.adminUser.fullName" label="Full name" data-testid="bootstrap-admin-full-name" /></v-col>
            <v-col cols="12" md="6"><v-text-field v-model="bootstrapForm.adminUser.username" label="Username" data-testid="bootstrap-admin-username" /></v-col>
            <v-col cols="12" md="6"><v-text-field v-model="bootstrapForm.adminUser.email" label="Email" data-testid="bootstrap-admin-email" /></v-col>
            <v-col cols="12" md="6"><v-text-field v-model="bootstrapForm.adminUser.password" type="password" label="Password" data-testid="bootstrap-admin-password" /></v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="createDialog = false">Cancel</v-btn>
          <v-btn color="primary" data-testid="submit-bootstrap-organization" :disabled="!canSubmitBootstrap" @click="submitBootstrap">Create company</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="editDialog" max-width="760">
      <v-card class="glass-dialog-card">
        <v-card-title>Edit organization</v-card-title>
        <v-card-text>
          <v-row>
            <v-col cols="12" md="6"><v-text-field v-model="editForm.name" label="Company name" /></v-col>
            <v-col cols="12" md="6"><v-text-field v-model="editForm.slug" label="Slug" /></v-col>
            <v-col cols="12" md="4"><v-select v-model="editForm.status" label="Status" :items="statusOptions" /></v-col>
            <v-col cols="12" md="4"><v-text-field v-model="editForm.timezone" label="Timezone" /></v-col>
            <v-col cols="12" md="4"><v-text-field v-model="editForm.locale" label="Locale" /></v-col>
            <v-col cols="12" md="6"><v-text-field v-model="editForm.contactEmail" label="Contact email" /></v-col>
            <v-col cols="12" md="3"><v-text-field v-model="editForm.planCode" label="Plan code" /></v-col>
            <v-col cols="12" md="3"><v-text-field v-model.number="editForm.dataRetentionDays" type="number" label="Retention days" /></v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="editDialog = false">Cancel</v-btn>
          <v-btn color="primary" @click="saveEdit">Save</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>
