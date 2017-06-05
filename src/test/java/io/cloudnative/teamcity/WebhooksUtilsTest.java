package io.cloudnative.teamcity;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.IdeaLogger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Logger.class, Files.class})
public class WebhooksUtilsTest {

    @Mock
    private IdeaLogger LOG;

    @Mock
    private IdeaLogger anyOtherLogger;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void before() {
        PowerMockito.mockStatic(Files.class);
        PowerMockito.mockStatic(Logger.class);
        when(Logger.getInstance(anyString())).thenReturn(anyOtherLogger);
        when(Logger.getInstance("jetbrains.buildServer.SERVER")).thenReturn(LOG);
    }

    @Test
    public void testMap() {
        assertEquals(ImmutableMap.of("a", "b"), WebhooksUtils.map("a", "b"));
    }

    @Test
    public void testReadJsonFile() throws IOException {
        File file = mock(File.class);
        when(Files.toString(file, Charset.forName("UTF-8"))).thenReturn("{\"a\": \"1\", \"b\": \"2\"}");

        Map<String, ?> stringMap = WebhooksUtils.readJsonFile(file);

        assertEquals(ImmutableMap.of(
                "a", "1",
                "b", "2"
        ), stringMap);
    }

    @Test
    public void testPath_canonical() throws IOException {
        File file = mock(File.class);

        when(file.getCanonicalPath()).thenReturn("/canonical_path");
        when(file.getAbsolutePath()).thenReturn("/absolute_path");

        String path = WebhooksUtils.path(file);

        assertEquals("/canonical_path", path);
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void testPath_absolute() throws IOException {
        File file = mock(File.class);

        doThrow(new IOException()).when(file).getCanonicalPath();
        when(file.getAbsolutePath()).thenReturn("/absolute_path");

        String path = WebhooksUtils.path(file);

        assertEquals("/absolute_path", path);
    }

    @Test
    public void testIsEmpty() throws IOException {
        assertFalse(WebhooksUtils.isEmpty("a", "b"));
        assertTrue(WebhooksUtils.isEmpty("", "b"));
        assertTrue(WebhooksUtils.isEmpty(null, "b"));
    }

    @Test
    public void testNotEmpty_noMessage() throws IOException {
        assertTrue(WebhooksUtils.notEmpty("a"));
        assertFalse(WebhooksUtils.notEmpty(""));
        assertFalse(WebhooksUtils.notEmpty(null));
    }

    @Test
    public void testNotEmpty_onNotEmptyWithMessage() throws IOException {
        assertEquals("a", WebhooksUtils.notEmpty("a", "b"));
    }

    @Test
    public void testNotEmpty_onEmptyWithMessage() throws IOException {
        exception.expect(RuntimeException.class);
        exception.expectMessage("is empty");

        assertEquals("a", WebhooksUtils.notEmpty("", "is empty"));
    }

    @Test
    public void testNotEmpty_onNullWithMessage() throws IOException {
        exception.expect(RuntimeException.class);
        exception.expectMessage("is null");

        assertEquals("a", WebhooksUtils.notEmpty(null, "is null"));
    }

    @Test
    public void testLog() {
        WebhooksUtils.log("message");

        verifyStatic();
        LOG.info("WebHooks plugin - message");
    }

    @Test
    public void testError_noThrowable() {
        WebhooksUtils.error("message");

        verifyStatic();
        LOG.error("WebHooks plugin - message");
    }

    @Test
    public void testError_withThrowable() {
        Throwable throwable = new RuntimeException();
        WebhooksUtils.error("message", throwable);

        verifyStatic();
        LOG.error("WebHooks plugin - message", throwable);
    }
}
