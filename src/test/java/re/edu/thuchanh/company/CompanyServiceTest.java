package re.edu.thuchanh.company;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import re.edu.thuchanh.common.ConflictException;
import re.edu.thuchanh.common.NotFoundException;
import re.edu.thuchanh.company.dto.CompanyRequest;
import re.edu.thuchanh.company.dto.CompanyResponse;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock CompanyRepository companyRepository;
    @InjectMocks CompanyService companyService;

    @Test
    void createPersistsAndReturnsResponse() {
        when(companyRepository.save(any(Company.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CompanyResponse response = companyService.create(
                new CompanyRequest("Acme", "Hanoi", "https://acme.example"));

        assertThat(response.name()).isEqualTo("Acme");
        assertThat(response.address()).isEqualTo("Hanoi");
    }

    @Test
    void getThrowsNotFoundForUnknownId() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.get(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void listReturnsAllCompanies() {
        when(companyRepository.findAll()).thenReturn(List.of(
                Company.create("Acme", null, null),
                Company.create("Globex", null, null)));

        assertThat(companyService.list()).hasSize(2);
    }

    @Test
    void updateReplacesFields() {
        Company existing = Company.create("Old", "Old addr", null);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(companyRepository.save(existing)).thenReturn(existing);

        CompanyResponse response = companyService.update(1L,
                new CompanyRequest("New", "New addr", "https://new.example"));

        assertThat(response.name()).isEqualTo("New");
        assertThat(response.address()).isEqualTo("New addr");
    }

    @Test
    void updateThrowsNotFoundForUnknownId() {
        when(companyRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.update(42L, new CompanyRequest("x", null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRemovesCompanyWithNoJobs() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(Company.create("Acme", null, null)));
        when(companyRepository.existsJobByCompanyId(1L)).thenReturn(false);

        companyService.delete(1L);

        verify(companyRepository).delete(any(Company.class));
    }

    @Test
    void deleteThrowsConflictWhenCompanyStillHasJobs() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(Company.create("Acme", null, null)));
        when(companyRepository.existsJobByCompanyId(1L)).thenReturn(true);

        assertThatThrownBy(() -> companyService.delete(1L))
                .isInstanceOf(ConflictException.class);

        verify(companyRepository, never()).delete(any(Company.class));
    }

    @Test
    void deleteThrowsNotFoundForUnknownId() {
        when(companyRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.delete(7L))
                .isInstanceOf(NotFoundException.class);
    }
}
