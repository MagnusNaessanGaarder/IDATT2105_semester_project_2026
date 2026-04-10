<template>
    <div class="control-item">
        <div class="control-item__top">
            <div class="control-card__left">
                <button
                    type="button"
                    class="control-card__checkbox"
                    :class="{ 'control-card__checkbox--checked': item.is_checked }"
                    :aria-label="props.item.is_checked ? `Marker ${props.item.name} som ikke fullført` : `Marker ${props.item.name} som fullført`"
                    @click="emitToggle"
                >
                    <span>{{ props.item.is_checked ? '✓' : '' }}</span>
                </button>

                <header class="control-card__header">
                    <div class="control-card__main">
                        <h2 class="control-card__title">{{ props.item.name }}</h2>
                        <p class="control-card__law">{{ props.item.law_unit }}</p>
                    </div>

                    <div v-if="props.item.is_checked" class="control-card__meta-row">
                        <p class="control-card__meta">Ansatt: {{ props.item.employee }}</p>
                        <p class="control-card__meta">
                            Dato: {{ formattedDate(props.item.completion_date.date) }} kl. {{ props.item.completion_date.time.slice(0, 5) }}
                        </p>
                    </div>

                    <button type="button" class="show-more" @click="emitViewMore">Vis mer</button>
                </header>
            </div>

            <div class="control-item__actions">
                <span class="control-card__status" :class="{ 'control-card__status--pending': !props.item.is_checked }">
                    {{ props.item.is_checked ? 'Fullført' : 'Mangler' }}
                </span>

                <div v-if="props.canManage" class="options-menu">
                    <button
                        class="options-menu__trigger"
                        type="button"
                        aria-label="Åpne handlinger"
                        :aria-expanded="optionsOpen"
                        @click="toggleOptions"
                    >
                        <span class="dot" />
                        <span class="dot" />
                        <span class="dot" />
                    </button>

                    <div v-if="optionsOpen" class="options-menu__list" role="menu">
                        <button class="options-menu__item" type="button" role="menuitem" @click="emitEdit">Rediger</button>
                        <button class="options-menu__item options-menu__item--danger" type="button" role="menuitem" @click="emitDelete">Slett</button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
    import { ref } from 'vue'
    import { useAlkoholData } from '../composables/useAlkoholData'

    const { formattedDate } = useAlkoholData()

    interface ControlItemProps {
        id: number,
        run_id: number | null,
        run_status: string | null,
        name: string,
        comment: string,
        employee: string,
        completion_date: {
            date: string,
            time: string,
        },
        law_unit: string,
        is_checked: boolean,
        attachment: string | null,
    }

    const props = defineProps<{
        item: ControlItemProps
        canManage?: boolean
    }>();

    const emit = defineEmits<{
        toggle: [id: number]
        edit: [id: number]
        delete: [id: number]
        viewMore: [id: number]
    }>()

    const optionsOpen = ref(false)

    const emitToggle = () => {
        emit('toggle', props.item.id)
    }

    const toggleOptions = () => {
        optionsOpen.value = !optionsOpen.value
    }

    const emitEdit = () => {
        optionsOpen.value = false
        emit('edit', props.item.id)
    }

    const emitDelete = () => {
        optionsOpen.value = false
        emit('delete', props.item.id)
    }

    const emitViewMore = () => {
        emit('viewMore', props.item.id)
    }

</script>
    
<style scoped>
    .control-item {
        width: 100%;
        display: flex;
        flex-direction: column;
    }

    .control-item__top {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 1rem;
    }

    .control-card__left {
        display: flex;
        align-items: flex-start;
        gap: 1.4rem;
        flex-direction: row;
        flex: 1;
    }

    .control-card__header {
        display: flex;
        flex-direction: column;
        gap: 10px;
        margin-bottom: 0.5rem;
    }

    .control-card__checkbox {
        border: 1px solid var(--color-border-strong);
        border-radius: var(--radius-md);
        min-height: var(--touch-target);
        background: var(--color-background-soft);
        color: var(--color-foreground);
        display: inline-flex;
        padding: 0.75rem;
        justify-content: center;
        align-items: center;
        font-size: var(--font-size-sm);
        flex-shrink: 0;
        overflow: hidden;
        cursor: pointer;
        transition: border-color var(--transition-fast), box-shadow var(--transition-fast), background-color var(--transition-fast), color var(--transition-fast);
    }

    .control-card__checkbox:hover {
        border-color: var(--color-border-strong);
    }

    .control-card__checkbox:focus,
    .control-card__checkbox:focus-visible {
        outline: none;
        background: var(--color-surface-raised);
        box-shadow: var(--shadow-focus);
    }
    
    .control-card__checkbox span {
        display: block;
        min-width: 1rem;
        min-height: 1rem;
        aspect-ratio: 1 / 1;
        text-align: center;
        align-items: center;
        justify-content: center;
    }

    .control-card__checkbox--checked {
        background: var(--color-cta);
        border-color: color-mix(in srgb, var(--color-cta-foreground) 28%, var(--color-border-strong));
        color: var(--color-cta-foreground);
        box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--color-cta-foreground) 18%, transparent);
    }

    .control-card__main {
        flex: 1;
    }

    .control-card__title {
        margin: 0;
        font-size: var(--font-size-base);
        color: var(--color-foreground);
    }

    .control-card__status {
        font-size: var(--font-size-xs);
        color: var(--color-cta-foreground);
        background: var(--color-cta);
        border: 1px solid color-mix(in srgb, var(--color-cta-foreground) 28%, var(--color-border-strong));
        padding: 0.25rem 0.5rem;
        border-radius: var(--radius-sm);
        white-space: nowrap;
        font-weight: var(--font-weight-semibold);
    }

    .control-card__status--pending {
        color: var(--color-brand-near-black-violet);
        background: color-mix(in srgb, var(--color-brand-soft-violet) 78%, var(--color-surface-raised));
        border-color: color-mix(in srgb, var(--color-brand-deep-violet) 30%, var(--color-border));
    }

    .control-item__actions {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        position: relative;
    }

    .options-menu {
        position: relative;
    }

    .options-menu__trigger {
        aspect-ratio: 1 / 1;
        width: var(--touch-target);
        height: var(--touch-target);
        min-width: var(--touch-target);
        min-height: var(--touch-target);
        border: 1px solid var(--color-border);
        background: var(--color-card);
        border-radius: var(--radius-sm);
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 0.15rem;
        cursor: pointer;
        padding: 0;
    }

    .options-menu__trigger:hover {
        background: var(--color-info-bg);
    }

    .dot {
        width: 0.2rem;
        height: 0.2rem;
        background: var(--color-gray-600);
        border-radius: 100%;
    }

    .options-menu__list {
        position: absolute;
        top: calc(100% + 0.25rem);
        right: 0;
        z-index: 20;
        min-width: 9rem;
        background: var(--color-card);
        border: 1px solid var(--color-border);
        border-radius: var(--radius-md);
        box-shadow: var(--shadow-sm);
        overflow: hidden;
    }

    .options-menu__item {
        width: 100%;
        border: 0;
        background: transparent;
        padding: 0.6rem 0.8rem;
        text-align: left;
        color: var(--color-foreground);
        font-size: var(--font-size-sm);
    }

    .options-menu__item:hover {
        background: var(--color-info-bg);
    }

    .options-menu__item--danger {
        color: var(--color-danger);
    }

    .control-card__law {
        margin: 2px 0 0;
        font-size: var(--font-size-xs);
        color: var(--color-gray-500);
    }

    .control-card__comment {
        margin: 0.35rem 0;
        font-size: var(--font-size-sm);
        color: var(--color-gray-600);
    }

    .control-card__meta-row {
        display: flex;
        gap: 10px;
        flex-wrap: wrap;
    }

    .control-card__meta {
        margin: 0;
        font-size: var(--font-size-xs);
        color: var(--color-gray-600);
    }

    .show-more {
        align-self: flex-start;
        border: 0;
        background: transparent;
        color: var(--color-brand-medium-violet);
        font-size: var(--font-size-xs);
        font-weight: var(--font-weight-semibold);
        text-decoration: underline;
        text-underline-offset: 0.15rem;
        padding: 0;
        cursor: pointer;
    }

    @media (max-width: 48rem) {
        .control-item__top {
            flex-direction: column;
            align-items: stretch;
        }

        .control-item__actions {
            justify-content: space-between;
        }
    }
</style>
