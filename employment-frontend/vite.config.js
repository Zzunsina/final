import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    host: 'localhost',
    port: 80,
    strictPort: true,
    proxy: { '/api': process.env.BACKEND_URL || 'http://localhost:8080' },
  },
});
