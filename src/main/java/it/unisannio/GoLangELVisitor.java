package it.unisannio;


import generated.GoParser;
import generated.GoParserBaseVisitor;
import org.antlr.v4.runtime.TokenStreamRewriter;


class GoLangELVisitor extends GoParserBaseVisitor<String> {
    public TokenStreamRewriter rewriter;

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
}
