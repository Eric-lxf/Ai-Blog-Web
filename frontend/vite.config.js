import { defineConfig, loadEnv } from 'vite'
import path from 'path'
import createVitePlugins from './vite/plugins'

const baseUrl = 'http://localhost:8080' // 后端接口

// https://vitejs.dev/config/
export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd())
  const { VITE_APP_ENV } = env
  const isBuild = command === 'build'
  const lowMemBuild = process.env.VITE_LOW_MEM_BUILD === '1' || process.env.VITE_LOW_MEM_BUILD === 'true'
  return {
    optimizeDeps: {
      include: [
        'bytemd',
        '@bytemd/vue-next',
        '@bytemd/plugin-gfm',
        '@bytemd/plugin-highlight',
        '@bytemd/plugin-mermaid',
      ],
    },
    // 部署生产环境和开发环境下的URL。
    // 默认情况下，vite 会假设你的应用是被部署在一个域名的根路径上
    // 例如 https://www.ruoyi.vip/。如果应用被部署在一个子路径上，你就需要用这个选项指定这个子路径。例如，如果你的应用被部署在 https://www.ruoyi.vip/admin/，则设置 baseUrl 为 /admin/。
    base: VITE_APP_ENV === 'production' ? '/' : '/',
    plugins: createVitePlugins(env, isBuild, lowMemBuild),
    resolve: {
      // https://cn.vitejs.dev/config/#resolve-alias
      alias: {
        // 设置路径
        '~': path.resolve(__dirname, './'),
        // 设置别名
        '@': path.resolve(__dirname, './src')
      },
      // https://cn.vitejs.dev/config/#resolve-extensions
      extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
    },
    // 打包配置
    build: {
      // https://vite.dev/config/build-options.html
      sourcemap: isBuild ? false : 'inline',
      outDir: 'dist',
      assetsDir: 'assets',
      chunkSizeWarningLimit: 2000,
      // 关闭 gzip 体积统计，显著降低 rendering chunks 阶段内存（2C4G 机器建议开启）
      reportCompressedSize: !lowMemBuild,
      cssCodeSplit: true,
      rollupOptions: {
        maxParallelFileOps: lowMemBuild ? 2 : 20,
        output: {
          chunkFileNames: 'static/js/[name]-[hash].js',
          entryFileNames: 'static/js/[name]-[hash].js',
          assetFileNames: 'static/[ext]/[name]-[hash].[ext]',
          manualChunks: isBuild ? manualChunks : undefined
        }
      }
    },
    // vite 相关配置
    server: {
      port: 80,
      host: true,
      open: true,
      proxy: {
        // https://cn.vitejs.dev/config/#server-proxy
        '/dev-api': {
          target: baseUrl,
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/dev-api/, ''),
          // 账单 OCR / PDF 识别超过默认代理超时会变成 504
          timeout: 300000,
          proxyTimeout: 300000
        },
         // springdoc proxy
         '^/v3/api-docs/(.*)': {
          target: baseUrl,
          changeOrigin: true,
        }
      }
    },
    css: {
      postcss: {
        plugins: [
          {
            postcssPlugin: 'internal:charset-removal',
            AtRule: {
              charset: (atRule) => {
                if (atRule.name === 'charset') {
                  atRule.remove()
                }
              }
            }
          }
        ]
      }
    }
  }
})

/** 拆分大依赖，降低单 chunk 渲染峰值内存（mermaid/echarts 等） */
function manualChunks(id) {
  if (!id.includes('node_modules')) {
    return
  }
  if (id.includes('echarts')) {
    return 'chunk-echarts'
  }
  if (id.includes('mermaid') || id.includes('/elk') || id.includes('dagre') || id.includes('cytoscape')) {
    return 'chunk-mermaid'
  }
  if (id.includes('element-plus')) {
    return 'chunk-element-plus'
  }
  if (id.includes('bytemd') || id.includes('highlight.js')) {
    return 'chunk-bytemd'
  }
  if (id.includes('vue') || id.includes('pinia') || id.includes('vue-router')) {
    return 'chunk-vue'
  }
  return 'chunk-vendor'
}
