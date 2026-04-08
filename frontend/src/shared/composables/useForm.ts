/**
 * useForm - Composable for skjemahåndtering med validering
 * 
 * Brukes for å håndtere skjemadata, validering og innsending.
 * Støtter felt-validering, dirty-sjekk og handleSubmit.
 * 
 * Eksempel:
 * const { values, errors, isSubmitting, handleSubmit } = useForm(
 *   { email: '', password: '' },
 *   {
 *     email: [rules.required(), rules.email()],
 *     password: [rules.required(), rules.minLength(8)]
 *   }
 * )
 * 
 * const submit = () => handleSubmit(async (formValues) => {
 *   await authStore.login(formValues)
 * })
 */
import { reactive, ref, watch } from 'vue'

type ValidationRule<T extends Record<string, unknown>> = (
  value: unknown,
  values: T
) => true | string

export function useForm<T extends Record<string, unknown>>(
  initialValues: T,
  validationRules: Record<string, ValidationRule<T>[]> = {}
) {
  const values = reactive({ ...initialValues })
  const errors = reactive<Record<string, string>>({})
  const isDirty = ref(false)
  const isSubmitting = ref(false)

  function validate(): boolean {
    Object.keys(errors).forEach((key) => delete errors[key])
    let isValid = true
    const typedValues = values as T

    for (const [field, rules] of Object.entries(validationRules)) {
      for (const rule of rules) {
        const result = rule(typedValues[field as keyof T], typedValues)
        if (result !== true) {
          errors[field] = result
          isValid = false
          break
        }
      }
    }
    return isValid
  }

  function reset() {
    Object.assign(values, initialValues)
    Object.keys(errors).forEach((key) => delete errors[key])
    isDirty.value = false
    isSubmitting.value = false
  }

  async function handleSubmit(onSubmit: (values: T) => Promise<void>): Promise<boolean> {
    if (!validate()) return false

    isSubmitting.value = true
    try {
      await onSubmit({ ...values } as T)
      return true
    } catch {
      return false
    } finally {
      isSubmitting.value = false
    }
  }

  watch(values, () => {
    isDirty.value = true
  }, { deep: true })

  return {
    values,
    errors,
    isDirty,
    isSubmitting,
    validate,
    reset,
    handleSubmit,
  }
}
