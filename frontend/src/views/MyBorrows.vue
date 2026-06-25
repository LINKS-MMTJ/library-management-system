<!-- Author: LINKS -->
<template>
  <div>
    <!-- 罚金提示 -->
    <div v-if="auth.user?.unpaidFine > 0" class="alert alert-danger">
      <span>待缴罚金：¥{{ (auth.user.unpaidFine / 100).toFixed(2) }}</span>
      <button class="btn btn-danger btn-sm" @click="showPay = true">缴纳罚金</button>
    </div>

    <!-- 当前借阅 -->
    <div class="card">
      <div class="card-title">当前借阅 ({{ unreturned.length }})</div>
      <div v-if="unreturned.length === 0" class="empty-state">暂无借阅</div>
      <div class="table-wrap" v-else>
        <table class="data-table">
          <thead><tr><th>图书</th><th>借阅日期</th><th>应还日期</th><th>状态</th><th>罚金</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="r in unreturned" :key="r.recordId">
              <td>{{ r.bookTitle }}</td>
              <td>{{ r.borrowDate }}</td>
              <td>{{ r.dueDate }}</td>
              <td><span class="badge" :class="r.statusDesc === '已逾期' ? 'badge-danger' : 'badge-success'">{{ r.statusDesc }}</span></td>
              <td>¥{{ (r.fineAmount / 100).toFixed(2) }}</td>
              <td>
                <div class="btn-group">
                  <button class="btn btn-success btn-sm" @click="doReturn(r.recordId)">归还</button>
                  <button class="btn btn-gray btn-sm" @click="doRenew(r)" :disabled="r.renewCount >= 2 || r.statusDesc === '已逾期'">续借</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 历史记录 -->
    <div class="card">
      <div class="card-title">历史记录 ({{ returned.length }})</div>
      <div v-if="returned.length === 0" class="empty-state">暂无历史记录</div>
      <div class="table-wrap" v-else>
        <table class="data-table">
          <thead><tr><th>图书</th><th>借阅日期</th><th>归还日期</th><th>罚金</th></tr></thead>
          <tbody>
            <tr v-for="r in returned" :key="r.recordId">
              <td>{{ r.bookTitle }}</td>
              <td>{{ r.borrowDate }}</td>
              <td>{{ r.returnDate || '-' }}</td>
              <td>¥{{ (r.fineAmount / 100).toFixed(2) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 缴纳罚金对话框 -->
    <div v-if="showPay" class="modal-overlay" @click.self="showPay=false">
      <div class="modal-box" style="width:360px">
        <div class="modal-title">缴纳罚金</div>
        <p style="margin-bottom:14px">当前欠款: <strong>¥{{ (auth.user?.unpaidFine / 100).toFixed(2) }}</strong></p>
        <div class="form-group"><label class="form-label">缴纳金额</label><input class="form-input" type="number" v-model="payAmount" /></div>
        <div v-if="error" class="alert alert-danger">{{ error }}</div>
        <div class="modal-actions">
          <button class="btn btn-outline" @click="showPay=false">取消</button>
          <button class="btn btn-danger" @click="doPay">确认缴纳</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMyBorrows, returnBookById, renewBookById, payFine } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const emit = defineEmits(['toast'])
const records = ref([])
const showPay = ref(false)
const payAmount = ref(0)
const error = ref('')

const unreturned = computed(() => records.value.filter(r => r.status !== 'RETURNED'))
const returned = computed(() => records.value.filter(r => r.status === 'RETURNED'))

async function loadRecords() {
  try { const r = await getMyBorrows(); records.value = r.data } catch (e) { console.error('MyBorrows load failed:', e) }
}
async function doReturn(id) {
  if (!confirm('确认归还该图书？如有逾期将自动计算罚金。')) return
  try { await returnBookById(id); emit('toast', '归还成功'); await Promise.all([loadRecords(), auth.refreshUser()]) } catch (e) { emit('toast', e.message) }
}
async function doRenew(r) {
  if (r.renewCount >= 2) { emit('toast', '已达到最大续借次数'); return }
  if (r.statusDesc === '已逾期') { emit('toast', '已逾期图书无法续借'); return }
  if (!confirm('确认续借？可延长15天。')) return
  try { await renewBookById(r.recordId); emit('toast', '续借成功'); await loadRecords() } catch (e) { emit('toast', e.message) }
}
async function doPay() {
  error.value = ''
  try {
    await payFine(auth.user.userId, Math.round(payAmount.value * 100))
    emit('toast', '缴纳成功')
    showPay.value = false
    await auth.refreshUser()
  } catch (e) { error.value = e.message }
}
onMounted(() => { loadRecords(); payAmount.value = (auth.user?.unpaidFine || 0) / 100 })
</script>
