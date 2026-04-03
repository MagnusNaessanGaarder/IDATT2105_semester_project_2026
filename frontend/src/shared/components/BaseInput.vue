<script setup lang="ts">
interface Props {
  id: string
  modelValue: string
  label: string
  type?: 'text' | 'email' | 'password' | 'number' | 'textarea'
  placeholder?: string
  required?: boolean
  disabled?: boolean
  error?: string
}

const props = withDefaults(defineProps<Props>(), {
  type: 'text',
  placeholder: '',
  required: false,
  disabled: false,
  error: '',
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const handleInput = (event: Event) => {
  const target = event.target as HTMLInputElement | HTMLTextAreaElement
  emit('update:modelValue', target.value)
}
</script>

<template>
  <div class="base-input">
    <label :for="id" class="base-input__label">
      {{ label }}
      <span v-if="required" class="base-input__required">*</span>
    </label>
    
    <textarea
      v-if="type === 'textarea'"
      :id="id"
      :value="modelValue"
      :placeholder="placeholder"
      :required="required"
      :disabled="disabled"
      :aria-invalid="!!error"
      :aria-describedby="error ? `${id}-error` : undefined"
      class="base-input__field"
      :class="{ 'base-input__field--error': error }"
      @input="handleInput"
    />
    
    <input
      v-else
      :id="id"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :required="required"
      :disabled="disabled"
      :aria-invalid="!!error"
      :aria-describedby="error ? `${id}-error` : undefined"
      class="base-input__field"
      :class="{ 'base-input__field--error': error }"
      @input="handleInput"
    />
    
    <span
      v-if="error"
      :id="`${id}-error`"
      class="base-input__error"
      role="alert"
    >
      {{ error }}
    </span>
  </div>
</template>

<style scoped>
.base-input {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.base-input__label {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-foreground);
}

.base-input__required {
  color: var(--color-danger);
}

.base-input__field {
  padding: 10px 12px;
  font-size: 16px;
  border: 1px solid var(--color-border);
  background: white;
  transition: border-color 0.15s;
}

.base-input__field:focus {
  outline: none;
  border-color: var(--color-accent);
}

.base-input__field--error {
  border-color: var(--color-danger);
}

.base-input__field:disabled {
  background: var(--color-gray-100);
  cursor: not-allowed;
}

.base-input__error {
  font-size: 14px;
  color: var(--color-danger);
}

textarea.base-input__field {
  min-height: 100px;
  resize: vertical;
}
</style>