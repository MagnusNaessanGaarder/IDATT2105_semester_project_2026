import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ChecklistsView from '../ChecklistsView.vue'

// Mock the composables
const mockChecklists = [
  {
    id: 1,
    name: 'Daily Cleaning',
    frequency: 'Daglig',
    law_unit: 'HMS-1',
    description: 'Daily cleaning tasks',
    items: [
      { id: 1, task: 'Clean floor', completed: false, required: true, notes: null },
      { id: 2, task: 'Clean tables', completed: true, required: true, notes: null },
    ],
    status: 'pending',
  },
  {
    id: 2,
    name: 'Weekly Inspection',
    frequency: 'Ukentlig',
    law_unit: 'HMS-2',
    description: 'Weekly inspection tasks',
    items: [
      { id: 3, task: 'Check fire extinguisher', completed: false, required: true, notes: null },
    ],
    status: 'pending',
  },
]

vi.mock('@/features/ik-mat/composables/useIkMatData', () => ({
  useIkMatData: () => ({
    checklists: mockChecklists,
    completionForChecklist: (checklist: typeof mockChecklists[0]) => {
      const completed = checklist.items.filter((i) => i.completed).length
      return Math.round((completed / checklist.items.length) * 100)
    },
  }),
}))

describe('ChecklistsView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders the page title', () => {
    const wrapper = mount(ChecklistsView)

    expect(wrapper.find('h1').text()).toBe('Sjekklister')
    expect(wrapper.find('.subtitle').text()).toContain('Operative kontrollpunkter')
  })

  it('renders frequency filter buttons', () => {
    const wrapper = mount(ChecklistsView)
    const buttons = wrapper.findAll('.filter-chip')

    expect(buttons).toHaveLength(4)
    expect(buttons[0].text()).toBe('Alle')
    expect(buttons[1].text()).toBe('Daglig')
    expect(buttons[2].text()).toBe('Ukentlig')
    expect(buttons[3].text()).toBe('Månedlig')
  })

  it('renders checklist cards', () => {
    const wrapper = mount(ChecklistsView)
    const cards = wrapper.findAll('.checklist-card')

    expect(cards).toHaveLength(2)
  })

  it('displays checklist name and meta info', () => {
    const wrapper = mount(ChecklistsView)

    expect(wrapper.text()).toContain('Daily Cleaning')
    expect(wrapper.text()).toContain('Daglig · HMS-1')
    expect(wrapper.text()).toContain('Weekly Inspection')
    expect(wrapper.text()).toContain('Ukentlig · HMS-2')
  })

  it('displays completion percentage', () => {
    const wrapper = mount(ChecklistsView)

    // First checklist: 1/2 completed = 50%
    expect(wrapper.text()).toContain('50%')
    // Second checklist: 0/1 completed = 0%
    expect(wrapper.text()).toContain('0%')
  })

  it('expands checklist when clicked', async () => {
    const wrapper = mount(ChecklistsView)
    const firstHeader = wrapper.findAll('.checklist-header__content')[0]

    // Initially collapsed
    expect(wrapper.find('.checklist-body').exists()).toBe(false)

    // Click to expand
    await firstHeader.trigger('click')

    // Now expanded
    expect(wrapper.find('.checklist-body').exists()).toBe(true)
    expect(wrapper.text()).toContain('Daily cleaning tasks')
    expect(wrapper.text()).toContain('Clean floor')
    expect(wrapper.text()).toContain('Clean tables')
  })

  it('collapses expanded checklist when clicked again', async () => {
    const wrapper = mount(ChecklistsView)
    const firstHeader = wrapper.findAll('.checklist-header__content')[0]

    // Expand
    await firstHeader.trigger('click')
    expect(wrapper.find('.checklist-body').exists()).toBe(true)

    // Collapse
    await firstHeader.trigger('click')
    expect(wrapper.find('.checklist-body').exists()).toBe(false)
  })

  it('toggles task completion', async () => {
    const wrapper = mount(ChecklistsView)
    const firstHeader = wrapper.findAll('.checklist-header__content')[0]

    // Expand first checklist
    await firstHeader.trigger('click')

    // Find first checkbox and toggle it
    const checkbox = wrapper.find('input[type="checkbox"]')
    expect(checkbox.element.checked).toBe(false)

    await checkbox.trigger('change')

    // After toggle, checkbox should be checked
    expect(checkbox.element.checked).toBe(true)
  })

  it('filters checklists by frequency', async () => {
    const wrapper = mount(ChecklistsView)

    // Initially shows all
    expect(wrapper.findAll('.checklist-card')).toHaveLength(2)

    // Click 'Daglig' filter
    const dagligButton = wrapper.findAll('.filter-chip')[1]
    await dagligButton.trigger('click')

    // Should only show daily checklists
    expect(wrapper.findAll('.checklist-card')).toHaveLength(1)
    expect(wrapper.text()).toContain('Daily Cleaning')
    expect(wrapper.text()).not.toContain('Weekly Inspection')
  })

  it('shows empty state when no checklists match filter', async () => {
    const wrapper = mount(ChecklistsView)

    // Click 'Månedlig' filter (no monthly checklists in mock)
    const monthlyButton = wrapper.findAll('.filter-chip')[3]
    await monthlyButton.trigger('click')

    expect(wrapper.find('.empty-state').exists()).toBe(true)
    expect(wrapper.text()).toContain('Ingen sjekklister matcher valgt filter')
  })

  it('sorts checklists by completion percentage', () => {
    const wrapper = mount(ChecklistsView)
    const cards = wrapper.findAll('.checklist-card')

    // Second checklist has 0% completion, should come first
    // First checklist has 50% completion, should come second
    expect(cards[0].text()).toContain('Weekly Inspection') // 0%
    expect(cards[1].text()).toContain('Daily Cleaning') // 50%
  })

  it('applies strikethrough to completed tasks', async () => {
    const wrapper = mount(ChecklistsView)
    const firstHeader = wrapper.findAll('.checklist-header__content')[0]

    await firstHeader.trigger('click')

    // Find all task rows and check their classes
    const taskRows = wrapper.findAll('.task-row')
    expect(taskRows.length).toBeGreaterThan(0)

    // Check first task (not completed)
    const firstTaskSpan = taskRows[0].find('span')
    expect(firstTaskSpan.classes()).not.toContain('task-done')

    // Check second task (completed)
    if (taskRows.length > 1) {
      const secondTaskSpan = taskRows[1].find('span')
      expect(secondTaskSpan.classes()).toContain('task-done')
    }
  })

  it('has correct aria attributes on progress bar', () => {
    const wrapper = mount(ChecklistsView)
    const progressTrack = wrapper.find('.progress-track')

    expect(progressTrack.attributes('role')).toBe('progressbar')
    expect(progressTrack.attributes('aria-valuemin')).toBe('0')
    expect(progressTrack.attributes('aria-valuemax')).toBe('100')
    expect(progressTrack.attributes('aria-valuenow')).toBeDefined()
  })

  it('has correct aria-expanded attribute on checklist header', async () => {
    const wrapper = mount(ChecklistsView)
    const header = wrapper.find('.checklist-header__content')

    expect(header.attributes('aria-expanded')).toBe('false')

    await header.trigger('click')

    expect(header.attributes('aria-expanded')).toBe('true')
  })

  it('maintains checklist expansion state independently', async () => {
    const wrapper = mount(ChecklistsView)
    const headers = wrapper.findAll('.checklist-header__content')

    expect(headers.length).toBe(2)

    // Expand first checklist
    await headers[0].trigger('click')
    expect(headers[0].attributes('aria-expanded')).toBe('true')
    expect(headers[1].attributes('aria-expanded')).toBe('false')

    // Expand second checklist (first should collapse)
    await headers[1].trigger('click')
    expect(headers[0].attributes('aria-expanded')).toBe('false')
    expect(headers[1].attributes('aria-expanded')).toBe('true')
  })

  it('options menu is not visible for non-admin users', () => {
    const wrapper = mount(ChecklistsView)
    
    // Options menu should not be rendered when isAdmin is false
    expect(wrapper.find('.options-menu').exists()).toBe(false)
  })

  it('has proper keyboard accessibility on expandable headers', () => {
    const wrapper = mount(ChecklistsView)
    const header = wrapper.find('.checklist-header__content')

    // Should have role="button" and tabindex
    expect(header.attributes('role')).toBe('button')
    expect(header.attributes('tabindex')).toBe('0')
  })
})