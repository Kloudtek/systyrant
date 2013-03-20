/*
 * Copyright (c) 2013 KloudTek Ltd
 */

package com.kloudtek.systyrant.dsl;

import com.kloudtek.systyrant.AbstractContextTest;
import com.kloudtek.systyrant.STContext;
import com.kloudtek.systyrant.annotation.Execute;
import com.kloudtek.systyrant.exception.InvalidQueryException;
import com.kloudtek.systyrant.resource.Resource;
import com.kloudtek.util.ReflectionUtils;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.testng.Assert.assertNotNull;

/**
 * Tests for the resource query language.
 */
public class DSLQueryLangTests extends AbstractContextTest {
    private Field field;

    @Test
    @SuppressWarnings("unchecked")
    public void testIdMatch() throws Throwable {
        Resource rs1 = createTestResource();
        Resource rs2 = createTestResource("someid");
        rs2.setParent(rs1);
        Resource rs3 = createTestResource();
        rs3.setParent(rs1);
        Resource rs4 = createTestResource("someid");
        field = STContext.class.getDeclaredField("resourceScope");
        field.setAccessible(true);
        ((ThreadLocal<Resource>) field.get(ctx)).set(rs3);
        List<Resource> result = resourceManager.findResources("someid");
        assertContainsSame(result, rs2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIdMatch2() throws Throwable {
        Resource rs1 = createTestResource();
        Resource rs2 = createTestResource("someid");
        Resource rs3 = createTestResource();
        field = STContext.class.getDeclaredField("resourceScope");
        field.setAccessible(true);
        ((ThreadLocal<Resource>) field.get(ctx)).set(rs3);
        List<Resource> result = resourceManager.findResources("someid");
        assertContainsSame(result, rs2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUidMatch() throws Throwable {
        Resource rs1 = createTestResource();
        Resource rs2 = createTestResource();
        rs2.setUid("someuid");
        Resource rs3 = createTestResource();
        List<Resource> result = resourceManager.findResources("someuid");
        assertContainsSame(result, rs2);
    }

    @Test
    public void testAttrCISEq() throws Throwable {
        createTestResource();
        Resource rs2 = createTestResource().set("attr", "val1");
        Resource rs3 = createTestResource().set("attr", "Val1");
        createTestResource().set("attr", "val2");
        List<Resource> result = resourceManager.findResources("@attr like 'val1'");
        assertContainsSame(result, rs2, rs3);
        List<Resource> result2 = resourceManager.findResources("@attr = 'val1'");
        assertContainsSame(result2, rs2, rs3);
    }

    @Test
    public void testAttrGt() throws Throwable {
        createTestResource();
        createTestResource().set("attr", "2");
        Resource rs2 = createTestResource().set("attr", "22");
        Resource rs3 = createTestResource().set("attr", "20");
        createTestResource().set("attr", "12");
        List<Resource> result = resourceManager.findResources("@attr gt 15");
        assertContainsSame(result, rs2, rs3);
        resourceManager.findResources("@attr > 15");
        assertContainsSame(result, rs2, rs3);
    }

    @Test
    public void testAttrGt2() throws Throwable {
        createTestResource();
        createTestResource().set("attr", "d");
        Resource rs2 = createTestResource().set("attr", "x");
        Resource rs3 = createTestResource().set("attr", "z");
        createTestResource().set("attr", "b");
        List<Resource> result = resourceManager.findResources("@attr gt f");
        assertContainsSame(result, rs2, rs3);
        result = resourceManager.findResources("@attr > f");
        assertContainsSame(result, rs2, rs3);
    }

    @Test
    public void testAttrLt() throws Throwable {
        createTestResource().set("attr", "100");
        Resource rs1 = createTestResource();
        Resource rs2 = createTestResource().set("attr", "22");
        createTestResource().set("attr", "120");
        List<Resource> result = resourceManager.findResources("@attr lt 50");
        assertContainsSame(result, rs1, rs2);
        result = resourceManager.findResources("@attr < 50");
        assertContainsSame(result, rs1, rs2);
    }

    @Test
    public void testAttrLt2() throws Throwable {
        createTestResource().set("attr", "y");
        Resource rs2 = createTestResource();
        Resource rs3 = createTestResource().set("attr", "a");
        createTestResource().set("attr", "z");
        List<Resource> result = resourceManager.findResources("@attr lt f");
        assertContainsSame(result, rs2, rs3);
        result = resourceManager.findResources("@attr < f");
        assertContainsSame(result, rs2, rs3);
    }

    @Test
    public void testAttrCSEq() throws Throwable {
        createTestResource().set("attr", "val1");
        Resource rs2 = createTestResource().set("attr", "Val1");
        Resource rs3 = createTestResource().set("attr", "Val1");
        List<Resource> result = resourceManager.findResources("@attr eq 'Val1'");
        assertContainsSame(result, rs2, rs3);
        result = resourceManager.findResources("@attr == 'Val1'");
        assertContainsSame(result, rs2, rs3);
    }

    @Test
    public void testAttrIsNull() throws Throwable {
        createTestResource().set("attr", "val1");
        Resource rs2 = createTestResource();
        Resource rs3 = createTestResource();
        List<Resource> result = resourceManager.findResources("@attr is null");
        assertContainsSame(result, rs2, rs3);
    }

    @Test
    public void testAttrIsNotNull() throws Throwable {
        createTestResource();
        Resource rs2 = createTestResource().set("attr", "val1");
        Resource rs3 = createTestResource().set("attr", "val2");
        List<Resource> result = resourceManager.findResources("@attr is not null");
        assertContainsSame(result, rs2, rs3);
    }

    @Test
    public void testAttrIsEmpty() throws Throwable {
        createTestResource().set("attr", "val1");
        Resource rs2 = createTestResource().set("attr", "");
        Resource rs3 = createTestResource().set("attr", "");
        List<Resource> result = resourceManager.findResources("@attr is empty");
        assertContainsSame(result, rs2, rs3);
    }

    @Test
    public void testAttrIsNotEmpty() throws Throwable {
        createTestResource().set("attr", "");
        Resource rs2 = createTestResource().set("attr", "val1");
        Resource rs3 = createTestResource().set("attr", "val2");
        List<Resource> result = resourceManager.findResources("@attr is not empty");
        assertContainsSame(result, rs2, rs3);
    }

    @Test
    public void testAttrRegex1() throws Throwable {
        createTestResource().set("attr", "xxx");
        Resource rs2 = createTestResource().set("attr", "vol");
        Resource rs3 = createTestResource().set("attr", "val");
        Resource rs4 = createTestResource().set("attr", "vala");
        List<Resource> result = resourceManager.findResources("@attr regex 'v.l'");
        assertContainsSame(result, rs2, rs3, rs4);
        result = resourceManager.findResources("@attr ~= 'v.l'");
        assertContainsSame(result, rs2, rs3, rs4);
    }

    @Test
    public void testAttrRegex2() throws Throwable {
        createTestResource().set("attr", "xxx");
        Resource rs2 = createTestResource().set("attr", "vol");
        Resource rs3 = createTestResource().set("attr", "val");
        createTestResource().set("attr", "vala");
        createTestResource().set("attr", "aval");
        createTestResource().set("attr", "avala");
        List<Resource> result = resourceManager.findResources("@attr regex '^v.l$'");
        assertContainsSame(result, rs2, rs3);
        result = resourceManager.findResources("@attr ~= '^v.l$'");
        assertContainsSame(result, rs2, rs3);
    }

    @Test
    public void testAttrNotLike() throws Throwable {
        createTestResource().set("attr", "val1");
        createTestResource().set("attr", "Val1");
        Resource rs2 = createTestResource().set("attr", "val2");
        Resource rs3 = createTestResource().set("attr", "Val2");
        List<Resource> result = resourceManager.findResources("@attr not like 'val1'");
        assertContainsSame(result, rs2, rs3);
    }

    @Test
    public void testOr() throws Throwable {
        createTestResource("id1");
        Resource rs2 = createTestResource("id2");
        Resource rs3 = createTestResource("id3");
        List<Resource> result = resourceManager.findResources("@id eq 'id2' or @id eq 'id3'");
        assertContainsSame(result, rs2, rs3);
    }

    @Test
    public void testOr2() throws Throwable {
        createTestResource("id1");
        Resource rs2 = createTestResource("id2");
        Resource rs3 = createTestResource("id3");
        Resource rs4 = createTestResource("id4");
        createTestResource("id5");
        List<Resource> result = resourceManager.findResources("@id eq 'id2' or @id eq 'id3' or @id eq 'id4'");
        assertContainsSame(result, rs2, rs3, rs4);
    }

    @Test(dependsOnMethods = "testOr2")
    public void testStrings() throws Throwable {
        createTestResource("id1");
        Resource rs2 = createTestResource("id2");
        Resource rs3 = createTestResource("id3");
        Resource rs4 = createTestResource("id4");
        createTestResource("id5");
        List<Resource> result = resourceManager.findResources("@id eq 'id2' or @id eq id3 or @id eq \"id4\"");
        assertContainsSame(result, rs2, rs3, rs4);
    }

    @Test(dependsOnMethods = "testOr2")
    public void testBadString() throws Throwable {
        createTestResource("id1");
        Resource rs2 = createTestResource("id2");
        Resource rs3 = createTestResource("id3");
        createTestResource("id4");
        createTestResource("id5");
        List<Resource> result = resourceManager.findResources("@id eq 'id2' or @id eq id3 or @id eq \"'id4\"");
        assertContainsSame(result, rs2, rs3);
    }

    @Test
    public void testAnd() throws Throwable {
        createTestResource().setUid("uid1");
        Resource rs2 = createTestResource("id2").set("attr1", "val1").set("attr2", "val2");
        createTestResource().set("uid", "uid3");
        List<Resource> result = resourceManager.findResources("@attr1 eq 'val1' and @attr2 eq 'val2'");
        assertContainsSame(result, rs2);
    }

    @Test
    public void testAnd2() throws Throwable {
        createTestResource().setUid("uid1");
        Resource rs2 = createTestResource("id2").set("attr1", "val1").set("attr2", "val2").set("attr3", "val3");
        createTestResource().set("uid", "uid3");
        List<Resource> result = resourceManager.findResources("@attr1 eq 'val1' and @attr2 eq 'val2' and @attr3 eq 'val3'");
        assertContainsSame(result, rs2);
    }

    @Test
    public void testChildOfScope() throws Throwable {
        register(ChildOfScope.class, "childofscope");
        Resource parent = resourceManager.createResource("test:childofscope", null, null);
        Resource child1 = createChildTestResource(null, parent);
        Resource child2 = createChildTestResource(null, parent);
        createChildTestResource(null, child2);
        createTestResource();
        execute();
        ChildOfScope impl1 = parent.getJavaImpl(ChildOfScope.class);
        assertNotNull(impl1);
        ChildOfScope impl = impl1;
        assertContainsSame(impl.found, child1, child2);
    }

    public static class ChildOfScope {
        private List<Resource> found;

        @Execute
        public void query() throws InvalidQueryException {
            STContext ctx = STContext.get();
            found = ctx.findResources("childof");
        }
    }

    @Test
    public void testChildOfParam() throws Throwable {
        createTestResource();
        Resource parent = createTestResource("id");
        Resource child1 = createChildTestResource(null, parent);
        Resource child2 = createChildTestResource(null, parent);
        createChildTestResource(null, child2);
        createTestResource();
        execute();
        List<Resource> childs = ctx.findResources("childof @id eq 'id'");
        assertContainsSame(childs, child1, child2);
    }

    @Test
    public void testChildOfRecursiveParam() throws Throwable {
        createTestResource();
        Resource parent = createTestResource("id");
        Resource child1 = createChildTestResource(null, parent);
        Resource child2 = createChildTestResource(null, parent);
        Resource child3 = createChildTestResource(null, child2);
        createTestResource();
        execute();
        List<Resource> childs = ctx.findResources("childof* @id eq 'id'");
        assertContainsSame(childs, child1, child2, child3);
    }

    @Test(enabled = false)
    public void testDepOfScope() throws Throwable {
        register(DepOfScope.class, "depsofscope");
        Resource r1 = resourceManager.createResource("test:depsofscope", null, null);
        Resource r2 = createTestResource();
        r2.addDependency(r1);
        Resource r3 = createTestResource();
        r3.addDependency(r1);
        createTestResource().addDependency(r3);
        createTestResource();
        execute();
        DepOfScope impl1 = r1.getJavaImpl(DepOfScope.class);
        assertNotNull(impl1);
        DepOfScope impl = impl1;
        assertContainsSame(impl.found, r2, r3);
    }

    public static class DepOfScope {
        private List<Resource> found;

        @Execute
        public void query() throws InvalidQueryException {
            STContext ctx = STContext.get();
            found = ctx.findResources("depends");
        }
    }

    @Test(enabled = false)
    public void testDepOfParam() throws Throwable {
        createTestResource();
        Resource res1 = createTestResource("id");
        Resource res2 = createTestResource();
        res2.addDependency(res1);
        Resource res3 = createTestResource();
        res3.addDependency(res3);
        createTestResource();
        execute();
        List<Resource> childs = ctx.findResources("depends @id eq 'id'");
        assertContainsSame(childs, res2, res3);
    }

    @Test(enabled = false)
    public void testDepOfRecursiveParam() throws Throwable {
        createTestResource();
        Resource parent = createTestResource("id");
        Resource child1 = createChildTestResource(null, parent);
        Resource child2 = createChildTestResource(null, parent);
        Resource child3 = createChildTestResource(null, child2);
        createTestResource();
        execute();
        List<Resource> childs = ctx.findResources("depends* @id eq 'id'");
        assertContainsSame(childs, child1, child2, child3);
    }

    @Test
    public void testFindByType() throws Throwable {
        register(FindByType.class, "findbytype");
        createTestResource();
        Resource r1 = resourceManager.createResource("test:findbytype");
        Resource r2 = resourceManager.createResource("test:findbytype");
        createTestResource();
        execute();
        List<Resource> resources = ctx.findResources("type test:findbytype");
        assertContainsSame(resources, r1, r2);
    }

    public static class FindByType {
    }
}
