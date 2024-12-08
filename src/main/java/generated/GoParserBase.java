package generated;

import org.antlr.v4.runtime.*;
import util.SymbolTable;
import util.SymbolTableFactory;
import util.FunctionRecord;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * All parser methods that used in grammar (p, prev, notLineTerminator, etc.)
 * should start with lower case char similar to parser rules.
 */
public abstract class GoParserBase extends Parser
{
    public SymbolTable sym = SymbolTableFactory.getInstance();
    public Stack<String> scopes = new Stack<>();
    public Stack<CheckCall> checks = new Stack<>();


    protected GoParserBase(TokenStream input) {
        super(input);
    }



    /**
     * Returns true if the current Token is a closing bracket (")" or "}")
     */
    protected boolean closingBracket()
    {
        BufferedTokenStream stream = (BufferedTokenStream)_input;
        int prevTokenType = stream.LA(1);

        return prevTokenType == GoParser.R_CURLY || prevTokenType == GoParser.R_PAREN;
    }


    // metodi di supporto al parser

    /**
     * Adds a semantic check method to the stack, deferring its execution until the end of the parsing.
     * This is useful for executing checks that involve references to entities declared later in the code,
     * where the necessary information is not yet available during the current parsing stage.
     *
     * @param m the {@link Method} representing the semantic check to be deferred
     * @param obj the {@link Object} array containing the parameters to be passed to the method when invoked
     */
    private void checkStackPut(Method m, Object[] obj){
        checks.push(new CheckCall(m, obj));
    }

    /**
     * Creates a {@link FunctionRecord} object based on the provided parameters.
     *
     * @param params the parameters for the function record
     * @return a new {@link FunctionRecord} object
     */
    protected FunctionRecord createfunctionRecord(String params){return new FunctionRecord(params);}

    /**
     * Declares a variable with the given value and type, and adds it to the symbol table for the given scopes.
     *
     * @param val the variable value
     * @param type the variable type
     * @param scopes the stack of scopes where the variable is defined
     */
    protected void varDeclRoutine(String val, String type, Stack<String> scopes){
        String[] vals = val.split(",");
        for (String identifier : vals) {
            sym.put(identifier, scopes);
            sym.setType(identifier, scopes, type);
        }
    }

    /**
     * Performs a semantic check for a variable's existence and type consistency.
     * Throws a {@link RuntimeException} if the variable is already defined in the current scope.
     *
     * @param sc the current scope stack
     * @param varName the name of the variable to check
     */
    protected void loadCsvSemanticCheck(Stack<String> sc, String varName){
        if (sym.getRecord(varName, sc) != null) throw new RuntimeException("Variable '" + varName + "' already defined in this scope");
    }


    /**
     * Performs a semantic check for the given variable and its associated function or target,
     * verifying that the variable has the appropriate collection type or that any associated lambda or
     * function is correctly defined.
     *
     * @param sc the current scope stack
     * @param varName the name of the variable to check
     * @param functId the function ID (if any) associated with the variable
     * @param funct the lambda function (if any) associated with the variable
     * @param target the target associated with the variable
     */
    protected void mapCSVSematicCheck(Stack<String> sc, String varName, String functId, String funct, String target){
        String datasetType = sym.getRecord(varName, sc).getType();
        String elementType = datasetType.replace("[", "").replace("]", "");

        //check if variable is a collection
        if(!(datasetType.contains("[]"))) throw new RuntimeException("Variable '" + varName + "' has not a collection type");

        // if it is a lambda do this
        if(funct != null){
            String sub;
            sub = funct.substring(0, funct.indexOf("{"));

            // check if the lambda has the right parameters and if it as no return values
            Pattern pattern = Pattern.compile(Pattern.quote(elementType));
            Matcher matcher = pattern.matcher(sub);

            int count = 0;
            while (matcher.find()) {
                count++;
            }

            if(!sub.contains("*"+elementType) || count > 1)
                throw  new RuntimeException("lambda must accept a single parameter of type *"+ elementType + " and return null");
        }
        else {
            // here we check if the function passed has the right parameters and return null, since the function
            // might be declared at the end of the file we defer this check
            try {
                Stack<String> scopy = new Stack<String>();
                scopy.addAll(sc);
                // Passaggio del metodo con i tipi corretti
                Object[] params = new Object[] {scopy, elementType, functId, target};
                checkStackPut(GoParserBase.class.getDeclaredMethod("mapCSVSemanticCheckDeferred", Stack.class, String.class, String.class, String.class), params);

            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Deferred semantic check for the function associated with the given function ID, verifying that the function's
     * parameters and return values are consistent with the expected type.
     *
     * @param sc the current scope stack
     * @param type the expected type of the function parameter
     * @param functId the function ID to check
     * @param target the target associated with the function
     */
    protected void mapCSVSemanticCheckDeferred(Stack<String> sc, String type, String functId,  String target){
        FunctionRecord fun = (FunctionRecord) sym.getRecord(functId, sc).getValue();

        for (String param : fun.getParams()) {
            if (!(param.contains("*" + type))) throw new IllegalArgumentException("function " + functId + " must accept a single parameter of type *" + type);
        }
        List<String> ret = fun.getParams();

        if(ret.size() > 1) throw new IllegalArgumentException("function " + functId + " must return null");

        for (String r : fun.getReturnVals()){
            if(!r.contains("null")) throw new IllegalArgumentException("function " + functId + " must return null");
        }
    }

    /**
     * Executes all deferred semantic checks that were added to the stack during parsing.
     * This method is called at the end of the parsing process to finalize the semantic checks.
     */
    protected void executeDeferredChecks() {
        while (!checks.isEmpty()) {
            CheckCall check = checks.pop();
            try {
                check.m.invoke(this, check.args);
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                e.getCause().printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }



    /**
     * A helper class used to store a method and its associated arguments for deferred execution.
     * This class is used in conjunction with the stack of deferred checks to hold the method and
     * its arguments until they are executed later in the parsing process.
     */
    private class CheckCall {

        /**
         * The method that represents the semantic check to be executed later.
         */
        private final Method m;

        /**
         * The arguments to be passed to the method when it is invoked.
         */
        private final Object[] args;


        /**
         * Constructs a new {@link CheckCall} instance with the specified method and arguments.
         *
         * @param m the {@link Method} to be invoked during deferred execution
         * @param args the array of {@link Object} arguments to be passed to the method
         */
        private CheckCall(Method m, Object[] args){
            this.m = m;
            this.args = args;
        }
    }


}
