package com.businessmanager.backend.common.audit;

import com.businessmanager.backend.common.audit.annotation.AuditAction;
import com.businessmanager.backend.common.audit.aspect.AuditLogAspect;
import com.businessmanager.backend.common.audit.entity.AuditLogEntry;
import com.businessmanager.backend.common.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {
        AuditLogAspectTest.TestServiceImpl.class,
        AuditLogAspect.class,
        ObjectMapper.class
})
@EnableAspectJAutoProxy
public class AuditLogAspectTest {

    @Autowired
    private TestService testService;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @Test
    @WithMockUser(username = "test-auditor")
    public void testAuditActionAspect_InterceptsAndSavesLog() {
        DummyEntity entity = new DummyEntity(42L, "John Doe");
        testService.createEntity(entity);

        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLogEntry capturedLog = captor.getValue();
        assertNotNull(capturedLog);
        assertEquals("test-auditor", capturedLog.getUsername());
        assertEquals("CREATE", capturedLog.getActionType());
        assertEquals("CUSTOMER", capturedLog.getModuleName());
        assertEquals("42", capturedLog.getEntityId());
        assertNotNull(capturedLog.getAfterValue());
    }

    interface TestService {
        DummyEntity createEntity(DummyEntity entity);
    }

    @Service
    static class TestServiceImpl implements TestService {
        @Override
        @AuditAction(action = "CREATE", module = "CUSTOMER")
        public DummyEntity createEntity(DummyEntity entity) {
            return entity;
        }
    }

    static class DummyEntity {
        private Long id;
        private String name;

        public DummyEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
