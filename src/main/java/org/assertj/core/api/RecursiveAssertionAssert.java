/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright 2012-2022 the original author or authors.
 */
package org.assertj.core.api;

import static org.assertj.core.error.ShouldNotSatisfyPredicateRecursively.shouldNotSatisfyRecursively;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.assertj.core.annotations.Beta;
import org.assertj.core.api.recursive.FieldLocation;
import org.assertj.core.api.recursive.assertion.RecursiveAssertionConfiguration;
import org.assertj.core.api.recursive.assertion.RecursiveAssertionDriver;

/**
 * <p>An assertion that supports asserting a {@link Predicate} over all the fields of an object graph. Cycle avoidance is used,
 * so a graph that has cyclic references is essentially reduced to a tree by this class (the actual object graph is not changed
 * of course, it is treated as an immutable value).</p>
 *
 * <p>This class is <em>absolutely not</em> thread safe!! When using this class, care must be taken to ensure that its instances
 * are thread-bound. However, it <em>can</em> be re-used for multiple assertions over the same object graph.</p>
 * @param <SELF>
 * 
 */
@Beta
public class RecursiveAssertionAssert<SELF extends RecursiveAssertionAssert<SELF>> extends AbstractAssert<SELF, Object> {

  private final RecursiveAssertionConfiguration recursiveAssertionConfiguration;
  private final RecursiveAssertionDriver recursiveAssertionDriver;

  public RecursiveAssertionAssert(Object o, RecursiveAssertionConfiguration recursiveAssertionConfiguration) {
    super(o, RecursiveAssertionAssert.class);
    this.recursiveAssertionConfiguration = recursiveAssertionConfiguration;
    this.recursiveAssertionDriver = new RecursiveAssertionDriver(recursiveAssertionConfiguration);
  }

  /**
   * <p>Asserts that the given predicate is met for all fields of the object under test <b>recursively</b>.</p>
   *
   * <p>For example if the object under test is an instance of class A, A has a B field and B a C field then the assertion checks A's B field and B's C field and all C's fields.</p>
   *
   * <p>The recursive algorithm employs cycle detection, so object graphs with cyclic references can safely be asserted over without causing looping.</p>
   *
   * <p>This method enables recursive asserting using default configuration, which means:</p>
   * <ul>
   *   <li>All fields of all objects have the {@link java.util.function.Predicate} applied to them (including primitive fields),
   *   no fields are excluded.</li>
   *   <li>No fields are excluded based on their (declared) type, but:</li>
   *   <ul>
   *     <li>The recursion does not enter into Java Class Library types (java.*, javax.*)</li>
   *     <li>The {@link java.util.function.Predicate} is applied to {@link java.util.Collection} and array objects and their elements</li>
   *     <li>The {@link java.util.function.Predicate} is applied to {@link java.util.Map} objects, their keys and their values</li>
   *   </ul>
   * </ul>
   *
   * <p>It is possible to assert several predicates over the object graph in a row.</p>
   *
   * <p>The classes used in recursive asserting are <em>not</em> thread safe. Care must be taken when running tests in parallel
   * not to run assertions over object graphs that are being shared between tests.</p>
   * 
   * <p><strong>Example:</strong></p>
   * <pre><code style='java'> class Author {
   *   String name;
   *   String email;
   *   List&lt;Book&gt; books books = new ArrayList&lt;&gt;();
   *
   *   Author(String name, String email) {
   *     this.name = name;
   *     this.email = email;
   *   }
   * }
   *
   * class Book {
   *   String title;
   *   Author[] authors;
   *
   *   Book(String title, Author[] authors) {
   *     this.title = title;
   *     this.authors = authors;
   *   }
   * }
   *
   * Author pramodSadalage = new Author("Pramod Sadalage", "p.sadalage@recursive.test");
   * Author martinFowler = new Author("Martin Fowler", "m.fowler@recursive.test");
   * Author kentBeck = new Author("Kent Beck", "k.beck@recursive.test");
   *
   * Book firstbook = new Book("NoSql Distilled", new Author[]{pramodSadalage, martinFowler});
   * pramodSadalage.books.add(firstbook);
   * martinFowler.books.add(firstbook);
   * 
   * Book otherbook = new Book("Refactoring", new Author[] {martinFowler, kentBeck});
   * martinFowler.books.add(otherbook);
   * kentBeck.books.add(otherbook);
   *
   * assertThat(pramodSadalage).withRecursiveAssertion().allFieldsSatisfy(theField -> theField != null); </code></pre>
   *
   * @param predicate The predicate that is recursively applied to all the fields in the object tree of which actual is the root.
   * @return {@code this} assertions object
   * @throws AssertionError if one or more fields as described above fail the predicate test.
   * @since 3.24.0
   */
  public RecursiveAssertionAssert<?> allFieldsSatisfy(Predicate<Object> predicate) {
    // Reset the driver in case this is not the first predicate being run over actual.
    recursiveAssertionDriver.reset();

    List<FieldLocation> failedFields = recursiveAssertionDriver.assertOverObjectGraph(predicate, actual);
    if (!failedFields.isEmpty()) {
      throw objects.getFailures().failure(info, shouldNotSatisfyRecursively(recursiveAssertionConfiguration, failedFields));
    }
    return this;
  }

  /**
   * <p>
   * Asserts that none of the fields of the object under test graph (i.e. recursively getting the fields) are null.</p>
   * <p>
   * This is a convenience method for a common test and it is equivalent to {@code allFieldsSatisfy(fld -> fld != null)}.</p>
   *
   * <p><strong>Example:</strong></p>
   * <pre><code style='java'> class Author {
   *   String name;
   *   String email;
   *   List&lt;Book&gt; books books = new ArrayList&lt;&gt;();
   *
   *   Author(String name, String email) {
   *     this.name = name;
   *     this.email = email;
   *   }
   * }
   *
   * class Book {
   *   String title;
   *   Author[] authors;
   *
   *   Book(String title, Author[] authors) {
   *     this.title = title;
   *     this.authors = authors;
   *   }
   * }
   *
   * Author pramodSadalage = new Author("Pramod Sadalage", "p.sadalage@recursive.test");
   * Author martinFowler = new Author("Martin Fowler", "m.fowler@recursive.test");
   * Author kentBeck = new Author("Kent Beck", "k.beck@recursive.test");
   *
   * Book firstbook = new Book("NoSql Distilled", new Author[]{pramodSadalage, martinFowler});
   * pramodSadalage.books.add(firstbook);
   * martinFowler.books.add(firstbook);
   * 
   * Book otherbook = new Book("Refactoring", new Author[] {martinFowler, kentBeck});
   * martinFowler.books.add(otherbook);
   * kentBeck.books.add(otherbook);
   *
   * assertThat(pramodSadalage).withRecursiveAssertion().hasNoNullFields(); </code></pre>
   *
   * @return {@code this} assertions object
   * @throws AssertionError if one or more fields as described above are null.
   * @since 3.24.0
   */
  public RecursiveAssertionAssert<?> hasNoNullFields() {
    return allFieldsSatisfy(Objects::nonNull);
  }
}
