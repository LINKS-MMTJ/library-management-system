<template>
  <div>
    <div class="stats-grid" v-if="stats">
      <div class="stat-card">
        <div class="stat-icon" style="background:#dbeafe;color:var(--primary)">藏</div>
        <div>
          <div class="stat-value">{{ stats.totalBooks }}</div>
          <div class="stat-label">图书总数 · 共 {{ stats.totalCopies }} 册</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#d1fae5;color:var(--success)">可</div>
        <div>
          <div class="stat-value">{{ stats.availableCopies }}</div>
          <div class="stat-label">当前可借阅</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#fef3c7;color:var(--warning)">借</div>
        <div>
          <div class="stat-value">{{ stats.activeBorrows }}</div>
          <div class="stat-label">当前借出 · 逾期 {{ stats.overdueCount }} 本</div>
        </div>
      </div>
      <div class="stat-card" v-if="auth.canManage">
        <div class="stat-icon" style="background:#dbeafe;color:var(--primary)">约</div>
        <div>
          <div class="stat-value">{{ stats.activeReservations }}</div>
          <div class="stat-label">活跃预约</div>
        </div>
      </div>
      <div class="stat-card" v-else>
        <div class="stat-icon" :style="stats.unreadNotifications > 0 ? 'background:#fee2e2;color:var(--danger)' : 'background:#d1fae5;color:var(--success)'">信</div>
        <div>
          <div class="stat-value">{{ stats.unreadNotifications }}</div>
          <div class="stat-label">{{ stats.unreadNotifications > 0 ? '有新消息' : '无新消息' }}</div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-title">最近借阅记录</div>
      <div v-if="!stats?.recentRecords?.length" class="empty-state">暂无数据</div>
      <div class="table-wrap" v-else>
        <table class="data-table">
          <thead>
            <tr><th>图书</th><th>借阅人</th><th>借阅日期</th><th>应还日期</th><th>状态</th></tr>
          </thead>
          <tbody>
            <tr v-for="r in stats.recentRecords" :key="r.borrowDate+r.bookTitle">
              <td>{{ r.bookTitle }}</td>
              <td>{{ r.userName }}</td>
              <td>{{ r.borrowDate }}</td>
              <td>{{ r.dueDate }}</td>
              <td><span class="badge" :class="statusClass(r.status)">{{ r.status }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDashboard } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const stats = ref(null)

function statusClass(s) {
  if (s === '已逾期') return 'badge-danger'
  if (s === '借阅中') return 'badge-success'
  return 'badge-gray'
}

onMounted(async () => {
  try { const r = await getDashboard(); stats.value = r.data } catch (e) {}
})
</script>
