
package com.monitor.repo;
import com.monitor.model.ApiLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ApiLogRepo extends JpaRepository<ApiLog,Long>{

 @Query("SELECT l FROM ApiLog l WHERE l.serviceId=?1 ORDER BY l.timestamp")
 List<ApiLog> logs(Long serviceId);

 @Query("SELECT AVG(l.responseTime) FROM ApiLog l WHERE l.serviceId=?1")
 Double avgLatency(Long serviceId);

 @Query("SELECT COUNT(l) FROM ApiLog l WHERE l.serviceId=?1")
 long total(Long serviceId);

 @Query("SELECT COUNT(l) FROM ApiLog l WHERE l.serviceId=?1 AND l.statusCode=200")
 long success(Long serviceId);

}
