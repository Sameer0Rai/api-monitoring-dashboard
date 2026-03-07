
package com.monitor.scheduler;

import com.monitor.repo.*;
import com.monitor.model.*;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class HealthScheduler{

 private final ApiServiceRepo serviceRepo;
 private final ApiLogRepo logRepo;

 private final RestTemplate restTemplate=new RestTemplate();

 public HealthScheduler(ApiServiceRepo s,ApiLogRepo l){
  this.serviceRepo=s;
  this.logRepo=l;
 }

 @Scheduled(fixedRate=60000)
 public void monitor(){

  List<ApiService> services=serviceRepo.findAll();

  for(ApiService s:services){

   long start=System.currentTimeMillis();
   int status=500;

   try{
    restTemplate.getForEntity(s.getUrl(),String.class);
    status=200;
   }catch(Exception e){}

   int latency=(int)(System.currentTimeMillis()-start);

   ApiLog log=new ApiLog();
   log.setServiceId(s.getId());
   log.setResponseTime(latency);
   log.setStatusCode(status);
   log.setTimestamp(LocalDateTime.now());

   logRepo.save(log);
  }
 }
}
