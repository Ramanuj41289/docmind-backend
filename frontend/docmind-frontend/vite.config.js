import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes) => {
            // Remove WWW-Authenticate header to prevent browser from showing native basic auth dialog
            delete proxyRes.headers['www-authenticate'];
          });
        },
      },
    },
  },
  preview: {
    port: 3000,
    host: '0.0.0.0',
  },
});
