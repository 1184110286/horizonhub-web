<!-- 文章封面组件 -->
<template>
  <div
    class="cover"
    :style="{ width: width + 'px', height: (height || width) + 'px' }"
  >
    <el-image
      :style="{ width: width + 'px', height: (height || width) + 'px' }"
      fit="cover"
      loading="lazy"
      :src="cover ? proxy.globalInfo.imageUrl + cover : localCover"
    >
    </el-image>
  </div>
</template>

<script setup>
import { getCurrentInstance } from "vue";
const { proxy } = getCurrentInstance();

const localCover = new URL("@/assets/default_cover.png", import.meta.url).href;

const props = defineProps({
  cover: {
    type: String,
  },
  width: {
    type: Number,
    default: 60,
  },
  height: {
    type: Number,
  },
});
</script>

<style lang="scss">
.cover {
  background: #ddd;
  border-radius: 5px;
  overflow: hidden;
}
</style>