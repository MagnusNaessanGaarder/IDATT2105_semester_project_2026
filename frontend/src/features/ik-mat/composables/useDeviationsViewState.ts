import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useIkMatData } from './useIkMatData'

export const useDeviationsViewState = () => {
  const route = useRoute()
  const router = useRouter()

  const authStore = useAuthStore()
  const { deviations, createDeviation, updateDeviation, deleteDeviation, resolveDeviation, startDeviationHandling } = useIkMatData()

  const selectedStatus = ref<'all' | 'open' | 'in-progress' | 'resolved'>('all')
  const selectedId = ref<number | null>(deviations[0]?.id ?? null)
  const createModalOpen = ref(false)
  const editModalOpen = ref(false)
  const createError = ref<string | null>(null)
  const editError = ref<string | null>(null)
  const createInFlight = ref(false)
  const editInFlight = ref(false)

  const createForm = reactive({
    title: '',
    description: '',
    locationText: '',
    severity: 'MAJOR' as 'MINOR' | 'MAJOR' | 'CRITICAL',
    discoveredByName: '',
    occurredDate: '',
    occurredTime: '',
    sourceTemperatureEntryId: null as number | null,
  })

  const editForm = reactive({
    id: null as number | null,
    title: '',
    description: '',
    locationText: '',
    severity: 'MAJOR' as 'MINOR' | 'MAJOR' | 'CRITICAL',
    discoveredByName: '',
    occurredDate: '',
    occurredTime: '',
  })

  const canAdmin = computed(() => authStore.hasRole('ADMIN'))
  const canManageDeviation = computed(() => authStore.hasRole('ADMIN', 'MANAGER'))

  const filtered = computed(() => {
    if (selectedStatus.value === 'all') {
      return deviations
    }

    return deviations.filter((item) => item.status === selectedStatus.value)
  })

  const selectedDeviation = computed(() => {
    return filtered.value.find((item) => item.id === selectedId.value) ?? filtered.value[0] ?? null
  })

  const canEditSelected = computed(() => {
    return Boolean(canAdmin.value && selectedDeviation.value && selectedDeviation.value.status !== 'resolved')
  })

  const canResolveSelected = computed(() => {
    return Boolean(canManageDeviation.value && selectedDeviation.value && selectedDeviation.value.status === 'in-progress')
  })

  const statusLabel = (status: 'open' | 'in-progress' | 'resolved') => {
    if (status === 'open') {
      return 'Åpen'
    }

    if (status === 'in-progress') {
      return 'Pågår'
    }

    return 'Løst'
  }

  const resetCreateForm = () => {
    createForm.title = ''
    createForm.description = ''
    createForm.locationText = ''
    createForm.severity = 'MAJOR'
    createForm.discoveredByName = ''
    createForm.occurredDate = ''
    createForm.occurredTime = ''
    createForm.sourceTemperatureEntryId = null
  }

  const clearCreateQuery = () => {
    const nextQuery = { ...route.query }
    delete nextQuery.openCreate
    delete nextQuery.source
    delete nextQuery.title
    delete nextQuery.description
    delete nextQuery.location
    delete nextQuery.severity
    delete nextQuery.discoverer
    delete nextQuery.sourceEntryId

    void router.replace({ query: nextQuery })
  }

  const openCreateModal = () => {
    createError.value = null
    createModalOpen.value = true
  }

  const closeCreateModal = () => {
    createModalOpen.value = false
    createError.value = null
    clearCreateQuery()
  }

  const openEditModal = () => {
    if (!selectedDeviation.value || !canAdmin.value) return
    if (selectedDeviation.value.status === 'resolved') {
      editError.value = 'Lukkede avvik kan ikke redigeres.'
      return
    }
    editError.value = null
    editForm.id = selectedDeviation.value.id
    editForm.title = selectedDeviation.value.title
    editForm.description = selectedDeviation.value.description
    editForm.locationText = selectedDeviation.value.location
    editForm.severity = selectedDeviation.value.severity === 'high' ? 'CRITICAL' : selectedDeviation.value.severity === 'medium' ? 'MAJOR' : 'MINOR'
    editForm.discoveredByName = selectedDeviation.value.reported_by
    editForm.occurredDate = selectedDeviation.value.reported_date || ''
    editForm.occurredTime = selectedDeviation.value.reported_time || ''
    editModalOpen.value = true
  }

  const closeEditModal = () => {
    editModalOpen.value = false
    editError.value = null
  }

  const startSelectedDeviationHandling = async () => {
    if (!selectedDeviation.value || !canManageDeviation.value) {
      return
    }

    if (selectedDeviation.value.status !== 'open') {
      editError.value = 'Avviket ma vaere apent for a starte behandling.'
      return
    }

    editError.value = null
    try {
      await startDeviationHandling(selectedDeviation.value.id)
    } catch (err: unknown) {
      const apiError = err as { response?: { data?: { message?: string } } }
      editError.value = apiError?.response?.data?.message ?? 'Kunne ikke starte behandling av avvik. Prov igjen.'
    }
  }

  const submitDeviation = async () => {
    if (!createForm.title.trim() || !createForm.description.trim()) {
      return
    }

    createError.value = null
    createInFlight.value = true

    try {
      const created = await createDeviation({
        reportType: 'INCIDENT',
        severity: createForm.severity,
        title: createForm.title.trim(),
        description: createForm.description.trim(),
        locationText: createForm.locationText.trim() || undefined,
        discoveredByName: createForm.discoveredByName.trim() || undefined,
        occurredDate: createForm.occurredDate || undefined,
        occurredTime: createForm.occurredTime || undefined,
        sourceTemperatureEntryId: createForm.sourceTemperatureEntryId ?? undefined,
      })

      selectedStatus.value = 'all'
      selectedId.value = created.reportId
      createModalOpen.value = false
      clearCreateQuery()
      resetCreateForm()
    } catch {
      createError.value = 'Kunne ikke opprette avvik. Prov igjen.'
    } finally {
      createInFlight.value = false
    }
  }

  const submitEditDeviation = async () => {
    if (!editForm.id || !editForm.title.trim() || !editForm.description.trim()) {
      return
    }

    editError.value = null
    editInFlight.value = true

    try {
      await updateDeviation(editForm.id, {
        reportType: 'INCIDENT',
        severity: editForm.severity,
        title: editForm.title.trim(),
        description: editForm.description.trim(),
        locationText: editForm.locationText.trim() || undefined,
        discoveredByName: editForm.discoveredByName.trim() || undefined,
        occurredDate: editForm.occurredDate || undefined,
        occurredTime: editForm.occurredTime || undefined,
      })
      closeEditModal()
    } catch (err: unknown) {
      const apiError = err as { response?: { data?: { message?: string } } }
      if (apiError?.response?.data?.message) {
        editError.value = apiError.response.data.message
      } else {
        editError.value = 'Kunne ikke oppdatere avvik. Prov igjen.'
      }
    } finally {
      editInFlight.value = false
    }
  }

  const removeSelectedDeviation = async () => {
    if (!selectedDeviation.value || !canAdmin.value) {
      return
    }

    const shouldDelete = window.confirm('Slette dette avviket?')
    if (!shouldDelete) {
      return
    }

    try {
      await deleteDeviation(selectedDeviation.value.id)
      selectedId.value = deviations[0]?.id ?? null
    } catch {
      editError.value = 'Kunne ikke slette avvik. Prov igjen.'
    }
  }

  const resolveSelectedDeviation = async () => {
    if (!selectedDeviation.value || !canManageDeviation.value) {
      return
    }

    if (selectedDeviation.value.status !== 'in-progress') {
      editError.value = 'Avviket ma vaere under behandling for a kunne loses.'
      return
    }

    try {
      await resolveDeviation(selectedDeviation.value.id)
    } catch (err: unknown) {
      const apiError = err as { response?: { data?: { message?: string } } }
      editError.value = apiError?.response?.data?.message ?? 'Kunne ikke løse avvik. Prov igjen.'
    }
  }

  watch(
    () => route.query,
    (query) => {
      if (query.openCreate !== '1') {
        return
      }

      createForm.title = typeof query.title === 'string' ? query.title : ''
      createForm.description = typeof query.description === 'string' ? query.description : ''
      createForm.locationText = typeof query.location === 'string' ? query.location : ''
      createForm.severity = query.severity === 'CRITICAL' ? 'CRITICAL' : query.severity === 'MINOR' ? 'MINOR' : 'MAJOR'
      createForm.discoveredByName = typeof query.discoverer === 'string' ? query.discoverer : ''
      createForm.sourceTemperatureEntryId = typeof query.sourceEntryId === 'string' ? Number(query.sourceEntryId) : null
      createForm.occurredDate = new Date().toISOString().slice(0, 10)
      createForm.occurredTime = new Date().toISOString().slice(11, 16)
      openCreateModal()
    },
    { immediate: true },
  )

  return {
    selectedStatus,
    selectedId,
    createModalOpen,
    editModalOpen,
    createError,
    editError,
    createInFlight,
    editInFlight,
    createForm,
    editForm,
    canAdmin,
    canManageDeviation,
    canEditSelected,
    canResolveSelected,
    filtered,
    selectedDeviation,
    statusLabel,
    openCreateModal,
    closeCreateModal,
    openEditModal,
    closeEditModal,
    startSelectedDeviationHandling,
    submitDeviation,
    submitEditDeviation,
    removeSelectedDeviation,
    resolveSelectedDeviation,
  }
}
