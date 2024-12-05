package it.unisannio;


import generated.GoParser;
import generated.GoParserBaseVisitor;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;

import java.text.MessageFormat;
import java.util.List;
import java.util.Random;


class GoLangELVisitor extends GoParserBaseVisitor<String> {
    private static final Random random = new Random();
    public TokenStreamRewriter rewriter;
    static final Logger logger = LogManager.getLogger("visitor");

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
                    \t"encoding/csv"
                    \t"fmt"
                    \t"os"
                    )""");
        }

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
    public String visitImportDecl(GoParser.ImportDeclContext ctx) {
        boolean verità = false;
        // if not already imported import library for csv manipulation
        List<GoParser.ImportSpecContext> imports = ctx.importSpec();

        if(imports.stream().noneMatch((i)-> i.getText().contains("encoding/csv"))){
            rewriter.insertAfter(imports.getLast().stop, "\n\"encoding/csv\"");
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
        String separator = "','";

        if(ctx.RUNE_LIT() != null)
            separator =  ctx.RUNE_LIT().getText();

        String identifier = ctx.IDENTIFIER().getText();

        String openFile = identifier + "_csv, _ := os.Open("+ csv+ ")";
        String deferring = "defer "+ identifier +"_csv.Close()";
        String reader = identifier + "_reader := csv.NewReader("+ identifier +"_csv)";
        String separatorSpecification = identifier + "_reader.Comma = " + separator;


        separatorSpecification = String.format(separatorSpecification, separator);
        logger.debug(separatorSpecification);




        String getCsv = identifier + ", _ :="+ identifier + "_reader.ReadAll()";
        String code = String.join("\n", comment, openFile, deferring, reader, separatorSpecification, getCsv, endComment);


        rewriter.replace(ctx.start, ctx.stop, code);
        return super.visitLoadCSV(ctx);
    }

    @Override
    public String visitFilterCSV(GoParser.FilterCSVContext ctx) {
        // Estrai l'espressione e l'identificatore dal contesto
        String expression = ctx.index().expression().getText();
        String operand1 = ctx.index().expression().expression(0).getText();
        String operand2 = ctx.index().expression().expression(1).getText();

        String identi = ctx.IDENTIFIER().getText();
        expression = expression.replace(operand1, "");
        String rndm =Integer.toString(Math.abs(random.nextInt()));


        String expressionString  = expression.contains("<") || expression.contains(">") ? "== \"mario\"" : expression.replace(operand2, "\""+ operand2 + "\"");
        logger.debug("this is result =" + getType(operand2));
        String expressionNumber = getType(operand2).equals("String")  ? expression.replace(operand2,"0") : expression;

        String conversion = MessageFormat.format(""" 
                convertstr{0}, convertint{0}, convertfloat{0} := ConvertString(input{0})""", rndm);


        // Numero casuale positivo
        // Usa MessageFormat per generare il codice Go
        String toInsert = MessageFormat.format("""
            var filteredRecords{0} [][]string
            var index{0} int
            for i{0}, record{0} := range {1} '{'
                    if i{0} == 0 '{'
                        filteredRecords{0} = append(filteredRecords{0}, record{0})
                        for i{0}, v{0} := range filteredRecords{0}[0] '{'
                            if v{0} == "{3}" '{'
                                index{0} = i{0}
                                break
                            '}'
                        continue
                    '}'
                    '}'
                    input{0} := record{0}[index{0}]
                    {4}
                    if convertstr{0} != "" '{'
                            if convertstr{0} {5} '{'
                            filteredRecords{0} = append(filteredRecords{0}, record{0}) '}'
                    '}' else if convertint{0} != nil '{'
                           if *convertint{0} {6} '{'
                            filteredRecords{0} = append(filteredRecords{0}, record{0}) '}'
                        '}' else '{'
                             if *convertfloat{0} {6} '{'
                            filteredRecords{0} = append(filteredRecords{0}, record{0}) '}'
                       '}'
            {1} = filteredRecords{0} '}'
            """, rndm, identi, expression, operand1, conversion, expressionString, expressionNumber);

        // Sostituisci il testo del contesto con il codice generato
        rewriter.replace(ctx.start, ctx.stop, "// generated by visitFilterCSV --\n" + toInsert +"\n//------------------------");

        // Restituisci il risultato dalla funzione visitFilterCSV
        return super.visitFilterCSV(ctx);
    }

    public static String getType(String input) {
        if (input == null || input.isEmpty()) {
            return "String";  // Consideriamo vuoto o nullo come Stringa
        }

        // Tentiamo di convertire la stringa in un intero
        try {
            Integer.parseInt(input);
            return "Integer";
        } catch (NumberFormatException e) {
            // Se non è un intero, proviamo a convertirlo in float
            try {
                Float.parseFloat(input);
                return "Float";
            } catch (NumberFormatException ex) {
                // Se non è né un intero né un float, la lasciamo come stringa
                return "String";
            }
        }
    }


}
