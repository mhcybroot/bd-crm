<template>
  <v-container class="fill-height">
    <v-row align="center" justify="center">
      <v-col cols="12" md="5">
        <v-card class="pa-8 text-center">
          <v-icon icon="mdi-shield-alert-outline" color="warning" size="56" class="mb-4" />
          <h1 class="text-h4 mb-2">Access restricted</h1>
          <p class="text-medium-emphasis mb-6">
            Your account does not have permission to open this section.
          </p>
          <v-btn color="primary" to="/dashboard">Back to dashboard</v-btn>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
