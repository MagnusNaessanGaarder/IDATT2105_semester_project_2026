import { describe, it, expect, beforeEach, vi, nextTick } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import NoOrganizationView from '../NoOrganizationView.vue'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/stores/auth', () => ({
    useAuthStore: vi.fn(),
}))

const mockRouter = createRouter({
    history: createMemoryHistory(),
    routes: [
        { path: '/', name: 'Dashboard', component: { template: '<div />' } },
        { path: '/ingen-organisasjon', name: 'NoOrganization', component: { template: '<div />' } },
        { path: '/login', name: 'Login', component: { template: '<div />' } },
    ],
})

const mockLogout = vi.fn()

const mountView = (email: string | null = 'ansatt@example.no') => {
    vi.mocked(useAuthStore).mockReturnValue({
        email,
        logout: mockLogout,
    } as unknown as ReturnType<typeof useAuthStore>)

    return mount(NoOrganizationView, {
        global: {
            plugins: [mockRouter],
            directives: {
                // Stub v-motion so it doesn't warn in the test environment
                motion: {},
            },
        },
    })
}

describe('NoOrganizationView', () => {
    beforeEach(() => {
        setActivePinia(createPinia())
        mockLogout.mockReset()
    })

    describe('Rendering', () => {
        it('renders the heading', () => {
            const wrapper = mountView()
            expect(wrapper.find('h1').text()).toBe('Venter på tilgang')
        })

        it('renders the kicker', () => {
            const wrapper = mountView()
            expect(wrapper.find('.no-org-kicker').text()).toBe('Internkontroll')
        })

        it('renders an explanation mentioning the manager', () => {
            const wrapper = mountView()
            expect(wrapper.find('.no-org-body').text()).toContain('leder')
        })

        it('renders the logout button', () => {
            const wrapper = mountView()
            expect(wrapper.find('.no-org-logout').exists()).toBe(true)
            expect(wrapper.find('.no-org-logout').text()).toBe('Logg ut')
        })
    })

    describe('Email display', () => {
        it('shows the user email from the auth store', () => {
            const wrapper = mountView('ola.nordmann@restaurant.no')
            expect(wrapper.find('.no-org-email').text()).toBe('ola.nordmann@restaurant.no')
        })

        it('shows empty when email is null', () => {
            const wrapper = mountView(null)
            expect(wrapper.find('.no-org-email').text()).toBe('')
        })

        it('renders the email label', () => {
            const wrapper = mountView()
            expect(wrapper.find('.no-org-email-label').text()).toBe('Din e-post')
        })
    })

    describe('Logout', () => {
        it('calls authStore.logout when button is clicked', async () => {
            await mockRouter.push('/ingen-organisasjon')
            const wrapper = mountView()
            await wrapper.find('.no-org-logout').trigger('click')
            expect(mockLogout).toHaveBeenCalledOnce()
        })

        it('navigates to Login after logout', async () => {
            await mockRouter.push('/ingen-organisasjon')
            const wrapper = mountView()
            await wrapper.find('.no-org-logout').trigger('click')
            await mockRouter.isReady()
            expect(mockRouter.currentRoute.value.name).toBe('Login')
        })
    })
})