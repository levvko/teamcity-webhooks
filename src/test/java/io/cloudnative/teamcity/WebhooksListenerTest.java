package io.cloudnative.teamcity;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.StatusDescriptor;
import jetbrains.buildServer.log.IdeaLogger;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.serverSide.artifacts.ArtifactsGuard;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifacts;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode;
import jodd.http.HttpRequest;
import jodd.http.HttpResponse;
import jodd.http.net.SocketHttpConnection;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Logger.class, HttpRequest.class})
public class WebhooksListenerTest {

    private static final Gson GSON = new Gson();

    private static IdeaLogger LOG = mock(IdeaLogger.class);

    @Mock
    private IdeaLogger anyOtherLogger;

    @Mock
    private WebhooksSettings settings;

    @Mock
    private SBuildServer buildServer;

    @Mock
    @SuppressWarnings("unused")
    private ServerPaths serverPaths;

    @Mock
    @SuppressWarnings("unused")
    private ArtifactsGuard artifactsGuard;

    @Mock
    private SRunningBuild build;

    @Mock
    private Socket socket;

    @InjectMocks
    private WebhooksListener target;

    @Before
    public void before() {
        reset(LOG);

        PowerMockito.mockStatic(Logger.class);
        PowerMockito.mockStatic(HttpRequest.class);

        when(Logger.getInstance(anyString())).thenReturn(anyOtherLogger);
        when(Logger.getInstance("jetbrains.buildServer.SERVER")).thenReturn(LOG);

        when(settings.getUrls("projectExternalId")).thenReturn(Collections.singleton("http://url"));
        when(buildServer.findBuildInstanceById(1L)).thenReturn(build);
        when(buildServer.getRootUrl()).thenReturn("http://root");
    }

    @Test
    public void testRegister() {
        target.register();

        verify(buildServer).addListener(target);
    }

    @Test
    public void testBuildFinished_statusDescriptorSuccess() throws SocketException {
        StatusDescriptor statusDescriptor = prepareStatusDescriptor("Success", true);
        prepareBuild(statusDescriptor);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        preparePostRequest("http://url", bodyCaptor, 200);

        target.buildFinished(build);

        String expectedBody = GSON.toJson(WebhookPayload.of("Build", "Build completed", "229911", Collections.singletonList(
                WebhookPayload.Section.builder()
                        .facts(Lists.newArrayList(
                                WebhookPayload.Fact.builder().name("Status").value("[Success](http://root/viewLog.html?buildTypeId=2&buildId=1)").build(),
                                WebhookPayload.Fact.builder().name("Artifacts").value("[View](http://root/viewLog.html?buildTypeId=2&buildId=1&tab=artifacts)").build()
                        ))
                        .build()
        )));
        assertEquals(expectedBody, bodyCaptor.getValue());
        verify(LOG).info("WebHooks plugin - Build 'Build/#3' finished, payload is '" + bodyCaptor.getValue() + "'");
        verify(socket).setSoTimeout(10000);
        verify(LOG).info("WebHooks plugin - Payload POST-ed to 'http://url'");
        verify(LOG).info(matches("WebHooks plugin - Operation finished in \\d+ ms"));
        verifyNoErrors(LOG);
    }

    @Test
    public void testBuildFinished_statusDescriptorFailure() throws SocketException {
        StatusDescriptor statusDescriptor = prepareStatusDescriptor("Failure", false);
        prepareBuild(statusDescriptor);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        preparePostRequest("http://url", bodyCaptor, 200);

        target.buildFinished(build);

        String expectedBody = GSON.toJson(WebhookPayload.of("Build", "Build completed", "AA0000", Collections.singletonList(
                WebhookPayload.Section.builder()
                        .facts(Lists.newArrayList(
                                WebhookPayload.Fact.builder().name("Status").value("[Failed](http://root/viewLog.html?buildTypeId=2&buildId=1)").build(),
                                WebhookPayload.Fact.builder().name("Message").value("Failure").build(),
                                WebhookPayload.Fact.builder().name("Artifacts").value("[View](http://root/viewLog.html?buildTypeId=2&buildId=1&tab=artifacts)").build()
                        ))
                        .build()
        )));
        assertEquals(expectedBody, bodyCaptor.getValue());
        verifyNoErrors(LOG);
    }

    @Test
    public void testBuildFinished_postPayloadFailure() throws SocketException {
        StatusDescriptor statusDescriptor = prepareStatusDescriptor("Success", true);
        prepareBuild(statusDescriptor);

        preparePostRequest("http://url", ArgumentCaptor.forClass(String.class), 500);

        target.buildFinished(build);

        verify(LOG).error(matches("WebHooks plugin - POST-ing payload to 'http://url' - got 500 response: Mock for HttpResponse, hashCode: \\d+"));
    }

    @Test
    public void testBuildFinished_postPayloadException() throws SocketException {
        StatusDescriptor statusDescriptor = prepareStatusDescriptor("Success", true);
        prepareBuild(statusDescriptor);

        HttpRequest httpRequest = preparePostRequest("http://url", ArgumentCaptor.forClass(String.class), 200);
        Throwable exception = new RuntimeException();
        doThrow(exception).when(httpRequest).send();

        target.buildFinished(build);

        verify(LOG).error("WebHooks plugin - Failed to POST payload to 'http://url'", exception);
    }

    @Test
    public void testBuildFinished_buildFinishedException() throws SocketException {
        StatusDescriptor statusDescriptor = prepareStatusDescriptor("Success", true);
        prepareBuild(statusDescriptor);
        Throwable exception = new RuntimeException();
        doThrow(exception).when(build).getProjectExternalId();

        target.buildFinished(build);

        verify(LOG).error("WebHooks plugin - Failed to listen on buildFinished() of 'Build' #3", exception);
    }

    private void prepareBuild(StatusDescriptor statusDescriptor) {
        when(build.getStatusDescriptor()).thenReturn(statusDescriptor);

        when(build.getBuildId()).thenReturn(1L);
        when(build.getBuildNumber()).thenReturn("3");
        when(build.getProjectExternalId()).thenReturn("projectExternalId");
        when(build.getFullName()).thenReturn("Build");

        SBuildType sBuildType = prepareSBuildType();
        when(build.getBuildType()).thenReturn(sBuildType);

        BuildArtifacts buildArtifacts = prepareBuildArtifacts();
        when(build.getArtifacts(BuildArtifactsViewMode.VIEW_DEFAULT)).thenReturn(buildArtifacts);
    }

    @NotNull
    private BuildArtifacts prepareBuildArtifacts() {
        BuildArtifacts buildArtifacts = mock(BuildArtifacts.class);
        when(buildArtifacts.isAvailable()).thenReturn(true);
        return buildArtifacts;
    }

    @NotNull
    private SBuildType prepareSBuildType() {
        SBuildType sBuildType = mock(SBuildType.class);
        when(sBuildType.getExternalId()).thenReturn("2");
        return sBuildType;
    }

    @NotNull
    private StatusDescriptor prepareStatusDescriptor(String text, boolean isSuccessful) {
        StatusDescriptor statusDescriptor = mock(StatusDescriptor.class);
        when(statusDescriptor.getText()).thenReturn(text);
        when(statusDescriptor.isSuccessful()).thenReturn(isSuccessful);
        return statusDescriptor;
    }

    @SuppressWarnings("SameParameterValue")
    private HttpRequest preparePostRequest(String url, ArgumentCaptor<String> bodyCaptor, int statusCode) {
        HttpRequest httpRequest = mock(HttpRequest.class);
        when(HttpRequest.post(url)).thenReturn(httpRequest);
        when(httpRequest.body(bodyCaptor.capture())).thenReturn(httpRequest);
        when(httpRequest.open()).thenReturn(httpRequest);

        SocketHttpConnection httpConnection = mock(SocketHttpConnection.class);
        when(httpRequest.httpConnection()).thenReturn(httpConnection);
        when(httpConnection.getSocket()).thenReturn(socket);
        HttpResponse response = mock(HttpResponse.class);
        when(httpRequest.send()).thenReturn(response);
        when(response.statusCode()).thenReturn(statusCode);

        return httpRequest;
    }

    private void verifyNoErrors(IdeaLogger LOG) {
        verify(LOG, never()).error(anyString());
        verify(LOG, never()).error(anyString(), any(Throwable.class));
    }
}
