package it.unisannio;


import generated.GoParser;
import generated.GoParserBaseVisitor;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.SymbolTable;
import util.SymbolTableFactory;
import util.Utilities;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Random;
import java.util.Stack;


class GoLangELVisitor extends GoParserBaseVisitor<String> {
    SymbolTable symbolTable = SymbolTableFactory.getInstance();
    private static final Random random = new Random();
    public TokenStreamRewriter rewriter;
    static final Logger logger = LogManager.getLogger("visitor");
    org.antlr.v4.runtime.Token main;
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
        // ---- study csv
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
                toAdd = toAdd.concat(intestazioneSplit[x] + " " + Utilities.getTGoType(firstlineSplit[x]) + "\n" );
            }

            String struct = String.format("type %s struct {\n %s }\n", typeName, toAdd);
            rewriter.insertBefore(main, struct);
            logger.debug(struct);



        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
        return super.visitLoadCSV(ctx);
    }

    @Override
    public String visitFilterCSV(GoParser.FilterCSVContext ctx) {
        String code = Integer.toString(Math.abs(random.nextInt()));

       String variable = ctx.IDENTIFIER().get(0).getText();
       String type = symbolTable.getRecord(variable, scopes).getType();
       String firstOpExpression = ctx.IDENTIFIER(1).getText();
       String opreator = ctx.operator().getText();
       String secondOpExpression = ctx.expression().getText();


       logger.debug("for " + variable + " in " + scopes);
       logger.debug("the type is " + type);


       String declareVar = String.format("var filtered%s []%s", code, type);
       String forDeclar = String.format("for _, people%s := range %s {", code, variable);
       String forBlock = String.format("if people%s.%s %s %s {",code, firstOpExpression, opreator,secondOpExpression);
       String insideInternalIf = String.format("filtered%s = append(filtered%s, people%s)",code, code,code);
       String closeBRKT ="\t}\n" +
               "\t}";
       String assignToVar = String.format("%s = filtered%s", variable, code);


       String toAdd = String.join("\n", declareVar,forDeclar,forBlock,insideInternalIf,closeBRKT,assignToVar);
        logger.debug("to Add:" + toAdd);
       rewriter.replace(ctx.start, ctx.stop, toAdd);
        return super.visitFilterCSV(ctx);
    }




}
