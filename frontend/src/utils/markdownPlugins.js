import gfm from '@bytemd/plugin-gfm'
import highlight from '@bytemd/plugin-highlight'
import mermaid from '@bytemd/plugin-mermaid'

export function createFullMarkdownPlugins() {
  return [gfm(), highlight(), mermaid()]
}

export function createLightMarkdownPlugins() {
  return [gfm(), highlight()]
}
