
import axios from "axios"

const BASE="http://localhost:8080/api/services"

export const getServices=()=>axios.get(BASE)
export const getLogs=(id)=>axios.get(BASE+"/logs/"+id)
export const getMetrics=(id)=>axios.get(BASE+"/metrics/"+id)
export const createApi=(data)=>axios.post(BASE,data)
