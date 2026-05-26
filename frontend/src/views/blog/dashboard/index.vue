<script setup>
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <span>工作台</span>
    </template>
    <el-descriptions :column="1" border>
      <el-descriptions-item label="项目">
        AI 博客管理：文章、智写向导、优化与前台展示已与若依系统集成。
      </el-descriptions-item>
      <el-descriptions-item label="说明">
        请使用左侧菜单进入「文章」「博客智写」等页面；访客可通过「博客首页」匿名访问前台。
      </el-descriptions-item>
    </el-descriptions>
    <el-alert
      class="tip"
      title="在「博客智写」向导式生成全文，或使用「编辑文章」与 AI 侧边栏实时辅助。"
      type="info"
      :closable="false"
      show-icon
    />
  </el-card>
</template>

<style scoped>
.tip {
  margin-top: 16px;
}
</style>
