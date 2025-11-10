#!/usr/bin/env bash
set -euo pipefail

log(){ echo -e "\033[1;32m==>\033[0m $*"; }

# 0) pré-checagens
[ -f package.json ] || { echo "rode dentro da pasta do app (onde existe package.json)"; exit 1; }

# 1) garantir deps mínimas
need_install=0
for dep in react-router-dom @tanstack/react-query axios zod react-hook-form @hookform/resolvers dayjs recharts; do
  npm ls "$dep" >/dev/null 2>&1 || need_install=1
done
if [ $need_install -eq 1 ]; then
  log "Instalando dependências faltantes…"
  npm i react-router-dom @tanstack/react-query axios zod react-hook-form @hookform/resolvers dayjs recharts
fi

# 2) estrutura
mkdir -p src/app src/features/pautas src/features/sessao src/lib

# 3) Providers/Router
cat > src/app/providers.tsx <<'EOF'
import { QueryClient } from '@tanstack/react-query'
import React from 'react'
export const queryClient = new QueryClient({
  defaultOptions: { queries: { refetchOnWindowFocus: true, retry: 1, staleTime: 5000 } }
})
export function Providers({ children }: { children: React.ReactNode }) { return <>{children}</> }
EOF

cat > src/app/router.tsx <<'EOF'
import { useRoutes } from 'react-router-dom'
import App from '../App'
import { PautasList } from '../features/pautas/PautasList'
import { SessaoPage } from '../features/sessao/SessaoPage'
export function AppRouter() {
  return useRoutes([
    { path: '/', element: <App />, children: [
      { index: true, element: <PautasList /> },
      { path: 'pautas/:id', element: <SessaoPage /> },
    ] }
  ])
}
EOF

# 4) main/App
cat > src/main.tsx <<'EOF'
import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import './index.css'
import { Providers, queryClient } from './app/providers'
import { AppRouter } from './app/router'
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Providers>
          <AppRouter />
        </Providers>
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>
)
EOF

cat > src/App.tsx <<'EOF'
import { Link, Outlet } from 'react-router-dom'
export default function App(){
  return (
    <div className="min-h-screen bg-neutral-50 text-neutral-900">
      <header className="border-b bg-white">
        <div className="mx-auto max-w-6xl px-4 py-3 flex items-center justify-between">
          <h1 className="text-xl font-semibold">Votação</h1>
          <nav className="flex items-center gap-4 text-sm">
            <Link className="hover:underline" to="/">Pautas</Link>
            <a className="hover:underline" href="/swagger-ui.html" target="_blank" rel="noreferrer">Swagger</a>
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
EOF

# 5) CSS base (se faltar)
[ -f src/index.css ] || cat > src/index.css <<'EOF'
@tailwind base;
@tailwind components;
@tailwind utilities;

html, body, #root { height: 100%; }
EOF

# 6) páginas/feature: Pautas
cat > src/features/pautas/types.ts <<'EOF'
export type Pauta={id:string; titulo:string; descricao?:string; createdAt:string}
export type SessaoVotacao={id:string;pautaId:string;openedAt:string;durationSeconds:number;closesAt:string;status:'ABERTA'|'ENCERRADA'}
EOF

cat > src/features/pautas/api.ts <<'EOF'
import { api } from '../../lib/axios'
import { Pauta, SessaoVotacao } from './types'
export async function criarPauta(data:{titulo:string;descricao?:string}):Promise<Pauta>{
  const r=await api.post('/api/v1/pautas',data); return r.data
}
export async function abrirSessao(pautaId:string,data?:{durationSeconds?:number}):Promise<SessaoVotacao>{
  const r=await api.post(`/api/v1/pautas/${pautaId}/sessoes`,data??{}); return r.data
}
export async function listarPautas():Promise<Pauta[]>{
  try{ const r=await api.get('/api/v1/pautas'); return r.data }catch{ return [] }
}
EOF

cat > src/features/pautas/hooks.ts <<'EOF'
import { useMutation,useQuery,useQueryClient } from '@tanstack/react-query'
import { abrirSessao,criarPauta,listarPautas } from './api'
export function usePautas(){ return useQuery({queryKey:['pautas'],queryFn:listarPautas}) }
export function useCriarPauta(){ const qc=useQueryClient(); return useMutation({mutationFn:criarPauta,onSuccess:()=>qc.invalidateQueries({queryKey:['pautas']})}) }
export function useAbrirSessao(){ const qc=useQueryClient(); return useMutation({
  mutationFn:({pautaId,durationSeconds}:{pautaId:string;durationSeconds?:number})=>abrirSessao(pautaId,durationSeconds?{durationSeconds}:undefined),
  onSuccess:(_,vars)=>{ qc.invalidateQueries({queryKey:['pauta',vars.pautaId]}); qc.invalidateQueries({queryKey:['resultado',vars.pautaId]}); qc.invalidateQueries({queryKey:['pautas']}); }
})}
EOF

cat > src/features/pautas/NovaPautaDialog.tsx <<'EOF'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useCriarPauta } from './hooks'
import { useState } from 'react'
const schema=z.object({titulo:z.string().min(3,'Informe um título (mín. 3)'),descricao:z.string().optional()})
type FormData=z.infer<typeof schema>
export function NovaPautaDialog({onClose}:{onClose:()=>void}){
  const {register,handleSubmit,formState:{errors,isSubmitting}}=useForm<FormData>({resolver:zodResolver(schema)})
  const criar=useCriarPauta(); const [errMsg,setErrMsg]=useState<string|null>(null)
  async function onSubmit(data:FormData){ setErrMsg(null); try{ await criar.mutateAsync(data); onClose() }catch(e:any){ setErrMsg(e?.userMessage||'Falha ao criar pauta') } }
  return(<div className="fixed inset-0 bg-black/40 flex items-center justify-center p-4">
    <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-lg">
      <h2 className="text-lg font-semibold mb-4">Nova Pauta</h2>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-3">
        <div><label className="block text-sm mb-1">Título</label>
          <input className="w-full border rounded-lg px-3 py-2" {...register('titulo')} placeholder="Assembleia 2025"/>
          {errors.titulo&&<p className="text-sm text-red-600 mt-1">{errors.titulo.message}</p>}
        </div>
        <div><label className="block text-sm mb-1">Descrição</label>
          <textarea className="w-full border rounded-lg px-3 py-2" rows={3} {...register('descricao')}/>
        </div>
        {errMsg&&<p className="text-sm text-red-600">{errMsg}</p>}
        <div className="flex justify-end gap-2 pt-2">
          <button type="button" onClick={onClose} className="px-3 py-2 rounded-lg border">Cancelar</button>
          <button type="submit" disabled={isSubmitting||criar.isPending} className="px-3 py-2 rounded-lg bg-black text-white disabled:opacity-60">
            {criar.isPending?'Salvando...':'Criar'}
          </button>
        </div>
      </form>
    </div></div>)}
EOF

cat > src/features/pautas/PautasList.tsx <<'EOF'
import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAbrirSessao, usePautas } from './hooks'
import { NovaPautaDialog } from './NovaPautaDialog'
import { formatDateTime } from '../../lib/time'
export function PautasList(){
  const pautas=usePautas(); const abrir=useAbrirSessao(); const [open,setOpen]=useState(false); const [q,setQ]=useState('')
  const rows=useMemo(()=>{ const data=pautas.data??[]; if(!q) return data; const qq=q.toLowerCase(); return data.filter(p=>p.titulo.toLowerCase().includes(qq)||(p.descricao??'').toLowerCase().includes(qq))},[pautas.data,q])
  return(<div className="space-y-4">
    <div className="flex items-center justify-between"><h2 className="text-xl font-semibold">Pautas</h2>
      <button onClick={()=>setOpen(true)} className="px-3 py-2 rounded-lg bg-black text-white">Nova Pauta</button></div>
    <div className="flex items-center gap-2"><input value={q} onChange={e=>setQ(e.target.value)} placeholder="Buscar..." className="w-full border rounded-lg px-3 py-2"/></div>
    {pautas.isLoading? <p>Carregando...</p> : rows.length===0 ? (
      <div className="border rounded-2xl p-8 text-center text-neutral-600">Nenhuma pauta. Clique em <b>Nova Pauta</b> para criar.</div>
    ):(
      <div className="overflow-x-auto"><table className="w-full text-sm">
        <thead><tr className="text-left border-b"><th className="py-2 pr-4">Título</th><th className="py-2 pr-4">Criada em</th><th className="py-2 pr-4">Ações</th></tr></thead>
        <tbody>{rows.map(p=>(
          <tr key={p.id} className="border-b last:border-0">
            <td className="py-2 pr-4">{p.titulo}</td>
            <td className="py-2 pr-4">{formatDateTime(p.createdAt)}</td>
            <td className="py-2 pr-4 flex items-center gap-2">
              <Link to={`/pautas/${p.id}`} className="px-3 py-1.5 rounded-lg border">Abrir</Link>
              <button onClick={()=>abrir.mutate({pautaId:p.id})} disabled={abrir.isPending} className="px-3 py-1.5 rounded-lg bg-black text-white disabled:opacity-60">
                {abrir.isPending?'Abrindo...':'Abrir sessão (60s)'}
              </button>
            </td>
          </tr>))}</tbody>
      </table></div>
    )}
    {open&&<NovaPautaDialog onClose={()=>setOpen(false)}/>}
  </div>)}
EOF

# 7) Sessão
cat > src/features/sessao/types.ts <<'EOF'
export type Resultado={sim:number;nao:number;total:number;status:'ABERTA'|'ENCERRADA'|string}
EOF

cat > src/features/sessao/api.ts <<'EOF'
import { api } from '../../lib/axios'
import { Resultado } from './types'
export async function votar(pautaId:string,data:{cpf:string;opcao:'SIM'|'NAO'}){ await api.post(`/api/v1/pautas/${pautaId}/votos`,data) }
export async function resultado(pautaId:string):Promise<Resultado>{ const r=await api.get(`/api/v1/pautas/${pautaId}/resultado`); return r.data }
EOF

cat > src/features/sessao/hooks.ts <<'EOF'
import { useMutation,useQuery,useQueryClient } from '@tanstack/react-query'
import { resultado, votar } from './api'
export function useResultado(pautaId:string,isAberta:boolean){
  return useQuery({queryKey:['resultado',pautaId],queryFn:()=>resultado(pautaId),refetchInterval:isAberta?2000:false})
}
export function useVotar(pautaId:string){
  const qc=useQueryClient()
  return useMutation({mutationFn:votar.bind(null,pautaId),onSuccess:()=>qc.invalidateQueries({queryKey:['resultado',pautaId]})})
}
EOF

cat > src/features/sessao/Countdown.tsx <<'EOF'
import { useEffect, useRef, useState } from 'react'
function clamp(x:number,min:number,max:number){ return Math.min(max,Math.max(min,x)) }
export function Countdown({ closesAt, onExpire }:{ closesAt:string; onExpire?:()=>void }){
  const [ms,setMs]=useState<number>(0); const fired=useRef(false)
  useEffect(()=>{ const target=new Date(closesAt).getTime(); let raf:number; let last=performance.now()
    const tick=()=>{ const now=Date.now(); const rem=target-now; setMs(rem)
      if(rem<=0){ if(!fired.current){ fired.current=true; onExpire?.() } return }
      const elapsed=performance.now()-last; last=performance.now(); const next=clamp(1000-(elapsed%1000),16,1000); setTimeout(()=>{ raf=requestAnimationFrame(tick) },next)
    }
    raf=requestAnimationFrame(tick); return ()=>cancelAnimationFrame(raf)
  },[closesAt,onExpire])
  const total=Math.max(0,Math.floor(ms/1000)); const hh=String(Math.floor(total/3600)).padStart(2,'0'); const mm=String(Math.floor((total%3600)/60)).padStart(2,'0'); const ss=String(Math.floor(total%60)).padStart(2,'0')
  return <span className="font-mono">{hh}:{mm}:{ss}</span>
}
EOF

cat > src/features/sessao/VotoForm.tsx <<'EOF'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
const schema=z.object({ cpf:z.string().regex(/^\d{11}$/,'CPF deve ter 11 dígitos'), opcao:z.enum(['SIM','NAO']) })
type FormData=z.infer<typeof schema>
export function VotoForm({disabled,onSubmit,pending,error}:{disabled?:boolean;pending?:boolean;error?:string|null;onSubmit:(d:FormData)=>void}){
  const {register,handleSubmit,formState:{errors},reset}=useForm<FormData>({resolver:zodResolver(schema),defaultValues:{opcao:'SIM'}})
  return (<form onSubmit={handleSubmit(d=>{ onSubmit(d); reset({cpf:'',opcao:d.opcao}) })} className="space-y-3">
    <div><label className="block text-sm mb-1">CPF</label>
      <input className="w-full border rounded-lg px-3 py-2" placeholder="somente dígitos (11)" disabled={disabled||pending} {...register('cpf')}/>
      {errors.cpf&&<p className="text-sm text-red-600 mt-1">{errors.cpf.message}</p>}
    </div>
    <div className="flex items-center gap-4">
      <label className="inline-flex items-center gap-2 text-sm"><input type="radio" value="SIM" {...register('opcao')} disabled={disabled||pending}/> SIM</label>
      <label className="inline-flex items-center gap-2 text-sm"><input type="radio" value="NAO" {...register('opcao')} disabled={disabled||pending}/> NÃO</label>
    </div>
    {error&&<p className="text-sm text-red-600">{error}</p>}
    <button type="submit" disabled={disabled||pending} className="px-3 py-2 rounded-lg bg-black text-white disabled:opacity-60">{pending?'Enviando...':'Votar'}</button>
  </form>)}
EOF

cat > src/features/sessao/ResultadoPanel.tsx <<'EOF'
import { Pie, PieChart, Cell, ResponsiveContainer, Tooltip } from 'recharts'
export function ResultadoPanel({sim,nao,total}:{sim:number;nao:number;total:number}){
  const data=[{name:'SIM',value:sim},{name:'NÃO',value:nao}]
  return (<div className="grid md:grid-cols-2 gap-4">
    <div className="rounded-2xl border bg-white p-4">
      <h3 className="font-semibold mb-2">Totais</h3>
      <div className="flex gap-6 text-sm">
        <div className="p-3 rounded-xl border bg-neutral-50"><div className="text-neutral-500">SIM</div><div className="text-xl font-bold">{sim}</div></div>
        <div className="p-3 rounded-xl border bg-neutral-50"><div className="text-neutral-500">NÃO</div><div className="text-xl font-bold">{nao}</div></div>
        <div className="p-3 rounded-xl border bg-neutral-50"><div className="text-neutral-500">TOTAL</div><div className="text-xl font-bold">{total}</div></div>
      </div>
    </div>
    <div className="rounded-2xl border bg-white p-4">
      <h3 className="font-semibold mb-2">Gráfico</h3>
      <div className="h-64"><ResponsiveContainer width="100%" height="100%">
        <PieChart><Pie data={data} dataKey="value" nameKey="name" outerRadius={100} label>
          {data.map((_,i)=><Cell key={i}/>)}
        </PieChart><Tooltip/></ResponsiveContainer></div>
    </div>
  </div>)}
EOF

cat > src/features/sessao/SessaoPage.tsx <<'EOF'
import { useParams } from 'react-router-dom'
import { useState, useCallback } from 'react'
import { useResultado, useVotar } from './hooks'
import { VotoForm } from './VotoForm'
import { ResultadoPanel } from './ResultadoPanel'
import { Countdown } from './Countdown'
export function SessaoPage(){
  const { id='' } = useParams(); const [err,setErr]=useState<string|null>(null)
  const res=useResultado(id,true)
  const sim=res.data?.sim??0, nao=res.data?.nao??0, total=res.data?.total??0
  const status=(res.data?.status??'ABERTA').toUpperCase(); const isAberta=status==='ABERTA'
  if(res.refetchInterval && !isAberta){ res.refetch({ cancelRefetch: true }) }
  const votar=useVotar(id)
  const onSubmit=useCallback(async(data:{cpf:string;opcao:'SIM'|'NAO'})=>{
    setErr(null); try{ await votar.mutateAsync(data) } catch(e:any){ setErr(e?.userMessage||'Falha ao votar') }
  },[votar])
  return (<div className="space-y-6">
    <div className="flex items-center justify-between"><h2 className="text-xl font-semibold">Sessão da Pauta</h2>
      <div className="text-sm">Status: <span className={`font-semibold ${isAberta?'text-green-600':'text-red-600'}`}>{status}</span></div></div>
    <div className="grid md:grid-cols-2 gap-6">
      <div className="rounded-2xl border bg-white p-4">
        <h3 className="font-semibold mb-2">Votar</h3>
        <p className="text-sm text-neutral-600 mb-3">{isAberta?'Sessão aberta. Você pode votar uma única vez por pauta.':'Sessão encerrada.'}</p>
        <VotoForm disabled={!isAberta} onSubmit={onSubmit} pending={votar.isPending} error={err}/>
      </div>
      <div className="rounded-2xl border bg-white p-4">
        <h3 className="font-semibold mb-2">Tempo restante</h3>
        <div className="text-3xl">
          {isAberta ? <Countdown closesAt={new Date(Date.now()+60000).toISOString()} onExpire={()=>res.refetch()}/> : '00:00:00'}
        </div>
        <p className="text-xs text-neutral-500 mt-2">*Contador local; o backend é a fonte da verdade.</p>
      </div>
    </div>
    <ResultadoPanel sim={sim} nao={nao} total={total}/>
  </div>)}
EOF

# 8) axios base (se faltar)
cat > src/lib/axios.ts <<'EOF'
import axios from 'axios'
export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080', timeout: 8000 })
api.interceptors.response.use(r=>r,(err)=>{
  const s=err?.response?.status
  if (s===404) err.userMessage='Recurso não encontrado.'
  if (s===403) err.userMessage='Ação não permitida.'
  if (s===409) err.userMessage='Operação conflitante (já existe).'
  if (s===422) err.userMessage='Requisição inválida para o estado atual.'
  return Promise.reject(err)
})
EOF

log "OK! Agora rode:"
echo "  npm run dev"
echo "e acesse http://localhost:5173"

