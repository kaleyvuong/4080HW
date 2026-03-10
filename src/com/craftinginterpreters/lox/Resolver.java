//> Resolving and Binding resolver
package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
  private int loopDepth = 0;
  private final Interpreter interpreter;
//> scopes-field
  private final Stack<Map<String, VariableInfo>> scopes = new Stack<>();
  private final Stack<Integer> scopeIndices = new Stack<>();
//< scopes-field
//> function-type-field
  private FunctionType currentFunction = FunctionType.NONE;
//< function-type-field

  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }
//> function-type
  private enum FunctionType {
    NONE,
/* Resolving and Binding function-type < Classes function-type-method
    FUNCTION
*/
//> Classes function-type-method
    FUNCTION,
//> function-type-initializer
    INITIALIZER,
//< function-type-initializer
    METHOD
//< Classes function-type-method
  }
//< function-type
//> Classes class-type

  private enum ClassType {
    NONE,
/* Classes class-type < Inheritance class-type-subclass
    CLASS
 */
//> Inheritance class-type-subclass
    CLASS,
    SUBCLASS
//< Inheritance class-type-subclass
  }

  private ClassType currentClass = ClassType.NONE;

//< Classes class-type
//> resolve-statements
  void resolve(List<Stmt> statements) {
    for (Stmt statement : statements) {
      resolve(statement);
    }
  }
//< resolve-statements
//> visit-block-stmt
  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    beginScope();
    resolve(stmt.statements);
    endScope();
    return null;
  }
//< visit-block-stmt
//> Classes resolver-visit-class
  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
//> set-current-class
    ClassType enclosingClass = currentClass;
    currentClass = ClassType.CLASS;

//< set-current-class
    declare(stmt.name, false);
    define(stmt.name);

    if (!scopes.isEmpty()) {
      VariableInfo info = scopes.peek().get(stmt.name.lexeme);
      if (info != null) {
        info.used = true;
      }
    }
//> Inheritance resolve-superclass
//> inherit-self
    if (stmt.superclass != null &&
        stmt.name.lexeme.equals(stmt.superclass.name.lexeme)) {
      Lox.error(stmt.superclass.name,
          "A class can't inherit from itself.");
    }

//< inherit-self
    if (stmt.superclass != null) {
//> set-current-subclass
      currentClass = ClassType.SUBCLASS;
//< set-current-subclass
      resolve(stmt.superclass);
    }
//< Inheritance resolve-superclass
    for (Expr.Variable mixin : stmt.mixins) {
      resolve(mixin);
    }
//> Inheritance begin-super-scope

    if (stmt.superclass != null) {
      beginScope();
      int index = scopeIndices.peek();
      scopes.peek().put("super", new VariableInfo(stmt.superclass.name, true, false, index));
      scopeIndices.push(scopeIndices.pop() + 1);
    }
//< Inheritance begin-super-scope
//> resolve-methods

    for (Stmt.Function method : stmt.methods) {
      if (method.isStatic) {
        FunctionType declaration = FunctionType.FUNCTION;
        resolveFunction(method, declaration);
      }
    }

//> resolver-begin-this-scope
    beginScope();
    int index = scopeIndices.peek();
    scopes.peek().put("this", new VariableInfo(stmt.name, true, false, index));
    scopeIndices.push(scopeIndices.pop() + 1);

//< resolver-begin-this-scope
    for (Stmt.Function method : stmt.methods) {
      if (!method.isStatic) {
        FunctionType declaration = FunctionType.METHOD;
        if (method.name.lexeme.equals("init")) {
          declaration = FunctionType.INITIALIZER;
        }
        resolveFunction(method, declaration);
      }
    }

//> resolver-end-this-scope
    endScope();

//< resolver-end-this-scope
//< resolve-methods
//> Inheritance end-super-scope
    if (stmt.superclass != null) endScope();

//< Inheritance end-super-scope
//> restore-current-class
    currentClass = enclosingClass;
//< restore-current-class
    return null;
  }
//< Classes resolver-visit-class
//> visit-expression-stmt
  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }
//< visit-expression-stmt
//> visit-function-stmt
  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name, false);
    define(stmt.name);

/* Resolving and Binding visit-function-stmt < Resolving and Binding pass-function-type
    resolveFunction(stmt);
*/
//> pass-function-type
    resolveFunction(stmt, FunctionType.FUNCTION);
//< pass-function-type
    return null;
  }
//< visit-function-stmt
//> visit-if-stmt
  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.condition);
    resolve(stmt.thenBranch);
    if (stmt.elseBranch != null) resolve(stmt.elseBranch);
    return null;
  }
//< visit-if-stmt
//> visit-print-stmt
  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression);
    return null;
  }
//< visit-print-stmt
//> visit-return-stmt
  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
//> return-from-top
    if (currentFunction == FunctionType.NONE) {
      Lox.error(stmt.keyword, "Can't return from top-level code.");
    }

//< return-from-top
    if (stmt.value != null) {
//> Classes return-in-initializer
      if (currentFunction == FunctionType.INITIALIZER) {
        Lox.error(stmt.keyword,
            "Can't return a value from an initializer.");
      }

//< Classes return-in-initializer
      resolve(stmt.value);
    }

    return null;
  }
//< visit-return-stmt
//> visit-var-stmt
  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    declare(stmt.name, false);
    if (stmt.initializer != null) {
      resolve(stmt.initializer);
    }
    define(stmt.name);
    return null;
  }
//< visit-var-stmt
//> visit-while-stmt
  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition);
    loopDepth++;
    try {
      resolve(stmt.body);
    } finally {
      loopDepth--;
    }
    return null;
  }

  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    if (loopDepth == 0) {
      Lox.error(stmt.keyword, "Can't use 'break' outside of a loop");
    }
    return null;
  }

  //< visit-while-stmt
//> visit-assign-expr
  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    resolve(expr.value);
    resolveLocal(expr, expr.name);
    return null;
  }
//< visit-assign-expr
//> visit-binary-expr
  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitCommaExpr(Expr.Comma expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitTernaryExpr(Expr.Ternary expr) {
    resolve(expr.condition);
    resolve(expr.thenBranch);
    resolve(expr.elseBranch);
    return null;
  }

  //< visit-binary-expr
//> visit-call-expr
  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee);

    for (Expr argument : expr.arguments) {
      resolve(argument);
    }

    return null;
  }
//< visit-call-expr
//> Classes resolver-visit-get
  @Override
  public Void visitGetExpr(Expr.Get expr) {
    resolve(expr.object);
    return null;
  }
//< Classes resolver-visit-get
//> visit-grouping-expr
  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }
//< visit-grouping-expr
//> visit-literal-expr
  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }
//< visit-literal-expr
//> visit-logical-expr
  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }
//< visit-logical-expr
//> Classes resolver-visit-set
  @Override
  public Void visitSetExpr(Expr.Set expr) {
    resolve(expr.value);
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitSuperExpr(Expr.Super expr) {
    return null;
  }
//< Classes resolver-visit-set

//> Classes resolver-visit-this
  @Override
  public Void visitThisExpr(Expr.This expr) {
//> this-outside-of-class
    if (currentClass == ClassType.NONE) {
      Lox.error(expr.keyword,
          "Can't use 'this' outside of a class.");
      return null;
    }

//< this-outside-of-class
    resolveLocal(expr, expr.keyword);
    return null;
  }

//< Classes resolver-visit-this
//> visit-unary-expr
  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }
//< visit-unary-expr
//> visit-variable-expr
  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    if (!scopes.isEmpty()) {
      VariableInfo info = scopes.peek().get(expr.name.lexeme);
      if (info != null && !info.defined) {
        Lox.error(expr.name,
                "Can't read local variable in its own initializer.");
      }
    }

    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitLambdaExpr(Expr.Lambda expr) {
    resolveLambda(expr, FunctionType.FUNCTION);
    return null;
  }

  @Override
  public Void visitInnerExpr(Expr.Inner expr) {
    if (currentClass == ClassType.NONE) {
      Lox.error(expr.keyword,
              "Can't use 'inner' outside of a class.");
      return null;
    }

    return null;
  }

  @Override
  public Void visitMixinStmt(Stmt.Mixin stmt) {
    declare(stmt.name, false);
    define(stmt.name);

    // Mark as used to avoid warnings
    if (!scopes.isEmpty()) {
      VariableInfo info = scopes.peek().get(stmt.name.lexeme);
      if (info != null) {
        info.used = true;
      }
    }

    // Resolve mixin methods (no 'this' or 'super' context)
    for (Stmt.Function method : stmt.methods) {
      FunctionType declaration = FunctionType.METHOD;
      resolveFunction(method, declaration);
    }

    return null;
  }

  private void resolveLambda(Expr.Lambda lambda, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    currentFunction = type;

    beginScope();
    for (Token param : lambda.params) {
      declare(param, true);
      define(param);
    }
    resolve(lambda.body);
    endScope();

    currentFunction = enclosingFunction;
  }
  //< visit-variable-expr
//> resolve-stmt
  private void resolve(Stmt stmt) {
    stmt.accept(this);
  }
//< resolve-stmt
//> resolve-expr
  void resolve(Expr expr) {
    expr.accept(this);
  }
//< resolve-expr
//> resolve-function
/* Resolving and Binding resolve-function < Resolving and Binding set-current-function
  private void resolveFunction(Stmt.Function function) {
*/
//> set-current-function
  private void resolveFunction(Stmt.Function function, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    currentFunction = type;

//< set-current-function
    beginScope();
    for (Token param : function.params) {
      declare(param, true);
      define(param);
    }

    if (function.isGetter && !function.params.isEmpty()) {
      Lox.error(function.name, "Getter cannot have parameters.");
    }

    resolve(function.body);
    endScope();
//> restore-current-function
    currentFunction = enclosingFunction;
//< restore-current-function
  }
//< resolve-function
//> begin-scope
  private void beginScope() {
    scopes.push(new HashMap<String, VariableInfo>());
    scopeIndices.push(0);
  }
//< begin-scope
//> end-scope
  private void endScope() {
    Map<String, VariableInfo> scope = scopes.pop();
    scopeIndices.pop();

    // Check for unused variables
    for (VariableInfo info : scope.values()) {
      if (!info.used && !info.isParameter) {
//        Lox.error(info.name,
//                "Local variable '" + info.name.lexeme + "' is never used.");
      }
    }
  }
//< end-scope
//> declare
  private void declare(Token name, boolean isParameter) {
    if (scopes.isEmpty()) return;

    Map<String, VariableInfo> scope = scopes.peek();
//> duplicate-variable
    if (scope.containsKey(name.lexeme)) {
      Lox.error(name,
          "Already a variable with this name in this scope.");
    }

//< duplicate-variable
    int index = scopeIndices.peek();  // Get current index
    scope.put(name.lexeme, new VariableInfo(name, false, isParameter, index));
    scopeIndices.push(scopeIndices.pop() + 1);  // Increment index for next variable
  }
//< declare
//> define
  private void define(Token name) {
    if (scopes.isEmpty()) return;
    VariableInfo info = scopes.peek().get(name.lexeme);
    if (info != null) {
      info.defined = true;
    }
  }
//< define
//> resolve-local
  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      Map<String, VariableInfo> scope = scopes.get(i);
      if (scope.containsKey(name.lexeme)) {
        VariableInfo info = scope.get(name.lexeme);
        info.used = true;  // Mark as used

        int depth = scopes.size() - 1 - i;
        interpreter.resolve(expr, depth, info.index);
        return;
      }
    }
  }
//< resolve-local
  private static class VariableInfo {
    final Token name;
    boolean defined;
    boolean used;
    final boolean isParameter;
    final int index;

    VariableInfo(Token name, boolean defined, boolean isParameter, int index) {
      this.name = name;
      this.defined = defined;
      this.used = false;
      this.isParameter = isParameter;
      this.index = index;
    }
  }
}
