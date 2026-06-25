<!-- Author: LINKS -->
<template>
  <div>
    <div class="card">
      <div class="card-title">消息列表 ({{ list.length }})</div>
      <div v-if="list.length === 0" class="empty-state">暂无消息</div>
      <div v-else>
        <div v-for="n in list" :key="n.notificationId" class="notif-item" :class="{ unread: !n.read }">
          <span class="notif-icon">{{ typeLabel(n.type) }}</span>
          <div class="notif-body">
            <div>{{ n.content }}</div>
            <div class="notif-time">{{ n.sendTime }}</div>
          </div>
          <div>
            <button v-if="!n.read" class="btn btn-primary btn-sm" @click="doRead(n.notificationId)">标记已读</button>
            <span v-else style="font-size:12px;color:var(--text-gray)">已读</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getNotifications, markAsRead } from '../api'

const emit = defineEmits(['toast'])
const list = ref([])

const typeLabels = { FINE: '[罚金]', OVERDUE_REMINDER: '[逾期]', RESERVATION_SUCCESS: '[预约]', BOOK_AVAILABLE: '[到书]', OUT_OF_STOCK: '[缺货]', SYSTEM: '[系统]' }
function typeLabel(t) { return typeLabels[t] || '[通知]' }

async function load() {
  try { const r = await getNotifications(); list.value = r.data } catch (e) { console.error('Notifications load failed:', e) }
}
async function doRead(id) {
  try { await markAsRead(id); emit('toast', '已标记'); await load() } catch (e) { emit('toast', e.message) }
}
onMounted(load)
</script>
