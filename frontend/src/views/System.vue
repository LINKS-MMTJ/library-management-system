<template>
  <div>
    <div class="card">
      <div class="card-title">系统操作</div>

      <!-- 长期未还提醒 -->
      <div style="margin-bottom: 20px">
        <div style="display:flex;align-items:center;gap:12px;margin-bottom:8px">
          <button class="btn btn-warning" @click="doOverdue">发送长期未还提醒</button>
          <span v-if="overdueResult" class="badge" :class="overdueResult.overdueCount > 0 ? 'badge-danger' : 'badge-success'">
            {{ overdueResult.message }}
          </span>
        </div>
        <div class="text-gray" style="font-size:12px;line-height:1.6">
          借阅超过 <strong>3个月（90天）</strong> 的用户 → 发送超期提醒<br/>
          借阅超过 <strong>2.5个月（75天）</strong> 的用户 → 发送即将到期提醒
        </div>
      </div>

      <hr style="border:none;border-top:1px solid var(--border-light);margin:16px 0" />

      <!-- 缺货通知 -->
      <div>
        <div style="display:flex;align-items:center;gap:12px;margin-bottom:8px">
          <button class="btn btn-gray" @click="doOutOfStock">发送缺货通知</button>
          <span v-if="oosResult" class="badge badge-gray">
            {{ oosResult.message }}
          </span>
        </div>
        <div class="text-gray" style="font-size:12px;line-height:1.6">
          向等待超过 30 天的预约用户发送缺货通知<br/>
          同时抄送所有管理员/图书管理员
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { sendOverdueReminders, sendOutOfStock } from '../api'
const emit = defineEmits(['toast'])

const overdueResult = ref(null)
const oosResult = ref(null)

async function doOverdue() {
  try {
    const r = await sendOverdueReminders()
    overdueResult.value = r.data
    emit('toast', r.data.message)
  } catch (e) { emit('toast', e.message) }
}
async function doOutOfStock() {
  try {
    const r = await sendOutOfStock()
    oosResult.value = r.data
    emit('toast', r.data.message)
  } catch (e) { emit('toast', e.message) }
}
</script>
