package re.edu.thuchanh.job;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import re.edu.thuchanh.common.ConflictException;
import re.edu.thuchanh.common.NotFoundException;
import re.edu.thuchanh.company.Company;
import re.edu.thuchanh.company.CompanyRepository;
import re.edu.thuchanh.job.dto.JobRequest;
import re.edu.thuchanh.job.dto.JobResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobService {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public JobResponse create(JobRequest request) {
        Company company = findCompanyOrThrow(request.companyId());
        Job job = Job.create(request.title(), request.description(), request.salaryMin(),
                request.salaryMax(), request.location(), request.status(), company);
        return JobResponse.from(jobRepository.save(job));
    }

    public JobResponse get(Long id) {
        return JobResponse.from(findOrThrow(id));
    }

    @Transactional
    public JobResponse update(Long id, JobRequest request) {
        Job job = findOrThrow(id);
        Company company = findCompanyOrThrow(request.companyId());
        job.setTitle(request.title());
        job.setDescription(request.description());
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        job.setLocation(request.location());
        job.setStatus(request.status());
        job.setCompany(company);
        return JobResponse.from(jobRepository.save(job));
    }

    @Transactional
    public void delete(Long id) {
        Job job = findOrThrow(id);
        if (jobRepository.existsApplicationByJobId(id)) {
            throw new ConflictException(
                    "Job %d still has applications and cannot be deleted".formatted(id));
        }
        jobRepository.delete(job);
    }

    private Job findOrThrow(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Job %d not found".formatted(id)));
    }

    private Company findCompanyOrThrow(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException(
                        "Company %d not found".formatted(companyId)));
    }
}
