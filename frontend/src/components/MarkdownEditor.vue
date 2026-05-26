<script setup>
import { Editor } from '@bytemd/vue-next'
import { createFullMarkdownPlugins } from '@/utils/markdownPlugins'
import { uploadImage } from '@/api/blog/upload'

import 'bytemd/dist/index.css'
import 'highlight.js/styles/github.css'

defineProps({
  modelValue: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])

const plugins = createFullMarkdownPlugins()

async function handleUpload(files) {
  return Promise.all(
    files.map(async file => {
      const url = await uploadImage(file)
      return { url, alt: file.name }
    }),
  )
}
</script>

<template>
  <div class="markdown-editor">
    <Editor
      :value="modelValue"
      :plugins="plugins"
      :upload-images="handleUpload"
      @change="emit('update:modelValue', $event)"
    />
  </div>
</template>

<style scoped>
.markdown-editor {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.markdown-editor :deep(.bytemd) {
  height: 520px;
}
</style>
