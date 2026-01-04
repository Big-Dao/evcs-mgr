---
applyTo: "evcs-admin/**/*.{vue,ts,js}"
priority: high
---

# 前端开发规范 (Frontend Standards)

> **最后更新**: 2025-12-18 | **维护者**: 前端团队 | **状态**: 已发布

本规范适用于 `evcs-admin` 模块的所有 Vue 3 + TypeScript 开发工作。

## 🚨 关键要求

### 1. 架构风格
**强制使用 Composition API**
- 所有组件必须使用 `<script setup lang="ts">` 语法。
- 禁止使用 Vue 2 风格的 Options API (`data`, `methods`, `computed` 对象)。
- 状态管理强制使用 **Pinia**，禁止使用 Vuex。

### 2. 组件规范
**命名与结构**
- **文件名**：使用 PascalCase (如 `ChargingStationList.vue`)。
- **Props 定义**：使用 TypeScript 接口定义 Props。
  ```typescript
  interface Props {
    stationId: string;
    isActive?: boolean;
  }
  const props = defineProps<Props>();
  ```
- **样式**：必须使用 `<style scoped>`，推荐使用 SCSS。

### 3. API 调用
**封装与解耦**
- 禁止在组件内部直接调用 `axios` 或 `fetch`。
- 所有 API 请求必须封装在 `src/api` 目录下的模块文件中。
- 组件只负责调用 API 方法并处理响应数据。

---

## ✅ 代码示例

### 推荐的组件结构

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getStationList } from '@/api/station';
import type { Station } from '@/types/station';

const stations = ref<Station[]>([]);
const loading = ref(false);

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getStationList();
    stations.value = res.data;
  } finally {
    loading.value = false;
  }
};

onMounted(loadData);
</script>

<template>
  <div class="station-list" v-loading="loading">
    <!-- Template content -->
  </div>
</template>
```
