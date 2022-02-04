package dev.vmix.jmxutils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;

import org.junit.Test;

public class JmxLocalConnectorTest {

    @Test
    public void test() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        if (name == null || name.indexOf('@') < 0)
            fail("Cannot get PID");
        String pidStr = name.split("@")[0];
        String addr = JmxLocalConnector.getLocalConnectorAddress(pidStr);
        assertThat(addr, is(notNullValue()));
    }
}
