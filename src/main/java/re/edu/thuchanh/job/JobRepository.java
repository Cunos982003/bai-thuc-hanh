package re.edu.thuchanh.job;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRepository extends JpaRepository<Job, Long> {

    @Query(value = "SELECT count(*) > 0 FROM applications WHERE job_id = :id", nativeQuery = true)
    boolean existsApplicationByJobId(@Param("id") Long id);

    @Query(value = """
            SELECT j FROM Job j JOIN FETCH j.company
            WHERE (:keyword IS NULL OR LOWER(j.title) LIKE :keyword ESCAPE '\\'
                                     OR LOWER(j.description) LIKE :keyword ESCAPE '\\')
              AND (:location IS NULL OR LOWER(j.location) = :location)
              AND (:status IS NULL OR j.status = :status)
            """,
            countQuery = """
            SELECT COUNT(j) FROM Job j
            WHERE (:keyword IS NULL OR LOWER(j.title) LIKE :keyword ESCAPE '\\'
                                     OR LOWER(j.description) LIKE :keyword ESCAPE '\\')
              AND (:location IS NULL OR LOWER(j.location) = :location)
              AND (:status IS NULL OR j.status = :status)
            """)
    Page<Job> search(@Param("keyword") String keyword,
                     @Param("location") String location,
                     @Param("status") JobStatus status,
                     Pageable pageable);
}
