package com.arthas.cataloger;

// Classe separada necessária para evitar erro "JavaFX runtime components are missing"
// ao executar o JAR sem o módulo javafx declarado explicitamente
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
