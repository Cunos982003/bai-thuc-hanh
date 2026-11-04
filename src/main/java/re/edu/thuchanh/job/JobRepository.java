package re.edu.thuchanh.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRepository extends JpaRepository<Job, Long> {

    @Query(value = "SELECT count(*) > 0 FROM applications WHERE job_id = :id", nativeQuery = true)
    boolean existsApplicationByJobId(@Param("id") Long id);
}
