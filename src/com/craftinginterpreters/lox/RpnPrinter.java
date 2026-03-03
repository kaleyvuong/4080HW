package com.craftinginterpreters.lox;

public class RpnPrinter implements Expr.Visitor<String>{

    public static void main (String[] args) {
//        Expr expression = new Expr.Binary(
//            new Expr.Grouping(
//                new Expr.Binary(
//                    new Expr.Literal(1),
//                    new Token(TokenType.PLUS, "+", null, 1),
//                    new Expr.Literal(2))),
//            new Token(TokenType.STAR, "*", null, 1),
//            new Expr.Grouping(
//                new Expr.Binary(
//                    new Expr.Literal(4),
//                    new Token(TokenType.MINUS, "-", null, 1),
//                    new Expr.Literal(3))));
//        System.out.println(new RpnPrinter().print(expression));
//        Expr commaExpr = new Expr.Comma(
//                new Expr.Comma(
//                        new Expr.Literal(1),
//                        new Expr.Literal(2)),
//                        new Expr.Literal(3));
//        System.out.println(new RpnPrinter().print(commaExpr));
//        // Output: 1 2 , 3 ,

        Expr ternaryExpr = new Expr.Ternary(
                new Expr.Literal(true),
                new Expr.Literal(1),
                new Expr.Literal(2));
        System.out.println(new RpnPrinter().print(ternaryExpr));
        // Output: true 1 2 ?:
    }
    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return null;
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return expr.left.accept(this) + " " + expr.right.accept(this) + " " + expr.operator.lexeme;
    }

    @Override
    public String visitCommaExpr(Expr.Comma expr) {
        return expr.left.accept(this) + " " + expr.right.accept(this) + " ,";
    }

    @Override
    public String visitTernaryExpr(Expr.Ternary expr) {
        return expr.condition.accept(this) + " " +
                expr.thenBranch.accept(this) + " " +
                expr.elseBranch.accept(this) + " ?:";
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        return null;
    }

    @Override
    public String visitGetExpr(Expr.Get expr) {
        return null;
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return expr.expression.accept(this);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        return expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return null;
    }

    @Override
    public String visitSetExpr(Expr.Set expr) {
        return null;
    }

    @Override
    public String visitSuperExpr(Expr.Super expr) {
        return null;
    }

    @Override
    public String visitThisExpr(Expr.This expr) {
        return null;
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return expr.right.accept(this) + " " + expr.operator.lexeme;
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return null;
    }

    @Override
    public String visitLambdaExpr(Expr.Lambda expr) {
        return null;
    }
}
