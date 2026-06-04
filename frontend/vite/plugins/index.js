import vue from '@vitejs/plugin-vue'

import createAutoImport from './auto-import'
import createSvgIcon from './svg-icon'
import createCompression from './compression'
import createSetupExtend from './setup-extend'

export default function createVitePlugins(viteEnv, isBuild = false, lowMemBuild = false) {
  const vitePlugins = [vue()]
  vitePlugins.push(createAutoImport())
  vitePlugins.push(createSetupExtend())
  vitePlugins.push(createSvgIcon(isBuild, lowMemBuild))
  // 低内存构建默认不跑 vite-plugin-compression（由 nginx 做 gzip）
  isBuild && !lowMemBuild && vitePlugins.push(...createCompression(viteEnv))
  return vitePlugins
}
