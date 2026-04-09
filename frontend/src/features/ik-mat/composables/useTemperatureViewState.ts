import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useIkMatData } from './useIkMatData'

type PointModalMode = 'add' | 'edit'

interface TemperatureInstance {
  pointId: number
  title: string
  locationId: number
  locationName: string
  minTemp: number
  maxTemp: number
  latestEntryId: number | null
  latestTemp: number | null
  latestRecordedBy: string
  latestDate: string
  latestTime: string
  isAlert: boolean
}

export const useTemperatureViewState = () => {
  const router = useRouter()
  const authStore = useAuthStore()

  const {
    temperatureRecords,
    temperaturePoints,
    locations,
    orgUsers,
    isTemperatureInRange,
    createTemperaturePointWithLocation,
    updateTemperaturePointAndLocation,
    deleteTemperaturePoint,
    clearTemperatureMeasurementsForPoint,
    createTemperatureMeasurement,
    updateTemperatureMeasurement,
    isLoading,
    error,
  } = useIkMatData()

  const isMobile = ref(false)
  const actionError = ref<string | null>(null)
  const isSubmitting = ref(false)

  const desktopMenuPointId = ref<number | null>(null)
  const selectedMobileCardId = ref<number | null>(null)

  const pointModalOpen = ref(false)
  const pointModalMode = ref<PointModalMode>('add')
  const editingPointId = ref<number | null>(null)
  const editingLocationId = ref<number | null>(null)
  const originalRange = ref<{ min: number | null; max: number | null }>({ min: null, max: null })
  const pointForm = reactive({
    name: '',
    locationName: '',
    minTempC: '',
    maxTempC: '',
  })

  const measurementModalOpen = ref(false)
  const measurementPointId = ref<number | null>(null)
  const editingMeasurementEntryId = ref<number | null>(null)
  const measurementForm = reactive({
    temperatureC: '',
    measuredDate: '',
    measuredTime: '',
    employeeId: null as number | null,
    employeeName: '',
  })

  const canManage = computed(() => authStore.hasRole('ADMIN', 'MANAGER'))

  const locationById = computed(() => {
    return new Map(locations.map((location) => [location.locationId, location]))
  })

  const latestRecordByPoint = computed(() => {
    const map = new Map<number, (typeof temperatureRecords)[number]>()

    for (const record of temperatureRecords) {
      if (!map.has(record.log_point_id)) {
        map.set(record.log_point_id, record)
      }
    }

    return map
  })

  const instances = computed<TemperatureInstance[]>(() => {
    return temperaturePoints
      .filter((point) => point.isActive !== false)
      .map((point) => {
        const latest = latestRecordByPoint.value.get(point.logPointId)
        const location = locationById.value.get(point.locationId)

        const minTemp = Number(location?.tempMinC ?? 0)
        const maxTemp = Number(location?.tempMaxC ?? 4)
        const isAlert = latest ? !isTemperatureInRange(latest) : false

        return {
          pointId: point.logPointId,
          title: point.name,
          locationId: point.locationId,
          locationName: point.locationName ?? location?.name ?? 'Ukjent lokasjon',
          minTemp,
          maxTemp,
          latestEntryId: latest?.id ?? null,
          latestTemp: latest ? latest.temperature_c : null,
          latestRecordedBy: latest?.recorded_by ?? '-',
          latestDate: latest?.recorded_date ?? '-',
          latestTime: latest?.recorded_time ?? '-',
          isAlert,
        }
      })
      .sort((a, b) => {
        if (a.isAlert !== b.isAlert) {
          return a.isAlert ? -1 : 1
        }
        return a.title.localeCompare(b.title, 'nb')
      })
  })

  const alerts = computed(() => instances.value.filter((item) => item.isAlert))
  const okCount = computed(() => instances.value.filter((item) => !item.isAlert && item.latestTemp !== null).length)

  const currentMeasurementPoint = computed(() => {
    return instances.value.find((item) => item.pointId === measurementPointId.value) ?? null
  })

  const selectedLocationLabel = computed(() => {
    if (!pointForm.locationName.trim()) {
      return null
    }
    return `${pointForm.locationName.trim()} (${pointForm.minTempC || '-'} til ${pointForm.maxTempC || '-'}°C)`
  })

  const employeeOptions = computed(() => {
    return orgUsers
      .filter((user) => user.isActive)
      .map((user) => ({
        id: user.userId,
        label: user.displayName || user.email,
      }))
  })

  const updateViewport = () => {
    if (typeof window === 'undefined') {
      return
    }

    isMobile.value = window.matchMedia('(max-width: 47.99rem)').matches
  }

  const resetPointForm = () => {
    pointForm.name = ''
    pointForm.locationName = ''
    pointForm.minTempC = ''
    pointForm.maxTempC = ''
    editingLocationId.value = null
    originalRange.value = { min: null, max: null }
  }

  const openAddPointModal = () => {
    pointModalMode.value = 'add'
    editingPointId.value = null
    resetPointForm()
    actionError.value = null
    pointModalOpen.value = true
  }

  const openEditPointModal = (pointId: number) => {
    const point = temperaturePoints.find((item) => item.logPointId === pointId)
    if (!point) {
      return
    }
    const location = locationById.value.get(point.locationId)

    pointModalMode.value = 'edit'
    editingPointId.value = pointId
    pointForm.name = point.name
    pointForm.locationName = location?.name ?? point.locationName ?? ''
    pointForm.minTempC = location?.tempMinC != null ? String(location.tempMinC) : ''
    pointForm.maxTempC = location?.tempMaxC != null ? String(location.tempMaxC) : ''
    originalRange.value = {
      min: location?.tempMinC != null ? Number(location.tempMinC) : null,
      max: location?.tempMaxC != null ? Number(location.tempMaxC) : null,
    }
    editingLocationId.value = point.locationId
    actionError.value = null
    pointModalOpen.value = true
    desktopMenuPointId.value = null
    selectedMobileCardId.value = null
  }

  const closePointModal = () => {
    pointModalOpen.value = false
  }

  const savePoint = async () => {
    const minTemp = Number(pointForm.minTempC)
    const maxTemp = Number(pointForm.maxTempC)

    if (!canManage.value || !pointForm.name.trim() || !pointForm.locationName.trim()) {
      return
    }

    if (!Number.isFinite(minTemp) || !Number.isFinite(maxTemp)) {
      return
    }

    actionError.value = null
    isSubmitting.value = true

    try {
      const locationPayload = {
        name: pointForm.locationName.trim(),
        locationType: 'OTHER' as const,
        tempMinC: minTemp,
        tempMaxC: maxTemp,
        isActive: true,
      }

      const pointPayload = {
        name: pointForm.name.trim(),
        isActive: true,
      }

      if (pointModalMode.value === 'add') {
        await createTemperaturePointWithLocation(locationPayload, pointPayload)
      } else if (editingPointId.value !== null && editingLocationId.value !== null) {
        const didChangeRange = originalRange.value.min !== minTemp || originalRange.value.max !== maxTemp

        await updateTemperaturePointAndLocation(editingPointId.value, editingLocationId.value, locationPayload, {
          ...pointPayload,
          locationId: editingLocationId.value,
        })

        if (didChangeRange) {
          await clearTemperatureMeasurementsForPoint(editingPointId.value)
        }
      }

      pointModalOpen.value = false
    } catch {
      actionError.value = 'Kunne ikke lagre temperaturpunkt. Prov igjen.'
    } finally {
      isSubmitting.value = false
    }
  }

  const removePoint = async (pointId: number) => {
    if (!canManage.value) {
      return
    }

    const shouldDelete = window.confirm('Slette temperaturpunktet?')
    if (!shouldDelete) {
      return
    }

    actionError.value = null
    isSubmitting.value = true

    try {
      await deleteTemperaturePoint(pointId)
      desktopMenuPointId.value = null
      selectedMobileCardId.value = null
    } catch {
      actionError.value = 'Kunne ikke slette temperaturpunkt. Prov igjen.'
    } finally {
      isSubmitting.value = false
    }
  }

  const openMeasurementModal = (pointId: number) => {
    measurementPointId.value = pointId
    const instance = instances.value.find((item) => item.pointId === pointId)
    const now = new Date()
    const defaultDate = now.toISOString().slice(0, 10)
    const defaultTime = now.toISOString().slice(11, 16)

    measurementForm.temperatureC = instance?.latestTemp != null ? String(instance.latestTemp) : ''
    measurementForm.measuredDate = instance?.latestDate && instance.latestDate !== '-' ? instance.latestDate : defaultDate
    measurementForm.measuredTime = instance?.latestTime && instance.latestTime !== '-' ? instance.latestTime : defaultTime
    measurementForm.employeeName = instance?.latestRecordedBy && instance.latestRecordedBy !== '-' ? instance.latestRecordedBy : (authStore.user?.name ?? '')

    const matchedEmployee = employeeOptions.value.find((employee) => employee.label === measurementForm.employeeName)
    measurementForm.employeeId = matchedEmployee?.id ?? null
    editingMeasurementEntryId.value = instance?.latestEntryId ?? null

    actionError.value = null
    measurementModalOpen.value = true
    desktopMenuPointId.value = null
  }

  const closeMeasurementModal = () => {
    measurementModalOpen.value = false
    editingMeasurementEntryId.value = null
  }

  const combinedMeasuredAt = () => {
    if (!measurementForm.measuredDate || !measurementForm.measuredTime) {
      return new Date().toISOString()
    }
    return new Date(`${measurementForm.measuredDate}T${measurementForm.measuredTime}:00`).toISOString()
  }

  const submitMeasurement = async () => {
    if (measurementPointId.value === null) {
      return
    }

    const numericTemp = Number(measurementForm.temperatureC)
    if (!Number.isFinite(numericTemp)) {
      return
    }

    actionError.value = null
    isSubmitting.value = true

    try {
      const selectedEmployee = measurementForm.employeeId != null
        ? employeeOptions.value.find((employee) => employee.id === measurementForm.employeeId)
        : null
      const measuredBy = selectedEmployee?.label ?? measurementForm.employeeName.trim()
      const payload = {
        logPointId: measurementPointId.value,
        temperatureC: numericTemp,
        measuredAt: combinedMeasuredAt(),
        noteText: measuredBy ? `Malt av: ${measuredBy}` : undefined,
        recordedByUserId: measurementForm.employeeId ?? undefined,
      }

      if (editingMeasurementEntryId.value) {
        await updateTemperatureMeasurement(editingMeasurementEntryId.value, payload)
      } else {
        await createTemperatureMeasurement(payload)
      }

      measurementModalOpen.value = false
      selectedMobileCardId.value = null
    } catch {
      actionError.value = 'Kunne ikke registrere temperaturmaling. Prov igjen.'
    } finally {
      isSubmitting.value = false
    }
  }

  const openDeviationFlow = (instance: TemperatureInstance) => {
    const measuredLabel = instance.latestTemp === null ? 'ukjent temperatur' : `${instance.latestTemp}°C`
    const description = `Temperatur ved ${instance.title} (${instance.locationName}) ble malt til ${measuredLabel}. Gyldig omrade er ${instance.minTemp}°C til ${instance.maxTemp}°C.`

    void router.push({
      name: 'Deviations',
      query: {
        openCreate: '1',
        source: 'temperature',
        title: `Temperaturavvik - ${instance.title}`,
        location: instance.locationName,
        description,
        severity: 'MAJOR',
        discoverer: authStore.user?.name ?? '',
        sourceEntryId: instance.latestEntryId != null ? String(instance.latestEntryId) : undefined,
      },
    })
  }

  const toggleDesktopMenu = (pointId: number) => {
    desktopMenuPointId.value = desktopMenuPointId.value === pointId ? null : pointId
  }

  const toggleMobileActions = (pointId: number) => {
    selectedMobileCardId.value = selectedMobileCardId.value === pointId ? null : pointId
  }

  onMounted(() => {
    updateViewport()
    window.addEventListener('resize', updateViewport)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', updateViewport)
  })

  return {
    isMobile,
    actionError,
    isSubmitting,
    desktopMenuPointId,
    selectedMobileCardId,
    pointModalOpen,
    pointModalMode,
    pointForm,
    measurementModalOpen,
    measurementForm,
    editingMeasurementEntryId,
    canManage,
    alerts,
    okCount,
    instances,
    currentMeasurementPoint,
    selectedLocationLabel,
    employeeOptions,
    isLoading,
    error,
    openAddPointModal,
    openEditPointModal,
    closePointModal,
    savePoint,
    removePoint,
    openMeasurementModal,
    closeMeasurementModal,
    submitMeasurement,
    openDeviationFlow,
    toggleDesktopMenu,
    toggleMobileActions,
  }
}
