import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import RegisterView from '../views/RegisterView.vue'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/stores/auth', () => ({
    useAuthStore: vi.fn(),
}))

const mockRouter = createRouter({
    history: createMemoryHistory(),
    routes: [
        { path: '/', name: 'Dashboard', component: { template: '<div />' } },
        { path: '/registrer', name: 'Register', component: { template: '<div />' } },
        { path: '/login', name: 'Login', component: { template: '<div />' } },
    ],
})

const mockRegister = vi.fn()
const mockAuthStore = {
    register: mockRegister,
    error: null as { message: string } | null,
    loading: false,
}

const mountView = () =>
    mount(RegisterView, {
        global: {
            plugins: [mockRouter],
            stubs: { 'router-link': { template: '<a><slot /></a>' } },
        },
    })

const VALID = {
    fullName: 'Ola Nordmann',
    email: 'ola@example.no',
    password: 'Test1234!',
    passwordConfirm: 'Test1234!',
}

async function fillForm(
    wrapper: ReturnType<typeof mountView>,
    overrides: Partial<typeof VALID & { phone: string }> = {}
) {
    const fields = { ...VALID, ...overrides }
    await wrapper.find('#fullName').setValue(fields.fullName)
    await wrapper.find('#email').setValue(fields.email)
    if (fields.phone !== undefined) await wrapper.find('#phone').setValue(fields.phone)
    await wrapper.find('#password').setValue(fields.password)
    await wrapper.find('#passwordConfirm').setValue(fields.passwordConfirm)
}

describe('RegisterView', () => {
    beforeEach(() => {
        setActivePinia(createPinia())
        vi.mocked(useAuthStore).mockReturnValue(mockAuthStore as ReturnType<typeof useAuthStore>)
        mockRegister.mockReset()
        mockAuthStore.error = null
    })

    describe('Rendering', () => {
        it('renders the heading and subtitle', () => {
            const wrapper = mountView()
            expect(wrapper.find('h2').text()).toBe('Opprett konto')
            expect(wrapper.find('.register-kicker').text()).toBe('Internkontroll')
            expect(wrapper.find('.register-subtitle').text()).toContain('leder')
        })

        it('renders all required form fields', () => {
            const wrapper = mountView()
            expect(wrapper.find('#fullName').exists()).toBe(true)
            expect(wrapper.find('#email').exists()).toBe(true)
            expect(wrapper.find('#phone').exists()).toBe(true)
            expect(wrapper.find('#password').exists()).toBe(true)
            expect(wrapper.find('#passwordConfirm').exists()).toBe(true)
        })

        it('renders the submit button', () => {
            const wrapper = mountView()
            const btn = wrapper.find('button[type="submit"]')
            expect(btn.exists()).toBe(true)
            expect(btn.text()).toBe('Opprett konto')
        })

        it('renders a link to the login page', () => {
            const wrapper = mountView()
            expect(wrapper.find('.login-link').text()).toContain('Logg inn')
        })

        it('does not show the error banner initially', () => {
            const wrapper = mountView()
            expect(wrapper.find('.error-message').exists()).toBe(false)
        })

        it('does not show the password strength bar when password is empty', () => {
            const wrapper = mountView()
            expect(wrapper.find('.password-strength').exists()).toBe(false)
        })
    })

    describe('Password strength indicator', () => {
        it('shows weak for a short password', async () => {
            const wrapper = mountView()
            await wrapper.find('#password').setValue('abc')
            expect(wrapper.find('.password-strength__fill--weak').exists()).toBe(true)
            expect(wrapper.find('.password-strength__label').text()).toBe('Svakt')
        })

        it('shows fair for a partially strong password', async () => {
            const wrapper = mountView()
            await wrapper.find('#password').setValue('Abcdef1')
            expect(wrapper.find('.password-strength__fill--fair').exists()).toBe(true)
            expect(wrapper.find('.password-strength__label').text()).toBe('Middels')
        })

        it('shows strong for a fully compliant password', async () => {
            const wrapper = mountView()
            await wrapper.find('#password').setValue('Test1234!')
            expect(wrapper.find('.password-strength__fill--strong').exists()).toBe(true)
            expect(wrapper.find('.password-strength__label').text()).toBe('Sterkt')
        })
    })

    describe('Password mismatch indicator', () => {
        it('shows mismatch error when confirm does not match', async () => {
            const wrapper = mountView()
            await wrapper.find('#password').setValue('Test1234!')
            await wrapper.find('#passwordConfirm').setValue('Wrong1234!')
            expect(wrapper.find('.field-error').exists()).toBe(true)
            expect(wrapper.find('#passwordConfirm').classes()).toContain('input--mismatch')
        })

        it('hides mismatch error when passwords match', async () => {
            const wrapper = mountView()
            await wrapper.find('#password').setValue('Test1234!')
            await wrapper.find('#passwordConfirm').setValue('Test1234!')
            expect(wrapper.find('.field-error').exists()).toBe(false)
        })
    })

    describe('Client-side validation', () => {
        it('shows error when full name is too short', async () => {
            const wrapper = mountView()
            await fillForm(wrapper, { fullName: 'A' })
            await wrapper.find('form').trigger('submit')
            expect(wrapper.find('.error-message').text()).toContain('Fullt navn')
            expect(mockRegister).not.toHaveBeenCalled()
        })

        it('shows error for invalid email format', async () => {
            const wrapper = mountView()
            await fillForm(wrapper, { email: 'not-an-email' })
            await wrapper.find('form').trigger('submit')
            expect(wrapper.find('.error-message').text()).toContain('e-postadresse')
            expect(mockRegister).not.toHaveBeenCalled()
        })

        it('shows error when password does not meet complexity requirements', async () => {
            const wrapper = mountView()
            await fillForm(wrapper, { password: 'simple', passwordConfirm: 'simple' })
            await wrapper.find('form').trigger('submit')
            expect(wrapper.find('.error-message').text()).toContain('Passordet')
            expect(mockRegister).not.toHaveBeenCalled()
        })

        it('shows error when passwords do not match', async () => {
            const wrapper = mountView()
            await fillForm(wrapper, { passwordConfirm: 'Different1!' })
            await wrapper.find('form').trigger('submit')
            expect(wrapper.find('.error-message').text()).toContain('stemmer ikke')
            expect(mockRegister).not.toHaveBeenCalled()
        })
    })

    describe('Successful registration', () => {
        it('calls authStore.register with correct payload', async () => {
            mockRegister.mockResolvedValue({})
            const wrapper = mountView()
            await fillForm(wrapper)
            await wrapper.find('form').trigger('submit')
            await Promise.resolve()

            expect(mockRegister).toHaveBeenCalledWith({
                fullName: VALID.fullName,
                email: VALID.email,
                phone: undefined,
                password: VALID.password,
            })
        })

        it('includes phone when provided', async () => {
            mockRegister.mockResolvedValue({})
            const wrapper = mountView()
            await fillForm(wrapper, { phone: '+47 123 45 678' })
            await wrapper.find('form').trigger('submit')
            await Promise.resolve()

            expect(mockRegister).toHaveBeenCalledWith(
                expect.objectContaining({ phone: '+47 123 45 678' })
            )
        })

        it('omits phone when left blank', async () => {
            mockRegister.mockResolvedValue({})
            const wrapper = mountView()
            await fillForm(wrapper, { phone: '   ' })
            await wrapper.find('form').trigger('submit')
            await Promise.resolve()

            expect(mockRegister).toHaveBeenCalledWith(
                expect.objectContaining({ phone: undefined })
            )
        })

        it('trims whitespace from fullName and email', async () => {
            mockRegister.mockResolvedValue({})
            const wrapper = mountView()
            await fillForm(wrapper, { fullName: '  Ola Nordmann  ', email: '  ola@example.no  ' })
            await wrapper.find('form').trigger('submit')
            await Promise.resolve()

            expect(mockRegister).toHaveBeenCalledWith(
                expect.objectContaining({ fullName: 'Ola Nordmann', email: 'ola@example.no' })
            )
        })

        it('navigates to Dashboard after successful registration', async () => {
            mockRegister.mockResolvedValue({})
            const wrapper = mountView()
            await mockRouter.push('/registrer')
            await fillForm(wrapper)
            await wrapper.find('form').trigger('submit')
            await Promise.resolve()
            await Promise.resolve()

            expect(mockRouter.currentRoute.value.name).toBe('Dashboard')
        })

        it('disables the submit button while loading', async () => {
            let resolve: () => void
            mockRegister.mockReturnValue(new Promise<void>((r) => { resolve = r }))
            const wrapper = mountView()
            await fillForm(wrapper)

            const btn = wrapper.find('button[type="submit"]')
            expect(btn.attributes('disabled')).toBeUndefined()

            await wrapper.find('form').trigger('submit')
            await Promise.resolve()

            expect(btn.attributes('disabled')).toBeDefined()
            expect(btn.text()).toContain('Oppretter konto')

            resolve!()
        })
    })

    describe('Registration failure', () => {
        it('shows error message from store on failure', async () => {
            mockRegister.mockRejectedValue(new Error('rejected'))
            mockAuthStore.error = { message: 'E-posten er allerede i bruk' }
            const wrapper = mountView()
            await fillForm(wrapper)
            await wrapper.find('form').trigger('submit')
            await Promise.resolve()

            expect(wrapper.find('.error-message').text()).toContain('E-posten er allerede i bruk')
        })

        it('falls back to a generic message when store error is null', async () => {
            mockRegister.mockRejectedValue(new Error('rejected'))
            mockAuthStore.error = null
            const wrapper = mountView()
            await fillForm(wrapper)
            await wrapper.find('form').trigger('submit')
            await Promise.resolve()

            expect(wrapper.find('.error-message').text()).toContain('Registrering feilet')
        })

        it('does not navigate on failure', async () => {
            mockRegister.mockRejectedValue(new Error('rejected'))
            mockAuthStore.error = { message: 'Feil' }
            await mockRouter.push('/registrer')
            const wrapper = mountView()
            await fillForm(wrapper)
            await wrapper.find('form').trigger('submit')
            await Promise.resolve()

            expect(mockRouter.currentRoute.value.name).toBe('Register')
        })

        it('re-enables the submit button after failure', async () => {
            mockRegister.mockRejectedValue(new Error('rejected'))
            mockAuthStore.error = { message: 'Feil' }
            const wrapper = mountView()
            await fillForm(wrapper)
            await wrapper.find('form').trigger('submit')
            await Promise.resolve()

            expect(wrapper.find('button[type="submit"]').attributes('disabled')).toBeUndefined()
        })
    })
})