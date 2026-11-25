import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';
export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'prompt',
      includeAssets: ['icon.svg'],
      manifest: {
        name: 'LMX · GMAO clinique',
        short_name: 'LMX',
        description: 'Gestion de maintenance technique de la polyclinique',
        lang: 'fr',
        theme_color: '#087f8c',
        background_color: '#f3f7f9',
        display: 'standalone',
        start_url: '/',
        icons: [{ src: '/icon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'any' }],
      },
      workbox: {
        navigateFallback: '/index.html',
        navigateFallbackDenylist: [/^\/api\//],
        globPatterns: ['**/*.{js,css,html,svg,woff2}'],
        maximumFileSizeToCacheInBytes: 4 * 1024 * 1024,
      },
    }),
  ],
  server: {
    port: 3000,
    proxy: {
      '/api': { target: 'http://127.0.0.1:8080' },
      '/api-docs': { target: 'http://127.0.0.1:8080' },
    },
  },
  build: { chunkSizeWarningLimit: 1200 },
});
