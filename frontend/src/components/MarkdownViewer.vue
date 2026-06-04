<script setup>
import { onMounted, ref } from 'vue'
import { Viewer } from '@bytemd/vue-next'
import { createFullMarkdownPlugins } from '@/utils/markdownPlugins'

import 'bytemd/dist/index.css'
import 'highlight.js/styles/github.css'

defineProps({
  value: { type: String, default: '' },
})

const plugins = ref([])
onMounted(async () => {
  plugins.value = await createFullMarkdownPlugins()
})
</script>

<template>
  <div class="markdown-viewer">
    <Viewer v-if="plugins.length" :value="value" :plugins="plugins" />
  </div>
</template>

<style scoped>
.markdown-viewer :deep(.markdown-body) {
  font-size: 16px;
  line-height: 1.75;
  color: #1f2937;
}

.markdown-viewer :deep(.markdown-body h1),
.markdown-viewer :deep(.markdown-body h2),
.markdown-viewer :deep(.markdown-body h3),
.markdown-viewer :deep(.markdown-body h4),
.markdown-viewer :deep(.markdown-body h5),
.markdown-viewer :deep(.markdown-body h6) {
  color: #0f172a;
  font-weight: 700;
  line-height: 1.35;
  margin-top: 1.25em;
  margin-bottom: 0.6em;
}

.markdown-viewer :deep(.markdown-body h1) {
  font-size: 1.75em;
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 0.3em;
}

.markdown-viewer :deep(.markdown-body h2) {
  font-size: 1.45em;
}

.markdown-viewer :deep(.markdown-body h3) {
  font-size: 1.2em;
}

.markdown-viewer :deep(pre) {
  border-radius: 8px;
}

.markdown-viewer :deep(.mermaid) {
  text-align: center;
  margin: 16px 0;
}
</style>
