import { computed, reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useIkMatData } from './useIkMatData'
import type { HaccpPoint, SupportingDocument } from '../types'

export const useHaccpViewState = () => {
  const authStore = useAuthStore()

  const {
    haccpPlan,
    createHaccpControlPoint,
    updateHaccpControlPoint,
    deleteHaccpControlPoint,
    uploadSupportingDocument,
    updateSupportingDocument,
    deleteSupportingDocument,
    downloadSupportingDocument,
  } = useIkMatData()

  const canAdmin = computed(() => authStore.hasRole('ADMIN'))

  const pointModalOpen = ref(false)
  const pointMode = ref<'create' | 'edit'>('create')
  const pointError = ref<string | null>(null)
  const pointInFlight = ref(false)
  const editingPointId = ref<number | null>(null)

  const pointForm = reactive({
    name: '',
    description: '',
    hazardsText: 'Biologisk fare, Temperaturavvik',
    critical_limits: '',
    monitoring: 'Daglig',
    corrective_actions: 'Registrer avvik og gjennomfor korrigerende tiltak',
    verification: 'Daglig gjennomgang av ansvarlig leder',
    responsible: 'Driftsansvarlig',
  })

  const docUploadModalOpen = ref(false)
  const docEditModalOpen = ref(false)
  const docOpenModalOpen = ref(false)
  const docError = ref<string | null>(null)
  const docInFlight = ref(false)
  const selectedDocId = ref<number | null>(null)
  const selectedDocName = ref('')
  const openDocMenuId = ref<number | null>(null)
  const openPointMenuId = ref<number | null>(null)

  const docForm = reactive({
    title: '',
    description: '',
    file: null as File | null,
  })

  const ccpStatusSummary = computed(() => {
    const total = haccpPlan.critical_control_points.length
    const followUp = haccpPlan.critical_control_points.filter((point) => {
      return point.name.toLowerCase().includes('varmholding') || point.name.toLowerCase().includes('nedkjøling')
    }).length

    return {
      total,
      followUp,
      ok: total - followUp,
    }
  })

  const resetPointForm = () => {
    pointForm.name = ''
    pointForm.description = ''
    pointForm.hazardsText = 'Biologisk fare, Temperaturavvik'
    pointForm.critical_limits = ''
    pointForm.monitoring = 'Daglig'
    pointForm.corrective_actions = 'Registrer avvik og gjennomfor korrigerende tiltak'
    pointForm.verification = 'Daglig gjennomgang av ansvarlig leder'
    pointForm.responsible = 'Driftsansvarlig'
  }

  const openCreatePointModal = () => {
    pointMode.value = 'create'
    editingPointId.value = null
    resetPointForm()
    pointError.value = null
    pointModalOpen.value = true
  }

  const openEditPointModal = (point: HaccpPoint) => {
    openPointMenuId.value = null
    pointMode.value = 'edit'
    editingPointId.value = point.id
    pointForm.name = point.name
    pointForm.description = point.description
    pointForm.hazardsText = point.hazards.join(', ')
    pointForm.critical_limits = point.critical_limits
    pointForm.monitoring = point.monitoring
    pointForm.corrective_actions = point.corrective_actions
    pointForm.verification = point.verification
    pointForm.responsible = point.responsible
    pointError.value = null
    pointModalOpen.value = true
  }

  const submitPoint = async () => {
    if (!pointForm.name.trim() || !pointForm.critical_limits.trim()) {
      return
    }

    pointError.value = null
    pointInFlight.value = true

    try {
      const payload = {
        name: pointForm.name.trim(),
        description: pointForm.description.trim() || 'Ingen beskrivelse registrert',
        hazards: pointForm.hazardsText.split(',').map((s) => s.trim()).filter(Boolean),
        critical_limits: pointForm.critical_limits.trim(),
        monitoring: pointForm.monitoring.trim() || 'Daglig',
        corrective_actions: pointForm.corrective_actions.trim() || 'Registrer avvik og gjennomfor korrigerende tiltak',
        verification: pointForm.verification.trim() || 'Daglig gjennomgang av ansvarlig leder',
        responsible: pointForm.responsible.trim() || 'Driftsansvarlig',
      }

      if (pointMode.value === 'create') {
        await createHaccpControlPoint(payload)
      } else if (editingPointId.value != null) {
        await updateHaccpControlPoint(editingPointId.value, payload)
      }

      pointModalOpen.value = false
    } catch {
      pointError.value = 'Kunne ikke lagre kontrollpunkt. Prov igjen.'
    } finally {
      pointInFlight.value = false
    }
  }

  const removePoint = async (id: number) => {
    if (!canAdmin.value) return
    if (!window.confirm('Slette dette kontrollpunktet?')) return
    try {
      await deleteHaccpControlPoint(id)
      openPointMenuId.value = null
    } catch {
      pointError.value = 'Kunne ikke slette kontrollpunkt.'
    }
  }

  const togglePointMenu = (pointId: number) => {
    openPointMenuId.value = openPointMenuId.value === pointId ? null : pointId
  }

  const openDocUploadModal = () => {
    docForm.title = ''
    docForm.description = ''
    docForm.file = null
    docError.value = null
    docUploadModalOpen.value = true
  }

  const handleDocFile = (event: Event) => {
    const target = event.target as HTMLInputElement
    const file = target.files?.[0] ?? null
    docForm.file = file
    if (file && file.type !== 'application/pdf') {
      docError.value = 'Kun PDF-filer er tillatt.'
      docForm.file = null
    }
  }

  const submitDocUpload = async () => {
    if (!docForm.file) return
    docError.value = null
    docInFlight.value = true
    try {
      await uploadSupportingDocument(docForm.file, docForm.title || docForm.file.name, docForm.description)
      docUploadModalOpen.value = false
    } catch {
      docError.value = 'Kunne ikke laste opp dokumentet.'
    } finally {
      docInFlight.value = false
    }
  }

  const openDocModal = (doc: SupportingDocument) => {
    openDocMenuId.value = null
    selectedDocId.value = doc.id
    selectedDocName.value = doc.name
    docOpenModalOpen.value = true
  }

  const toggleDocMenu = (docId: number) => {
    openDocMenuId.value = openDocMenuId.value === docId ? null : docId
  }

  const downloadCurrentDoc = async () => {
    if (selectedDocId.value == null) return
    const blob = await downloadSupportingDocument(selectedDocId.value)
    const url = URL.createObjectURL(blob)
    window.open(url, '_blank', 'noopener,noreferrer')
  }

  const openEditDocModal = (doc: SupportingDocument) => {
    openDocMenuId.value = null
    selectedDocId.value = doc.id
    docForm.title = doc.name
    docForm.description = doc.description
    docError.value = null
    docEditModalOpen.value = true
  }

  const submitDocEdit = async () => {
    if (selectedDocId.value == null || !docForm.title.trim()) return
    docError.value = null
    docInFlight.value = true
    try {
      await updateSupportingDocument(selectedDocId.value, {
        title: docForm.title.trim(),
        description: docForm.description.trim() || '',
      })
      docEditModalOpen.value = false
    } catch {
      docError.value = 'Kunne ikke oppdatere dokument.'
    } finally {
      docInFlight.value = false
    }
  }

  const removeDocument = async (docId: number) => {
    if (!canAdmin.value) return
    if (!window.confirm('Slette dette dokumentet?')) return
    try {
      await deleteSupportingDocument(docId)
      openDocMenuId.value = null
    } catch {
      docError.value = 'Kunne ikke slette dokument.'
    }
  }

  return {
    haccpPlan,
    canAdmin,
    pointModalOpen,
    pointMode,
    pointError,
    pointInFlight,
    pointForm,
    docUploadModalOpen,
    docEditModalOpen,
    docOpenModalOpen,
    docError,
    docInFlight,
    selectedDocName,
    openDocMenuId,
    openPointMenuId,
    docForm,
    ccpStatusSummary,
    openCreatePointModal,
    openEditPointModal,
    submitPoint,
    removePoint,
    togglePointMenu,
    openDocUploadModal,
    handleDocFile,
    submitDocUpload,
    openDocModal,
    toggleDocMenu,
    downloadCurrentDoc,
    openEditDocModal,
    submitDocEdit,
    removeDocument,
  }
}
