import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],

  // ── Dev server ─────────────────────────────────────────────────────────────
  server: {
    port: 5173,
    strictPort: true,

    /**
     * Proxy /api/* to the Spring Boot backend.
     *
     * Why: in development the frontend runs on :5173 and Spring on :8080.
     * Without a proxy, every fetch to /api/speech/history would need
     * http://localhost:8080/api/... hardcoded.
     *
     * With the proxy, the frontend just calls /api/speech/history and
     * Vite forwards it to Spring. No CORS headers needed in dev.
     *
     * In production (Vercel / Nginx), configure the same proxy at the
     * web server level.
     */
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      }
    }
  },

  // ── Build ──────────────────────────────────────────────────────────────────
  build: {
    outDir: 'dist',
    sourcemap: true,
  },

  // ── Test (Vitest) ──────────────────────────────────────────────────────────
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.js'],
  },

  // ── Path aliases ───────────────────────────────────────────────────────────
  resolve: {
    alias: {
      '@': '/src',
      '@components': '/src/components',
      '@pages':      '/src/pages',
      '@services':   '/src/services',
      '@hooks':      '/src/hooks',
      '@context':    '/src/context',
      '@utils':      '/src/utils',
      '@assets':     '/src/assets',
    }
  }
})