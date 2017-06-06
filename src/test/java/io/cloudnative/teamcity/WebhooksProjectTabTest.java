package io.cloudnative.teamcity;

import com.google.common.collect.Maps;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.IdeaLogger;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.auth.Permissions;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PagePlace;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PlaceId;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Logger.class)
public class WebhooksProjectTabTest {

    @Mock
    private IdeaLogger anyLogger;

    @Mock
    private PagePlaces pagePlaces;

    @Mock
    private ProjectManager projectManager;

    @Mock
    private PluginDescriptor pluginDescriptor;

    @Mock
    private WebhooksSettings settings;

    @Mock
    private HttpServletRequest request;

    private WebhooksProjectTab target;

    @Before
    public void before() {
        PowerMockito.mockStatic(Logger.class);

        when(Logger.getInstance(anyString())).thenReturn(anyLogger);

        PagePlace pagePlace = mock(PagePlace.class);
        when(pagePlaces.getPlaceById(any(PlaceId.class))).thenReturn(pagePlace);

        target = new WebhooksProjectTab(pagePlaces, projectManager, pluginDescriptor, settings);
    }

    @Test
    public void testFillModel() {
        Map<String, Object> model = Maps.newHashMap();

        SProject sProject = mock(SProject.class);
        when(sProject.getExternalId()).thenReturn("projectExternalId");

        Permissions permissions = mock(Permissions.class);
        when(permissions.contains(Permission.EDIT_PROJECT)).thenReturn(true);

        SUser sUser = mock(SUser.class);
        when(sUser.getPermissionsGrantedForProject("projectExternalId")).thenReturn(permissions);

        when(settings.getUrls("projectExternalId")).thenReturn(Collections.singleton("http://url"));

        target.fillModel(model, request, sProject, sUser);

        assertEquals("webhooks/index.html", model.get("action"));
        assertEquals("http://url", ((List) model.get("urls")).get(0));
        assertEquals("projectExternalId", model.get("projectId"));
        assertEquals(true, model.get("canEdit"));
    }

    @Test
    public void testGetIncludeUrl() {
        when(pluginDescriptor.getPluginResourcesPath("projectTab.jsp")).thenReturn("pluginResourcesPath");

        assertEquals("pluginResourcesPath", target.getIncludeUrl());
    }
}
