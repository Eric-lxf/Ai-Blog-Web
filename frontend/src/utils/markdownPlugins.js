import gfm from '@bytemd/plugin-gfm'
import highlight from '@bytemd/plugin-highlight'

export function createLightMarkdownPlugins() {
  return [gfm(), highlight()]
}

/** 运行时按需加载 mermaid，避免构建期整图进 Rollup 变换峰值 */
export async function createFullMarkdownPlugins() {
  const plugins = [gfm(), highlight()]
  const { default: mermaid } = await import('@bytemd/plugin-mermaid')
  plugins.push(mermaid())
  return plugins
}
