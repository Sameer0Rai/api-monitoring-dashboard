
package com.monitor.controller;

import com.monitor.repo.*;
import com.monitor.model.*;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/services")
@CrossOrigin
public class ApiController{

 private final ApiServiceRepo serviceRepo;
 private final ApiLogRepo logRepo;

 public ApiController(ApiServiceRepo s,ApiLogRepo l){
  this.serviceRepo=s;
  this.logRepo=l;
 }

 @GetMapping
 public List<ApiService> services(){
  return serviceRepo.findAll();
 }

 @PostMapping
 public ApiService create(@RequestBody ApiService s){
  return serviceRepo.save(s);
 }

 @GetMapping("/logs/{id}")
 public List<ApiLog> logs(@PathVariable Long id){
  return logRepo.logs(id);
 }

 @GetMapping("/metrics/{id}")
 public Map<String,Object> metrics(@PathVariable Long id){

  long total=logRepo.total(id);
  long success=logRepo.success(id);

  double uptime= total==0?0:(success*100.0/total);
  Double avg=logRepo.avgLatency(id);

  String status="HEALTHY";

  if(avg!=null){
   if(avg>800) status="DOWN";
   else if(avg>400) status="SLOW";
  }

  Map<String,Object> m=new HashMap<>();
  m.put("uptime",uptime);
  m.put("avgLatency",avg==null?0:avg);
  m.put("status",status);

  return m;
 }

 @GetMapping("/summary")
 public Map<String,Object> summary(){

  Map<String,Object> m=new HashMap<>();
  m.put("apis",serviceRepo.count());

  return m;
 }
}
