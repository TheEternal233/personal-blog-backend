<script setup lang="ts">
import { reactive, ref, onMounted, watch } from 'vue'
import dayjs from 'dayjs'
import StationmasterInfo from './stationmaster-info/index.vue'
import WebInfo from './web-info/index.vue'
import { websiteInfo } from '~/api/blog/webInfo'

const data = reactive(['站长信息', '网站信息'])
const value = ref(data[0])
const info = ref({
  webmasterName: '',
  webmasterCopy: '',
  githubLink: '',
  giteeLink: '',
  websiteName: '',
  headerNotification: '',
  sidebarAnnouncement: '',
  startTime: dayjs(),
  recordInfo: ''
})
const loading = ref(true)

onMounted(() => {
  websiteInfo().then((res) => {
    if (res.code === 200 && res.data) {
      info.value = { ...info.value, ...res.data }
      if (res.data.startTime) {
        info.value.startTime = dayjs(res.data.startTime)
      }
    }
  }).finally(() => loading.value = false)
})

const resetStationmasterInfo = () => {
  Object.assign(info.value, {
    webmasterName: '',
    webmasterCopy: '',
    githubLink: '',
    giteeLink: ''
  })
}

const resetWebInfo = () => {
  Object.assign(info.value, {
    websiteName: '',
    headerNotification: '',
    sidebarAnnouncement: '',
    startTime: dayjs(),
    recordInfo: ''
  })
}

const isDark = ref(useDark().value)
</script>

<template>
  <page-container>
    <template #content>
      <h2>信息管理</h2>
    </template>
    <template #default>
      <div :style="{ background: isDark ? 'none' : 'white', minHeight: '80vh', padding: '20px', boxSizing: 'border-box' }">
        <a-segmented v-model:value="value" :options="data" style="margin-bottom: 20px" />

        <!-- 站长信息 -->
        <div v-show="value === '站长信息'" class="info_container">
          <a-spin :spinning="loading">
            <StationmasterInfo 
              v-if="!loading" 
              :info="info" 
              @reset:stationmaster:info="resetStationmasterInfo" 
            />
          </a-spin>
        </div>

        <!-- 网站信息 -->
        <div v-show="value === '网站信息'" class="info_container">
          <a-spin :spinning="loading">
            <WebInfo 
              v-if="!loading" 
              :info="info" 
              @reset:web:info="resetWebInfo" 
            />
          </a-spin>
        </div>

        <!-- 调试面板：移到最底部，不遮挡表单 -->
        <div v-if="false" style="margin-top: 40px; padding: 16px; border: 1px solid #eee; font-size: 12px;">
          <h4>调试：当前info数据</h4>
          <pre>{{ JSON.stringify(info, null, 2) }}</pre>
        </div>
      </div>
    </template>
  </page-container>
</template>

<style scoped lang="scss">
.info_container{
  // 🔴 关键修复：给容器足够高度，移除多余边距
  min-height: 60vh;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 20px 0;
  box-sizing: border-box;
}
</style>