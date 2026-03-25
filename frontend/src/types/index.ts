export interface User {
  id: string
  name: string
  email: string
  role: 'ADMIN' | 'MANAGER' | 'EMPLOYEE'
}

export interface Checklist {
  id: string
  title: string
  items: ChecklistItem[]
  status: 'PENDING' | 'COMPLETED' | 'OVERDUE'
  createdAt: string
  updatedAt: string
}

export interface ChecklistItem {
  id: string
  label: string
  completed: boolean
}

export interface Deviation {
  id: string
  title: string
  description: string
  status: 'OPEN' | 'RESOLVED'
  createdAt: string
  updatedAt: string
}

export interface TemperatureLog {
  id: string
  temperature: number
  location: string
  measuredAt: string
  isDeviation: boolean
}