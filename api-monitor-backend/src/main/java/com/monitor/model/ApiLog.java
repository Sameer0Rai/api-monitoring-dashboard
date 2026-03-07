
package com.monitor.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ApiLog{

 @Id
 @GeneratedValue(strategy=GenerationType.IDENTITY)
 private Long id;

 private Long serviceId;
 private int responseTime;
 private int statusCode;
 private LocalDateTime timestamp;

 public Long getId(){return id;}
 public Long getServiceId(){return serviceId;}
 public int getResponseTime(){return responseTime;}
 public int getStatusCode(){return statusCode;}
 public LocalDateTime getTimestamp(){return timestamp;}

 public void setId(Long id){this.id=id;}
 public void setServiceId(Long serviceId){this.serviceId=serviceId;}
 public void setResponseTime(int responseTime){this.responseTime=responseTime;}
 public void setStatusCode(int statusCode){this.statusCode=statusCode;}
 public void setTimestamp(LocalDateTime timestamp){this.timestamp=timestamp;}
}
