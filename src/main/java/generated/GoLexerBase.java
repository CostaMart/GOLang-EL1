package generated;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.atn.ATN;
import util.SymbolTable;

public class GoLexerBase extends Lexer {
    private static String[] makeRuleNames() {
        return new String[] {
                "LOAD", "BREAK", "DEFAULT", "FUNC", "INTERFACE", "SELECT", "CASE", "DEFER",
                "GO", "MAP", "STRUCT", "CHAN", "ELSE", "GOTO", "PACKAGE", "SWITCH", "CONST",
                "FALLTHROUGH", "IF", "RANGE", "TYPE", "CONTINUE", "FOR", "IMPORT", "RETURN",
                "VAR", "NIL_LIT", "IDENTIFIER", "L_PAREN", "R_PAREN", "L_CURLY", "R_CURLY",
                "L_BRACKET", "R_BRACKET", "ASSIGN", "COMMA", "SEMI", "COLON", "DOT",
                "PLUS_PLUS", "MINUS_MINUS", "DECLARE_ASSIGN", "ELLIPSIS", "LOGICAL_OR",
                "LOGICAL_AND", "EQUALS", "NOT_EQUALS", "LESS", "LESS_OR_EQUALS", "GREATER",
                "GREATER_OR_EQUALS", "PRINTER", "OR", "DIV", "MOD", "LSHIFT", "RSHIFT",
                "BIT_CLEAR", "UNDERLYING", "EXCLAMATION", "PLUS", "MINUS", "CARET", "STAR",
                "AMPERSAND", "RECEIVE", "DECIMAL_LIT", "BINARY_LIT", "OCTAL_LIT", "HEX_LIT",
                "FLOAT_LIT", "DECIMAL_FLOAT_LIT", "HEX_FLOAT_LIT", "HEX_MANTISSA", "HEX_EXPONENT",
                "IMAGINARY_LIT", "RUNE", "RUNE_LIT", "BYTE_VALUE", "OCTAL_BYTE_VALUE",
                "HEX_BYTE_VALUE", "LITTLE_U_VALUE", "BIG_U_VALUE", "RAW_STRING_LIT",
                "INTERPRETED_STRING_LIT", "WS", "COMMENT", "TERMINATOR", "LINE_COMMENT",
                "UNICODE_VALUE", "ESCAPED_VALUE", "DECIMALS", "OCTAL_DIGIT", "HEX_DIGIT",
                "BIN_DIGIT", "EXPONENT", "LETTER", "UNICODE_DIGIT", "UNICODE_LETTER",
                "WS_NLSEMI", "COMMENT_NLSEMI", "LINE_COMMENT_NLSEMI", "EOS", "OTHER"
        };
    }

    public static final String[] ruleNames = makeRuleNames();

    public SymbolTable symboltable = new SymbolTable();


    public GoLexerBase(CharStream input) {
        super(input);
    }

    @Override
    public String getGrammarFileName() { return "GoLexer.g4"; }

    @Override
    public ATN getATN() {
        return null;
    }

    @Override
    public String[] getRuleNames() { return ruleNames; }



}
