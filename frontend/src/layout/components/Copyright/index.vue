<template>
  <footer v-if="visible" class="copyright">
    <span class="copy-text">{{ content }}</span>
    <span class="sep">|</span>
    <a
      href="https://beian.miit.gov.cn/"
      target="_blank"
      rel="noreferrer noopener"
      class="beian-link"
    >皖ICP备2026017896号</a>
    <span class="sep">|</span>
    <a
      href="https://beian.mps.gov.cn/#/query/webSearch?code=34122502000696"
      target="_blank"
      rel="noreferrer noopener"
      class="beian-link gongan"
    >
      <img
        src="https://www.beian.gov.cn/img/new/gongan.png"
        alt="公安备案图标"
        class="gongan-icon"
      />皖公网安备34122502000696号
    </a>
  </footer>
</template>

<script setup>
import useSettingsStore from '@/store/modules/settings'

const settingsStore = useSettingsStore()
const visible = computed(() => settingsStore.footerVisible)
const content = computed(() => settingsStore.footerContent)
</script>

<style scoped>
.copyright {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 20px;
  background-color: #f8f8f8;
  border-top: 1px solid #e7e7e7;
  color: #666;
  font-size: 13px;
  z-index: 999;
  white-space: nowrap;
  overflow: hidden;
}
.sep { color: #ccc; }
.beian-link {
  color: #666;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: color 0.2s;
}
.beian-link:hover { color: #409eff; }
.gongan-icon {
  width: 16px;
  height: 16px;
  vertical-align: middle;
}
</style>