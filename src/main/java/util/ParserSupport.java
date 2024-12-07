package util;

import generated.GoParser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;
import java.util.Stack;

public class ParserSupport {
    static SymbolTable symbolTable = SymbolTableFactory.getInstance();

    public static void manageVarDecl(String[] vars, String type, Stack<String> scopes){
        for (String identifier : vars) {
            symbolTable.put(identifier, scopes);
            symbolTable.setType(identifier, scopes, type);
        }
    }

}
