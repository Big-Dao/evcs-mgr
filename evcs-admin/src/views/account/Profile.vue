<template>
  <div class="profile-page" v-loading="loading">
    <el-card shadow="never" class="basic-card">
      <template #header>
        <div class="card-header">
          <span>个人资料</span>
          <el-button type="primary" size="small" @click="saveBasic" :loading="savingBasic">保存</el-button>
        </div>
      </template>
      <el-form :model="form" label-width="90px" :rules="rules" ref="formRef">
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="登录账号">
          <el-input v-model="form.loginIdentifier" disabled />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender" placeholder="选择">
            <el-option :value="1" label="男" />
            <el-option :value="2" label="女" />
            <el-option :value="0" label="保密" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="password-card" style="margin-top:20px;">
      <template #header>
        <div class="card-header">
          <span>修改密码</span>
          <el-button type="primary" size="small" @click="changePassword" :loading="changingPwd">修改</el-button>
        </div>
      </template>
      <el-form :model="pwdForm" label-width="90px" :rules="pwdRules" ref="pwdRef">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" show-password />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getUserDetail, updateUser } from '@/api/user'

const currentUserId = Number(localStorage.getItem('userId') || '0')
const loading = ref(false)
const savingBasic = ref(false)
const changingPwd = ref(false)

const formRef = ref<FormInstance>()
const pwdRef = ref<FormInstance>()

const form = reactive({
  id: 0,
  username: '',
  loginIdentifier: '',
  realName: '',
  email: '',
  phone: '',
  gender: 0,
  status: 1,
  userType: 2,
  tenantId: 0
})

const rules: FormRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] }],
  phone: [{ min: 5, max: 20, message: '手机号长度不合法', trigger: 'blur' }]
}

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '至少6位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请再次输入', trigger: 'blur' }, {
    validator: (_r, v, cb) => { v !== pwdForm.newPassword ? cb('两次密码不一致') : cb() }, trigger: 'blur'
  }]
}

const loadProfile = async () => {
  if (!currentUserId) return
  loading.value = true
  try {
    const res = await getUserDetail(currentUserId)
    if (res.data) Object.assign(form, res.data)
  } catch (e) {
    ElMessage.error('加载个人资料失败')
  } finally { loading.value = false }
}

const saveBasic = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    savingBasic.value = true
    try {
      await updateUser(form.id, {
        username: form.username,
        loginIdentifier: form.loginIdentifier,
        realName: form.realName,
        email: form.email,
        phone: form.phone,
        gender: form.gender,
        status: form.status ?? 1,
        userType: form.userType ?? 2,
        tenantId: form.tenantId || 0
      })
      ElMessage.success('保存成功')
    } catch { ElMessage.error('保存失败') } finally { savingBasic.value = false }
  })
}

const changePassword = () => {
  pwdRef.value?.validate(async (valid) => {
    if (!valid) return
    changingPwd.value = true
    try {
      ElMessage.warning('修改密码接口尚未实现')
    } finally { changingPwd.value = false }
  })
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-page { max-width: 760px; }
.card-header { display:flex; justify-content:space-between; align-items:center; }
</style>
