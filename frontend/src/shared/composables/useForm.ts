import { reactive, ref, watch } from 'vue'

export function useForm<T extends Record<string, any>>(
  initialValues: T,
  validationRules: Record<string, ((value: any, values: T) => true | string)[]> = {}
) {
  const values = reactive({ ...initialValues })
  const errors = reactive<Record<string, string>>({})
  const isDirty = ref(false)
  const isSubmitting = ref(false)

  function validate(): boolean {
    Object.keys(errors).forEach((key) => delete errors[key])
    let isValid = true

    for (const [field, rules] of Object.entries(validationRules)) {
      for (const rule of rules) {
        const result = rule(values[field], values as T)
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