package re.edu.thuchanh.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import re.edu.thuchanh.job.Job;

import java.time.Instant;

@Entity
@Table(name = "applications")
@Getter
@Setter
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    @Column(name = "cv_url", nullable = false, length = 1000)
    private String cvUrl;

    @Column(name = "applied_at", nullable = false, updatable = false)
    private Instant appliedAt;

    protected JobApplication() {
    }

    public static JobApplication create(Job job, String fullName, String email, String cvUrl) {
        JobApplication application = new JobApplication();
        application.job = job;
        application.fullName = fullName;
        application.email = email;
        application.cvUrl = cvUrl;
        application.appliedAt = Instant.now();
        return application;
    }
}
