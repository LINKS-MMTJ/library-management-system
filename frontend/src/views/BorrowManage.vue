<template>
  <div>
    <!-- 逾期警告 -->
    <div v-if="overdue.length > 0" class="card" style="border-color:var(--danger)">
      <div class="card-title" style="color:var(--danger)">逾期未还 ({{ overdue.length }})</div>
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>图书</th><th>借阅人</th><th>应还日期</th><th>罚金</th></tr></thead>
          <tbody>
            <tr v-for="r in overdue" :key="r.recordId">
              <td>{{ r.bookTitle }}</td>
              <td>{{ r.userName }}</td>
              <td>{{ r.dueDate }}</td>
              <td style="color:var(--danger);font-weight:600">¥{{ r.fineAmount?.toFixed(2) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 当前借出 -->
    <div class="card">
      <div class="card-title">当前借出 ({{ active.length }})</div>
      <div v-if="active.length === 0" class="empty-state">暂无借出记录</div>
      <div class="table-wrap" v-else>
        <table class="data-table">
          <thead><tr><th>图书</th><th>借阅人</th><th>借阅日期</th><th>应还日期</th><th>状态</th><th>续借</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="r in active" :key="r.recordId">
              <td>{{ r.bookTitle }}</td>
              <td>{{ r.userName }}</td>
              <td>{{ r.borrowDate }}</td>
              <td>{{ r.dueDate }}</td>
              <td><span class="badge" :class="r.statusDesc==='已逾期'?'badge-danger':'badge-success'">{{ r.statusDesc }}</span></td>
              <td>{{ r.renewCount }}/2</td>
              <td><button class="btn btn-success btn-sm" @click="doReturn(r.recordId)">归还</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAllBorrows, returnBookById } from '../api'

const emit = defineEmits(['toast'])
const records = ref([])

const active = computed(() => records.value.filter(r => r.status !== 'RETURNED'))
const overdue = computed(() => active.value.filter(r => r.statusDesc === '已逾期'))

async function loadRecords() {
  try { const r = await getAllBorrows(); records.value = r.data } catch (e) {}
}
async function doReturn(id) {
  if (!confirm('确认归还该图书？')) return
  try { await returnBookById(id); emit('toast', '归还成功'); await loadRecords() } catch (e) { emit('toast', e.message) }
}
onMounted(loadRecords)
</script>
