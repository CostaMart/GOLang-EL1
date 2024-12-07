package generated;

import org.antlr.v4.runtime.*;
import util.SymbolTable;
import util.SymbolTableFactory;
import util.FunctionRecord;
import java.util.Stack;
import util.ParserSupport;

/**
 * All parser methods that used in grammar (p, prev, notLineTerminator, etc.)
 * should start with lower case char similar to parser rules.
 */
public abstract class GoParserBase extends Parser
{
    public SymbolTable sym = SymbolTableFactory.getInstance();
    public Stack<String> scopes = new Stack<>();
    
    protected GoParserBase(TokenStream input) {
        super(input);
    }

    public void invokeSupportDecl(String val, String type, Stack<String> scopes){
        String[] vals = val.split(",");
        ParserSupport.manageVarDecl(vals, type, scopes);
    }

    public FunctionRecord createfunctionRecord(String params){return new FunctionRecord(params);}


    /**
     * Returns true if the current Token is a closing bracket (")" or "}")
     */
    protected boolean closingBracket()
    {
        BufferedTokenStream stream = (BufferedTokenStream)_input;
        int prevTokenType = stream.LA(1);

        return prevTokenType == GoParser.R_CURLY || prevTokenType == GoParser.R_PAREN;
    }
}
