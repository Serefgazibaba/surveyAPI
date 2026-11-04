import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// Port 3000: backend'in varsayılan FRONTEND_BASE_URL (davet linkleri) ve CORS ayarıyla uyumlu.
// /api istekleri backend'e proxy'lenir, böylece geliştirmede CORS gerekmez.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [react()],
    server: {
      port: 3000,
      strictPort: true,
      proxy: {
        '/api': {
          target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
          // İstek tarayıcı açısından aynı origin'e gidiyor; Origin başlığı iletilirse backend
          // CORS listesinde olmayan portlar (örn. 3001) için 403 "Invalid CORS request" döner.
          configure: proxy => {
            proxy.on('proxyReq', proxyReq => proxyReq.removeHeader('origin'))
          },
        },
      },
    },
  }
})
