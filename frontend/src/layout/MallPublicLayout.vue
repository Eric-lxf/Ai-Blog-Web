<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="mall-layout">
    <header class="mall-header">
      <RouterLink to="/mall" class="logo">
        <span class="logo-mark">N</span>
        <span>NovaMall</span>
      </RouterLink>
      <nav class="nav">
        <RouterLink to="/mall">首页</RouterLink>
        <RouterLink to="/mall/list">全部商品</RouterLink>
        <RouterLink to="/mall/cart">购物车</RouterLink>
        <RouterLink to="/mall/orders">我的订单</RouterLink>
        <RouterLink to="/index">管理后台</RouterLink>
      </nav>
    </header>
    <main class="mall-main">
      <RouterView />
    </main>
    <footer class="mall-footer">
      <span>NovaMall · 品质好物与智能运营平台</span>
    </footer>
  </div>
</template>

<style scoped>
.mall-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fb;
}

.mall-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  height: 64px;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid #e5e7eb;
  backdrop-filter: blur(10px);
}

.logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 21px;
  font-weight: 800;
  color: #0f172a;
  text-decoration: none;
}

.logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #7c3aed);
}

.nav {
  display: flex;
  align-items: center;
  gap: 22px;
}

.nav a {
  color: #4b5563;
  text-decoration: none;
}

.nav a.router-link-active {
  color: #2563eb;
  font-weight: 700;
}

.mall-main {
  flex: 1;
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 28px 16px 56px;
}

.mall-footer {
  text-align: center;
  padding: 18px;
  color: #9ca3af;
  font-size: 13px;
  background: #fff;
  border-top: 1px solid #e5e7eb;
}

@media (max-width: 768px) {
  .mall-header {
    height: auto;
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
    padding: 14px 16px;
  }

  .nav {
    gap: 14px;
    flex-wrap: wrap;
  }
}
</style>
