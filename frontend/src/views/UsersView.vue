<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import * as userApi from '@/api/users'
import { useUiStore } from '@/stores/ui'
import type { RoleName, UserCreateRequest, UserResponse } from '@/types/api'

const uiStore = useUiStore()
const users = ref<UserResponse[]>([])
const dialog = ref(false)
const form = reactive<UserCreateRequest>({
  username: '',
  password: '',
  fullName: '',
  email: '',
  managerId: null,
  roles: ['REP'],
})

async function load() {
  users.value = await userApi.listUsers()
}

async function submit() {
  try {
    await userApi.createUser(form)
    dialog.value = false
    uiStore.showSuccess('User created')
    await load()
  } catch (error) {
    uiStore.showError(error instanceof Error ? error.message : 'Unable to create user')
  }
}

async function toggleStatus(user: UserResponse) {
  try {
    await userApi.updateUserStatus(user.id, { active: !user.active })
    await load()
  } catch (error) {
    uiStore.showError(error instanceof Error ? error.message : 'Unable to update status')
  }
}

async function updateRoles(user: UserResponse, roles: RoleName[]) {
  try {
    await userApi.updateUserRoles(user.id, { roles })
    await load()
  } catch (error) {
    uiStore.showError(error instanceof Error ? error.message : 'Unable to update roles')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">User Administration</h1>
        <p class="page-subtitle">Create internal accounts, assign roles, and control access.</p>
      </div>
      <v-btn color="primary" prepend-icon="mdi-account-plus-outline" @click="dialog = true">New user</v-btn>
    </div>

    <v-card>
      <v-data-table :items="users">
        <template #headers>
          <tr>
            <th class="text-left">Name</th>
            <th class="text-left">Username</th>
            <th class="text-left">Email</th>
            <th class="text-left">Roles</th>
            <th class="text-left">Status</th>
            <th class="text-left">Actions</th>
          </tr>
        </template>
        <template #item="{ item }">
          <tr>
            <td>{{ item.fullName }}</td>
            <td>{{ item.username }}</td>
            <td>{{ item.email }}</td>
            <td style="min-width: 240px;">
              <v-select
                :model-value="item.roles"
                :items="['ADMIN', 'MANAGER', 'REP']"
                multiple
                chips
                hide-details
                @update:model-value="updateRoles(item, $event)"
              />
            </td>
            <td>{{ item.active ? 'Active' : 'Inactive' }}</td>
            <td>
              <v-btn size="small" variant="tonal" @click="toggleStatus(item)">
                {{ item.active ? 'Deactivate' : 'Activate' }}
              </v-btn>
            </td>
          </tr>
        </template>
      </v-data-table>
    </v-card>

    <v-dialog v-model="dialog" max-width="640">
      <v-card>
        <v-card-title>Create user</v-card-title>
        <v-card-text>
          <v-row>
            <v-col cols="12" md="6"><v-text-field v-model="form.fullName" label="Full name" /></v-col>
            <v-col cols="12" md="6"><v-text-field v-model="form.username" label="Username" /></v-col>
            <v-col cols="12" md="6"><v-text-field v-model="form.email" label="Email" /></v-col>
            <v-col cols="12" md="6"><v-text-field v-model="form.password" type="password" label="Password" /></v-col>
            <v-col cols="12"><v-select v-model="form.roles" label="Roles" :items="['ADMIN', 'MANAGER', 'REP']" chips multiple /></v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialog = false">Cancel</v-btn>
          <v-btn color="primary" @click="submit">Create</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>
