package de.triology.cas.services;

import org.junit.Test;

import static org.junit.Assert.*;

public class GetDoguNodeFromEtcdExceptionTest {

    @Test
    public void instantiateGetDoguNodeFromEtcdExceptionTest() {
        GetDoguNodeFromEtcdException getDoguNodeFromEtcdException = new GetDoguNodeFromEtcdException();
        assertNotNull(getDoguNodeFromEtcdException);
    }

    @Test(expected = GetDoguNodeFromEtcdException.class)
    public void throwGetDoguNodeFromEtcdExceptionTest() throws GetDoguNodeFromEtcdException {
        GetDoguNodeFromEtcdException getDoguNodeFromEtcdException = new GetDoguNodeFromEtcdException();
        throw getDoguNodeFromEtcdException;
    }

}