
import React,{useEffect,useState} from "react"
import {getServices,getLogs,getMetrics,createApi} from "./api"
import {LineChart,Line,XAxis,YAxis,Tooltip,CartesianGrid,ResponsiveContainer} from "recharts"

export default function App(){

 const [services,setServices]=useState([])
 const [logs,setLogs]=useState({})
 const [metrics,setMetrics]=useState({})
 const [name,setName]=useState("")
 const [url,setUrl]=useState("")

 const load=()=>{

  getServices().then(res=>{
   setServices(res.data)

   res.data.forEach(s=>{

    getLogs(s.id).then(r=>{
     const data=r.data.map((l,i)=>({time:i,latency:l.responseTime}))
     setLogs(prev=>({...prev,[s.id]:data}))
    })

    getMetrics(s.id).then(r=>{
     setMetrics(prev=>({...prev,[s.id]:r.data}))
    })

   })

  })

 }

 useEffect(()=>{
  load()
  const i=setInterval(load,5000)
  return()=>clearInterval(i)
 },[])

 const addApi=()=>{
  createApi({name,url}).then(()=>{
   setName("")
   setUrl("")
   load()
  })
 }

 const statusBadge=(s)=>{
  if(s==="HEALTHY") return "badge green"
  if(s==="SLOW") return "badge yellow"
  return "badge red"
 }

 return(
 <div className="container">

 <h1 style={{fontSize:"36px"}}>API Monitoring Dashboard</h1>

 <form onSubmit={(e)=>{e.preventDefault();addApi()}}>
  <input placeholder="API Name" value={name} onChange={e=>setName(e.target.value)}/>
  <input placeholder="API URL" value={url} onChange={e=>setUrl(e.target.value)}/>
  <button>Add API</button>
 </form>

 <div className="cards">

 {services.map(s=>{

 const m=metrics[s.id]

 return(
 <div key={s.id} className="card">

 <h3>{s.name}</h3>
 <p>{s.url}</p>

 {m && (
 <>
 <p>Uptime: {m.uptime.toFixed(2)}%</p>
 <span className={statusBadge(m.status)}>
 {m.status}
 </span>
 </>
 )}

 </div>
 )
 })}

 </div>

 {services.map(s=>(

 <div key={s.id} className="chart">

 <h2>{s.name} Latency</h2>

 <ResponsiveContainer width="100%" height={250}>
 <LineChart data={logs[s.id]||[]}>
 <CartesianGrid stroke="#1e293b"/>
 <XAxis dataKey="time"/>
 <YAxis/>
 <Tooltip/>
 <Line type="monotone" dataKey="latency" stroke="#22c55e" strokeWidth={3} dot={false}/>
 </LineChart>
 </ResponsiveContainer>

 </div>

 ))}

 </div>
 )
}
