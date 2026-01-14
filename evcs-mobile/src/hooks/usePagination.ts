import { ref, computed } from 'vue'

export interface PaginationOptions {
  pageSize?: number
  immediate?: boolean
}

export function usePagination<T>(
  fetchFn: (params: { current: number; size: number }) => Promise<{ records: T[]; total: number }>,
  options: PaginationOptions = {}
) {
  const { pageSize = 10, immediate = false } = options
  
  const list = ref<T[]>([]) as Ref<T[]>
  const loading = ref(false)
  const refreshing = ref(false)
  const currentPage = ref(1)
  const total = ref(0)
  
  const noMore = computed(() => list.value.length >= total.value && total.value > 0)
  const isEmpty = computed(() => !loading.value && list.value.length === 0)
  
  async function loadData(isRefresh = false) {
    if (loading.value) return
    
    if (isRefresh) {
      currentPage.value = 1
      refreshing.value = true
    }
    
    loading.value = true
    
    try {
      const result = await fetchFn({
        current: currentPage.value,
        size: pageSize
      })
      
      if (isRefresh) {
        list.value = result.records
      } else {
        list.value = [...list.value, ...result.records]
      }
      
      total.value = result.total
    } catch (error) {
      console.error('Load data error:', error)
    } finally {
      loading.value = false
      refreshing.value = false
    }
  }
  
  async function refresh() {
    await loadData(true)
  }
  
  async function loadMore() {
    if (noMore.value || loading.value) return
    currentPage.value++
    await loadData()
  }
  
  function reset() {
    list.value = []
    currentPage.value = 1
    total.value = 0
  }
  
  if (immediate) {
    loadData()
  }
  
  return {
    list,
    loading,
    refreshing,
    currentPage,
    total,
    noMore,
    isEmpty,
    loadData,
    refresh,
    loadMore,
    reset
  }
}

import type { Ref } from 'vue'
