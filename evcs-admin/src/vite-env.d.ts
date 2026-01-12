/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module 'axios' {
  export interface AxiosRequestConfig {
    /**
     * If true, suppress global ElMessage errors in interceptors.
     * Useful for pages that handle errors per-row.
     */
    silent?: boolean
  }
}
