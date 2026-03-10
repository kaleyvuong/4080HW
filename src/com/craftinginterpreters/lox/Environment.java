//> Statements and State environment-class
package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
//> enclosing-field
  final Environment enclosing;
//< enclosing-field
  private final Map<String, Variable> values = new HashMap<>();
//> environment-constructors
  Environment() {
    enclosing = null;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }
//< environment-constructors
//> environment-get

  private static class Variable {
    final Object value;
    final boolean initialized;

    Variable(Object value, boolean initialized) {
      this.value = value;
      this.initialized = initialized;
    }
  }
  Object get(Token name) {
    if (values.containsKey(name.lexeme)) {
      Variable var = values.get(name.lexeme);
      if (!var.initialized) {
        throw new RuntimeError(name,
                "Variable '" + name.lexeme + "' has not been initialized.");
      }
      return var.value;
    }
//> environment-get-enclosing

    if (enclosing != null) return enclosing.get(name);
//< environment-get-enclosing

    throw new RuntimeError(name,
        "Undefined variable '" + name.lexeme + "'.");
  }

//< environment-get

  Object getIfExists(String name) {
    if (values.containsKey(name)) {
      Variable var = values.get(name);
      if (var.initialized) {
        return var.value;
      }
    }
    if (enclosing != null) {
      return enclosing.getIfExists(name);
    }
    return null;  // Return null instead of throwing
  }
//> environment-assign
  void assign(Token name, Object value) {
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, new Variable(value, true));
      return;
    }

//> environment-assign-enclosing
    if (enclosing != null) {
      enclosing.assign(name, value);
      return;
    }

//< environment-assign-enclosing
    throw new RuntimeError(name,
        "Undefined variable '" + name.lexeme + "'.");
  }
//< environment-assign
//> environment-define
  void define(String name, Object value, boolean initialized) {
    values.put(name, new Variable(value, initialized));
  }
//< environment-define
//> Resolving and Binding ancestor
  Environment ancestor(int distance) {
    Environment environment = this;
    for (int i = 0; i < distance; i++) {
      environment = environment.enclosing; // [coupled]
    }

    return environment;
  }
//< Resolving and Binding ancestor
//> Resolving and Binding get-at
  Object getAt(int distance, String name) {
    Variable var = ancestor(distance).values.get(name);
    if (!var.initialized) {
      throw new RuntimeError(new Token(TokenType.IDENTIFIER, name, null, 0),
              "Variable '" + name + "' has not been initialized.");
    }
    return var.value;
  }
//< Resolving and Binding get-at
  Object getAt(int distance, int index) {

    Environment env = ancestor(distance);

    int currentIndex = 0;
    for (Map.Entry<String, Variable> entry : env.values.entrySet()) {
      if (currentIndex == index) {
        Variable var = entry.getValue();
        if (!var.initialized) {
          throw new RuntimeError(null, "Variable has not been initialized.");
        }
        return var.value;
      }
      currentIndex++;
    }

    throw new RuntimeError(null, "Variable index out of bounds.");
  }

//> Resolving and Binding assign-at
  void assignAt(int distance, int index, Object value) {
    Environment env = ancestor(distance);

    int currentIndex = 0;
    for (Map.Entry<String, Variable> entry : env.values.entrySet()) {
      if (currentIndex == index) {
        env.values.put(entry.getKey(), new Variable(value, true));
        return;
      }
      currentIndex++;
    }

    throw new RuntimeError(null, "Variable index out of bounds.");
  }
//< Resolving and Binding assign-at


//> omit
  @Override
  public String toString() {
    String result = values.toString();
    if (enclosing != null) {
      result += " -> " + enclosing.toString();
    }

    return result;
  }
//< omit
}
