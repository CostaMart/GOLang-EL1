package it.unisannio;


import generated.GoParser;
import generated.GoParserBaseVisitor;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


class GoLangELVisitor extends GoParserBaseVisitor<String> {

    SymbolTable symbolTable = SymbolTableFactory.getInstance();
    private static final Random random = new Random();
    public TokenStreamRewriter rewriter;
    static final Logger logger = LogManager.getLogger("visitor");

    org.antlr.v4.runtime.Token main; // point first function of the file starts, used to create structs on top
    org.antlr.v4.runtime.Token importEnd;
    org.antlr.v4.runtime.Token packageDeclaration;

    // keep track of scope during code analysis
    Stack<String> scopes = new Stack<>();

    // necessary to understand if its necessary to add dependencies
    static boolean IMPORTSPRESENT = false;
    static boolean OSUSED, GOCSVUSED, FMTUSED = false;
    static boolean OSPRESENT, GOCSVPRESENT, FMTPRESENT = false;


    @Override
    public String visitPackageClause(GoParser.PackageClauseContext ctx) {
        packageDeclaration = ctx.stop;
        return super.visitPackageClause(ctx);
    }

    public GoLangELVisitor (TokenStreamRewriter rewriter) {
        super();
        this.rewriter = rewriter;
    }

    @Override
    public String visitParameterDecl(GoParser.ParameterDeclContext ctx) {
        return super.visitParameterDecl(ctx);
    }

    @Override
    public String visitSourceFile(GoParser.SourceFileContext ctx) {

        // if not import are declared add necessary imports
        if (ctx.importDecl() == null) {
            rewriter.insertAfter(ctx.packageClause().stop, """
                    
                    
                     import (
                    \t"github.com/gocarina/gocsv"
                    \t"fmt"
                    \t"os"
                    )""");
        }
        main = ctx.functionDecl(0).start;
        String value = super.visitSourceFile(ctx);
        String imports = "";
        if (!IMPORTSPRESENT)
            imports = "\n import (";
        if(!FMTPRESENT && FMTUSED)
            imports = imports.concat("\n\"fmt\"");

        if(!OSPRESENT && OSUSED)
            imports = imports.concat("\n\"os\"");

        if(!GOCSVPRESENT && GOCSVUSED)
            imports = imports.concat("\n\"github.com/gocarina/gocsv\"");

        if (!IMPORTSPRESENT)
            imports = imports.concat(")");

        if (!IMPORTSPRESENT) rewriter.insertAfter(packageDeclaration, imports);
        else rewriter.insertAfter(importEnd, imports);

        return value;
    }

    @Override
    public String visitPrintExpr(GoParser.PrintExprContext ctx) {
        String str = visit(ctx.string_());
        str = str.replace("\"", "");
        String replacement = "fmt.Printf(\"" + str + "\");";
        rewriter.replace(ctx.start, ctx.stop, replacement);
        return super.visitPrintExpr(ctx);
    }

    @Override
    public String visitString_(GoParser.String_Context ctx) {
        return ctx.getText();
    }

    @Override
    public String visitForStmt(GoParser.ForStmtContext ctx) {
        scopes.push("for" + "-" + ctx.FOR.getLine());
        logger.debug(scopes);

        // visit deeper
        String result = super.visitForStmt(ctx);
        // return from visit

        scopes.pop();
        return result;
    }

    @Override
    public String visitImportDecl(GoParser.ImportDeclContext ctx) {
        IMPORTSPRESENT = true;

        boolean verità = false;
        // if not already imported import library for csv manipulation
        List<GoParser.ImportSpecContext> imports = ctx.importSpec();

        if(imports.stream().anyMatch((i)-> i.getText().contains("\"fmt\"\n"))){
            FMTPRESENT = true;
        }


        if(imports.stream().anyMatch((i)-> i.getText().contains("\"github.com/gocarina/gocsv\"\n"))){
            GOCSVPRESENT = true;
        }

        verità = imports.stream().anyMatch((i)-> i.getText().contains("os"));
        if(imports.stream().anyMatch((i)-> i.getText().contains("os"))){
            OSPRESENT = true;
        }

        importEnd = imports.getLast().getStop();
        return super.visitImportDecl(ctx);
    }

    @Override
    public String visitLoadCSV(GoParser.LoadCSVContext ctx) {
        GOCSVUSED = true;
        OSUSED = true;
        FMTUSED = true;





        String comment = "\n// generated from visitLoadCSV start--";
        String endComment = "//-------------------------------";
        String csv = visitString_(ctx.string_());
        csv = csv.replace("\"","");
        String  typeName = ctx.IDENTIFIER(0).getText();


        // add new dataset to dataset symboltable
        if (symbolTable.getRecord("datasets") == null){
            symbolTable.put("datasets", new Stack<>());
            symbolTable.assing("datasets", new Stack<>(), new HashMap<String, DatasetRecord>());
        }


        // ---- study csv
        // create only if struct doesn't already exist
        if(!(((HashMap<String, DatasetRecord>)symbolTable.getRecord("datasets").getValue())).containsKey(typeName)){
            try {
                FileReader reader = new FileReader(csv);
                BufferedReader br = new BufferedReader(reader);

                String intestazione = br.readLine();
                logger.debug(intestazione);
                String[] intestazioneSplit = intestazione.split(",");

                String firstline = br.readLine();
                String[] firstlineSplit = firstline.split(",");

                String toAdd = "";
                for (int x = 0; x < intestazioneSplit.length; x++) {
                    toAdd = toAdd.concat(intestazioneSplit[x] + " " + GoUtilities.getTGoType(firstlineSplit[x]) + "\n" );
                }

                String struct = String.format("type %s struct {\n %s }\n", typeName, toAdd);
                rewriter.insertBefore(main, struct);
                logger.debug(struct);

                SymbolTable.Record r = symbolTable.getRecord("datasets", new Stack<>());
                HashMap<String, DatasetRecord> value = (HashMap<String, DatasetRecord>) r.getValue();

                if(!(value.containsKey(typeName))){
                    value.put(typeName, new DatasetRecord(intestazioneSplit));
                    r.setValue(value);
                    r.setType(typeName);
                }

            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);

            } catch (IOException e) {

                throw new RuntimeException(e);
            }
        }

        // -------------------
        // generate go
        String identifier = ctx.IDENTIFIER(1).getText();
        String decoder = "decoder" + Integer.toString(Math.abs(random.nextInt()));
        String openFile = identifier + "_csv, _ := os.Open(\""+ csv+ "\")";
        String deferring = "defer "+ identifier +"_csv.Close()";
        String containerDecl = "var " + identifier + " []" + typeName;
        String decode =  "if err := gocsv.UnmarshalFile("+ identifier + "_csv "+ ", &"+ identifier + "); err != nil {" +
                "\nfmt.Println(\"Error decoding CSV:\", err) " +
                "\nreturn}";


        String code = String.join("\n", comment, openFile, deferring, containerDecl, decode,  endComment);
        rewriter.replace(ctx.start, ctx.stop, code);





        return super.visitLoadCSV(ctx);
    }

    @Override
    public String visitFilterCSV(GoParser.FilterCSVContext ctx) {
        String code = Integer.toString(Math.abs(random.nextInt()));

        // get what you need to generate code
       String variable = ctx.IDENTIFIER(0).getText();
       String type = symbolTable.getRecord(variable, scopes).getType();
        logger.debug("thi is type: "  + type + " searching in scope " + scopes);
       String firstOpExpression = ctx.expression(0).getText();

       String opreator = ctx.operator().getText();
       String secondOpExpression = ctx.expression(1).getText();

       // Check if there are references to members of the struct in the expression
        Map<String, DatasetRecord> datasets = (Map<String, DatasetRecord>) symbolTable.getRecord("datasets").getValue();
        type = type.replace("[", "").replace("]", "");
        DatasetRecord record = datasets.get(type);
        String[] header = record.getHeader();

        // if tehre are reference to the members of the sctruct in the expression of the filter put the name of the variable in front of them to access them correctly
        // e.g. with a Pesron type dataset: x[Age > 20] -> if variable.Age > 20
        for(String s : header){
            firstOpExpression = firstOpExpression.replace(s, "variable" + code + "." + s);
        }

       logger.debug("for " + firstOpExpression + " in " + scopes);
       logger.debug("the type is " + type);

        // generate go
        String firm = "\n// generated from visitFilterCSV start--";
       String declareVar = String.format("var filtered%s []%s", code, type);
       String forDeclar = String.format("for _, variable%s := range %s {", code, variable);
       String forBlock = String.format("if %s %s %s {",firstOpExpression, opreator,secondOpExpression);
       String insideInternalIf = String.format("filtered%s = append(filtered%s, variable%s)",code, code,code);
       String closeBRKT ="\t}\n" +
               "\t}";
       String assignToVar = String.format("%s = filtered%s", variable, code);
        String closeFirm = "//----------------------------------\n";

       String toAdd = String.join("\n", firm, declareVar,forDeclar,forBlock,insideInternalIf,closeBRKT,assignToVar, closeFirm);

       logger.debug("to Add:" + toAdd);
       rewriter.replace(ctx.start, ctx.stop, toAdd);
        return super.visitFilterCSV(ctx);
    }


    @Override
    public String visitIfStmt(GoParser.IfStmtContext ctx) {
        // push new scope in stack when 'if' starts
        scopes.push("if" + "-" + ctx.IF.getLine());
        logger.debug(scopes);
        // visi deeper
        String result = super.visitIfStmt(ctx);


        // pop out of 'if' scope when IF structure ends
        scopes.pop();
        return result;
    }

    @Override
    public String visitMapCSV(GoParser.MapCSVContext ctx) {
        // TODO: implement this
        String rndm = Integer.toString(Math.abs(random.nextInt()));
        String variable = ctx.IDENTIFIER(0).getText();
        String functionLit = null;
        
        if (ctx.functionLit() != null)
            functionLit = ctx.functionLit().getText();

        TerminalNode signatureNode = ctx.IDENTIFIER(1);

        String closeFirm = "//-------------------------------\n";
        String firm = "\n// generated from visitMapCSV start--";

        // check if the target node is in second or third place
        TerminalNode targetNode;
        if(functionLit != null){
            targetNode = ctx.IDENTIFIER(1);
        }
        else {
            targetNode = ctx.IDENTIFIER(2);
        }

        String forOnStcut = "";
        // se viene passata la firma di una funzione già definita fai qusta
        if(functionLit == null){
            String signature = signatureNode.getText();
            // prepare execution

            forOnStcut = String.format("""
                    for i%s, _ := range %s {
                        %s(&%s[i%s])
                        }""",rndm, variable, signature,variable, rndm);


            forOnStcut = String.join("\n", firm, forOnStcut, closeFirm);
            rewriter.replace(ctx.start, ctx.stop, forOnStcut);

        }

        // se viene passata una lambda fai questo
        else {
            String codeBlock = ctx.functionLit().block().getText();
            List<GoParser. ParameterDeclContext> codeParams = ctx.functionLit().signature().parameters().parameterDecl();

            // recupero nome delle variabili della funzione passata
            String paramVar = codeParams.getFirst().identifierList().getText();
            String paramType = codeParams.getFirst().type_().getText();
            // --------------------


            // prepare params string
            String param = String.format("%s %s", paramVar, paramType);
            logger.debug("params " + param);


            // prepare execution

            String instanciateFunct = String.format("function%s := func (%s) %s\n", rndm,  param, codeBlock);

            forOnStcut = String.format("""
                    for i%s, _ := range %s {
                        function%s(&%s[i%s])
                        }""",rndm, variable, rndm, variable, rndm);


            forOnStcut = String.join("\n", instanciateFunct, forOnStcut, closeFirm);

        }

        // if targetNode is not null the mapping is not in place
        String type = symbolTable.getRecord(variable, scopes).getType();
        System.out.println("il tipooooo " + type);

        if (targetNode != null) {
            String doBefore = String.format(
                    """
                            \n x%s := make(%s, len(%s))
                                copy( x%s, %s)
                            """, rndm, type, variable, rndm, variable
            );

            String doAfter = String.format("""
                           \n%s = %s
                           %s = x%s """,targetNode.getText(), variable, variable, rndm);

            forOnStcut = String.join("\n", doBefore, forOnStcut, doAfter);
        }

        forOnStcut = String.join("\n", firm, forOnStcut, closeFirm);
        rewriter.replace(ctx.start, ctx.stop, forOnStcut);
        return super.visitMapCSV(ctx);
    }

    @Override
    public String visitReduceCSV(GoParser.ReduceCSVContext ctx) {
        String rndm = Integer.toString(Math.abs(random.nextInt()));
        String variable = ctx.IDENTIFIER(0).getText();
        TerminalNode signatureNode = ctx.IDENTIFIER(1);
        String cumulator = ctx.IDENTIFIER(2).getText();

        // se viene passata la firma di una funzione già definita fai qusta
        if(signatureNode != null){
            String signature = signatureNode.getText();
            logger.debug("this is signature " + signature);

            SymbolTable.Record functionSignature = symbolTable.getRecord(signature);
            FunctionRecord functionrecord = (FunctionRecord) functionSignature.getValue();
            String[] paramAndType = functionrecord.getParams().get(1).split(" ");
            String returnVal = functionrecord.getReturnVals().getFirst();


            // prepare execution
            String firm = "\n// generated from visitReduceCSV start--";


            String forOnStcut = String.format("""
                    for  _ , element%s := range %s {
                        %s = %s(&%s, &element%s)
                        }""",rndm, variable, cumulator, signature, cumulator, rndm);
            String closeFirm = "//-------------------------------\n";

            forOnStcut = String.join("\n", firm, forOnStcut, closeFirm);
            rewriter.replace(ctx.start, ctx.stop, forOnStcut);
        }




        return super.visitReduceCSV(ctx);
    }
}
