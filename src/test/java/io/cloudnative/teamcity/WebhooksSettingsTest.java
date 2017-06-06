package io.cloudnative.teamcity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.IdeaLogger;
import jetbrains.buildServer.serverSide.ServerPaths;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Logger.class, WebhooksUtils.class, Files.class})
public class WebhooksSettingsTest {

    @Mock
    private IdeaLogger anyLogger;

    @Mock
    private ServerPaths serverPaths;

    @Mock
    private File settingsFile;

    private WebhooksSettings target;

    @Before
    public void before() throws Exception {
        PowerMockito.mockStatic(Logger.class);
        PowerMockito.mockStatic(WebhooksUtils.class);
        PowerMockito.mockStatic(Files.class);

        when(Logger.getInstance(anyString())).thenReturn(anyLogger);
    }

    @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
    private void prepareTarget() {
        Map<String, List<String>> map = Maps.newLinkedHashMap();
        map.put("projectIdA", Lists.newArrayList("http://a1", "http://a2"));
        map.put("projectIdB", Lists.newArrayList("http://b1", "http://b2"));
        when(WebhooksUtils.readJsonFile(settingsFile)).thenReturn((Map) map);

        when(serverPaths.getConfigDir()).thenReturn("/config_dir");
        when(WebhooksUtils.newFile("/config_dir", "webhooks.json")).thenReturn(settingsFile);

        doReturn(true).when(settingsFile).isFile();

        target = new WebhooksSettings(serverPaths);
    }

    @Test
    public void testGetUrls_existingId() {
        prepareTarget();

        when(WebhooksUtils.notEmpty(eq("projectIdA"), anyString())).thenReturn("projectIdA");

        Set<String> urls = target.getUrls("projectIdA");

        assertEquals(asSet("http://a1", "http://a2"), urls);
    }

    @Test
    public void testGetUrls_addId() throws IOException {
        prepareTarget();

        when(WebhooksUtils.notEmpty(eq("projectIdC"), anyString())).thenReturn("projectIdC");
        when(WebhooksUtils.notEmpty(eq("http://c1"), anyString())).thenReturn("http://c1");

        target.addUrl("projectIdC", "http://c1");
        Set<String> urls = target.getUrls("projectIdC");

        assertEquals(asSet("http://c1"), urls);

        verifyStatic();
        Files.write("{\"projectIdA\":[\"http://a1\",\"http://a2\"],\"projectIdB\":[\"http://b1\",\"http://b2\"],\"projectIdC\":[\"http://c1\"]}",
                settingsFile, Charset.forName("UTF-8"));
    }

    @Test
    public void testGetUrls_removeId() throws IOException {
        prepareTarget();

        when(WebhooksUtils.notEmpty(eq("projectIdA"), anyString())).thenReturn("projectIdA");
        when(WebhooksUtils.notEmpty(eq("http://a1"), anyString())).thenReturn("http://a1");

        target.removeUrl("projectIdA", "http://a1");
        Set<String> urls = target.getUrls("projectIdA");

        assertEquals(asSet("http://a2"), urls);

        verifyStatic();
        Files.write("{\"projectIdA\":[\"http://a2\"],\"projectIdB\":[\"http://b1\",\"http://b2\"]}",
                settingsFile, Charset.forName("UTF-8"));
    }

    @NotNull
    private Set<String> asSet(String ... args) {
        return Sets.newLinkedHashSet(Arrays.asList(args));
    }
}
