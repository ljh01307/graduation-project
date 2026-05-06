<template>
  <img :src="logoUrl" alt="ZJUT Logo" class="zjut-logo" />
</template>

<script>
import logoUrl from '../../assets/ZJUT-logo.svg'

export default {
  data() {
    return {
      logoUrl
    }
  }
}
</script>

<style scoped>
.zjut-logo {
  width: 48px;
  height: 48px;
  display: block;
}
</style>
