package it.unisannio;


import generated.GoParser;
import generated.GoParserBaseVisitor;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.SymbolTable;
import util.SymbolTableFactory;
import util.GoUtilities;

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
    Stack<String> scopes = new Stack<>();

    public GoLangELVisitor (TokenStreamRewriter rewriter) {
        super();
        this.rewriter = rewriter;
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
        return super.visitSourceFile(ctx);
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
        boolean verità = false;
        // if not already imported import library for csv manipulation
        List<GoParser.ImportSpecContext> imports = ctx.importSpec();

        if(imports.stream().noneMatch((i)-> i.getText().contains("\"github.com/gocarina/gocsv\"\n"))){
            rewriter.insertAfter(imports.getLast().stop, "\n\"github.com/gocarina/gocsv\"\n");
        }

        verità = imports.stream().noneMatch((i)-> i.getText().contains("os"));
        if(imports.stream().noneMatch((i)-> i.getText().contains("os"))){
            rewriter.insertAfter(imports.getLast().stop, "\n\"os\"");
        }

        return super.visitImportDecl(ctx);
    }

    @Override
    public String visitLoadCSV(GoParser.LoadCSVContext ctx) {
        String comment = "\n// generated from visitLoadCSV start--";
        String endComment = "//-------------------------------";
        String csv = visitString_(ctx.string_());
        csv = csv.replace("\"","");
        String  typeName = ctx.IDENTIFIER(1).getText();


        // add new dataset to dataset symboltable
        if (symbolTable.getRecord("datasets") == null){
            symbolTable.put("datasets", new Stack<>());
            symbolTable.assing("datasets", new Stack<>(), new ArrayList<String>());
        }


        // ---- study csv
        // create only if struct doesn't already exist
        if(!(((List<String>)symbolTable.getRecord("datasets").getValue()).contains(typeName))){
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



            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);

            } catch (IOException e) {

                throw new RuntimeException(e);
            }
        }

        // -------------------
        // generate go
        String identifier = ctx.IDENTIFIER(0).getText();
        String decoder = "decoder" + Integer.toString(Math.abs(random.nextInt()));
        String openFile = identifier + "_csv, _ := os.Open(\""+ csv+ "\")";
        String deferring = "defer "+ identifier +"_csv.Close()";
        String containerDecl = "var " + identifier + " []" + typeName;
        String decode =  "if err := gocsv.UnmarshalFile("+ identifier + "_csv "+ ", &"+ identifier + "); err != nil {" +
                "\nfmt.Println(\"Error decoding CSV:\", err) " +
                "\nreturn}";


        String code = String.join("\n", comment, openFile, deferring, containerDecl, decode,  endComment);
        rewriter.replace(ctx.start, ctx.stop, code);



        SymbolTable.Record r = symbolTable.getRecord("datasets", new Stack<>());
        List<String> value = (List<String>)r.getValue();

        if(!(value.contains(typeName))){
            value.add(typeName);
            r.setValue(value);
        }

        return super.visitLoadCSV(ctx);
    }

    @Override
    public String visitFilterCSV(GoParser.FilterCSVContext ctx) {
        String code = Integer.toString(Math.abs(random.nextInt()));

        // get what you need to generate code
       String variable = ctx.IDENTIFIER().get(0).getText();
       String type = symbolTable.getRecord(variable, scopes).getType();
       String firstOpExpression = ctx.IDENTIFIER(1).getText();
       String opreator = ctx.operator().getText();
       String secondOpExpression = ctx.expression().getText();


       logger.debug("for " + variable + " in " + scopes);
       logger.debug("the type is " + type);

        // generate go
        String firm = "\n// generated from visitFilterCSV start--";
       String declareVar = String.format("var filtered%s []%s", code, type);
       String forDeclar = String.format("for _, people%s := range %s {", code, variable);
       String forBlock = String.format("if people%s.%s %s %s {",code, firstOpExpression, opreator,secondOpExpression);
       String insideInternalIf = String.format("filtered%s = append(filtered%s, people%s)",code, code,code);
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
        TerminalNode signatureNode = ctx.IDENTIFIER(1);

        // se viene passata la firma di una funzione già definita fai qusta
        if(signatureNode != null){
            String signature = signatureNode.getText();
            // prepare execution
            String firm = "\n// generated from visitMapCSV start--";
            String forOnStcut = String.format("""
                    for i%s, _ := range %s {
                        %s(&%s[i%s])
                        }""",rndm, variable, signature,variable, rndm);
            String closeFirm = "//-------------------------------\n";

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
            String firm = "\n// generated from visitMapCSV start--";
            String instanciateFunct = String.format("function%s := func (%s) %s\n", rndm,  param, codeBlock);

            String forOnStcut = String.format("""
                    for i%s, _ := range %s {
                        function%s(&%s[i%s])
                        }""",rndm, variable, rndm, variable, rndm);
            String closeFirm = "//-------------------------------\n";

            forOnStcut = String.join("\n", firm, instanciateFunct, forOnStcut, closeFirm);
            rewriter.replace(ctx.start, ctx.stop, forOnStcut);
        }

        return super.visitMapCSV(ctx);
    }
}
