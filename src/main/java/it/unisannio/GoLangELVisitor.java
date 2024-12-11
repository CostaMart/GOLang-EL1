package it.unisannio;


import generated.GoParser;
import generated.GoParserBaseVisitor;
import org.antlr.v4.runtime.Token;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class GoLangELVisitor extends GoParserBaseVisitor<String> {

    SymbolTable symbolTable = SymbolTableFactory.getInstance();
    private static final Random random = new Random();
    public TokenStreamRewriter rewriter;
    static final Logger logger = LogManager.getLogger("visitor");

    Token main; // point first function of the file starts, used to create structs on top
    Token importEnd;
    Token packageDeclaration;

    // keep track of scope during code analysis
    Stack<String> scopes = new Stack<>();

    // necessary to understand if its necessary to add dependencies
    static boolean IMPORTSPRESENT = false;
    static boolean OSUSED, GOCSVUSED, FMTUSED, MATHUSED, BASEUSED, KNNUSED = false;
    static boolean OSPRESENT, GOCSVPRESENT, FMTPRESENT, MATHPRESENT, BASEPRESENT, KNNPRESENT = false;


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
                    \t "time"
                    \t "github.com/sjwhitworth/golearn/base"
                    \t "github.com/sjwhitworth/golearn/knn"
                    \t "github.com/rocketlaunchr/dataframe-go"
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

        if(!MATHPRESENT && MATHUSED)
            imports = imports.concat("\n\"math/rand\"\n\"time\"");

        if(!BASEPRESENT && BASEUSED)
            imports = imports.concat("\n\"github.com/sjwhitworth/golearn/base\"");

        if(!KNNPRESENT && KNNUSED)
            imports = imports.concat("\n\"github.com/sjwhitworth/golearn/knn\"\n\"github.com/rocketlaunchr/dataframe-go\"");

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


        if(imports.stream().anyMatch((i)-> i.getText().contains("math/rand"))){
            MATHPRESENT = true;
        }

        if(imports.stream().anyMatch((i)-> i.getText().contains("github.com/sjwhitworth/golearn/base"))){
            BASEPRESENT = true;
        }

        if(imports.stream().anyMatch((i)-> i.getText().contains("github.com/sjwhitworth/golearn/knn"))){
            KNNPRESENT = true;
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
                String[] firstLineTypes = new String[firstlineSplit.length];

                String toAdd = "";
                for (int x = 0; x < intestazioneSplit.length; x++) {
                    firstLineTypes[x] = GoUtilities.getTGoType(firstlineSplit[x]);
                    toAdd = toAdd.concat(intestazioneSplit[x] + " " + firstLineTypes[x] + "\n" );

                }



                String struct = String.format("type %s struct {\n %s }\n", typeName, toAdd);
                rewriter.insertBefore(main, struct);
                logger.debug(struct);

                SymbolTable.Record r = symbolTable.getRecord("datasets", new Stack<>());
                HashMap<String, DatasetRecord> value = (HashMap<String, DatasetRecord>) r.getValue();

                if(!(value.containsKey(typeName))){
                    value.put(typeName, new DatasetRecord(intestazioneSplit, firstLineTypes));
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
        if (functionLit != null) {
            targetNode = ctx.IDENTIFIER(1);
        } else {
            targetNode = ctx.IDENTIFIER(2);
        }

        String forOnStcut = "";
        // se viene passata la firma di una funzione già definita fai qusta
        if (functionLit == null) {
            String signature = signatureNode.getText();
            // prepare execution

            forOnStcut = String.format("""
                    for i%s, _ := range %s {
                        %s(&%s[i%s])
                        }""", rndm, variable, signature, variable, rndm);


            forOnStcut = String.join("\n", firm, forOnStcut, closeFirm);
            rewriter.replace(ctx.start, ctx.stop, forOnStcut);

        }

        // se viene passata una lambda fai questo
        else {
            String codeBlock = ctx.functionLit().block().getText();
            List<GoParser.ParameterDeclContext> codeParams = ctx.functionLit().signature().parameters().parameterDecl();

            // recupero nome delle variabili della funzione passata
            String paramVar = codeParams.getFirst().identifierList().getText();
            String paramType = codeParams.getFirst().type_().getText();
            // --------------------


            // prepare params string
            String param = String.format("%s %s", paramVar, paramType);
            logger.debug("params " + param);


            // prepare execution

            String instanciateFunct = String.format("function%s := func (%s) %s\n", rndm, param, codeBlock);

            forOnStcut = String.format("""
                    for i%s, _ := range %s {
                        function%s(&%s[i%s])
                        }""", rndm, variable, rndm, variable, rndm);


            forOnStcut = String.join("\n", instanciateFunct, forOnStcut, closeFirm);

        }

        // if targetNode is not null the mapping is not in place

        if (targetNode != null) {
            String type = symbolTable.getRecord(variable, scopes).getType();
            String elemType = type;

            if (type.contains("Dataset[")){
                elemType = type.replace("]", "").replace("Dataset[", "[]");
            }
            else {
                Pattern pat = Pattern.compile(".*\\[(.*)]");
                Matcher mat = pat.matcher(elemType);
                boolean b = mat.find();
                String group = mat.group(1);
                elemType = elemType.replace(group, "");

            }
            String doBefore = String.format(
                    """
                            \n x%s := make(%s, len(%s))
                                copy( x%s, %s[:])
                            """, rndm, elemType, variable, rndm, variable
            );

            if (type.contains("Dataset[")){
                String moo =String.format("\n%s = make(%s, len(%s))\n", targetNode.getText(), elemType, variable);
                doBefore = moo.concat(doBefore);
            }

            String doAfter = String.format("""
                           \ncopy(%s[:] ,%s[:])
                           copy(%s[:],x%s)""",targetNode.getText(), variable, variable, rndm);

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
        TerminalNode signatureNode = null;
        String cumulator;
        String firm = "\n// generated from visitReduceCSV start--";
        String closeFirm = "//-------------------------------\n";
        String forOnStcut = "";


        if (ctx.functionLit() == null) {
            signatureNode = ctx.IDENTIFIER(1);
            cumulator = ctx.IDENTIFIER(2).getText();
        }
        else {
            cumulator = ctx.IDENTIFIER(1).getText();
        }
        // se viene passata la firma di una funzione già definita fai questa
        if(signatureNode != null){
            String signature = signatureNode.getText();
            logger.debug("this is signature " + signature);

            SymbolTable.Record functionSignature = symbolTable.getRecord(signature);
            FunctionRecord functionrecord = (FunctionRecord) functionSignature.getValue();
            String[] paramAndType = functionrecord.getParams().get(1).split(" ");
            String returnVal = functionrecord.getReturnVals().getFirst();


            // prepare execution



            forOnStcut = String.format("""
                    for  _ , element%s := range %s {
                        %s = %s(&%s, &element%s)
                        }""",rndm, variable, cumulator, signature, cumulator, rndm);


            forOnStcut = String.join("\n", firm, forOnStcut, closeFirm);
            rewriter.replace(ctx.start, ctx.stop, forOnStcut);
        }
        // gestiamo il caso in cui viene passata una lambda
        else{
            String codeBlock = ctx.functionLit().getText();
            String type = symbolTable.getRecord(variable, scopes).getType();
            type = type.replace("Dataset[","").replace("]","");
            type = type.trim();

            String retType = ctx.functionLit().signature().result().parameters().parameterDecl().getFirst().type_().getText();

            String returnPart = codeBlock.substring(codeBlock.indexOf(")"), codeBlock.indexOf("{"));
            String returnPartnew = returnPart.replace(retType, " "+retType );
            codeBlock = codeBlock.replace(returnPart, returnPartnew);


            List<GoParser. ParameterDeclContext> codeParams = ctx.functionLit().signature().parameters().parameterDecl();
           // separa il return altrimenti lo attacca alla variabile
            codeBlock = codeBlock.replace("return", " return ");

            String createFunVar = String.format("fu%s := " + codeBlock, rndm);

            forOnStcut = String.format("""
                    for  _ , element%s := range %s {
                        %s = fu%s(&%s, &element%s)
                        }""",rndm, variable, cumulator, rndm , cumulator, rndm);

            // recupero nome delle variabili della funzione passata
            String paramVar = codeParams.getFirst().identifierList().getText();
            String paramType = codeParams.getFirst().type_().getText();
            // --------------------


            // prepare params string
            String param = String.format("%s %s", paramVar, paramType);
            logger.debug("params " + param);


            // prepare execution

            forOnStcut = String.join("\n", firm, createFunVar, forOnStcut, closeFirm);



        }


        rewriter.replace(ctx.start, ctx.stop, forOnStcut);

        return super.visitReduceCSV(ctx);
    }


    @Override
    public String visitSplitCSV(GoParser.SplitCSVContext ctx) {
        MATHUSED = true;
        String rndm = Integer.toString(Math.abs(random.nextInt()));
        String var = ctx.IDENTIFIER(0).getText();

        String target1 = ctx.IDENTIFIER(1).getText();
        String target2 = ctx.IDENTIFIER(2).getText();
        String target3 = "";


        if (ctx.IDENTIFIER(3) != null)
            target3 = ctx.IDENTIFIER(3).getText();

        String validate = null;
        String train;
        String test;
        SymbolTable.Record variableType = symbolTable.getRecord(var);



     
            // utilizzando float
            train = ctx.FLOAT_LIT(0).getText();
            test = ctx.FLOAT_LIT(1).getText();
            if (ctx.FLOAT_LIT(2) != null)
                validate = ctx.FLOAT_LIT(2).getText();

        String regex = ".*\\[([a-zA-Z_][a-zA-Z0-9_]*)]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(variableType.getType());
        String type = "";
        if(matcher.find()){
            type = matcher.group(1);
        }

        // make a copy
        String makeCopy = String.format("\ncopy%s := make(%s, len(%s))\n copy(copy%s, %s)\n", rndm, "[]" + type, var, rndm, var);


        // shuffle
        String shuffle =String.format( """
        

        rand.Seed(time.Now().UnixNano())
        rand.Shuffle(len(copy%s), func(i, j int) {
            copy%s[i], copy%s[j] = copy%s[j], copy%s[i]
        })""",rndm, rndm, rndm, rndm, rndm) ;

        String determinIndexes = String.format("""
                
                	total%s := len(copy%s)
                	trainEnd%s := int( %s * float64(total%s))
                	valEnd%s := trainEnd%s + int(%s *float64(total%s))
                """, rndm, rndm, rndm, train, rndm, rndm, rndm, test, rndm);

        // split
        String splitting = String.format("""
                
                trainSet%s := copy%s[:trainEnd%s]
                validationSet%s := copy%s[trainEnd%s:valEnd%s]
                testSet%s := copy%s[valEnd%s:]
                """, rndm, rndm, rndm, rndm, rndm, rndm,rndm,rndm,rndm,rndm
        );

        String returnString = "";
        // return
        if(validate != null) {
             returnString = String.format("""
                    
                    %s = trainSet%s
                    %s = validationSet%s
                    %s = testSet%s
                    """, target1, rndm, target2, rndm, target3, rndm);
        }
        else {

            returnString = String.format("""
                    
                    %s = trainSet%s
                    validationSl%s := validationSet%s[:]
                    testSetSl%s := testSet%s[:]
                    %s = append(validationSl%s, testSetSl%s...)
                    
                    """, target1, rndm, rndm, rndm, rndm, rndm, target2, rndm, rndm);


        }

        String firm = "\n// generated from visitSplitCSV start--";
        String closeFirm = "// ----------------------------------\n";

        String fin = String.join(" ", firm, makeCopy, shuffle, determinIndexes, splitting, returnString, closeFirm);
        rewriter.replace(ctx.start, ctx.stop, fin);
        return super.visitSplitCSV(ctx);
    }

    @Override
    public String visitTrainModel(GoParser.TrainModelContext ctx) {
        KNNUSED = true;
        BASEUSED = true;


        String rndm = Integer.toString(Math.abs(random.nextInt()));
        String var = ctx.IDENTIFIER(0).getText();

        String dataset = ctx.IDENTIFIER(1).getText();

        Map<String, DatasetRecord> datasetsMap = (Map<String, DatasetRecord>) symbolTable.getRecord("datasets").getValue();

        String datasetType = symbolTable.getRecord(dataset, scopes).getType();

        datasetType = datasetType.substring(datasetType.indexOf("[")+1, datasetType.indexOf("]"));


        DatasetRecord datasetInfo = datasetsMap.get(datasetType);
        String[] types = datasetInfo.getHeaderTypes();
        String[] names = datasetInfo.getHeader();

        int toPredict = types.length - 1;

        // create columns
        String addition = "";
        String finalString = "";

        for (int x = 0; x < types.length; x++) {
            addition = switch (types[x]){
                case "int" -> String.format("%s%s := dataframe.NewSeriesInt64(\"%s\", nil)", names[x], rndm, names[x]);
                case "float64" -> String.format("%s%s := dataframe.NewSeriesFloat64(\"%s\", nil)", names[x], rndm, names[x]);
                case "string", "bool" -> String.format("%s%s := dataframe.NewSeriesString(\"%s\", nil)", names[x], rndm, names[x]);
                default -> throw new IllegalStateException("Unexpected value: " + types[x]);
            };

            finalString = finalString.concat("\n" + addition);

        }

        // create dataset
        List<String> namesWithRndm = new ArrayList<String>();


        for (String name : names) {
            namesWithRndm.add(name+rndm);
        }


        String columns = String.join(",", namesWithRndm);
        String dataDecl = String.format("df%s := dataframe.NewDataFrame(%s)", rndm, columns);


        // populate dataset
        String appending = "";
        for(String s : names){
            appending = appending.concat(String.format("\n %s%s.Append(element%s.%s)",s, rndm,rndm, s ));
        }


        String populate = String.format("""
                for _, element%s := range %s {
                    %s
                
                }""", rndm, dataset, appending);

        // convert to instances
        String conversion = String.format( "instances%s := base.ConvertDataFrameToInstances(df%s, %d)", rndm, rndm, toPredict);


        // create classifier
        String classifier = String.format("cls%s := knn.NewKnnClassifier(\"euclidean\", \"linear\", 2);", rndm);

        // train
        String training = String.format("cls%s.Fit(instances%s)\n %s := cls%s", rndm, rndm, var, rndm);

        String firm = "\n// generated from visitTrainModel start--";
        String closeFirm = "// ----------------------------------\n";


        String returnString = String.join("\n",firm, finalString,  populate, dataDecl, conversion, classifier, training, closeFirm);

        rewriter.replace(ctx.start, ctx.stop, returnString  );
        return super.visitTrainModel(ctx);
    }


    @Override
    public String visitVarDecl(GoParser.VarDeclContext ctx) {
        String text  = "";
        text = ctx.getText();
        if (text.contains("Dataset[")) {
            String regex = ".*\\[([a-zA-Z_][a-zA-Z0-9_]*)]";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(text);
            String type = "";
            if (matcher.find()) {
                type = matcher.group(1);
            }

            text = text.replace("Dataset[" + type + "]", " []" + type);
            text = text.replace("var", "var ");
            rewriter.replace(ctx.start, ctx.stop, text);


        }
        return super.visitVarDecl(ctx);
    }

    @Override
    public String visitTestModel(GoParser.TestModelContext ctx) {

        String rndm = Integer.toString(Math.abs(random.nextInt()));
        String var = ctx.IDENTIFIER(0).getText();

        String dataset = ctx.IDENTIFIER(1).getText();

        Map<String, DatasetRecord> datasetsMap = (Map<String, DatasetRecord>) symbolTable.getRecord("datasets").getValue();

        String datasetType = symbolTable.getRecord(dataset, scopes).getType();

        datasetType = datasetType.substring(datasetType.indexOf("[")+1, datasetType.indexOf("]"));


        DatasetRecord datasetInfo = datasetsMap.get(datasetType);
        String[] types = datasetInfo.getHeaderTypes();
        String[] names = datasetInfo.getHeader();

        int toPredict = types.length - 1;

        // create columns
        String addition = "";
        String finalString = "";

        for (int x = 0; x < types.length; x++) {
            addition = switch (types[x]){
                case "int" -> String.format("%s%s := dataframe.NewSeriesInt64(\"%s\", nil)", names[x], rndm, names[x]);
                case "float64" -> String.format("%s%s := dataframe.NewSeriesFloat64(\"%s\", nil)", names[x], rndm, names[x]);
                case "string", "bool" -> String.format("%s%s := dataframe.NewSeriesString(\"%s\", nil)", names[x], rndm, names[x]);
                default -> throw new IllegalStateException("Unexpected value: " + types[x]);
            };

            finalString = finalString.concat("\n" + addition);

        }

        // create dataset
        List<String> namesWithRndm = new ArrayList<String>();


        for (String name : names) {
            namesWithRndm.add(name+rndm);
        }


        String columns = String.join(",", namesWithRndm);
        String dataDecl = String.format("df%s := dataframe.NewDataFrame(%s)", rndm, columns);


        // populate dataset
        String appending = "";
        for(String s : names){
            appending = appending.concat(String.format("\n %s%s.Append(element%s.%s)",s, rndm,rndm, s ));
        }


        String populate = String.format("""
                for _, element%s := range %s {
                    %s
                
                }""", rndm, dataset, appending);

        // convert to instances
        String conversion = String.format( "instances%s := base.ConvertDataFrameToInstances(df%s, %d)", rndm, rndm, toPredict);


        String infer = String.format("%s.Predict(instances%s)", var, rndm);


        String firm = "\n// generated from visitTestModel start--";
        String closeFirm = "// ----------------------------------\n";


        String returnString = String.join("\n",firm, finalString,  populate, dataDecl, conversion, infer, closeFirm);

        rewriter.replace(ctx.start, ctx.stop, returnString);


        return super.visitTestModel(ctx);
    }
}
