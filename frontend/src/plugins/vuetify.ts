import { createVuetify } from 'vuetify'

const vuetify = createVuetify({
  theme: {
    defaultTheme: 'crmLight',
    themes: {
      crmLight: {
        dark: false,
        colors: {
          primary: '#0f6c5c',
          secondary: '#1f2937',
          accent: '#d97706',
          background: '#f4f7f3',
          surface: '#ffffff',
          success: '#2e7d32',
          warning: '#ef6c00',
          error: '#c62828',
          info: '#1565c0',
        },
      },
    },
  },
  defaults: {
    VCard: {
      rounded: 'lg',
      elevation: 1,
    },
    VTextField: {
      variant: 'outlined',
      density: 'comfortable',
    },
    VSelect: {
      variant: 'outlined',
      density: 'comfortable',
    },
    VTextarea: {
      variant: 'outlined',
      density: 'comfortable',
    },
    VBtn: {
      rounded: 'lg',
    },
  },
})

export default vuetify
