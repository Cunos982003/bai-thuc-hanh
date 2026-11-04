package re.edu.thuchanh.company;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import re.edu.thuchanh.common.ConflictException;
import re.edu.thuchanh.common.NotFoundException;
import re.edu.thuchanh.company.dto.CompanyRequest;
import re.edu.thuchanh.company.dto.CompanyResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional
    public CompanyResponse create(CompanyRequest request) {
        Company company = Company.create(request.name(), request.address(), request.website());
        return CompanyResponse.from(companyRepository.save(company));
    }

    public CompanyResponse get(Long id) {
        return CompanyResponse.from(findOrThrow(id));
    }

    public List<CompanyResponse> list() {
        return companyRepository.findAll().stream()
                .map(CompanyResponse::from)
                .toList();
    }

    @Transactional
    public CompanyResponse update(Long id, CompanyRequest request) {
        Company company = findOrThrow(id);
        company.setName(request.name());
        company.setAddress(request.address());
        company.setWebsite(request.website());
        return CompanyResponse.from(companyRepository.save(company));
    }

    @Transactional
    public void delete(Long id) {
        Company company = findOrThrow(id);
        if (companyRepository.existsJobByCompanyId(id)) {
            throw new ConflictException(
                    "Company %d still has jobs and cannot be deleted".formatted(id));
        }
        companyRepository.delete(company);
    }

    private Company findOrThrow(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Company %d not found".formatted(id)));
    }
}
