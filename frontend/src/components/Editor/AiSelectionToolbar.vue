<script setup>
import { MagicStick } from '@element-plus/icons-vue'

defineProps({
  visible: { type: Boolean, default: false },
  top: { type: Number, default: 0 },
  left: { type: Number, default: 0 },
  loading: { type: Boolean, default: false },
})

const emit = defineEmits(['action'])

const actions = [
  {
    key: 'rewrite',
    label: '润色',
    scene: 'REWRITE',
    buildPrompt: t => t,
  },
  {
    key: 'expand',
    label: '扩写',
    scene: 'EXPAND',
    buildPrompt: t => t,
  },
  {
    key: 'shorten',
    label: '精简',
    scene: 'SHORTEN',
    buildPrompt: t => t,
  },
  {
    key: 'continue',
    label: '续写',
    scene: 'CONTINUE',
    buildPrompt: t => `请根据以下上文续写：\n\n${t}`,
  },
]

function onClick(action) {
  emit('action', action)
}
</script>

<template>
  <Teleport to="body">
    <div
      v-show="visible"
      class="selection-toolbar"
      :style="{ top: `${top}px`, left: `${left}px` }"
      @mousedown.prevent
    >
      <el-icon class="icon"><MagicStick /></el-icon>
      <el-button
        v-for="item in actions"
        :key="item.key"
        size="small"
        text
        :disabled="loading"
        @click="onClick(item)"
      >
        {{ item.label }}
      </el-button>
      <span v-if="loading" class="loading-tip">生成中...</span>
    </div>
  </Teleport>
</template>

<style scoped>
.selection-toolbar {
  position: fixed;
  z-index: 3000;
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 4px 8px;
  background: #1f2937;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  transform: translate(-50%, -100%) translateY(-8px);
}

.icon {
  color: #34d399;
  margin-right: 4px;
}

.selection-toolbar :deep(.el-button) {
  color: #f9fafb;
}

.selection-toolbar :deep(.el-button:hover) {
  color: #34d399;
  background: rgba(52, 211, 153, 0.15);
}

.loading-tip {
  font-size: 12px;
  color: #9ca3af;
  margin-left: 6px;
}
</style>
