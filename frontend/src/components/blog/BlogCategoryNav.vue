<script setup>
defineProps({
  categories: {
    type: Array,
    default: () => []
  },
  modelValue: {
    type: Number,
    default: undefined
  }
})

const emit = defineEmits(['update:modelValue'])

function select(id) {
  emit('update:modelValue', id)
}
</script>

<template>
  <nav class="category-nav" aria-label="文章分类">
    <h2 class="nav-title">分类</h2>
    <ul class="nav-list">
      <li>
        <button
          type="button"
          class="nav-item"
          :class="{ active: modelValue == null }"
          @click="select(undefined)"
        >
          全部分类
        </button>
      </li>
      <li v-for="c in categories" :key="c.id">
        <button
          type="button"
          class="nav-item"
          :class="{ active: modelValue === c.id }"
          @click="select(c.id)"
        >
          {{ c.name }}
        </button>
      </li>
    </ul>
  </nav>
</template>

<style scoped>
.category-nav {
  width: 200px;
  flex-shrink: 0;
}

.nav-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 0.02em;
}

.nav-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  display: block;
  width: 100%;
  text-align: left;
  border: none;
  background: transparent;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 14px;
  color: #4b5563;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.nav-item:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.nav-item.active {
  background: #ecfdf5;
  color: #047857;
  font-weight: 600;
}

@media (max-width: 959px) {
  .category-nav {
    width: 100%;
  }

  .nav-title {
    display: none;
  }

  .nav-list {
    flex-direction: row;
    flex-wrap: nowrap;
    overflow-x: auto;
    gap: 8px;
    padding-bottom: 4px;
    -webkit-overflow-scrolling: touch;
  }

  .nav-item {
    white-space: nowrap;
    width: auto;
    padding: 6px 12px;
    border: 1px solid #e5e7eb;
    background: #fff;
  }

  .nav-item.active {
    border-color: #059669;
  }
}
</style>
