package io.cloudnative.teamcity;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.IdeaLogger;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Logger.class)
public class WebhooksControllerTest {

    @Mock
    private IdeaLogger anyLogger;

    @Mock
    private WebControllerManager webManager;

    @Mock
    private WebhooksSettings settings;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private WebhooksController target;

    @Before
    public void before() {
        PowerMockito.mockStatic(Logger.class);

        when(Logger.getInstance(anyString())).thenReturn(anyLogger);
    }

    @Test
    public void testRegister() {
        target.register();

        verify(webManager).registerController("/webhooks/index.html", target);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testDoHandle_addUrl() throws Exception {
        when(request.getParameter("projectId")).thenReturn("1");
        when(request.getParameter("add")).thenReturn("urlToAdd");
        when(request.getParameter("urlToAdd")).thenReturn("http://add");

        ModelAndView modelAndView = target.doHandle(request, response);

        verify(settings).addUrl("1", "http://add");
        assertEquals("redirect:/project.html?projectId=1&tab=webhooks", modelAndView.getViewName());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testDoHandle_deleteUrl() throws Exception {
        when(request.getParameter("projectId")).thenReturn("1");
        when(request.getParameter("delete")).thenReturn("urlToDelete");
        when(request.getParameter("urlToDelete")).thenReturn("http://delete");

        ModelAndView modelAndView = target.doHandle(request, response);

        verify(settings).removeUrl("1", "http://delete");
        assertEquals("redirect:/project.html?projectId=1&tab=webhooks", modelAndView.getViewName());
    }
}
