<template>
    <div class="control-item">
        <div class="control-item__top">
            <div class="control-card__left">
                <button
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
        border-radius: var(--radius-sm);
        border: 1px solid var(--color-gray-300);
        height: 100%;
        background: #ffffff;
        color: #ffffff;
        display: inline-flex;
        padding: 0.75rem;
        justify-content: center;
        align-items: center;
        font-size: var(--font-size-sm);
        flex-shrink: 0;
        overflow: hidden;
        cursor: pointer;
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
        background: var(--color-success);
        border-color: var(--color-success);
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
        color: var(--color-success);
        background: var(--color-success-bg);
        border: 1px solid color-mix(in srgb, var(--color-success) 30%, var(--color-border));
        padding: 0.25rem 0.5rem;
        border-radius: var(--radius-sm);
        white-space: nowrap;
    }

    .control-card__status--pending {
        color: var(--color-warning);
        background: var(--color-warning-bg);
        border-color: color-mix(in srgb, var(--color-warning) 35%, var(--color-border));
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
        border: 1px solid var(--color-border);
        background: var(--color-card);
        border-radius: var(--radius-sm);
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 0.15rem;
        cursor: pointer;
    }

    .options-menu__trigger:hover {
        background: var(--color-accent);
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
        background: var(--color-accent);
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
        color: var(--ik-alkohol-primary);
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
