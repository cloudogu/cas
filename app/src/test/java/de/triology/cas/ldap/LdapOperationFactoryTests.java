package de.triology.cas.ldap;

import org.ldaptive.AddOperation;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.ModifyOperation;
import org.ldaptive.SearchOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LdapOperationFactory}.
 */
class LdapOperationFactoryTests {

    private LdapOperationFactory factory;
    private ConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        connectionFactory = mock(ConnectionFactory.class);
        factory = new LdapOperationFactory(connectionFactory);
    }

    @Test
    void searchOperation_ShouldReturnSearchOperationInstance() {
        // when
        SearchOperation operation = factory.searchOperation();

        // then
        assertNotNull(operation, "SearchOperation should not be null");
        assertTrue(operation instanceof SearchOperation, "Should return a SearchOperation instance");
    }

    @Test
    void addOperation_ShouldReturnAddOperationInstance() {
        // when
        AddOperation operation = factory.addOperation();

        // then
        assertNotNull(operation, "AddOperation should not be null");
        assertTrue(operation instanceof AddOperation, "Should return an AddOperation instance");
    }

    @Test
    void modifyOperation_ShouldReturnModifyOperationInstance() {
        // when
        ModifyOperation operation = factory.modifyOperation();

        // then
        assertNotNull(operation, "ModifyOperation should not be null");
        assertTrue(operation instanceof ModifyOperation, "Should return a ModifyOperation instance");
    }
}
