<script setup>
import { ref } from 'vue'

defineProps({
  visible: { type: Boolean, default: false },
  top: { type: Number, default: 0 },
  left: { type: Number, default: 0 },
})

const emit = defineEmits(['select', 'close'])

const commands = [
  {
    key: 'continue',
    label: '/续写',
    desc: '根据上文继续写',
    scene: 'CONTINUE',
    buildPrompt: (input, prefix) =>
      input ? `上文：\n${prefix}\n\n请续写：${input}` : `请根据上文续写：\n${prefix}`,
  },
  {
    key: 'mermaid',
    label: '/架构图',
    desc: '生成 Mermaid 图',
    scene: 'MERMAID_GEN',
    needInput: true,
    inputPlaceholder: '描述架构或流程，如：Redis 分布式锁',
    buildPrompt: input => input || '微服务基础架构',
  },
  {
    key: 'code',
    label: '/写代码',
    desc: '生成代码块',
    scene: 'CODE_GEN',
    needInput: true,
    inputPlaceholder: '如：Java 21 虚拟线程示例',
    buildPrompt: input => input || 'Hello World',
  },
  {
    key: 'rewrite',
    label: '/润色',
    desc: '润色当前行或段落',
    scene: 'REWRITE',
    buildPrompt: (input, prefix) => input || prefix,
  },
  {
    key: 'expand',
    label: '/扩写',
    desc: '扩写选中或当前内容',
    scene: 'EXPAND',
    buildPrompt: (input, prefix) => input || prefix,
  },
]

const activeInput = ref('')
const pendingCmd = ref(null)

function pick(cmd) {
  if (cmd.needInput) {
    pendingCmd.value = cmd
    activeInput.value = ''
    return
  }
  emit('select', cmd, '')
  emit('close')
}

function confirmInput() {
  if (!pendingCmd.value) return
  emit('select', pendingCmd.value, activeInput.value.trim())
  pendingCmd.value = null
  activeInput.value = ''
  emit('close')
}

function cancelInput() {
  pendingCmd.value = null
  activeInput.value = ''
}
</script>

<template>
  <Teleport to="body">
    <div
      v-show="visible"
      class="slash-menu"
      :style="{ top: `${top}px`, left: `${left}px` }"
      @mousedown.prevent
    >
      <template v-if="pendingCmd">
        <div class="input-panel">
          <div class="input-title">{{ pendingCmd.label }} — {{ pendingCmd.desc }}</div>
          <el-input
            v-model="activeInput"
            size="small"
            :placeholder="pendingCmd.inputPlaceholder"
            autofocus
            @keydown.enter.prevent="confirmInput"
            @keydown.esc.prevent="cancelInput"
          />
          <div class="input-actions">
            <el-button size="small" @click="cancelInput">取消</el-button>
            <el-button size="small" type="primary" @click="confirmInput">生成</el-button>
          </div>
        </div>
      </template>
      <template v-else>
        <div
          v-for="cmd in commands"
          :key="cmd.key"
          class="slash-item"
          @click="pick(cmd)"
        >
          <span class="label">{{ cmd.label }}</span>
          <span class="desc">{{ cmd.desc }}</span>
        </div>
      </template>
    </div>
  </Teleport>
</template>

<style scoped>
.slash-menu {
  position: fixed;
  z-index: 3000;
  min-width: 220px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  padding: 6px;
  transform: translateY(4px);
}

.slash-item {
  display: flex;
  flex-direction: column;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
}

.slash-item:hover {
  background: #ecfdf5;
}

.label {
  font-weight: 600;
  font-size: 14px;
  color: #059669;
}

.desc {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

.input-panel {
  padding: 8px;
}

.input-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
</style>
