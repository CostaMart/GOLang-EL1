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
    protected void loadCsvSemanticCheck(String varName,Stack<String> sc){
        if (sym.isInConflict(varName, sc)) throw new RuntimeException("Variable '" + varName + "' already defined in this scope");
    }

    protected void declareSemanticCheck(String varName, Stack<String> sc){
        if (sym.isInConflict(varName, sc)) throw new RuntimeException("Variable '" + varName + "' already defined in this scope");
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
            String toCheck = "*".concat(elementType);

            // taglia l'intestazione
            int ind = funct.indexOf("{");
            String funHeader = funct.substring(0, ind + 1 );

            // check if the required pattern is respected by the regex
            Pattern pattern = Pattern.compile("func\\s*\\([a-zA-Z_][a-zA-Z0-9_]*\\s*\\*[a-zA-Z_][a-zA-Z0-9_]*\\)\\s*\\{");
            Matcher matcher = pattern.matcher(funHeader);
            boolean foundParam = matcher.find();

            if(!foundParam)
                throw  new RuntimeException("lambda must accept a single parameter of type *"+ elementType + " and return null to be used with map statement");
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

        // check if the target variable is of the right type
        if(target != null) {
            String type = sym.getRecord(target, sc).getType();
           if (!type.equals(datasetType)) throw new RuntimeException("Variable '" + target + "' is not the same type as the dataset: " + datasetType);
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
        List<String> ret = fun.getReturnVals();

        if(ret.size() > 1) throw new IllegalArgumentException("function '" + functId + "' must return null to be used with map");

        for (String r : fun.getReturnVals()){
            if(!r.contains("null")) throw new IllegalArgumentException("function '" + functId + "' must return null to be used with map");
        }
    }

    /**
     * Performs a semantic check for the given variable and its associated function or target.
     * It verifies that the variable has the appropriate collection type and that any associated lambda or
     * function is correctly defined. The check ensures that the lambda function (if provided) has the correct
     * parameters and return values, and that the variable is of the correct collection type.
     *
     * @param sc the current scope stack, representing the variable's scope and context during parsing
     * @param varName the name of the variable to check
     * @param functId the function ID (if any) associated with the variable, used for deferred checks
     * @param funct the lambda function (if any) associated with the variable, or null if not applicable
     * @param target the target associated with the variable, used for additional checks (optional)
     * @throws RuntimeException if the variable does not have the correct collection type, or if the lambda or function is not defined correctly
     */
    protected void reduceCSVSemanticCheck(Stack<String> sc, String varName, String functId, String funct, String target){
        String datasetType = sym.getRecord(varName, sc).getType();
        String elementType = datasetType.replace("[", "").replace("]", "");

        if(target == null) throw new RuntimeException("reduce statements requires a target variable");

        //check if variable is a collection
        if(!(datasetType.contains("[]"))) throw new RuntimeException("Variable '" + varName + "' has not a collection type");

        // TODO: for now reduce cannot accept lambdas
        // if it is a lambda do this
        if(funct != null){
            
            String funcHeader;
            funcHeader = funct.substring(0, funct.indexOf("{") +1 );


            // check if the lambda has the right parameters and if it as no return values


            Pattern pattern = Pattern.compile("func\\s*\\(\\s*[a-zA-Z_][a-zA-Z0-9_]*\\s*,\\s*[a-zA-Z_][a-zA-Z0-9_]*\\s*\\*" + elementType+ "\\) \\s*(\\(\\s*[a-zA-Z_][a-zA-Z0-9_]*\\s* " + elementType+"\\)|" + elementType + ")\\s*\\{");
            Matcher matcher = pattern.matcher(funcHeader);

            boolean found = matcher.find();

            if(!found)
                throw  new RuntimeException("lambda must accept a single parameter of type *"+ elementType + " and return null to be used in reduce statement e.g: func (cumulated, single *StructType) (StructType)");
        }
        else {
            // here we check if the function passed has the right parameters and return null, since the function
            // might be declared at the end of the file we defer this check
            try {
                Stack<String> scopy = new Stack<String>();
                scopy.addAll(sc);
                // Passaggio del metodo con i tipi corretti
                Object[] params = new Object[] {scopy, elementType, functId, target};
                checkStackPut(GoParserBase.class.getDeclaredMethod("reduceCSVSemanticCheckDeferred", Stack.class, String.class, String.class, String.class), params);

            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        // check if the target variable is of the right type
        /*if(target != null) {
            String type = sym.getRecord(target, sc).getType();
            if (!type.equals(datasetType)) throw new RuntimeException("Variable '" + target + "' is not the same type as the dataset: " + datasetType);
        }*/

    }

    /**
     * Deferred semantic check for the function associated with the given function ID. It verifies that the function's
     * parameters and return values are consistent with the expected type. The check ensures that the function accepts a
     * single parameter of the expected type, and that the return type is also of the correct type.
     *
     * @param sc the current scope stack, representing the function's scope and context during parsing
     * @param type the expected type of the function parameter
     * @param functId the function ID to check, used to retrieve the function record for validation
     * @param target the target associated with the function (optional, for future extensions)
     * @throws IllegalArgumentException if the function does not accept the correct parameter type or return type
     */
    protected void reduceCSVSemanticCheckDeferred(Stack<String> sc, String type, String functId,  String target){
        FunctionRecord fun = (FunctionRecord) sym.getRecord(functId, sc).getValue();
        List<String> params = fun.getParams();

        for (int i =0; i < params.size(); i++) {
            String param = params.get(i);
            if(i == params.size() -1){
                if (!(params.get(i).contains("*" + type))) throw new IllegalArgumentException("function '" + functId +
                        "' must accept a single parameter of type *" + type +
                        ", function firm must respect following syntax: func " + functId + "(param1, param2 *"+ type +")" );
            }
            else if (params.get(i).split(" ").length > 1) throw new IllegalArgumentException("function '" +
                    functId + "' must accept a single parameter of type *" + type +
                    ", function firm must respect following syntax: func " + functId + "(param1, param2 *"+ type +")" );
        }
        List<String> ret = fun.getReturnVals();

        if(ret.size() > 1) throw new IllegalArgumentException("function " + functId + " must have a single return of type: " + type);


        if ((!ret.getFirst().contains(type) || (ret.getFirst().contains("*" + type)) || (ret.getFirst().contains("[]" + type)))) {
            throw new IllegalArgumentException("function '" + functId + "' must have a single return of type: " + type);
        }

    }

    protected void splitSemanticCheckDeferred(Stack<String> sc, String from, String percentage1, String percentage2, String percentage3, String target1, String target2, String target3){

        // check if the user splitted correctly (sum of percentage is 100%)
        double p1 = Double.parseDouble(percentage1);
        double p2 = Double.parseDouble(percentage2);
        double p3 = 0.0;

        if(percentage3 != null) {
            p3 = Double.parseDouble(percentage3);
        }

        if((percentage3 == null) != (target3 == null))  throw new RuntimeException("if 3 percentages are used, there must be 3 target variables");

        if(p1+ p2 + p3 != 1.0) throw new RuntimeException("sum of percentages must be 1");

        String fromType = sym.getRecord(from, sc).getType();
        String t1Type = sym.getRecord(target1, sc).getType();
        String t2Type = sym.getRecord(target2, sc).getType();
        String t3Type = null;

        if(target3 != null) {
         t3Type = sym.getRecord(target3, sc).getType();
         if(!fromType.equals(t3Type))
            throw new RuntimeException("target variables must have same type of source");
        }

        if(!fromType.equals(t1Type) || !fromType.equals(t2Type) ){
            throw new RuntimeException("target variables must have same type as source");
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
                throw new RuntimeException();
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
