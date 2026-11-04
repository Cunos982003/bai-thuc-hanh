package re.edu.thuchanh.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    @Query(value = "SELECT count(*) > 0 FROM jobs WHERE company_id = :id", nativeQuery = true)
    boolean existsJobByCompanyId(@Param("id") Long id);
}
