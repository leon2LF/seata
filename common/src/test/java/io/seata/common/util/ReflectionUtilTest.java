/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.common.util;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.seata.common.BranchDO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

public class ReflectionUtilTest {

    //Prevent jvm from optimizing final
    public static final String testValue = (null != null ? "hello" : "hello");

    public final String testValue2 = (null != null ? "hello world" : "hello world");

    @Test
    public void testGetClassByName() throws ClassNotFoundException {
        Assertions.assertEquals(String.class,
                ReflectionUtil.getClassByName("java.lang.String"));
    }

    @Test
    public void testGetWrappedClass() {
        Assertions.assertEquals(Byte.class, ReflectionUtil.getWrappedClass(byte.class));
        Assertions.assertEquals(Boolean.class, ReflectionUtil.getWrappedClass(boolean.class));
        Assertions.assertEquals(Character.class, ReflectionUtil.getWrappedClass(char.class));
        Assertions.assertEquals(Short.class, ReflectionUtil.getWrappedClass(short.class));
        Assertions.assertEquals(Integer.class, ReflectionUtil.getWrappedClass(int.class));
        Assertions.assertEquals(Long.class, ReflectionUtil.getWrappedClass(long.class));
        Assertions.assertEquals(Float.class, ReflectionUtil.getWrappedClass(float.class));
        Assertions.assertEquals(Double.class, ReflectionUtil.getWrappedClass(double.class));
        Assertions.assertEquals(Void.class, ReflectionUtil.getWrappedClass(void.class));
        Assertions.assertEquals(Object.class, ReflectionUtil.getWrappedClass(Object.class));
    }

    @Test
    public void testSetFieldValue() throws NoSuchFieldException {
        BranchDO branchDO = new BranchDO("xid123123", 123L, 1, 2.2, new Date());
        ReflectionUtil.setFieldValue(branchDO, "xid", "xid456");
        Assertions.assertEquals("xid456", branchDO.getXid());
    }

    @Test
    public void testGetFieldValue() throws NoSuchFieldException {
        Assertions.assertEquals("d",
                ReflectionUtil.getFieldValue(new DurationUtil(), "DAY_UNIT"));
        Assertions.assertThrows(ClassCastException.class, () -> {
            Integer var = ReflectionUtil.getFieldValue(new DurationUtil(), "DAY_UNIT");
        });

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ReflectionUtil.getFieldValue(null, "a1b2c3"));
        Assertions.assertThrows(NoSuchFieldException.class,
                () -> ReflectionUtil.getFieldValue(new Object(), "A1B2C3"));
    }

    @Test
    public void testInvokeMethod() throws NoSuchMethodException, InvocationTargetException {
        Assertions.assertEquals(0, ReflectionUtil.invokeMethod("", "length"));
        Assertions.assertEquals(3,
                ReflectionUtil.invokeMethod("foo", "length"));

        Assertions.assertThrows(NoSuchMethodException.class,
                () -> ReflectionUtil.invokeMethod("", "size"));
    }

    @Test
    public void testInvokeMethod2() throws NoSuchMethodException, InvocationTargetException {
        Assertions.assertEquals(0, ReflectionUtil
                .invokeMethod("", "length", null, ReflectionUtil.EMPTY_ARGS));
        Assertions.assertEquals(3, ReflectionUtil
                .invokeMethod("foo", "length", null, ReflectionUtil.EMPTY_ARGS));

        Assertions.assertThrows(NoSuchMethodException.class, () -> ReflectionUtil
                .invokeMethod("", "size", null, ReflectionUtil.EMPTY_ARGS));
    }

    @Test
    public void testInvokeMethod3() throws NoSuchMethodException, InvocationTargetException {
        Assertions.assertEquals("0", ReflectionUtil.invokeStaticMethod(
                String.class, "valueOf",
                new Class<?>[]{int.class}, 0));
        Assertions.assertEquals("123", ReflectionUtil.invokeStaticMethod(
                String.class, "valueOf",
                new Class<?>[]{int.class}, new Object[]{123}));

        Assertions.assertThrows(NoSuchMethodException.class, () -> ReflectionUtil
                .invokeStaticMethod(String.class, "size", null, ReflectionUtil.EMPTY_ARGS));
    }

    @Test
    public void testGetInterfaces() {
        Assertions.assertArrayEquals(new Object[]{Serializable.class},
                ReflectionUtil.getInterfaces(Serializable.class).toArray());

        Assertions.assertArrayEquals(new Object[]{
                        Map.class, Cloneable.class, Serializable.class},
                ReflectionUtil.getInterfaces(HashMap.class).toArray());
    }

    @Test
    @DisabledOnJre(JRE.JAVA_17) // `ReflectionUtil.modifyStaticFinalField` does not supported java17
    public void testModifyStaticFinalField() throws NoSuchFieldException, IllegalAccessException {
        Assertions.assertEquals("hello", testValue);
        ReflectionUtil.modifyStaticFinalField(ReflectionUtilTest.class, "testValue", "hello world");
        Assertions.assertEquals("hello world", testValue);

        // case: not a static field
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ReflectionUtil.modifyStaticFinalField(ReflectionUtilTest.class, "testValue2", "hello");
        });
    }


    //region test the method 'getAllFields'

    @Test
    public void testGetAllFields() {
        // TestClass
        this.testGetAllFieldsInternal(TestClass.class, "f1", "f2");
        // TestSuperClass
        this.testGetAllFieldsInternal(TestSuperClass.class, "f2");
        // EmptyClass
        this.testGetAllFieldsInternal(EmptyClass.class);
        // TestInterface
        this.testGetAllFieldsInternal(TestInterface.class);
        // Object
        this.testGetAllFieldsInternal(Object.class);

        // case: The fields of EmptyClass is `EMPTY_FIELD_ARRAY`
        Assertions.assertSame(ReflectionUtil.EMPTY_FIELD_ARRAY, ReflectionUtil.getAllFields(EmptyClass.class));
        // case: The fields of TestInterface is `EMPTY_FIELD_ARRAY`
        Assertions.assertSame(ReflectionUtil.EMPTY_FIELD_ARRAY, ReflectionUtil.getAllFields(TestInterface.class));
        // case: The fields of Object is `EMPTY_FIELD_ARRAY`
        Assertions.assertSame(ReflectionUtil.EMPTY_FIELD_ARRAY, ReflectionUtil.getAllFields(Object.class));
    }

    private void testGetAllFieldsInternal(Class<?> clazz, String... fieldNames) {
        Field[] fields = ReflectionUtil.getAllFields(clazz);
        Assertions.assertEquals(fieldNames.length, fields.length);
        Field[] fields2 = ReflectionUtil.getAllFields(clazz);
        Assertions.assertSame(fields, fields2);

        if (fieldNames.length == 0) {
            return;
        }

        List<String> fieldNameList = Arrays.asList(fieldNames);
        for (Field field : fields) {
            Assertions.assertTrue(fieldNameList.contains(field.getName()));
        }
    }

    @Test
    public void testMethodToString() throws NoSuchMethodException {
        Assertions.assertEquals("Method<ReflectionUtilTest.testMethodToString()>",
            ReflectionUtil.methodToString(this.getClass().getMethod("testMethodToString")));
    }

    @Test
    public void testAnnotationToString() throws NoSuchMethodException {
        Assertions.assertEquals("@Test()", ReflectionUtil
            .annotationToString(this.getClass().getMethod("testAnnotationToString").getAnnotation(Test.class)));
    }

    @Test
    public void testGetAnnotationValues() throws NoSuchMethodException, NoSuchFieldException {
        Assertions.assertEquals(new LinkedHashMap<>(), ReflectionUtil
            .getAnnotationValues(this.getClass().getMethod("testGetAnnotationValues").getAnnotation(Test.class)));
    }

    //region the test class and interface

    class EmptyClass {
    }

    class TestClass extends TestSuperClass implements TestInterface {

        private String f1;

        public String getF1() {
            return f1;
        }

        public void setF1(String f1) {
            this.f1 = f1;
        }
    }

    class TestSuperClass implements TestInterface {
        private String f2;

        public String getF2() {
            return f2;
        }

        public void setF2(String f2) {
            this.f2 = f2;
        }
    }

    interface TestInterface {
    }

    //endregion

    //endregion
}
