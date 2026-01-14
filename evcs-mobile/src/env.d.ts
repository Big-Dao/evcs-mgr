/// <reference types="@dcloudio/types" />
/// <reference types="@uni-helper/uni-app-types" />

declare module '*.vue' {
  import { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
