package osmo.tester.model;

import osmo.common.Logger;
import osmo.tester.model.data.SearchableInput;

import java.lang.reflect.Field;

/**
 * Represents a field for a state variable in the model as tagged by {@link osmo.tester.annotation.Variable} annotation.
 *
 * @author Teemu Kanstren
 */
public class VariableField {
  private static final Logger log = new Logger(VariableField.class);
  /** The model object itself, hosting the actual variable object. */
  private final Object modelObject;
  /** The field for the variable. */
  private final Field field;
  /** Field name, for faster access. */
  private final String name;
  /** If the variable object implements the {@link VariableValue} interface, we store it here for faster access. */
  private VariableValue variable = null;

  public VariableField(Object modelObject, Field field, String name) {
    this.modelObject = modelObject;
    this.field = field;
    if (name.equals("")) {
      //this means the user did not specify an explicit name in annotation so we use field name from class file
      name = field.getName();
    }
    this.name = name;
    ///check only once here to avoid overhead of repeating it on every access
    checkIfVariableValue();
  }

  /**
   * Checks the field represented by this value for implementing the {@link VariableValue} interface.
   * If this interface is implemented, the object is typecast and stored in the "variable" variable in
   * this object for faster access when the value is queried.
   */
  private void checkIfVariableValue() {
    Class<?> type = field.getType();
    while (type != null) {
      checkVariableType(type);
      Class<?>[] interfaces = type.getInterfaces();
      for (Class<?> i : interfaces) {
        checkVariableType(i);
      }
      type = type.getSuperclass();
    }
  }

  private void checkVariableType(Class clazz) {
    //we assume same classloader and use ==, most naughty
    if (clazz == VariableValue.class) {
      try {
        variable = (VariableValue) field.get(modelObject);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Failed to read @Variable instance for field:" + field, e);
      }
    }
  }

  /**
   * Field name as read from the class object.
   *
   * @return The field name.
   */
  public String getName() {
    return name;
  }

  /**
   * Gives the value of this field as relevant for the model state persistence.
   * If the field implements the {@link VariableValue} interface, the value() method in this
   * interface is called to read the value to store.
   * Otherwise the value of the object/primitive is given as such.
   *
   * @return The value to store for this field in the current model state.
   */
  public Object getValue() {
    if (variable != null) {
      return variable.value();
    }
    try {
      return field.get(modelObject);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Failed to read state variable value for field:" + field, e);
    }
  }
  
  public boolean isSearchableInput() {
    try {
      return (field.get(modelObject) instanceof SearchableInput);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Failed to read state variable value for field:" + field, e);
    }
  }
  
  @Override
  public String toString() {
    return "VariableField{" +
            "name='" + name + '\'' +
            '}';
  }
}
