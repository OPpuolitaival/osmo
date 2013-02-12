package osmo.tester.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Methods annotated with this are always executed as the last step in test generation.
 * Difference to {@link AfterTest} is that
 * this is executed as part of the test generation flow, and thus errors reported are considered part of
 * the test case. In {@link AfterTest} the errors considered general failures of the model and not part of the test
 * generation process. Thus, for example, errors in methods with this annotation will mark a test as a failure
 * whereas errors in {@link AfterTest} mark the test generation process as a failure.
 * <p/>
 * An example use case is to generate a set of inputs (one batch) and in the end invoke test execution from the
 * last step.
 *
 * @author Teemu Kanstren
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LastStep {
}
