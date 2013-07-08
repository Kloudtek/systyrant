/*
 * Copyright (c) 2013 KloudTek Ltd
 */

package com.kloudtek.systyrant.resource;

import com.kloudtek.systyrant.AbstractVagrantTest;
import com.kloudtek.systyrant.exception.InvalidResourceDefinitionException;
import com.kloudtek.systyrant.exception.ResourceCreationException;
import com.kloudtek.systyrant.exception.STRuntimeException;
import com.kloudtek.systyrant.host.Host;
import com.kloudtek.systyrant.resource.core.AptPackageProvider;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.testng.Assert.*;

public class AptPackageProviderSSHTest extends AbstractVagrantTest {
    private static final String APTCMD = "apt-get -o Dpkg::Options::='--force-confdef' -o Dpkg::Options::='--force-confold' -f -q -y ";
    public static final String AESPIPE = "aespipe";
    private AptPackageProvider provider;

    @BeforeClass(groups = "vagrant")
    public void init() throws STRuntimeException, IOException, ResourceCreationException, InvalidResourceDefinitionException {
        super.init();
        provider = new AptPackageProvider(sshHost);
        sshHost.exec("apt-get update");
    }

    @Test(groups = "vagrant")
    public void testCheckCurrentlyInstalledThatIs() throws Exception {
        String version = findVersion(AESPIPE);
        installPkg(AESPIPE);
        assertEquals(provider.checkCurrentlyInstalled(AESPIPE), version);
    }

    @Test(groups = "vagrant")
    public void testCheckCurrentlyInstalledThatIsNot() throws Exception {
        removePkg(AESPIPE);
        assertNull(provider.checkCurrentlyInstalled(AESPIPE));
    }

    @Test(groups = "vagrant")
    public void testCheckAvailableExistent() throws Exception {
        String version = findVersion(AESPIPE);
        assertEquals(provider.checkLatestAvailable(AESPIPE), version);
    }

    @Test(groups = "vagrant")
    public void testOneIsNewerThanNothing() throws Exception {
        assertTrue(provider.isNewer("1.0", null));
    }

    @Test(groups = "vagrant")
    public void testNothingIsNotNewerThanOne() throws Exception {
        assertFalse(provider.isNewer(null, "1.0"));
    }

    @Test(groups = "vagrant")
    public void test10IsNotNewerThan10() throws Exception {
        assertFalse(provider.isNewer("1.0", "1.0"));
    }

    @Test(groups = "vagrant")
    public void test10IsNotNewerThan11() throws Exception {
        assertFalse(provider.isNewer("1.0", "1.1"));
    }

    @Test(groups = "vagrant")
    public void testNullIsNotNewerThan11() throws Exception {
        assertFalse(provider.isNewer(null, "1.1"));
    }

    @Test(groups = "vagrant")
    public void test10devIsNotNewerThan10() throws Exception {
        assertFalse(provider.isNewer("1.0~dev", "1.0"));
    }

    @Test(groups = "vagrant")
    public void test11IsNewerThan10() throws Exception {
        assertTrue(provider.isNewer("1.1", "1.0"));
    }

    @Test(groups = "vagrant")
    public void test11IsNewerThanNull() throws Exception {
        assertTrue(provider.isNewer("1.1", null));
    }

    @Test(groups = "vagrant")
    public void test10IsNewerThan10test() throws Exception {
        assertTrue(provider.isNewer("1.0", "1.0~test"));
    }

    @Test(groups = "vagrant")
    public void testInstallLatest() throws Exception {
        String available = sshHost.exec("apt-cache show aespipe | grep Version", null, 0, Host.Logging.YES, "root").getOutput().substring(9).trim();
        removePkg(AESPIPE);
        provider.install(AESPIPE, null);
        String installed = sshHost.exec("dpkg -s aespipe | grep Version").substring(9).trim();
        assertEquals(installed, available);
    }

    private String findVersion(String pkg) throws STRuntimeException {
        return sshHost.exec("apt-cache show " + pkg + " | grep Version").substring(9).trim();
    }

    private void removePkg(String pkg) throws STRuntimeException {
        apt("purge -y " + pkg);
    }

    private String apt(String cmd) throws STRuntimeException {
        HashMap<String, String> env = new HashMap<>();
        env.put("DEBIAN_FRONTEND", "noninteractive");
        return sshHost.exec(APTCMD + " " + cmd, null, Host.Logging.YES, env).getOutput();
    }

    private void installPkg(String pkg) throws STRuntimeException {
        apt("install -y " + pkg);
        apt("upgrade -y " + pkg);
    }
}