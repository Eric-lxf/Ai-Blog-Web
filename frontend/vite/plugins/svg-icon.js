import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import path from 'path'

export default function createSvgIcon(isBuild, lowMemBuild = false) {
  return createSvgIconsPlugin({
    iconDirs: [path.resolve(process.cwd(), 'src/assets/icons/svg')],
    symbolId: 'icon-[dir]-[name]',
    // 低内存构建跳过 SVGO，减少构建期 CPU/内存
    svgoOptions: isBuild && !lowMemBuild
  })
}
