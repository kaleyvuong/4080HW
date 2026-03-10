package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class LoxMixin {
    final String name;
    private final Map<String, LoxFunction> methods;

    LoxMixin(String name, Map<String, LoxFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }
        return null;
    }

    Map<String, LoxFunction> getMethods() {
        return new HashMap<>(methods);
    }

    @Override
    public String toString() {
        return "<mixin " + name + ">";
    }
}