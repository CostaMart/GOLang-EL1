package util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class SymbolTable {
    static final Logger logger = LogManager.getLogger("symboltable");
    // hash table -> records in the table are indexed by their lex
    private final Map<String, Record> table = new HashMap<String, Record>();


    // put a new value in the table with the given scope, in doing it checks if the symbol is already present in a higer
    // table in its gerarchy
    public void put(String lex, Stack<String> scopes){
        Stack<String> toRevert = (Stack<String>) scopes.clone();
        Stack<String> working = new Stack<String>();

        // revert for working top down
        while(!(toRevert.isEmpty())){
                working.add(toRevert.pop());
        }

        if(table.get(lex) != null) throw new RuntimeException("Duplicate lexical symbol: " + lex);

        Map<String, Record> t = table;

        while(!working.isEmpty()){
            String popped = working.pop();
            logger.debug(popped);
            Record record= t.get(popped);
            if (record == null) break;
            t = record.table;
            if (working.empty())  break;

        }

            t.put(lex, new Record(1));
    }

    public void assing(String lex, Stack<String> scopes, Object value){
        Record r = getRecord(lex, scopes);
        r.setValue(value);
    }

    // returns the most deep scoped reference to the given token, relatively to the given scope
    public Record getRecord(String lex, Stack<String> scopes){
        Stack<String> toRevert = (Stack<String>) scopes.clone();
        Stack<String> working = new Stack<String>();

        // revert for working top down
        while(!(toRevert.isEmpty())){
            working.add(toRevert.pop());
        }

        Record toReturn = table.get(lex);
        Map<String, Record> t = table;
        if(t.get(lex) != null) toReturn = t.get(lex);

        do{
            String s = working.pop();
            t = t.get(s).table;
            if(t.get(lex) != null) toReturn = t.get(lex);

        } while(!(working.empty()));

        return toReturn;


    }

    public void printSymbolTable() {
        System.out.println("Symbol Table:");
        System.out.println("--------------------------------------------------------------");
        System.out.printf("%-20s %-10s %-10s %-10s %-10s %-10s%n", "Lexeme", "Token", "B.Line", "E.Line", "B.Col", "E.Col");
        System.out.println("--------------------------------------------------------------");
        for (Map.Entry<String, Record> entry : table.entrySet()) {
            String lexeme = entry.getKey();
            Record record = entry.getValue();

            System.out.printf("%-20s %-10d %-10d %-10d %-10d %-10d%n",
                    lexeme,
                    record.getToken(),
                    record.getBeginLine(),
                    record.getEndLine(),
                    record.getBeginColumn(),
                    record.getEndColumn()
            );

            // Stampa le informazioni delle tabelle nidificate (se presenti)
            if (!record.table.isEmpty()) {
                System.out.println("  Nested Table for: " + lexeme);
                System.out.println("  ------------------------------------------------------------");
                record.printTable(); // Usa il metodo di stampa interna
                System.out.println("  ------------------------------------------------------------");
            }
        }
        System.out.println("--------------------------------------------------------------");
    }





    public class Record {


        Map<String, Record> table = new HashMap<String, Record>();
        Object value;
    	String type;
		int token; // token
        int beginLine; // punto di inizio del lessema nel codice (rispetto alla linea)
        int endLine; // punto di fine lessema nel codice (rispetto alla linea)
        int beginColumn; // punto di inizio lessema nel codice (rispetto alla colonna)
        int endColumn; // punto di fine lessema nel codice (rispetto alla colonna)

        public Record(int token, int beginLine, int endLine, int endColumn, int beginColumn) {
            this.token = token;
            this.beginLine = beginLine;
            this.endLine = endLine;
            this.endColumn = endColumn;
            this.beginColumn = beginColumn;
        }

        public Record(int token) {
            this.token = token;
        }

        public int getToken() {
            return token;
        }

        public int getBeginLine() {
            return beginLine;
        }

        public int getEndLine() {
            return endLine;
        }

        public int getEndColumn() {
            return endColumn;
        }

        public int getBeginColumn() {
            return beginColumn;
        }

        public Object getValue() {
			return value;
		}


		public void setValue(Object value) {
			this.value = value;
		}
		
		public String getType() {
			return type;
		}
		
		public void setType(String type) {
			this.type = type;
		}

        public void printTable() {
            for (Map.Entry<String, Record> entry : table.entrySet()) {


                // Stampa l'intestazione della tabella per ogni entry
                System.out.println("\n\n");
                System.out.printf("Tabella per lessema: %s%n", entry.getKey());
                System.out.printf("%-10s %-10s %-10s %-10s %-10s%n", "token", "ILinea", "FLinea", "IColonna", "type");
                System.out.println("--------------------------------------------------------------");

                // Stampa i record associati a quella chiave







            }
        }
        
    }

}
