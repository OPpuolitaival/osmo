package osmo.tester;

import osmo.tester.generator.listener.GenerationListener;
import osmo.tester.generator.listener.GenerationListenerList;
import osmo.tester.generator.algorithm.FSMTraversalAlgorithm;
import osmo.tester.generator.algorithm.RandomAlgorithm;
import osmo.tester.generator.endcondition.logical.And;
import osmo.tester.generator.endcondition.EndCondition;
import osmo.tester.generator.endcondition.Length;
import osmo.tester.generator.endcondition.Probability;
import osmo.tester.scenario.SliceFilter;
import osmo.tester.scenario.StartupFilter;
import osmo.tester.generator.filter.StepFilter;
import osmo.tester.model.FSM;
import osmo.tester.model.ModelFactory;
import osmo.tester.model.data.SearchableInput;
import osmo.tester.parser.ModelObject;
import osmo.tester.scenario.Scenario;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Defines configuration for test generation.
 *
 * @author Teemu Kanstren
 */
public class OSMOConfiguration implements ModelFactory {
  /** The set of test model objects, given by the user. */
  private final Collection<ModelObject> modelObjects = new ArrayList<>();
  /** When do we stop generating the overall test suite? (stopping all test generation). Ignored if junitLength is set. */
  private EndCondition suiteEndCondition = new And(new Length(1), new Probability(0.05d));
  /** When do we stop generating individual tests and start a new one? */
  private EndCondition testCaseEndCondition = new And(new Length(1), new Probability(0.1d));
  /** Set of filters to define when given transitions should not be considered for execution. */
  private Collection<StepFilter> filters = new ArrayList<>();
  /** The algorithm to traverse the test model to generate test steps. */
  private FSMTraversalAlgorithm algorithm;
  /** Should we fail then test generation if there is no enabled transition? Otherwise we just end the test. */
  private boolean failWhenNoWayForward = true;
  /** Should we fail test generation if an Exception is thrown? */
  private boolean failWhenError = true;
  /** Listeners to be notified about test generation events. */
  private GenerationListenerList listeners = new GenerationListenerList();
  /** Number of tests to generate when using over JUnit. */
  private int junitLength = -1;
  /** Should we try to throw original exception if model throws (remove OSMO Tester trace from the top)? */
  private boolean unwrapExceptions = true;
  /** Factory for creating model objects, alternative to adding them one by one. */
  private ModelFactory factory = null;
  /** Is manual drive enabled? Used to enable manual GUI etc. */
  private static boolean manual = false;
  /** Model slicing scenario. */
  private Scenario scenario = null;

  public OSMOConfiguration() {
  }

  public void setScenario(Scenario scenario) {
    this.scenario = scenario;
  }

  /**
   * Adds a new model object, to be composed by OSMO to a single internal model along with other model objects.
   *
   * @param modelObject The model object (with OSMO annotations) to be added.
   */
  public void addModelObject(Object modelObject) {
    modelObjects.add(new ModelObject(modelObject));
  }

  /**
   * Adds a model object with a given prefix, allowing the same object class to be re-used with different configuration
   * where the names of transitions (test steps), guards and other elements is preceded by the given prefix.
   *
   * @param prefix      The model prefix.
   * @param modelObject The model object itself.
   */
  public void addModelObject(String prefix, Object modelObject) {
    modelObjects.add(new ModelObject(prefix, modelObject));
  }

  public void setFactory(ModelFactory factory) {
    this.factory = factory;
  }

  public ModelFactory getFactory() {
    return factory;
  }

  public Collection<ModelObject> createModelObjects() {
    if (factory != null) {
      return factory.createModelObjects();
    }
    return modelObjects;
  }

  public void setAlgorithm(FSMTraversalAlgorithm algorithm) {
    this.algorithm = algorithm;
  }

  public FSMTraversalAlgorithm getAlgorithm() {
    return algorithm;
  }

  /**
   * @return The defined suite end condition.
   */
  public EndCondition getSuiteEndCondition() {
    return suiteEndCondition;
  }

  /**
   * @return The test end condition.
   */
  public EndCondition getTestCaseEndCondition() {
    return testCaseEndCondition;
  }

  /**
   * Set the condition for stopping the generation of whole test suite.
   *
   * @param condition The new condition to stop overall suite generation.
   */
  public void setSuiteEndCondition(EndCondition condition) {
    suiteEndCondition = condition;
  }

  /**
   * Set the condition for stopping the generation of individual test cases.
   *
   * @param condition The new condition to stop individual test generation.
   */
  public void setTestEndCondition(EndCondition condition) {
    testCaseEndCondition = condition;
  }

  /**
   * Adds a filter for removing some possible test steps (transitions).
   *
   * @param filter The new filter.
   */
  public void addFilter(StepFilter filter) {
    filters.add(filter);
    listeners.addListener(filter);
  }

  /**
   * Defines if test generation should be completely stopped when the generation of a test throws an exception.
   *
   * @return True if we should stop test generation completely if the model execution throws.
   */
  public boolean shouldFailWhenError() {
    return failWhenError;
  }

  /**
   * Defines if test generation should be stopped when the model program reaches a state where no step is available.
   * If this is false, the test being generated ends there, if true all test generation stops and exception is thrown.
   *
   * @return True if should fail when model is a dead end.
   */
  public boolean shouldFailWhenNoWayForward() {
    return failWhenNoWayForward;
  }

  public Collection<StepFilter> getFilters() {
    return filters;
  }

  /**
   * Adds a listener to be notified about test generation progress.
   *
   * @param listener The listener to be added.
   */
  public void addListener(GenerationListener listener) {
    listeners.addListener(listener);
  }

  public GenerationListenerList getListeners() {
    return listeners;
  }

  /**
   * Initializes test generation configuration with the model to be used in test generation.
   * Includes initializing parameters for algorithms, end conditions, listeners, ..
   *
   * @param seed Test generation seed.
   * @param fsm The parsing results.
   */
  public void initialize(long seed, FSM fsm) {
    if (algorithm == null) {
      algorithm = new RandomAlgorithm();
    }
    algorithm.init(seed, fsm);
    suiteEndCondition.init(seed, fsm);
    if (scenario != null) {
      scenario.validate(fsm);
      StartupFilter startupFilter = new StartupFilter(scenario);
      addFilter(startupFilter);
      addFilter(new SliceFilter(scenario, startupFilter));
      setTestEndCondition(scenario.createEndCondition(testCaseEndCondition));
    }
    //test end condition is initialized in generator between each test case
    listeners.init(seed, fsm, this);
  }

  public void setFailWhenError(boolean fail) {
    failWhenError = fail;
  }

  public void setFailWhenNoWayForward(boolean fail) {
    this.failWhenNoWayForward = fail;
  }

  public int getJUnitLength() {
    return junitLength;
  }

  /**
   * Defines the number of tests to be generated when JUnit integration is used.
   *
   * @param junitLength The number of tests to be generated.
   */
  public void setJUnitLength(int junitLength) {
    this.junitLength = junitLength;
  }

  /**
   * If true, we try to throw "original" exceptions when the model program throws one.
   *
   * @return True if we try.
   */
  public boolean shouldUnwrapExceptions() {
    return unwrapExceptions || junitLength > 0;
  }

  public void setUnwrapExceptions(boolean unwrapExceptions) {
    this.unwrapExceptions = unwrapExceptions;
  }

  public static void check(SearchableInput si) {
    if (si.isChecked()) {
      return;
    }
    si.setChecked(true);
    if (manual == true) {
      si.enableGUI();
    }
  }

  public void setManual(boolean manual) {
    this.manual = manual;
  }
}
