import { computed } from 'vue'
import { useIkMatData } from './useIkMatData'

export const useIkMatDashboardState = () => {
  const {
    dashboardStats,
    recentChecks,
    checklists,
    temperatureRecords,
    deviations,
    completionForChecklist,
    isTemperatureInRange,
    formatDate,
    isLoading,
    error,
    reload,
  } = useIkMatData()

  const openDeviations = computed(() => deviations.filter((item) => item.status !== 'resolved'))

  const checklistCompletion = computed(() => {
    if (checklists.length === 0) {
      return 0
    }

    const sum = checklists.reduce((acc, checklist) => acc + completionForChecklist(checklist), 0)
    return Math.round(sum / checklists.length)
  })

  const temperatureAlerts = computed(() => {
    return temperatureRecords.filter((record) => !isTemperatureInRange(record))
  })

  const cardTone = (color: 'success' | 'warning' | 'info') => {
    if (color === 'success') {
      return 'stat-card--success'
    }

    if (color === 'warning') {
      return 'stat-card--warning'
    }

    return 'stat-card--info'
  }

  return {
    dashboardStats,
    recentChecks,
    temperatureRecords,
    openDeviations,
    checklistCompletion,
    temperatureAlerts,
    isTemperatureInRange,
    formatDate,
    isLoading,
    error,
    reload,
    cardTone,
  }
}
