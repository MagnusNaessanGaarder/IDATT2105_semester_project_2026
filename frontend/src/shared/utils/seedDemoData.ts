import { client } from '@/api/client'
import { getOrgNumber, withOrgNumber } from '@/shared/utils/orgContext'

const SEEDED_SESSION_KEY = 'demo_data_seeded_v1'

let seedInFlight: Promise<void> | null = null

interface LocationApi {
  locationId: number
}

interface TemplateApi {
  templateId: number
}

interface RunApi {
  runId: number
}

interface PointApi {
  logPointId: number
}

interface EntryApi {
  entryId: number
}

interface DeviationApi {
  reportId: number
}

interface ExportPageApi {
  content: Array<{ exportJobId: number }>
}

const isFulfilled = <T>(result: PromiseSettledResult<T>): result is PromiseFulfilledResult<T> => {
  return result.status === 'fulfilled'
}

const canManageSeedData = (): boolean => {
  const role = (sessionStorage.getItem('role') || '').replace(/^ROLE_/, '').toUpperCase()
  return role === 'ADMIN' || role === 'MANAGER' || role === 'KITCHEN_MANAGER'
}

const todayDate = (): string => new Date().toISOString().slice(0, 10)

const canSeedFromListResult = (result: PromiseSettledResult<unknown>): boolean => {
  return isFulfilled(result)
}

export const ensureDemoData = async (): Promise<void> => {
  if (!import.meta.env.DEV) {
    return
  }

  if (sessionStorage.getItem(SEEDED_SESSION_KEY) === 'done') {
    return
  }

  if (seedInFlight) {
    return seedInFlight
  }

  seedInFlight = (async () => {
    const orgNumber = getOrgNumber()
    const canManage = canManageSeedData()

    try {
      const [
        locationsRes,
        foodTemplatesRes,
        alcoholTemplatesRes,
        runsRes,
        pointsRes,
        entriesRes,
        deviationsRes,
        exportsRes,
      ] = await Promise.allSettled([
        client.get<LocationApi[]>('/locations', { params: withOrgNumber({}) }),
        client.get<TemplateApi[]>('/checklists/templates/module/FOOD', { params: withOrgNumber({}) }),
        client.get<TemplateApi[]>('/checklists/templates/module/ALCOHOL', { params: withOrgNumber({}) }),
        client.get<RunApi[]>('/checklists/runs', { params: withOrgNumber({}) }),
        client.get<PointApi[]>('/temperature/points', { params: withOrgNumber({}) }),
        client.get<EntryApi[]>('/temperature/entries', { params: withOrgNumber({}) }),
        client.get<DeviationApi[]>('/deviations', { params: withOrgNumber({}) }),
        client.get<ExportPageApi>('/exports', { params: withOrgNumber({ page: 0, size: 1 }) }),
      ])

      let locationId = isFulfilled(locationsRes) ? locationsRes.value.data[0]?.locationId : undefined
      let foodTemplateId = isFulfilled(foodTemplatesRes) ? foodTemplatesRes.value.data[0]?.templateId : undefined
      let alcoholTemplateId = isFulfilled(alcoholTemplatesRes) ? alcoholTemplatesRes.value.data[0]?.templateId : undefined
      const hasRuns = isFulfilled(runsRes) && runsRes.value.data.length > 0
      let pointId = isFulfilled(pointsRes) ? pointsRes.value.data[0]?.logPointId : undefined
      const hasEntries = isFulfilled(entriesRes) && entriesRes.value.data.length > 0
      const hasDeviation = isFulfilled(deviationsRes) && deviationsRes.value.data.length > 0
      const hasExport = isFulfilled(exportsRes) && exportsRes.value.data.content.length > 0

      if (!locationId && canManage && canSeedFromListResult(locationsRes)) {
        try {
          const createdLocation = await client.post<LocationApi>(
            '/locations',
            {
              name: 'Kjokken Demo',
              description: 'Autogenerert plassering for demo',
              locationType: 'KITCHEN',
              tempMinC: 0,
              tempMaxC: 4,
              isActive: true,
            },
            { params: withOrgNumber({}) },
          )
          locationId = createdLocation.data.locationId
        } catch {
          const retryLocations = await client.get<LocationApi[]>('/locations', {
            params: withOrgNumber({}),
          })
          locationId = retryLocations.data[0]?.locationId
        }
      }

      if (!foodTemplateId && canManage && canSeedFromListResult(foodTemplatesRes)) {
        const createdFoodTemplate = await client.post<TemplateApi>(
          '/checklists/templates',
          {
            title: 'Daglig IK-MAT kontroll',
            description: 'Autogenerert sjekkliste for demo',
            moduleType: 'FOOD',
            frequency: 'DAILY',
            items: [
              {
                sortOrder: 1,
                label: 'Kontroller temperatur i kjol',
                description: 'Skal ligge innenfor godkjent intervall',
                itemType: 'BOOLEAN',
                isRequired: true,
              },
            ],
          },
          { params: withOrgNumber({}) },
        )
        foodTemplateId = createdFoodTemplate.data.templateId
      }

      if (!alcoholTemplateId && canManage && canSeedFromListResult(alcoholTemplatesRes)) {
        const createdAlcoholTemplate = await client.post<TemplateApi>(
          '/checklists/templates',
          {
            title: 'Daglig IK-Alkohol kontroll',
            description: 'Autogenerert alkoholkontroll for demo',
            moduleType: 'ALCOHOL',
            frequency: 'DAILY',
            items: [
              {
                sortOrder: 1,
                label: 'Kontroller internkontrollskjema',
                description: 'Skjema skal fylles ut hver dag',
                itemType: 'BOOLEAN',
                isRequired: true,
              },
            ],
          },
          { params: withOrgNumber({}) },
        )
        alcoholTemplateId = createdAlcoholTemplate.data.templateId
      }

      if (!hasRuns && canManage && canSeedFromListResult(runsRes)) {
        const runDate = todayDate()

        if (foodTemplateId) {
          await client.post(
            '/checklists/runs',
            {
              templateId: foodTemplateId,
              runDate,
              notes: 'Autogenerert demo-run',
            },
            { params: withOrgNumber({}) },
          )
        }

        if (alcoholTemplateId) {
          await client.post(
            '/checklists/runs',
            {
              templateId: alcoholTemplateId,
              runDate,
              notes: 'Autogenerert demo-run',
            },
            { params: withOrgNumber({}) },
          )
        }
      }

      if (!pointId && canManage && locationId && canSeedFromListResult(pointsRes)) {
        const createdPoint = await client.post<PointApi>(
          '/temperature/points',
          {
            locationId,
            name: 'Kjol hovedpunkt',
            isActive: true,
          },
          { params: withOrgNumber({}) },
        )
        pointId = createdPoint.data.logPointId
      }

      if (!hasEntries && pointId && canSeedFromListResult(entriesRes)) {
        await client.post(
          '/temperature/entries',
          {
            logPointId: pointId,
            temperatureC: 3.2,
            measuredAt: new Date().toISOString(),
            noteText: 'Autogenerert demo-malning',
          },
          { params: withOrgNumber({}) },
        )
      }

      if (!hasDeviation && canSeedFromListResult(deviationsRes)) {
        await client.post(
          '/deviations',
          {
            reportType: 'INCIDENT',
            severity: 'MAJOR',
            title: 'Autogenerert avvik',
            description: 'Demoavvik opprettet for at frontend skal vise data.',
            locationId: locationId ?? null,
            locationText: locationId ? null : 'Kjokken',
            occurredDate: todayDate(),
          },
          { params: withOrgNumber({}) },
        )
      }

      if (!hasExport && canSeedFromListResult(exportsRes)) {
        await client.post(
          '/exports',
          {
            exportType: 'FULL_COMPLIANCE_REPORT',
            format: 'JSON',
          },
          { params: withOrgNumber({}) },
        )
      }

      sessionStorage.setItem(SEEDED_SESSION_KEY, 'done')

      // The files endpoint only supports multipart upload. If empty, keep it empty.
      void orgNumber
    } catch {
      // Seeding is best effort; normal loading continues with available backend data.
    }
  })().finally(() => {
    seedInFlight = null
  })

  return seedInFlight
}
