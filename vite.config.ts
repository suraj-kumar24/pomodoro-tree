import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  // Capacitor serves the bundle from the app's local origin, so asset URLs stay relative.
  base: './',
  build: { outDir: 'dist', target: 'es2020' },
  test: { environment: 'node' },
});
