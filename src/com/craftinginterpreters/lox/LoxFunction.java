package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final String name;  // Add this field - may be null for lambdas
  private final List<Token> params;  // Add this field
  private final List<Stmt> body;  // Add this field
  private final Environment closure;
  private final boolean isInitializer;

  // EXISTING constructor for named functions
  LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
    this.isInitializer = isInitializer;
    this.closure = closure;
    this.name = declaration.name.lexeme;  // Extract these from declaration
    this.params = declaration.params;
    this.body = declaration.body;
  }

  // NEW constructor for lambdas
  LoxFunction(Expr.Lambda lambda, Environment closure) {
    this.name = null;  // Anonymous
    this.params = lambda.params;
    this.body = lambda.body;
    this.closure = closure;
    this.isInitializer = false;
  }

  // PRIVATE constructor for bind() - helper to avoid reconstructing declaration
  private LoxFunction(String name, List<Token> params, List<Stmt> body,
                      Environment closure, boolean isInitializer) {
    this.name = name;
    this.params = params;
    this.body = body;
    this.closure = closure;
    this.isInitializer = isInitializer;
  }

  LoxFunction bind(LoxInstance instance) {
    Environment environment = new Environment(closure);
    environment.define("this", instance, true);
    // Use private constructor instead of recreating Stmt.Function
    return new LoxFunction(name, params, body, environment, isInitializer);
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