import { createVuetify } from 'vuetify'

const vuetify = createVuetify({
  theme: {
    defaultTheme: 'crmLight',
    themes: {
      crmLight: {
        dark: false,
        colors: {
          primary: '#0f766e',
          secondary: '#0f172a',
          accent: '#ea580c',
          background: '#f6f1e7',
          surface: '#fffdf8',
          success: '#15803d',
          warning: '#d97706',
          error: '#dc2626',
          info: '#0f766e',
        },
      },
    },
  },
  defaults: {
    VCard: {
      rounded: 'xl',
      elevation: 0,
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
      rounded: 'xl',
      style: 'text-transform:none;font-weight:700;letter-spacing:0.01em;',
    },
    VChip: {
      rounded: 'xl',
    },
    VDialog: {
      scrim: '#0f172acc',
    },
    VDataTable: {
      density: 'comfortable',
    },
  },
})

export default vuetify
