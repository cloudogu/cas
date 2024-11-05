package de.triology.cas.ldap;

import org.ldaptive.AddOperation;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.ModifyOperation;
import org.ldaptive.SearchOperation;

public class LdapOperationFactory {

    private final ConnectionFactory connectionFactory;

    public LdapOperationFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public SearchOperation searchOperation() {
        return new SearchOperation(connectionFactory);
    }

    public AddOperation addOperation() {
        return new AddOperation(connectionFactory);
    }

    public ModifyOperation modifyOperation() {
        return new ModifyOperation(connectionFactory);
    }
}
