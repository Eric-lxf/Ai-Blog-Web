<script setup>
import { Viewer } from '@bytemd/vue-next'
import { createLightMarkdownPlugins } from '@/utils/markdownPlugins'

import 'bytemd/dist/index.css'
import 'highlight.js/styles/github.css'

defineProps({
  value: { type: String, default: '' },
})

const plugins = createLightMarkdownPlugins()
</script>

<template>
  <div class="simple-markdown">
    <Viewer :value="value" :plugins="plugins" />
  </div>
</template>

<style scoped>
.simple-markdown :deep(.markdown-body) {
  font-size: 14px;
  line-height: 1.65;
  color: #334155;
}
</style>
