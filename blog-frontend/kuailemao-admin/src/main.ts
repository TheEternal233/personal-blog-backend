import { createPinia } from 'pinia'
import { createApp } from 'vue'
import type { App } from 'vue'
import * as Icons from '@ant-design/icons-vue'
import Root from './App.vue'
import { setupI18n } from './locales'
import {
  setupAccessDirectiveHasPermi,
  setupAccessDirectiveHasRole,
  setupLoadingDirective,
} from './directive'
import router from '~/router'
import '~/router/router-guard'
// ✅ 保持正确的 reset.css 引入
import 'ant-design-vue/dist/reset.css'
import 'uno.css'

// 自定义样式
import '~/assets/styles/my-a-button.css'

const pinia = createPinia()

// 注册全局图标
function registerIcons(app: App, icons: { [key: string]: any }) {
  for (const i in icons)
    app.component(i, icons[i])
}

async function start() {
  const app: App = createApp(Root)

  // 顺序很重要
  app.use(pinia)
  await setupI18n(app)

  // 直接调用指令
  setupLoadingDirective(app)
  setupAccessDirectiveHasRole(app)
  setupAccessDirectiveHasPermi(app)

  app.use(router)
  registerIcons(app, Icons as { [key: string]: any })

  // 最后挂载
  app.mount('#app')
  app.config.performance = true
}

start()