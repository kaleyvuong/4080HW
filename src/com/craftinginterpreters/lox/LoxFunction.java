package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final String name;
  private final List<Token> params;
  private final List<Stmt> body;
  private final Environment closure;
  private final boolean isInitializer;
  private final boolean isGetter;
  private final LoxClass definingClass;

  // EXISTING constructor for named functions
  LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer, LoxClass definingClass) {
    this.isInitializer = isInitializer;
    this.closure = closure;
    this.name = declaration.name.lexeme;
    this.params = declaration.params;
    this.body = declaration.body;
    this.isGetter = declaration.isGetter;
    this.definingClass = definingClass;
  }

  // NEW constructor for lambdas
  LoxFunction(Expr.Lambda lambda, Environment closure) {
    this.isGetter = false;
    this.name = null;  // Anonymous
    this.params = lambda.params;
    this.body = lambda.body;
    this.closure = closure;
    this.isInitializer = false;
    this.definingClass = null;
  }

  // PRIVATE constructor for bind() - helper to avoid reconstructing declaration
  private LoxFunction(String name, List<Token> params, List<Stmt> body,
                      Environment closure, boolean isInitializer, boolean isGetter, LoxClass definingClass) {
    this.name = name;
    this.params = params;
    this.body = body;
    this.closure = closure;
    this.isInitializer = isInitializer;
    this.isGetter = isGetter;
    this.definingClass = definingClass;
  }

  LoxFunction bind(LoxInstance instance) {
    Environment environment = new Environment(closure);
    environment.define("this", instance, true);
    // Use private constructor instead of recreating Stmt.Function
    return new LoxFunction(name, params, body, environment, isInitializer, isGetter, definingClass);
  }

  public boolean isGetter() {
    return isGetter;
  }

  String getName() {
    return name;
  }

  LoxClass getDefiningClass() {
    return definingClass;
  }

  @Override
  public String toString() {
    return name != null ? "<fn " + name + ">" : "<lambda>";
  }

  @Override
  public int arity() {
    return params.size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    Environment environment = new Environment(closure);
    for (int i = 0; i < params.size(); i++) {
      environment.define(params.get(i).lexeme, arguments.get(i), true);
    }

    if (definingClass != null && name != null) {
      environment.define("__current_method__", name, true);
      environment.define("__defining_class__", definingClass, true);
    }

    try {
      interpreter.executeBlock(body, environment);
    } catch (Return returnValue) {
      if (isInitializer) return closure.getAt(0, "this");
      return returnValue.value;
    }

    if (isInitializer) return closure.getAt(0, "this");
    return null;
  }
}