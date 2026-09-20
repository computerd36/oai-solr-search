import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    // proxy to the spring app, that way the browser sees one origin and CORS never comes up
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
