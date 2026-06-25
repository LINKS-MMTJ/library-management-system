<template>
  <div>
    <div class="card">
      <div class="card-title">活跃预约 ({{ active.length }})</div>
      <div v-if="active.length === 0" class="empty-state">暂无活跃预约</div>
      <div class="table-wrap" v-else>
        <table class="data-table">
          <thead>
            <tr>
              <th>图书</th><th>作者</th>
              <th v-if="showUserCol">预约人</th>
              <th>预约日期</th><th>状态</th><th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in active" :key="r.reservationId">
              <td>{{ r.bookTitle }}</td>
              <td>{{ r.bookAuthor }}</td>
              <td v-if="showUserCol">{{ r.userName }}</td>
              <td>{{ r.requestDate }}</td>
              <td><span class="badge badge-warning">{{ r.statusDesc }}</span></td>
              <td><button class="btn btn-danger btn-sm" @click="doCancel(r.reservationId)">取消预约</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="card" v-if="history.length > 0">
      <div class="card-title">历史预约 ({{ history.length }})</div>
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>图书</th><th>预约日期</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-for="r in history" :key="r.reservationId">
              <td>{{ r.bookTitle }}</td>
              <td>{{ r.requestDate }}</td>
              <td><span class="badge badge-gray">{{ r.statusDesc }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getReservations, cancelReservation } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const emit = defineEmits(['toast'])
const list = ref([])
const showUserCol = computed(() => auth.canManage)

const active = computed(() => list.value.filter(r => r.status === 'ACTIVE'))
const history = computed(() => list.value.filter(r => r.status !== 'ACTIVE'))

async function load() {
  try { const r = await getReservations(); list.value = r.data } catch (e) { console.error('Reservations load failed:', e) }
}
async function doCancel(id) {
  if (!confirm('确认取消该预约？')) return
  try { await cancelReservation(id); emit('toast', '预约已取消'); await load() } catch (e) { emit('toast', e.message) }
}
onMounted(load)
</script>
