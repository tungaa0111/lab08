package mn.edu.must.sqat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {
    @Mock StudentRepository repository;
    @Mock EmailSender emailSender;
    CourseRegistrationService service;

    @BeforeEach void setUp() { service = new CourseRegistrationService(repository, emailSender); }
    private Student activeStudent() { return new Student("S001", "student@example.com", true, List.of("CS101")); }

    @Test void activeStudentWithCompletedPrerequisites_registersAndSendsEmail() {
        when(repository.findStudent("S001")).thenReturn(activeStudent());
        when(repository.courseExists("CS201")).thenReturn(true);
        when(repository.prerequisites("CS201")).thenReturn(List.of("CS101"));
        assertEquals("OK", service.register("S001", "CS201"));
        verify(repository).saveRegistration("S001", "CS201");
        verify(emailSender).send(eq("student@example.com"), eq("Бүртгэл баталгаажлаа"), contains("CS201"));
    }

    @Test void unknownStudent_returnsErrorWithoutSavingOrEmail() {
        when(repository.findStudent("missing")).thenReturn(null);
        assertEquals("ERROR_NO_STUDENT", service.register("missing", "CS201"));
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    @Test void inactiveStudent_returnsErrorWithoutEmailInteraction() {
        when(repository.findStudent("S002")).thenReturn(new Student("S002", "inactive@example.com", false, List.of()));
        assertEquals("ERROR_INACTIVE_STUDENT", service.register("S002", "CS201"));
        verifyNoInteractions(emailSender);
        verify(repository, never()).saveRegistration(anyString(), anyString());
    }

    @Test void missingPrerequisite_returnsErrorWithoutSaving() {
        when(repository.findStudent("S001")).thenReturn(activeStudent());
        when(repository.courseExists("CS201")).thenReturn(true);
        when(repository.prerequisites("CS201")).thenReturn(List.of("CS101", "CS102"));
        assertEquals("ERROR_PREREQUISITES", service.register("S001", "CS201"));
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    @Test void emailFailure_doesNotCancelSuccessfulRegistration() {
        when(repository.findStudent("S001")).thenReturn(activeStudent());
        when(repository.courseExists("CS201")).thenReturn(true);
        when(repository.prerequisites("CS201")).thenReturn(List.of("CS101"));
        doThrow(new RuntimeException("SMTP unavailable")).when(emailSender).send(anyString(), anyString(), anyString());
        assertEquals("OK", service.register("S001", "CS201"));
        verify(repository).saveRegistration("S001", "CS201");
        verify(emailSender).send(anyString(), anyString(), contains("CS201"));
    }

    @Test void nullStudentIdOrCourseId_throwsWithoutDependencyInteraction() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> service.register(null, "CS201")),
            () -> assertThrows(IllegalArgumentException.class, () -> service.register("S001", null))
        );
        verifyNoInteractions(repository, emailSender);
    }

    @Test void successfulRegistration_emailBodyContainsCourseCode() {
        when(repository.findStudent("S001")).thenReturn(activeStudent());
        when(repository.courseExists("CS201")).thenReturn(true);
        when(repository.prerequisites("CS201")).thenReturn(List.of("CS101"));
        service.register("S001", "CS201");
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq("student@example.com"), anyString(), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("CS201"));
    }

    @ParameterizedTest(name = "A8: {0} -> {1}")
    @CsvSource({"missing, ERROR_NO_STUDENT", "inactive, ERROR_INACTIVE_STUDENT", "no-course, ERROR_NO_COURSE"})
    void registrationErrorCases_returnExpectedError(String scenario, String expected) {
        if ("missing".equals(scenario)) {
            lenient().when(repository.findStudent(anyString())).thenReturn(null);
        } else if ("inactive".equals(scenario)) {
            lenient().when(repository.findStudent(anyString())).thenReturn(new Student("S002", "inactive@example.com", false, List.of()));
        } else {
            lenient().when(repository.findStudent(anyString())).thenReturn(activeStudent());
            lenient().when(repository.courseExists(anyString())).thenReturn(false);
        }
        assertEquals(expected, service.register("S001", "CS201"));
        verifyNoInteractions(emailSender);
    }
}
