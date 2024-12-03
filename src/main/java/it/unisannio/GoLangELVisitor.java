package it.unisannio;


import generated.GoParser;
import generated.GoParserBaseVisitor;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;


class GoLangELVisitor extends GoParserBaseVisitor<String> {
    public TokenStreamRewriter rewriter;
    static final Logger logger = LogManager.getLogger(GoLangELVisitor.class.getName());

    public GoLangELVisitor (TokenStreamRewriter rewriter) {
        super();
        this.rewriter = rewriter;
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

        // if not already imported import library for csv manipulation
        List<GoParser.ImportSpecContext> imports = ctx.importSpec();
        if(imports.stream().noneMatch((i)-> i.getText().contains("encoding/csv"))){
            rewriter.insertAfter(imports.getLast().stop, "\n\"encoding/csv\"");
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
        String separatorSpecification = identifier + "_reader.Comma = %s";


        separatorSpecification = String.format(separatorSpecification, separator);
        logger.debug(separatorSpecification);




        String getCsv = identifier + ", _ :="+ identifier + "_reader.ReadAll()";
        String code = String.join("\n", comment, openFile, deferring, reader, separatorSpecification, getCsv, endComment);


        rewriter.replace(ctx.start, ctx.stop, code);
        return super.visitLoadCSV(ctx);
    }
}
