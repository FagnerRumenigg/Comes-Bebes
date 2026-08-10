import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { beforeAll, describe, expect, it } from 'vitest'

import BaseButton from '@/components/base/BaseButton.vue'
import BaseCheckbox from '@/components/base/BaseCheckbox.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'
import BaseFieldError from '@/components/base/BaseFieldError.vue'
import BaseIconButton from '@/components/base/BaseIconButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseTextarea from '@/components/base/BaseTextarea.vue'
import BaseToast from '@/components/base/BaseToast.vue'
import PreparationStepsEditor from '@/components/publication/PreparationStepsEditor.vue'

beforeAll(() => {
  HTMLDialogElement.prototype.showModal = function showModal() {
    this.open = true
  }
  HTMLDialogElement.prototype.close = function close() {
    this.open = false
    this.dispatchEvent(new Event('close'))
  }
})

describe('botões base', () => {
  it('mantém semântica nativa e comunica seleção', async () => {
    const wrapper = mount(BaseButton, {
      props: { pressed: true },
      slots: { default: 'Salvar' },
    })

    const button = wrapper.get('button')
    expect(button.attributes('type')).toBe('button')
    expect(button.attributes('aria-pressed')).toBe('true')

    await button.trigger('click')
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('bloqueia interação e expõe carregamento', async () => {
    const wrapper = mount(BaseButton, {
      props: { loading: true },
      slots: { default: 'Publicar' },
    })
    const button = wrapper.get('button')

    expect(button.attributes('disabled')).toBeDefined()
    expect(button.attributes('aria-busy')).toBe('true')
    await button.trigger('click')
    expect(wrapper.emitted('click')).toBeUndefined()
  })

  it('exige nome acessível no botão de ícone', () => {
    const wrapper = mount(BaseIconButton, {
      props: { label: 'Salvar publicação' },
      slots: { default: '♡' },
    })

    expect(wrapper.get('button').attributes('aria-label')).toBe('Salvar publicação')
  })
})

describe('campos base', () => {
  it('associa label, dica e valor no input', async () => {
    const wrapper = mount(BaseInput, {
      props: { id: 'titulo', label: 'Título', hint: 'Use um título curto.' },
    })
    const input = wrapper.get('input')

    expect(wrapper.get('label').attributes('for')).toBe('titulo')
    expect(input.attributes('aria-describedby')).toBe('titulo-hint')

    await input.setValue('Bolo de milho')
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['Bolo de milho'])
  })

  it('conecta o erro ao campo inválido', () => {
    const wrapper = mount(BaseInput, {
      props: { id: 'email', label: 'E-mail', error: 'Informe um e-mail válido.' },
    })
    const input = wrapper.get('input')

    expect(input.attributes('aria-invalid')).toBe('true')
    expect(input.attributes('aria-describedby')).toBe('email-error')
    expect(wrapper.get('[role="alert"]').text()).toBe('Informe um e-mail válido.')
  })

  it('atualiza textarea e select sem alterar seus tipos nativos', async () => {
    const textarea = mount(BaseTextarea, {
      props: { label: 'Preparo' },
    })
    await textarea.get('textarea').setValue('Misture os ingredientes.')
    expect(textarea.emitted('update:modelValue')?.[0]).toEqual(['Misture os ingredientes.'])

    const select = mount(BaseSelect, {
      props: { label: 'Visibilidade' },
      slots: {
        default: '<option value="PUBLIC">Pública</option><option value="INTERNAL">Interna</option>',
      },
    })
    await select.get('select').setValue('INTERNAL')
    expect(select.emitted('update:modelValue')?.[0]).toEqual(['INTERNAL'])
  })

  it('permite alternar checkbox pelo controle nativo', async () => {
    const wrapper = mount(BaseCheckbox, {
      props: { id: 'lembrar', label: 'Lembrar-me' },
    })

    await wrapper.get('input').setValue(true)

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([true])
    expect(wrapper.get('label').attributes('for')).toBe('lembrar')
  })

  it('renderiza erro de campo apenas quando há conteúdo', () => {
    expect(mount(BaseFieldError).find('[role="alert"]').exists()).toBe(false)
    expect(
      mount(BaseFieldError, { props: { message: 'Campo obrigatório.' } })
        .get('[role="alert"]')
        .text(),
    ).toBe('Campo obrigatório.')
  })
})

describe('feedback e sobreposição', () => {
  it('fecha o diálogo pelo evento de cancelamento do teclado', async () => {
    const wrapper = mount(BaseDialog, {
      attachTo: document.body,
      props: { open: true, title: 'Confirmar publicação' },
      slots: { default: 'Deseja continuar?' },
    })
    await nextTick()

    const dialog = wrapper.get('dialog')
    expect((dialog.element as HTMLDialogElement).open).toBe(true)
    expect(dialog.attributes('aria-labelledby')).toBeDefined()

    await dialog.trigger('cancel')
    expect(wrapper.emitted('update:open')?.[0]).toEqual([false])
  })

  it('restaura o foco depois do fechamento do diálogo', async () => {
    const trigger = document.createElement('button')
    document.body.append(trigger)
    trigger.focus()

    const wrapper = mount(BaseDialog, {
      attachTo: document.body,
      props: { open: true, title: 'Editar ingrediente' },
    })
    await nextTick()
    await wrapper.setProps({ open: false })
    await nextTick()

    expect(document.activeElement).toBe(trigger)
  })

  it('usa região viva adequada e permite dispensar o toast', async () => {
    const wrapper = mount(BaseToast, {
      props: { title: 'Não foi possível publicar', kind: 'error' },
      slots: { default: 'Tente novamente.' },
    })

    expect(wrapper.attributes('role')).toBe('alert')
    expect(wrapper.attributes('aria-live')).toBe('assertive')

    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('dismiss')).toHaveLength(1)
  })
})

describe('editor de modo de preparo', () => {
  it('cria e focaliza o próximo passo ao pressionar Enter', async () => {
    const wrapper = mount(PreparationStepsEditor, {
      attachTo: document.body,
      props: { modelValue: 'Misture os ingredientes.' },
    })

    await wrapper.get('textarea').trigger('keydown', { key: 'Enter' })
    await nextTick()

    expect(wrapper.findAll('textarea')).toHaveLength(2)
    expect(wrapper.text()).toContain('Passo 2')
    expect(document.activeElement).toBe(wrapper.findAll('textarea')[1]!.element)
    expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual(['Misture os ingredientes.\n'])
  })
})
