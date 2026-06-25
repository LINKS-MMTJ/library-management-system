<template>
  <div>
    <div class="toolbar">
      <div></div>
      <button class="btn btn-primary btn-sm" @click="openDialog(null)">新建用户</button>
    </div>

    <div class="card">
      <div v-if="users.length === 0" class="empty-state">暂无用户</div>
      <div class="table-wrap" v-else>
        <table class="data-table">
          <thead><tr><th>ID</th><th>用户名</th><th>姓名</th><th>角色</th><th>状态</th><th>罚金</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="u in users" :key="u.userId">
              <td>{{ u.userId }}</td>
              <td>{{ u.username }}</td>
              <td>{{ u.name }}</td>
              <td>{{ roleLabel(u.role) }}</td>
              <td><span class="badge" :class="u.status === 'ACTIVE' ? 'badge-success' : 'badge-gray'">{{ u.status === 'ACTIVE' ? '正常' : '禁用' }}</span></td>
              <td>¥{{ u.unpaidFine?.toFixed(2) }}</td>
              <td>
                <div class="btn-group">
                  <button class="btn btn-gray btn-sm" @click="openDialog(u.userId)">编辑</button>
                  <button v-if="u.status==='ACTIVE'" class="btn btn-warning btn-sm" @click="toggleUser(u, 'disable')">禁用</button>
                  <button v-else class="btn btn-success btn-sm" @click="toggleUser(u, 'enable')">启用</button>
                  <button class="btn btn-danger btn-sm" @click="delUser(u)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 用户对话框 -->
    <div v-if="showDialog" class="modal-overlay" @click.self="showDialog=false">
      <div class="modal-box" style="width:420px">
        <div class="modal-title">{{ editingId ? '编辑用户' : '新建用户' }}</div>
        <div class="form-group" v-if="!editingId"><label class="form-label">用户名 *</label><input class="form-input" v-model="form.username" /></div>
        <div class="form-group" v-if="!editingId"><label class="form-label">密码 *</label><input class="form-input" type="password" v-model="form.password" /></div>
        <div class="form-group"><label class="form-label">姓名</label><input class="form-input" v-model="form.name" /></div>
        <div class="form-group"><label class="form-label">邮箱</label><input class="form-input" v-model="form.email" /></div>
        <div class="form-group"><label class="form-label">电话</label><input class="form-input" v-model="form.phone" /></div>
        <div class="form-group"><label class="form-label">角色</label>
          <select class="form-input" v-model="form.role">
            <option value="BORROWER">借阅者</option>
            <option value="LIBRARIAN">图书管理员</option>
            <option value="ADMIN">系统管理员</option>
          </select>
        </div>
        <div class="form-group" v-if="editingId"><label class="form-label">状态</label>
          <select class="form-input" v-model="form.status">
            <option value="ACTIVE">正常</option>
            <option value="INACTIVE">禁用</option>
          </select>
        </div>
        <div class="form-group" v-if="editingId"><label class="form-label">新密码（留空不修改）</label><input class="form-input" type="password" v-model="form.newPassword" /></div>
        <div v-if="error" class="alert alert-danger">{{ error }}</div>
        <div class="modal-actions">
          <button class="btn btn-outline" @click="showDialog=false">取消</button>
          <button class="btn btn-primary" @click="saveUser">{{ editingId ? '保存' : '创建' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUsers, getUser, createUser, updateUser, deleteUser, disableUser, enableUser } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const emit = defineEmits(['toast'])
const users = ref([])
const showDialog = ref(false)
const editingId = ref(null)
const error = ref('')
const form = ref({ username:'', password:'', name:'', email:'', phone:'', role:'BORROWER', status:'ACTIVE', newPassword:'' })

function roleLabel(r) {
  if (r === 'ADMIN') return '系统管理员'
  if (r === 'LIBRARIAN') return '图书管理员'
  return '借阅者'
}
async function loadUsers() {
  try { const r = await getUsers(); users.value = r.data } catch (e) {}
}
function openDialog(id) {
  error.value = ''
  if (id) {
    editingId.value = id
    getUser(id).then(r => {
      form.value = { ...r.data, password: '', newPassword: '', status: r.data.status || 'ACTIVE', role: r.data.role || 'BORROWER' }
    }).catch(e => {})
  } else {
    editingId.value = null
    form.value = { username:'', password:'', name:'', email:'', phone:'', role:'BORROWER', status:'ACTIVE', newPassword:'' }
  }
  showDialog.value = true
}
async function saveUser() {
  error.value = ''
  try {
    if (editingId.value) {
      const data = { name: form.value.name, email: form.value.email, phone: form.value.phone, role: form.value.role, status: form.value.status }
      if (form.value.newPassword) data.password = form.value.newPassword
      await updateUser(editingId.value, data)
      emit('toast', '用户信息已更新')
    } else {
      if (!form.value.username || !form.value.password) { error.value = '用户名和密码不能为空'; return }
      if (form.value.password.length < 6) { error.value = '密码至少需要6位'; return }
      await createUser(form.value)
      emit('toast', '用户创建成功')
    }
    showDialog.value = false
    await loadUsers()
  } catch (e) { error.value = e.message }
}
async function toggleUser(u, action) {
  try {
    if (action === 'disable') await disableUser(u.userId)
    else await enableUser(u.userId)
    emit('toast', action === 'disable' ? '用户已禁用' : '用户已启用')
    await loadUsers()
  } catch (e) { emit('toast', e.message) }
}
async function delUser(u) {
  if (!confirm(`确认删除用户「${u.username}」？此操作不可恢复。`)) return
  try {
    await deleteUser(u.userId)
    emit('toast', '用户已删除')
    await loadUsers()
  } catch (e) { emit('toast', e.message) }
}
onMounted(loadUsers)
</script>
