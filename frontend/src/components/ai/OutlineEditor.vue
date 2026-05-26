<script setup>
import { Plus, Delete } from '@element-plus/icons-vue'

const model = defineModel({ required: true })

function addRoot() {
  model.value.push({
    id: `${Date.now()}`,
    title: '新章节',
    children: [],
  })
}

function addChild(node) {
  if (!node.children) node.children = []
  node.children.push({
    id: `${Date.now()}`,
    title: '新小节',
    children: [],
  })
}

function removeAt(list, index) {
  list.splice(index, 1)
}

function removeChild(parent, index) {
  if (!parent.children) return
  removeAt(parent.children, index)
}
</script>

<template>
  <div class="outline-editor">
    <el-button type="primary" link :icon="Plus" @click="addRoot">添加章节</el-button>
    <div v-for="(node, i) in model" :key="node.id" class="outline-block">
      <div class="outline-row level-1">
        <el-input v-model="node.title" placeholder="一级标题" />
        <el-button :icon="Plus" circle size="small" @click="addChild(node)" />
        <el-button :icon="Delete" circle size="small" type="danger" @click="removeAt(model, i)" />
      </div>
      <div
        v-for="(child, j) in node.children || []"
        :key="child.id"
        class="outline-row level-2"
      >
        <el-input v-model="child.title" placeholder="二级标题" />
        <el-button
          :icon="Delete"
          circle
          size="small"
          type="danger"
          @click="removeChild(node, j)"
        />
      </div>
    </div>
    <el-empty v-if="model.length === 0" description="暂无大纲，可点击添加或重新生成" />
  </div>
</template>

<style scoped>
.outline-editor {
  width: 100%;
}

.outline-block {
  margin-top: 12px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
}

.outline-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.level-2 {
  margin-left: 24px;
}
</style>
