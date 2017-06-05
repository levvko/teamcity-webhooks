package io.cloudnative.teamcity;

import lombok.experimental.ExtensionMethod;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

@ExtensionMethod(LombokExtensions.class)
public class LombokExtensionsTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testOr() {
        Object a = new Object();
        Object b = new Object();

        assertEquals(a, a.or(b));
        assertEquals(a, a.or(null));
        assertEquals(b, null.or(b));
        assertEquals(null, null.or(null));
    }

    @Test
    public void testLast_emptyArray() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Array specified is empty");

        (new String[]{}).last();
    }

    @Test
    public void testLast_oneItemArray() {
        assertEquals("a", (new String[]{"a"}).last());
    }

    @Test
    public void testLast_manyItemsArray() {
        assertEquals("c", (new String[]{"a", "b", "c"}).last());
    }

    @Test
    public void testF_noArg() {
        assertEquals("Message: body", "Message: body".f());
    }

    @Test
    public void test_oneArg() {
        assertEquals("Message: body", "Message: %s".f("body"));
    }

    @Test
    public void test_manyArgs() {
        assertEquals("Message: 1, 2, 3", "Message: %s, %s, %s".f(1, 2, 3));
    }
}
