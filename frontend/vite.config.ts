import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Port 3000 is required: Keycloak redirect URI, gateway CORS and PayOS return URLs all point to http://localhost:3000
export default defineConfig({
  plugins: [react()],
  server: { port: 3000, strictPort: true },
  preview: { port: 3000, strictPort: true },
});
