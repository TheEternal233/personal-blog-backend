<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { LoadingOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons-vue'
import type { UploadProps } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { updateStationmaster, uploadAckgroundImage, uploadAvatar } from "~/api/blog/webInfo";
import { compressImage } from "~/utils/CompressedImage.ts";

const emit = defineEmits(["reset:stationmaster:info"])

const props = defineProps({
  info: {
    type: Object,
    default: () => ({})
  },
})

interface FormDataType {
  webmasterAvatar: string;
  webmasterName: string;
  webmasterCopy: string;
  webmasterProfileBackground: string;
  giteeLink: string;
  githubLink: string;
}

const formData = reactive<Partial<FormDataType>>({ ...props.info });

const avatarFileList = ref([])
const loading = ref<boolean>(false)
const imageAvatarUrl = ref<string>()
const backgroundPreviewUrl = ref<string>()

// 🔥 核心修复：头像逻辑完全独立，页面加载立即渲染
onMounted(() => {
  // 1. 头像渲染：严格校验，确保有值就显示
  const avatarUrl = formData.webmasterAvatar
  if (avatarUrl && typeof avatarUrl === 'string' && avatarUrl.trim() !== '') {
    imageAvatarUrl.value = avatarUrl
  }

  // 2. 背景图渲染：独立逻辑，不影响头像
  const bgUrl = formData.webmasterProfileBackground
  if (bgUrl && typeof bgUrl === 'string' && bgUrl.trim() !== '') {
    backgroundPreviewUrl.value = bgUrl
  }
})

// 头像上传
async function beforeUploadAvatar(file: UploadProps['fileList'][number]) {
  loading.value = true;

  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/webp';
  if (!isJpgOrPng) {
    message.error('文件格式必须是 jpg / png / webp');
    loading.value = false;
    return;
  }

  const isLt10M = file.size / 1024 / 1024 < 10;
  if (!isLt10M) {
    message.error('头像大小必须小于 10MB');
    loading.value = false;
    return;
  }

  const compressedFile = await compressImage(file);

  await fileToBase64(file).then(base64Url => {
    imageAvatarUrl.value = base64Url;
    loading.value = false;
  });

  const form = new FormData();
  form.append('avatar', compressedFile, compressedFile.name);

  uploadAvatar(form).then(res => {
    if (res.code === 200) {
      message.success('头像上传成功');
      formData.webmasterAvatar = res.data;
    } else {
      message.error(`上传失败：${res.msg}`);
    }
  });

  return false;
}

// 背景图上传
async function beforeUploadAckgroundImag(file: UploadProps['fileList'][number]) {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/webp';
  if (!isJpgOrPng) {
    message.error('文件格式必须是 jpg / png / webp');
    return;
  }

  const compressedFile = await compressImage(file);
  const form = new FormData();
  form.append('background', compressedFile, compressedFile.name);

  uploadAckgroundImage(form).then(res => {
    if (res.code === 200) {
      const imgUrl = res.data;
      backgroundPreviewUrl.value = imgUrl
      message.success('资料卡背景图上传成功');
      formData.webmasterProfileBackground = imgUrl;
    } else {
      message.error(`资料卡背景图上传失败：${res.msg}`)
    }
  })

  return false;
}

// base64
function fileToBase64(file: any) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result);
    reader.onerror = error => reject(error);
  });
}

// 保存
function updateStationmasterInfo() {
  updateStationmaster(formData).then(res => {
    if (res.code === 200) {
      message.success('保存成功');
    }
  });
}

// 重置
function resetStationmasterInfo() {
  emit('reset:stationmaster:info');
  // 重置时清空预览
  imageAvatarUrl.value = undefined
  backgroundPreviewUrl.value = undefined
}
</script>

<template>
  <div class="info-container">
    <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 14 }" :colon="false">
      <!-- 头像 -->
      <a-form-item label="头像">
        <a-upload
            :file-list="avatarFileList"
            name="avatar"
            list-type="picture-card"
            class="avatar-uploader"
            :show-upload-list="false"
            :before-upload="beforeUploadAvatar"
        >
          <img 
            v-if="imageAvatarUrl" 
            :src="imageAvatarUrl" 
            alt="avatar"
            @error="imageAvatarUrl = undefined"
          >
          <div v-else class="avatar-placeholder">
            <LoadingOutlined v-if="loading"/>
            <PlusOutlined v-else/>
            <div class="ant-upload-text">头像上传</div>
          </div>
        </a-upload>
      </a-form-item>

      <!-- 名称 -->
      <a-form-item label="名称">
        <a-input v-model:value="formData.webmasterName" placeholder="请输入站长名称" style="width: 300px" />
      </a-form-item>

      <!-- 文案 -->
      <a-form-item label="文案">
        <a-input v-model:value="formData.webmasterCopy" placeholder="请输入个人简介" style="width: 300px" />
      </a-form-item>

      <!-- 背景 -->
      <a-form-item label="背景">
        <a-upload
            :file-list="[]"
            name="background"
            :show-upload-list="false"
            :before-upload="beforeUploadAckgroundImag"
            :max-count="1"
        >
          <a-button>
            <UploadOutlined />
            背景上传
          </a-button>
        </a-upload>
        <!-- 自定义背景预览 -->
        <div v-if="backgroundPreviewUrl" class="background-preview">
          <img :src="backgroundPreviewUrl" alt="background" @error="backgroundPreviewUrl = undefined">
        </div>
        <div class="upload-tip">图片资源上传与保存按钮无关</div>
      </a-form-item>

      <a-divider />

      <!-- 相关链接 -->
      <a-form-item label="Github">
        <a-input v-model:value="formData.githubLink" placeholder="请输入Github链接" style="width: 300px" />
      </a-form-item>
      <a-form-item label="Gitee">
        <a-input v-model:value="formData.giteeLink" placeholder="请输入Gitee链接" style="width: 300px" />
      </a-form-item>

      <!-- 操作按钮 -->
      <a-form-item :wrapper-col="{ offset: 4, span: 14 }">
        <a-space>
          <a-button type="primary" @click="updateStationmasterInfo">保存</a-button>
          <a-button @click="resetStationmasterInfo">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>
  </div>
</template>

<style scoped lang="scss">
.info-container {
  width: 100%;
  max-width: 800px;
  padding: 1rem 2rem;

  :deep(.ant-form-item) {
    margin-bottom: 1.5rem;
  }

  // 头像样式
  :deep(.ant-upload.ant-upload-select-picture-card) {
    width: 100px;
    height: 100px;
    margin-right: 0;
    overflow: hidden;
    border-radius: 8px;
  }

  :deep(.ant-upload img) {
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: center;
  }

  .avatar-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    color: #999;

    .ant-upload-text {
      font-size: 12px;
      margin-top: 4px;
    }
  }

  // 背景预览样式
  .background-preview {
    width: 300px;
    height: 150px;
    margin-top: 8px;
    border-radius: 8px;
    overflow: hidden;
    border: 1px solid #d9d9d9;
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      object-position: center;
    }
  }

  .upload-tip {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
  }

  :deep(.ant-divider) {
    margin: 1.5rem 0;
  }
}
</style>