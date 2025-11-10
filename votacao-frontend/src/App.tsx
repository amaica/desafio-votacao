// App.tsx
import { useEffect, useRef, useState } from 'react'
import axios from 'axios'
import { Toast } from 'primereact/toast'
import { Button } from 'primereact/button'
import { DataTable } from 'primereact/datatable'
import { Column } from 'primereact/column'
import { Dialog } from 'primereact/dialog'
import { InputText } from 'primereact/inputtext'
import { RadioButton } from 'primereact/radiobutton'
import { Tag } from 'primereact/tag'

type SessaoStatus = 'ABERTA' | 'ENCERRADA'
type Pauta = {
  id: string
  titulo: string
  descricao?: string
  createdAt: string
  sessaoStatus?: SessaoStatus
}
type Resultado = { sim: number; nao: number; total: number; status: SessaoStatus }

const API = (import.meta.env.VITE_API_URL as string) || 'http://localhost:8080'
const URL = `${API}/pautas`
const onlyDigits = (s: string) => (s || '').replace(/\D/g, '')


function SessaoButton({
  pautaId,
  toast,
  statusAberta,
  onAberta
}: {
  pautaId: string
  toast: React.RefObject<Toast>
  statusAberta: boolean
  onAberta: () => void
}) {
  const [busy, setBusy] = useState(false)
  const label = statusAberta ? 'aberta' : 'Abrir sessão'

  async function abrir() {
    if (busy || statusAberta) return
    setBusy(true)
    try {
      // backend: POST /pautas/{id}/sessao?duracao=60 (default 60min)
      await axios.post(`${URL}/${pautaId}/sessao?duracao=60`)
      toast.current?.show({ severity: 'success', summary: 'Sessão aberta' })
      onAberta()
    } catch (e: any) {
      const code = e?.response?.status
      const msg = e?.response?.data?.message || ''
      if (code === 409) {
        toast.current?.show({ severity: 'info', summary: msg || 'Sessão já estava aberta' })
        onAberta()
      } else {
        toast.current?.show({ severity: 'warn', summary: 'Atenção', detail: msg || 'Erro ao abrir sessão' })
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <Button
      label={label}
      icon={statusAberta ? 'pi pi-check' : 'pi pi-play'}
      className={`p-button-sm p-button-raised ${statusAberta ? 'p-button-success' : ''}`}
      onClick={abrir}
      disabled={statusAberta}
      loading={busy}
    />
  )
}

export default function App() {
  const toast = useRef<Toast>(null)
  const [pautas, setPautas] = useState<Pauta[]>([])
  const [loading, setLoading] = useState(false)

  // nova pauta
  const [open, setOpen] = useState(false)
  const [titulo, setTitulo] = useState('')
  const [descricao, setDescricao] = useState('')

  // votar
  const [votarOpen, setVotarOpen] = useState(false)
  const [votoCpf, setVotoCpf] = useState('')
  const [votoOpcao, setVotoOpcao] = useState<'SIM' | 'NAO'>('SIM')
  const [pautaSel, setPautaSel] = useState<Pauta | null>(null)

  // resultado
  const [resOpen, setResOpen] = useState(false)
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const [pollId, setPollId] = useState<number | null>(null)


  async function carregar() {
    setLoading(true)
    try {
      const { data } = await axios.get<Pauta[]>(URL)
      const lista = data ?? []

   
      const statuses = await Promise.all(
        lista.map(async (p) => {
          try {
            const r = await axios.get<Resultado>(`${URL}/${p.id}/resultado`)
            const raw = (r?.data as any)?.status
            const status = typeof raw === 'string' ? (raw.trim().toUpperCase() as SessaoStatus) : 'ENCERRADA'
            return { id: p.id, status }
          } catch {
            return { id: p.id, status: 'ENCERRADA' as SessaoStatus }
          }
        })
      )

      const map = new Map(statuses.map((s) => [s.id, s.status]))
      setPautas(lista.map((p) => ({ ...p, sessaoStatus: map.get(p.id) })))
    } catch {
      toast.current?.show({ severity: 'error', summary: 'Erro', detail: 'Falha ao buscar pautas' })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    carregar()
  }, [])

  async function criarPauta() {
    try {
      const { data } = await axios.post<Pauta>(URL, { titulo, descricao })
      setOpen(false)
      setTitulo('')
      setDescricao('')
      // adiciona e hidrata só essa
      const p = data
      try {
        const r = await axios.get<Resultado>(`${URL}/${p.id}/resultado`)
        const status = (r?.data?.status || 'ENCERRADA') as SessaoStatus
        setPautas((prev) => [{ ...p, sessaoStatus: status }, ...prev])
      } catch {
        setPautas((prev) => [{ ...p, sessaoStatus: 'ENCERRADA' }, ...prev])
      }
      toast.current?.show({ severity: 'success', summary: 'Pauta criada' })
    } catch {
      toast.current?.show({ severity: 'error', summary: 'Erro', detail: 'Não foi possível criar' })
    }
  }

  function openVotar(row: Pauta) {
    setPautaSel(row)
    setVotoCpf('')
    setVotoOpcao('SIM')
    setVotarOpen(true)
  }

  async function enviarVoto() {
    if (!pautaSel) return

    const cpf = onlyDigits(votoCpf)
    if (cpf.length !== 11) {
      toast.current?.show({
        severity: 'warn',
        summary: 'CPF inválido',
        detail: 'Informe os 11 dígitos do CPF antes de votar.'
      })
      return
    }

    try {
      await axios.post(`${URL}/${pautaSel.id}/votar`, {
        cpf,
        opcao: votoOpcao
      })
      setVotarOpen(false)
      toast.current?.show({ severity: 'success', summary: 'Voto computado' })
    } catch (e: any) {
      toast.current?.show({
        severity: 'error',
        summary: 'Erro',
        detail: e?.response?.data?.message || 'Falha ao votar'
      })
    }
  }

  async function openResultado(row: Pauta) {
    setPautaSel(row)
    setResOpen(true)
    await carregarResultado(row.id)
    // polling enquanto ABERTA
    const id = window.setInterval(async () => {
      const r = await carregarResultado(row.id)
      if (r?.status !== 'ABERTA') {
        window.clearInterval(id)
        setPollId(null)
      }
    }, 2000)
    setPollId(id)
  }

  async function carregarResultado(id: string) {
    try {
      const { data } = await axios.get<Resultado>(`${URL}/${id}/resultado`)
      setResultado(data)
      // espelha status na linha
      setPautas((prev) => prev.map((p) => (p.id === id ? { ...p, sessaoStatus: data.status } : p)))
      return data
    } catch {
      toast.current?.show({ severity: 'error', summary: 'Erro', detail: 'Falha ao obter resultado' })
    }
  }

  function fecharResultado() {
    setResOpen(false)
    if (pollId) {
      window.clearInterval(pollId)
      setPollId(null)
    }
  }

  const acaoBody = (row: Pauta) => (
    <div className="actions-wrap">
      <SessaoButton
        key={`${row.id}-${row.sessaoStatus ?? 'x'}`}
        pautaId={row.id}
        toast={toast}
        statusAberta={row.sessaoStatus === 'ABERTA'}
        onAberta={() => {
          setPautas((prev) => prev.map((p) => (p.id === row.id ? { ...p, sessaoStatus: 'ABERTA' } : p)))
        }}
      />
      <Button
        label="Votar"
        icon="pi pi-check-circle"
        className="p-button-sm p-button-raised p-button-help"
        onClick={() => openVotar(row)}
      />
      <Button
        label="Resultado"
        icon="pi pi-chart-bar"
        className="p-button-sm p-button-raised p-button-info"
        onClick={() => openResultado(row)}
      />
    </div>
  )

  const dataBody = (row: Pauta) => new Date(row.createdAt).toLocaleString('pt-BR')

  const footerNova = (
    <div className="flex justify-content-end gap-2">
      <Button label="Cancelar" icon="pi pi-times" className="p-button-text" onClick={() => setOpen(false)} />
      <Button label="Salvar" icon="pi pi-check" onClick={criarPauta} />
    </div>
  )

  const footerVoto = (
    <div className="flex justify-content-end gap-2">
      <Button label="Fechar" className="p-button-text" onClick={() => setVotarOpen(false)} />
      <Button label="Votar" icon="pi pi-send" onClick={enviarVoto} />
    </div>
  )

  return (
    <div className="surface-ground min-h-screen p-3">
      <style>{css}</style>
      <Toast ref={toast} />

      <div className="app-container">
        <div className="flex align-items-center justify-content-between mb-3 header-bar">
          <h2 className="m-0">🗳️ Sistema de Votação</h2>
          <div className="flex gap-2">
            <Button
              label="Atualizar"
              icon="pi pi-refresh"
              onClick={carregar}
              loading={loading}
              className="p-button-raised"
            />
            <Button
              label="Nova Pauta"
              icon="pi pi-plus"
              onClick={() => setOpen(true)}
              className="p-button-raised p-button-success"
            />
          </div>
        </div>

        <div className="surface-card shadow-2 border-round-lg p-3">
          <DataTable
            value={pautas}
            loading={loading}
            paginator
            rows={10}
            responsiveLayout="stack"
            breakpoint="960px"
            emptyMessage="Nenhuma pauta encontrada"
            className="p-datatable-sm"
          >
            <Column field="titulo" header="Título" sortable />
            <Column field="descricao" header="Descrição" />
            <Column header="Criada em" body={dataBody} sortable />
            <Column header="Ações" body={acaoBody} className="actions-col" />
          </DataTable>
        </div>
      </div>

      {/* Nova pauta */}
      <Dialog
        header="Nova Pauta"
        visible={open}
        style={{ width: 'min(32rem, 95vw)' }}
        modal
        footer={footerNova}
        onHide={() => setOpen(false)}
      >
        <div className="p-fluid">
          <div className="field mb-3">
            <label htmlFor="titulo">Título</label>
            <InputText id="titulo" value={titulo} onChange={(e) => setTitulo(e.target.value)} />
          </div>
          <div className="field">
            <label htmlFor="descricao">Descrição</label>
            <InputText id="descricao" value={descricao} onChange={(e) => setDescricao(e.target.value)} />
          </div>
        </div>
      </Dialog>

      {/* Votar */}
      <Dialog
        header={pautaSel ? `Votar: ${pautaSel.titulo}` : 'Votar'}
        visible={votarOpen}
        style={{ width: 'min(28rem, 95vw)' }}
        modal
        footer={footerVoto}
        onHide={() => setVotarOpen(false)}
      >
        <div className="p-fluid">
          <div className="field mb-3">
            <label htmlFor="cpf">CPF (11 dígitos)</label>
            <InputText
              id="cpf"
              value={votoCpf}
              onChange={(e) => setVotoCpf(onlyDigits(e.target.value))}
              maxLength={11}
            />
          </div>
          <div className="field">
            <div className="flex gap-4">
              <div className="flex align-items-center gap-2">
                <RadioButton
                  inputId="sim"
                  name="opcao"
                  value="SIM"
                  onChange={(e) => setVotoOpcao(e.value)}
                  checked={votoOpcao === 'SIM'}
                />
                <label htmlFor="sim">SIM</label>
              </div>
              <div className="flex align-items-center gap-2">
                <RadioButton
                  inputId="nao"
                  name="opcao"
                  value="NAO"
                  onChange={(e) => setVotoOpcao(e.value)}
                  checked={votoOpcao === 'NAO'}
                />
                <label htmlFor="nao">NÃO</label>
              </div>
            </div>
          </div>
        </div>
      </Dialog>

      {/* Resultado */}
      <Dialog
        header={pautaSel ? `Resultado: ${pautaSel.titulo}` : 'Resultado'}
        visible={resOpen}
        style={{ width: 'min(28rem, 95vw)' }}
        modal
        onHide={fecharResultado}
      >
        {resultado ? (
          <div className="p-2">
            <div className="mb-3">
              Status:{' '}
              <Tag value={resultado.status} severity={resultado.status === 'ABERTA' ? 'success' : 'danger'} />
            </div>
            <div className="flex gap-3 flex-wrap">
              <div className="surface-card border-round p-3 shadow-1 stat-card">
                <div>SIM</div>
                <h3 className="mt-2">{resultado.sim}</h3>
              </div>
              <div className="surface-card border-round p-3 shadow-1 stat-card">
                <div>NÃO</div>
                <h3 className="mt-2">{resultado.nao}</h3>
              </div>
              <div className="surface-card border-round p-3 shadow-1 stat-card">
                <div>TOTAL</div>
                <h3 className="mt-2">{resultado.total}</h3>
              </div>
            </div>
          </div>
        ) : (
          <div className="p-3">Carregando…</div>
        )}
      </Dialog>
    </div>
  )
}

/** CSS responsivo embutido */
const css = `
.app-container { max-width: 1100px; margin: 0 auto; }
.actions-wrap { display: flex; gap: .5rem; flex-wrap: wrap; }
.actions-wrap > .p-button { flex: 1 1 auto; }

.header-bar { flex-wrap: wrap; gap: .75rem; }
.header-bar h2 { font-size: 1.25rem; }

.stat-card { min-width: 100px; }

@media (max-width: 960px) {
  .actions-col { width: auto !important; }
  .surface-card { padding: .75rem; }
  .p-datatable .p-paginator { flex-wrap: wrap; gap: .5rem; }
}

@media (max-width: 640px) {
  .header-bar h2 { font-size: 1.1rem; }
}
`
