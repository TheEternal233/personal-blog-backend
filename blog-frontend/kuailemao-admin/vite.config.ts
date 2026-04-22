/// <reference types="vitest" />
import { resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import * as process from 'node:process'
import { loadEnv } from 'vite'
import type { ConfigEnv, UserConfig } from 'vite'
import { createVitePlugins } from './plugins'
import { OUTPUT_DIR } from './plugins/constants'

const baseSrc = fileURLToPath(new URL('./src', import.meta.url))

export default ({ mode }: ConfigEnv): UserConfig => {
  const env = loadEnv(mode, process.cwd())

  const baseApi = env.VITE_APP_BASE_API || '/api'
  const baseUrl = env.VITE_APP_BASE_URL || 'http://localhost:8088'

  return {
    plugins: createVitePlugins(env),
    resolve: {
      alias: [
        { find: 'dayjs', replacement: 'dayjs/esm' },
        { find: /^dayjs\/locale/, replacement: 'dayjs/esm/locale' },
        { find: /^dayjs\/plugin/, replacement: 'dayjs/esm/plugin' },
        {
          find: 'vue-i18n',
          replacement: mode === 'development'
            ? 'vue-i18n/dist/vue-i18n.esm-browser.js'
            : 'vue-i18n/dist/vue-i18n.esm-bundler.js',
        },
        { find: /^ant-design-vue$/, replacement: 'ant-design-vue/es' },
        { find: /^ant-design-vue\/(es|lib)(\/.*)?$/, replacement: 'ant-design-vue/es/$2' },
        { find: 'lodash', replacement: 'lodash-es' },
        { find: '~@', replacement: baseSrc },
        { find: '~', replacement: baseSrc },
        { find: '@', replacement: baseSrc },
        { find: '~#', replacement: resolve(baseSrc, './enums') },
      ],
    },
    build: {
      chunkSizeWarningLimit: 4096,
      outDir: OUTPUT_DIR,
      rollupOptions: {
        output: {
          manualChunks: {
            vue: ['vue', 'vue-router', 'pinia', 'vue-i18n', '@vueuse/core'],
            antd: ['ant-design-vue', '@ant-design/icons-vue', 'dayjs'],
          },
        },
      },
    },
    server: {
      port: 6678,
      host: '0.0.0.0',
      historyApiFallback: true,
      allowedHosts: [
        'ua636e64.natappfree.cc',
        '.natappfree.cc'
      ],
      proxy: {
        // 1. 后端接口代理
        '/api': {
          target: 'http://localhost:8088',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '')
        },
        '/web': {
          target: 'http://localhost:8088',
          changeOrigin: true
        },

        // ✅ 2. 静态资源统一代理 MinIO（包含头像、封面、Logo）
        // 匹配 /blog/user/avatar
        '/blog/user/avatar': {
          target: 'http://192.168.100.132:9000',
          changeOrigin: true,
        },
        // 匹配 /blog/article/cover 或 /blog/article/articleCover
        '/blog/article': {
          target: 'http://192.168.100.132:9000',
          changeOrigin: true,
        },
        // 匹配 /blog/logo
        '/blog/logo': {
          target: 'http://192.168.100.132:9000',
          changeOrigin: true,
        },
        // 原 upload 代理
        '/upload': {
          target: 'http://192.168.100.132:9000',
          changeOrigin: true
        }
      }
    },
    test: {
      globals: true,
      environment: 'jsdom',
    },
  }
}