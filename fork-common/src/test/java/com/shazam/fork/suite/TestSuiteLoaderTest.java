/*
 * Copyright 2016 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.shazam.fork.suite;

import com.shazam.fork.io.DexFileExtractor;
import com.shazam.fork.model.TestCaseEvent;

import org.hamcrest.Matcher;
import org.jf.dexlib.DexFile;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import static com.shazam.fork.io.FakeDexFileExtractor.fakeDexFileExtractor;
import static com.shazam.fork.io.Files.convertFileToDexFile;
import static com.shazam.fork.model.TestCaseEvent.newTestCase;
import static com.shazam.fork.suite.FakeTestClassMatcher.fakeTestClassMatcher;
import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * This test is based on the <code>tests.dex</code> file, which contains test classes with the following code:
 * <blockquote><pre>
 *{@literal}@Ignore
 *public class IgnoredClassTest {
 *    {@literal}@Test
 *    public void methodOfAnIgnoredTestClass() {
 *    }
 *}
 *
 *public class ClassWithNoIgnoredMethodsTest {
 *    {@literal}@Test
 *    public void firstTestMethod() {
 *    }
 *
 *    {@literal}@Test
 *    public void secondTestMethod() {
 *    }
 *}
 *
 *public class ClassWithSomeIgnoredMethodsTest {
 *    {@literal}@Test
 *    public void nonIgnoredTestMethod() {
 *    }
 *
 *    {@literal}@Test
 *    {@literal}@Ignore
 *    public void ignoredTestMethod() {
 *    }
 *}
 * </pre></blockquote>
 */
public class TestSuiteLoaderTest {
    private static final File ANY_INSTRUMENTATION_APK_FILE = null;

    private final DexFileExtractor fakeDexFileExtractor = fakeDexFileExtractor().thatReturns(testDexFile());
    private final TestClassMatcher fakeTestClassMatcher = fakeTestClassMatcher().thatAlwaysMatches();

    private DexFile testDexFile() {
        URL testDexResourceUrl = this.getClass().getResource("/tests.dex");
        String testDexFile = testDexResourceUrl.getFile();
        File file = new File(testDexFile);
        return convertFileToDexFile().apply(file);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void populatesTestCaseEvents() throws Exception {
        Map<String, String> multiPropertiesMap = new HashMap() {{
            put("foo", "bar");
            put("bux", "poi");
        }};
        TestSuiteLoader testSuiteLoader = new TestSuiteLoader(ANY_INSTRUMENTATION_APK_FILE, fakeDexFileExtractor,
                fakeTestClassMatcher);

        assertThat(testSuiteLoader.loadTestSuite(), containsInAnyOrder(
                sameTestEventAs("com.shazam.forktest.IgnoredClassTest", "methodOfAnIgnoredTestClass", true),
                sameTestEventAs("com.shazam.forktest.ClassWithNoIgnoredMethodsTest", "firstTestMethod", false),
                sameTestEventAs("com.shazam.forktest.ClassWithNoIgnoredMethodsTest", "secondTestMethod", false),
                sameTestEventAs("com.shazam.forktest.ClassWithSomeIgnoredMethodsTest", "nonIgnoredTestMethod", false),
                sameTestEventAs("com.shazam.forktest.ClassWithSomeIgnoredMethodsTest", "ignoredTestMethod", true),
                sameTestEventAs("com.shazam.forktest.RevokePermissionsClassTest", "methodAnnotatedWithRevokePermissionsTest",
                        false, asList("android.permission.RECORD_AUDIO", "android.permission.ACCESS_FINE_LOCATION")),
                sameTestEventAs("com.shazam.forktest.RevokePermissionsClassTest", "methodAnnotatedWithEmptyRevokePermissionsTest", false),
                sameTestEventAs("com.shazam.forktest.PropertiesClassTest", "methodWithPropertiesTestClass", singletonMap("foo", "bar")),
                sameTestEventAs("com.shazam.forktest.PropertiesClassTest", "methodWithMultiplePropertiesTestClass", multiPropertiesMap )
                )
        );

    }

    @Nonnull
    private Matcher<TestCaseEvent> sameTestEventAs(String testClass, String testMethod, Map<String, String> properties) {
        return sameBeanAs(newTestCase(testClass, testMethod, false, emptyList(), properties));
    }

    @Nonnull
    private Matcher<TestCaseEvent> sameTestEventAs(String testClass, String testMethod, boolean isIgnored) {
        return sameBeanAs(newTestCase(testMethod, testClass, isIgnored, emptyList(), emptyMap()));
    }

    @Nonnull
    private Matcher<TestCaseEvent> sameTestEventAs(String testClass, String testMethod, boolean isIgnored, List<String> permissions) {
        return sameBeanAs(newTestCase(testMethod, testClass, isIgnored, permissions, emptyMap()));
    }
}
