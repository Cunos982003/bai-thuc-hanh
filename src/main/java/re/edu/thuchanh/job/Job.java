package re.edu.thuchanh.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import re.edu.thuchanh.company.Company;

import java.time.Instant;

@Entity
@Table(name = "jobs")
@Getter
@Setter
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    private Long salaryMin;

    private Long salaryMax;

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private JobStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Job() {
    }

    public static Job create(String title, String description, Long salaryMin, Long salaryMax,
                             String location, JobStatus status, Company company) {
        Job job = new Job();
        job.title = title;
        job.description = description;
        job.salaryMin = salaryMin;
        job.salaryMax = salaryMax;
        job.location = location;
        job.status = status;
        job.company = company;
        job.createdAt = Instant.now();
        return job;
    }
}
