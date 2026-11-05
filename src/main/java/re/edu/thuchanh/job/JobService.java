package re.edu.thuchanh.job;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import re.edu.thuchanh.common.BadRequestException;
import re.edu.thuchanh.common.ConflictException;
import re.edu.thuchanh.common.LikePattern;
import re.edu.thuchanh.common.NotFoundException;
import re.edu.thuchanh.common.PageResponse;
import re.edu.thuchanh.company.Company;
import re.edu.thuchanh.company.CompanyRepository;
import re.edu.thuchanh.job.dto.JobRequest;
import re.edu.thuchanh.job.dto.JobResponse;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("title", "salaryMin", "salaryMax", "location", "createdAt");

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

    public PageResponse<JobResponse> search(String keyword, String location, JobStatus status,
                                            int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        Page<Job> found = jobRepository.search(
                LikePattern.containing(blankToNull(keyword)),
                lowercased(blankToNull(location)),
                status,
                pageable);
        return PageResponse.of(found.map(JobResponse::from));
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

    private static Sort resolveSort(String sort) {
        String field = sort == null ? "title" : sort.trim();
        boolean descending = field.startsWith("-");
        if (descending) {
            field = field.substring(1);
        }
        if (!SORTABLE_FIELDS.contains(field)) {
            throw new BadRequestException("Unsupported sort field: " + field);
        }
        return descending ? Sort.by(field).descending() : Sort.by(field).ascending();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String lowercased(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
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
