<template>
  <div>
    <div class="toolbar">
      <div class="search-group">
        <input class="search-input" v-model="keyword" placeholder="搜索书名/作者/ISBN..." @keyup.enter="search" />
        <button class="btn btn-primary btn-sm" @click="search">搜索</button>
        <button class="btn btn-outline btn-sm" @click="keyword='';search()">清空</button>
      </div>
      <button v-if="auth.canManage" class="btn btn-primary btn-sm" @click="openDialog(null)">新书上架</button>
    </div>

    <div class="card">
      <div v-if="books.length === 0" class="empty-state">暂无图书</div>
      <div class="table-wrap" v-else>
        <table class="data-table">
          <thead>
            <tr><th>ISBN</th><th>书名</th><th>作者</th><th>分类</th><th>位置</th><th>馆藏/可借</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="b in books" :key="b.bookId">
              <td>{{ b.isbn }}</td>
              <td>{{ b.title }}</td>
              <td>{{ b.author }}</td>
              <td>{{ b.category }}</td>
              <td>{{ b.location }}</td>
              <td :style="{ color: (b.availableCopies||0) > 0 ? 'var(--success)' : 'var(--danger)', fontWeight: 600 }">
                {{ b.totalCopies || 0 }} / {{ b.availableCopies || 0 }}
              </td>
              <td>
                <div class="btn-group">
                  <template v-if="auth.canManage">
                    <button class="btn btn-gray btn-sm" @click="openDialog(b.bookId)">编辑</button>
                    <button class="btn btn-danger btn-sm" @click="confirmRemove(b)">下架</button>
                  </template>
                  <template v-if="auth.isBorrower">
                    <button v-if="(b.availableCopies||0) > 0" class="btn btn-primary btn-sm" @click="doBorrow(b.bookId)">借阅</button>
                    <button v-else class="btn btn-warning btn-sm" @click="doReserve(b.bookId)">预约</button>
                  </template>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="count-label">共 {{ books.length }} 本图书</div>
    </div>

    <!-- 图书对话框 -->
    <div v-if="showDialog" class="modal-overlay" @click.self="showDialog = false">
      <div class="modal-box">
        <div class="modal-title">{{ editingId ? '编辑图书' : '新书上架' }}</div>
        <div class="form-group"><label class="form-label">ISBN *</label><input class="form-input" v-model="form.isbn" :disabled="!!editingId" /></div>
        <div class="form-group"><label class="form-label">书名 *</label><input class="form-input" v-model="form.title" /></div>
        <div class="form-group"><label class="form-label">作者</label><input class="form-input" v-model="form.author" /></div>
        <div class="form-group"><label class="form-label">出版社</label><input class="form-input" v-model="form.publisher" /></div>
        <div class="form-group"><label class="form-label">分类</label><input class="form-input" v-model="form.category" /></div>
        <div class="form-group"><label class="form-label">馆藏位置</label><input class="form-input" v-model="form.location" /></div>
        <div class="form-group"><label class="form-label">出版日期</label><input class="form-input" v-model="form.publishDate" placeholder="yyyy-MM-dd" /></div>
        <div class="form-group" v-if="!editingId"><label class="form-label">入库数量</label><input class="form-input" type="number" v-model="form.quantity" /></div>
        <div v-if="error" class="alert alert-danger">{{ error }}</div>
        <div class="modal-actions">
          <button class="btn btn-outline" @click="showDialog=false">取消</button>
          <button class="btn btn-primary" @click="saveBook">{{ editingId ? '保存' : '上架' }}</button>
        </div>
      </div>
    </div>

    <!-- 下架对话框 -->
    <div v-if="showRemove" class="modal-overlay" @click.self="showRemove=false">
      <div class="modal-box" style="width:380px">
        <div class="modal-title">下架《{{ removeTarget?.title }}》</div>
        <div class="form-group"><label class="form-label">下架数量</label><input class="form-input" type="number" v-model="removeQty" /></div>
        <div class="form-group"><label class="form-label">原因</label><input class="form-input" v-model="removeReason" /></div>
        <div v-if="error" class="alert alert-danger">{{ error }}</div>
        <div class="modal-actions">
          <button class="btn btn-outline" @click="showRemove=false">取消</button>
          <button class="btn btn-danger" @click="doRemove">确认下架</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getBooks, getBook, addBook, updateBook, removeBook, borrowBook, reserveBook } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const emit = defineEmits(['toast'])
const books = ref([])
const keyword = ref('')
const showDialog = ref(false)
const showRemove = ref(false)
const editingId = ref(null)
const error = ref('')
const form = ref({ isbn:'', title:'', author:'', publisher:'', category:'', location:'', publishDate:'', quantity:1 })
const removeTarget = ref(null)
const removeQty = ref(1)
const removeReason = ref('')

async function search() { await loadBooks() }
async function loadBooks() {
  try { const r = await getBooks(keyword.value || undefined); books.value = r.data } catch (e) { console.error('Books load failed:', e) }
}
function openDialog(id) {
  error.value = ''
  if (id) {
    editingId.value = id
    getBook(id).then(r => { form.value = { ...r.data, quantity: 1, publishDate: r.data.publishDate || '' } }).catch(e => { console.error('Book detail load failed:', e) })
  } else {
    editingId.value = null
    form.value = { isbn:'', title:'', author:'', publisher:'', category:'', location:'', publishDate:'', quantity:1 }
  }
  showDialog.value = true
}
async function saveBook() {
  error.value = ''
  if (!form.value.isbn || !form.value.title) { error.value = 'ISBN和书名不能为空'; return }
  try {
    if (editingId.value) {
      await updateBook(editingId.value, form.value)
      emit('toast', '图书信息已更新')
    } else {
      await addBook(form.value)
      emit('toast', '新书上架成功')
    }
    showDialog.value = false
    await loadBooks()
  } catch (e) { error.value = e.message }
}
function confirmRemove(b) { removeTarget.value = b; removeQty.value = 1; removeReason.value = ''; error.value = ''; showRemove.value = true }
async function doRemove() {
  error.value = ''
  try {
    await removeBook(removeTarget.value.bookId, { quantity: removeQty.value, reason: removeReason.value })
    emit('toast', '图书已下架')
    showRemove.value = false
    await loadBooks()
  } catch (e) { error.value = e.message }
}
async function doBorrow(id) {
  try { await borrowBook(id); emit('toast', '借阅成功'); await loadBooks() } catch (e) { emit('toast', e.message) }
}
async function doReserve(id) {
  try { await reserveBook(id); emit('toast', '预约成功'); await loadBooks() } catch (e) { emit('toast', e.message) }
}
onMounted(loadBooks)
</script>
