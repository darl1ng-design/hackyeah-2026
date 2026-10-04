import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Dev: proxy API + Spring login/logout to the backend so cookies stay same-origin (no CORS).
const backend = process.env.BACKEND_URL ?? 'http://localhost:8083';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': backend,
      '/login': backend,
      '/logout': backend,
    },
  },
});
