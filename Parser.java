import java.io.*;
import java.util.*;

class Parser {

    private ArrayList<DB> Databases = new ArrayList<>();
    private DB currentDatabase;
    private Table currentTable;
    private Table secondTable;
    private ArrayList<String> attributes = new ArrayList<>();
    private boolean allFields;
    private ArrayList<ArrayList<String>> values = new ArrayList<>();
    private int currToken;
    private String keyWord;
    private BufferedWriter out;
    private String newValue;
    private String toAlter;
    private Serializer fileClass = new Serializer();
    private boolean haveRead = false;
    private boolean programRunning = false;
    private Stack<ArrayList<ArrayList<String>>> evaluationStack = new Stack<>();
    private Stack<ArrayList<ArrayList<String>>> stack2 = new Stack<>();

    void parseCommands(String command, BufferedWriter out) {
        fileClass.setOut(out);
        this.out = out;
        if(command.isEmpty()) {
            return;
        }
        if(fileClass.getToUse().length()!=0 && !haveRead && !programRunning) {
            readFromFile();
            haveRead = true;
        }
        command = command.trim();
        String[] tokens = command.split(" ");
        assert tokens.length != 0;
        
        if(tokens[tokens.length - 1].charAt(tokens[tokens.length-1].length()-1) != ';') {
            try {
                out.write("ERROR Missing ;\n");
                return;
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        if(tokens[0].equals("USE")) {
            use(tokens);
        }
        if(tokens[0].equals("CREATE")) {
            if(tokens[1].equals("DATABASE")) {
                createDatabase(tokens);
            }
            if(tokens[1].equals("TABLE")) {
                createTable(tokens);
            }
        }
        if(tokens[0].equals("DROP")) {
            drop(tokens);
        }
        if(tokens[0].equals("ALTER")) {
            if(tokens[1].equals("TABLE")) {
                alterTable(tokens);
            }
        }
        if(tokens[0].equals("INSERT")) {
            if(tokens[1].equals("INTO")) {
                insert(tokens);
            }
        }
        if(tokens[0].equals("SELECT")) {
            keyWord = "SELECT";
            select(tokens);
        }
        if(tokens[0].equals("DELETE")) {
            keyWord = "DELETE";
            delete(tokens);
        }
        if(tokens[0].equals("JOIN")) {
            join(tokens);
        }
        if(tokens[0].equals("UPDATE")) {
            update(tokens);
        }
        writeToFile();
        attributes.clear();
        programRunning = true;
    }

    private void writeToFile() {
        fileClass.emptyFile();
        for(DB db : Databases) {
            fileClass.writeDatabase(db);
        }
    }

    private void readFromFile() {
        Databases.addAll(fileClass.readFromFile());
    }

    private void use(String[] tokens) {
        String name = tokens[1].substring(0, tokens[1].length()-1);

        for(DB db : Databases) {
            if(name.equals(db.getName())) {
                currentDatabase = db;
            }
        }
        //if current database is null or name isn't equivalent to query, it doesn't exist
        if(currentDatabase == null || !currentDatabase.getName().equals(name)) {
            try {
                out.write("ERROR: No database of that name." + "\n");
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } else {
            try {
                out.write("OK. Using " + currentDatabase.getName() + "\n");
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private void createDatabase(String[] tokens) {

        String newDB = tokens[2].substring(0, tokens[2].length()-1);
        DB newDatabase = new DB(newDB);
        Databases.add(newDatabase);
        try {
            out.write("Server response: OK. Created new database " + newDB + "\n");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void createTable(String[] tokens) {
        String name;
        if(tokens.length == 3) {
            name = tokens[2].substring(0, tokens[2].length()-1);
        } else {
            name = tokens[2];
        }

        ArrayList<String> columns = new ArrayList<>();
        
        if(name.charAt(name.length()-1)==';') {
            currentDatabase.getTables().add(new Table(name, columns, out));
        }
        currToken = 3;
        columns = createListFromCommandVals(tokens);
        Table t = new Table(name, columns, out);
        currentDatabase.getTables().add(t);
        t.setWithinDatabase(currentDatabase);

        try {
            out.write("Server response: OK. Created new table " + name + "\n");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    //Appends sequences of tokens contained withing quotes
    private String checkComposite(String[] tokens) {
        StringBuilder compValue = new StringBuilder();
        int i = currToken;
        if(tokens[i].charAt(tokens[i].length()-2) == '\'') {
            return tokens[i].substring(0, tokens[i].length()-1);
        }

        while(i < tokens.length) {
            if(lastTwoChars(tokens[i])) {
                compValue.append(tokens[i], 0, tokens[i].length()-2);
                currToken = i;
                return compValue.toString();
            }
            if(tokens[i].charAt(tokens[i].length()-1) == ',') {
                compValue.append(tokens[i], 0, tokens[i].length()-1);
                currToken = i;
                return compValue.toString();
            }
            compValue.append(tokens[i]).append(' ');
            i++;
        }

        return compValue.toString();
    }
    //need a helper function that checks for quotes, also needs to check when there is a , / ) / ;
    private ArrayList<String> createListFromCommandVals(String[] tokens) {

        String value;
        ArrayList<String> valueList = new ArrayList<>();

        while(currToken < tokens.length) {

            if(tokens[currToken].charAt(0) == '(') {
                if(tokens[currToken].charAt(1) == '\'' &&
                        tokens[currToken].charAt(tokens[currToken].length()-2) != '\'') {
                    value = checkComposite(tokens);
                    value = value.substring(1);
                } else {
                    value = tokens[currToken].substring(1);
                }

            } else {
                if(tokens[currToken].charAt(0) == '\'') {
                    value = checkComposite(tokens);
                } else {
                    value = tokens[currToken];
                }
            }
            if(value.charAt(value.length()-1) == ',') {
                value = value.substring(0, value.length()-1);
                //-1 to remove the comma
            } else if (value.charAt(value.length()-1) == ';') {
                //-2 to remove the closing parenthesis and closing ;
                value = value.substring(0, value.length()-2);
            }
            valueList.add(value);
            currToken++;
        }
        return valueList;
    }

    private void drop(String[] tokens) {

        String type = tokens[1];
        String name = tokens[2];
        name = name.substring(0, name.length()-1);
        if(type.equals("DATABASE")) {
            dropDatabase(name);
        } else if(type.equals("TABLE")) {
            dropTable(name);
        }
    }

    private void dropTable(String name) {
        if(currentDatabase != null) {
            int i = currentDatabase.getTables().size();
            currentDatabase.deleteTable(name);
            if (currentDatabase.getTables().size() == i) {
                try {
                    out.write("ERROR: No table of that name" + "\n");
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            } else {
                try {
                    out.write("Table deleted" + "\n");
                }catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        } else {
            try{
                out.write("No Database selected" + "\n");
            }catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private void dropDatabase(String name) {
        int i = Databases.size();
        for(DB db : Databases) {
            if(db.getName().equals(name)) {
                for(Table t : db.getTables()) {
                    t.setWithinDatabase(null);
                }
                currentDatabase = null;
                fileClass.drop(db);
                Databases.remove(db);
                try {
                    out.write("Database deleted\n");
                } catch(IOException ioe) {
                    ioe.printStackTrace();
                }
                break;
            }
        }
        if(Databases.size()==i) {
            try {
                out.write("ERROR: No database with that name" +"\n");
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private void alterTable(String[] tokens) {

        String name = tokens[2];
        currentTable = checkExists(name);
        if(!currentTable.getName().equals(name)) {
            return;
        }
        String colName = tokens[4].substring(0, tokens[4].length()-1);

        if(tokens[3].equals("ADD")) {
            currentTable.addCol(colName);
        } else if(tokens[3].equals("DROP")) {
            currentTable.dropCol(colName);
        } else {
            try {
                out.write("ERROR: invalid query." + "\n");
            }catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        try {
            out.write("OK\n");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

    private void insert(String[] tokens) {

        String tableName = tokens[2];
        currentTable = checkExists(tableName);
        if(!currentTable.getName().equals(tableName)) {
            System.out.println(currentTable.getName());
            return;
        }
        if(tokens[3].equals("VALUES")) {
            currToken = 4;
            ArrayList<String> values = createListFromCommandVals(tokens);
            try {
                currentTable.addValues(values);
                out.write("Values added: " + values + "\n");
            } catch(NullPointerException | IOException ioe) {
                System.out.println("Failed to add values - was equivalent to null.");
            }
        }

    }

    private void update(String[] tokens) {
        keyWord = "UPDATE";
        currToken = 1;
        currentTable = checkExists(tokens[currToken]);
        if(!currentTable.getName().equals(tokens[currToken])) {
            return;
        }
        currToken++;
        try {
            if(tokens[currToken].equals("SET")) {
                currToken++;
                int x = currentTable.getColumns().indexOf(tokens[currToken]);
                if (x == -1) {
                    out.write("ERROR: no column with this name.\n");
                    return;
                }
                toAlter = currentTable.getColumns().get(x);
                currToken++;
                if (tokens[currToken].equals("=")) {
                    currToken++;
                    newValue = tokens[currToken];
                    currToken++;
                    if (tokens[currToken].equals("WHERE")) {
                        currToken++;
                        if (containsAndOr(tokens)) {
                            handleSingleComparison(tokens);
                        } else {
                            buildStack(tokens);
                            checkKeyWord();
                        }
                    }
                } else {
                    out.write("ERROR missing =\n");
                }
            } else {
                out.write("ERROR missing SET\n");
            }
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }

    }

    private void delete(String[] tokens){
        currToken = 1;
        keyWord = "DELETE";
        try {
            if (tokens[currToken].equals("FROM")) {
                currToken++;
                currentTable = checkExists(tokens[currToken]);
                if(!currentTable.getName().equals(tokens[currToken])) {
                    return;
                }
                currToken++;
                if (tokens[currToken].equals("WHERE")) {
                    currToken++;
                    if (containsAndOr(tokens)) {
                        handleSingleComparison(tokens);
                    } else {
                        buildStack(tokens);
                        checkKeyWord();
                    }
                } else {
                    out.write("ERROR: Missing WHERE\n");
                }
            } else {
                out.write("ERROR: Missing FROM\n");
            }
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void join(String[] tokens) {
        currToken = 1;
        currentTable = checkExists(tokens[currToken]);
        if(currentTable == null || !currentTable.getName().equals(tokens[currToken])) {
                return;
        }
        currToken++;
        String attribute1;
        String attribute2;
        try {
            if (tokens[currToken].equals("AND")) {
                currToken++;
                secondTable = checkExists(tokens[currToken]);
                if(!secondTable.getName().equals(tokens[currToken])) {
                    return;
                }
                currToken++;
                if (tokens[currToken].equals("ON")) {
                    currToken++;
                    attribute1 = tokens[currToken];
                    currToken++;
                    if (tokens[currToken].equals("AND")) {
                        currToken++;
                        attribute2 = tokens[currToken].substring(0, tokens[currToken].length() - 1);
                        joinTables(attribute1, attribute2);
                    } else {
                        out.write("ERROR: Missing AND\n");
                    }
                } else {
                    out.write("ERROR: Missing ON\n");
                }
            } else {
                out.write("ERROR: Missing AND\n");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

    private void joinTables(String attribute1, String attribute2) {
        Table temp;
        ArrayList<String> tempColumns = new ArrayList<>();
        for(String s : currentTable.getColumns()) {
            if(!s.equals("id")) {
                tempColumns.add(currentTable.getName() + '.' + s);
            }
        }
        for(String s : secondTable.getColumns()) {
            if(!s.equals("id")) {
                tempColumns.add(secondTable.getName() + '.' + s);
            }
        }

        temp = new Table("temp", tempColumns, out);
        int index1 = getColIndex(attribute1, currentTable);
        int index2 = getColIndex(attribute2, secondTable);

        for(ArrayList<String> entry : currentTable.getData()) {
            for(ArrayList<String> entry2 : secondTable.getData()) {
                if(entry.get(index1).equals(entry2.get(index2))) {
                    temp.addValues(addValuesNotId(entry, entry2));
                }
            }
        }

        temp.printAll();
    }

    private void select(String[] tokens) {

        currToken = 1;
        boolean isAll = false;

        while(!tokens[currToken].equals("FROM")) {
            if(tokens[currToken].charAt(tokens[currToken].length()-1) == ',') {
                attributes.add(tokens[currToken].substring(0, tokens[currToken].length()-1));
            } else {
                attributes.add(tokens[currToken]);
            }
            currToken++;
            if(currToken == tokens.length) {
                try {
                    out.write("error: no FROM in SELECT statement\n");
                } catch(IOException ioe){
                    ioe.printStackTrace();
                }
                attributes.clear();
                return;
            }
        }

        if(attributes.get(0).equals("*")) {
            isAll = true;
            allFields = true;
        } else {
            allFields = false;
        }

        currToken++;

        where(tokens, isAll);
    }

    private void where(String[] tokens, boolean isAll) {
        boolean wherePresent = false;

        for(String s : tokens) {
            if (s.equals("WHERE")) {
                wherePresent = true;
                break;
            }
        }

        if(wherePresent) {
            currentTable = checkExists(tokens[currToken]);
            if(currentTable == null || !currentTable.getName().equals(tokens[currToken])) {
                return;
            }
            currToken+=2;
            if(checkAttributesExist()) return;
        } else {
            currentTable = checkExists(tokens[currToken].substring(0, tokens[currToken].length()-1));
            if(currentTable == null || !currentTable.getName()
                    .equals(tokens[currToken].substring(0, tokens[currToken].length()-1))) {
                return;
            }
            if(checkAttributesExist()) return;
            if(isAll) {
                currentTable.printAll();
                return;
            } else {
                currentTable.printSelected(attributes);
                return;
            }
        }
        if(containsAndOr(tokens)) {
            handleSingleComparison(tokens);
        } else {
            buildStack(tokens);
            checkKeyWord();
        }

    }

    private boolean checkAttributesExist() {
        for(String s : attributes) {
            if(!currentTable.getColumns().contains(s) && !s.equals("*")) {
                try {
                    attributes.clear();
                    out.write("ERROR: Attribute doesn't exist" + "\n");
                    return true;
                }catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
        return false;
    }

    private void handleSingleComparison(String[] tokens) {
        handleCondition(tokens);
        if(!evaluationStack.isEmpty()) {
            values = evaluationStack.pop();
            checkKeyWord();
            values.clear();
        } else {
            try {
                out.write("ERROR: Invalid query");
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private void addBrackets(String s, char c) {
        String b = "";
        b += c;
        if(countNumBrackets(s, c) == 2) {
            ArrayList<String> bb = new ArrayList<>(Collections.singleton(b));
            ArrayList<ArrayList<String>> bracket = new ArrayList<>(Collections.singleton(bb));
            evaluationStack.push(bracket);
        }
    }

    private void addAndOr(String s) {
        ArrayList<String> ss = new ArrayList<>(Collections.singleton(s));
        ArrayList<ArrayList<String>> sss =  new ArrayList<>(Collections.singleton(ss));
        evaluationStack.push(sss);
    }

    private  ArrayList<ArrayList<String>> checkAndOperation(Stack<ArrayList<ArrayList<String>>> stack) {


        ArrayList<ArrayList<String>> operatedValues = new ArrayList<>();
        ArrayList<ArrayList<String>> operatedValues1 = new ArrayList<>(stack.pop());
        stack.pop();
        ArrayList<ArrayList<String>> operatedValues2 = new ArrayList<>(stack.pop());

        for(ArrayList<String> entry : operatedValues1) {
            if(operatedValues2.contains(entry) && !entry.get(0).equals("id")) {
                operatedValues.add(entry);
            }
        }

        return operatedValues;
    }

    private ArrayList<ArrayList<String>> checkOrOperation(Stack<ArrayList<ArrayList<String>>> stack) {

        ArrayList<ArrayList<String>> operatedValues1 = new ArrayList<>(stack.pop());
        stack.pop();
        ArrayList<ArrayList<String>> operatedValues2 = new ArrayList<>(stack.pop());

        for(ArrayList<String> entry : operatedValues2) {
            if(!operatedValues1.contains(entry)) {
                if(!entry.get(0).equals("id")) {
                    operatedValues1.add(entry);
                }
            }
        }

        return operatedValues1;
    }

    private void buildStack(String[] tokens) {
        //Create stack
        if(currToken == tokens.length) {
            evaluateStack();
            return;
        }

        addBrackets(tokens[currToken], '(');

        if(tokens[currToken].equals("AND") || tokens[currToken].equals("OR")) {
            addAndOr(tokens[currToken]);
            currToken++;
        } else {
            handleCondition(tokens);
        }
        buildStack(tokens);
    }

    private void evaluateStack() {

        ArrayList<String> closingBracket = new ArrayList<>(Collections.singleton(")"));
        Stack<ArrayList<ArrayList<String>>> finalEval = new Stack<>();
        ArrayList<String> and = new ArrayList<>(Collections.singleton("AND"));
        ArrayList<ArrayList<String>> AND = new ArrayList<>(Collections.singleton(and));
        
        while(!evaluationStack.isEmpty()) {
            ArrayList<ArrayList<String>> value = evaluationStack.pop();
            if(value.contains(closingBracket)) {
                handleBracketStack(finalEval);
            } else {
                finalEval.push(value);
            }
        }

        if(finalEval.contains(AND)) {
            values = checkAndOperation(finalEval);
        } else {
            values = checkOrOperation(finalEval);
        }

    }

    private void handleBracketStack(Stack<ArrayList<ArrayList<String>>> finalEval) {

        boolean isAnd = false;
        ArrayList<String> or = new ArrayList<>(Collections.singleton("OR"));
        ArrayList<String> openingBracket = new ArrayList<>(Collections.singleton("("));
        ArrayList<String> and = new ArrayList<>(Collections.singleton("AND"));

        ArrayList<ArrayList<String>> value = evaluationStack.pop();
        while(!value.contains(openingBracket)) {
            if(value.contains(and)) {
                isAnd = true;
            } else if(value.contains(or)) {
                isAnd = false;
            }
            stack2.push(value);
            value = evaluationStack.pop();
        }
        if(isAnd) {
            finalEval.push(checkAndOperation(stack2));
        } else {
            finalEval.push(checkOrOperation(stack2));
        }
    }

    
    private void checkKeyWord() {
        //use keyword saved as attribute to determine what to do with sorted values
        switch (keyWord) {
            case "DELETE":
                handleDelete();
                break;
            case "SELECT":
                checkAllFields(values);
                break;
            case "UPDATE":
                handleUpdate();
                break;
        }
        evaluationStack.clear();
        values.clear();
        attributes.clear();
    }

    private void handleDelete() {
        for (ArrayList<String> entry : values) {
            currentTable.getData().remove(entry);
            try {
                out.write("OK" + "\n");
            }catch(IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private void handleUpdate() {
        //if value to change is for a new column, do this
        if (values.get(0).size()-1 < getColIndex(toAlter, currentTable)) {
            values.get(0).add(newValue);
            //otherwise set the value by finding the index of the column to alter
        } else {
            currentTable.getData()
                    .get(currentTable.getData().indexOf(values.get(0)))
                    .set(getColIndex(toAlter, currentTable), newValue);
        }
        try{
            out.write("VALUE UPDATED TO " + newValue + "\n");
        }catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void handleCondition(String[] tokens) {
        String toCheck = null;
        String withoutBrackets = tokens[currToken].replaceAll("[()]", "");
        for(String s:currentTable.getColumns()) {
            if (withoutBrackets.equals(s)) {
                toCheck = withoutBrackets;
                break;
            }
        }
        handleOperator(toCheck, tokens);
    }

    private void handleOperator(String toCheck, String[] tokens) {
        currToken++;
        switch (tokens[currToken]) {
            case "==":
                checkEquals(tokens, toCheck, true);
                break;
            case ">":
                checkGreaterLessThan(tokens, toCheck, false, true);
                break;
            case "<":
                checkGreaterLessThan(tokens, toCheck, false, false);
                break;
            case ">=":
                checkGreaterLessThan(tokens, toCheck, true, true);
                break;
            case "<=":
                checkGreaterLessThan(tokens, toCheck, true, false);
                break;
            case "!=":
                checkEquals(tokens, toCheck, false);
                break;
            case "LIKE":
                checkLike(tokens, toCheck);
                break;
            default:
                try {
                    out.write("Do not recognise operator" + "\n");
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
        }
     }

     private void checkLike(String[] tokens, String toCheck) {
         currToken++;
         String s = removeSemiColon(tokens[currToken]);
         if (isNumeric(s) || isToCheckNumeric(toCheck)) {
             try {
                 out.write("ERROR: Cannot use LIKE on numeric values" + "\n");
                 return;
             } catch (IOException ioe) {
                 ioe.printStackTrace();
             }
         }

         int colIndex = getColIndex(toCheck, currentTable);
         ArrayList<ArrayList<String>> toAdd = new ArrayList<>();
         String appended;

         if (tokens[currToken].charAt(0) == '\'') {
             appended = concatenateToFind(tokens);
         } else {
             appended = tokens[currToken];
         }
         appended = appended.replaceAll("[()]", "")
                 .replaceAll("\'", "");
         for (ArrayList<String> a : currentTable.getData()) {
             if (a.get(colIndex).contains(appended)) {
                 toAdd.add(a);
             }
         }
         evaluationStack.push(toAdd);
         addBrackets(tokens[currToken], ')');
         currToken++;
     }

     private void checkEquals(String[] tokens, String toCheck, boolean isEquals) {

        currToken++;
        int colIndex = getColIndex(toCheck, currentTable);
        String appended;
        ArrayList<ArrayList<String>> toAdd = new ArrayList<>();
        if(tokens[currToken].charAt(0) == '\'') {
            appended = concatenateToFind(tokens);
        } else {
            appended = tokens[currToken];
        }
        if(!checkForMatchQuotes(appended)) {
            try {
                out.write("ERROR: Missing quote" + "\n");
                currToken++;
                return;
            }catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        appended = appended.replaceAll("[()]", "");
        findEquals(isEquals, appended, colIndex, toAdd, tokens);
     }

     private void findEquals(boolean isEquals, String appended, int colIndex,
                             ArrayList<ArrayList<String>> toAdd, String[] tokens) {
         for(ArrayList<String> a : currentTable.getData()) {
             if(isEquals) {
                 if(appended.equals(a.get(colIndex))) {
                     toAdd.add(a);
                 }
             } else {
                 if(!appended.equals(a.get(colIndex))) {
                     toAdd.add(a);
                 }
             }
         }
         evaluationStack.push(toAdd);
         addBrackets(tokens[currToken], ')');
         currToken++;
     }

    private String concatenateToFind(String[] tokens) {

        if(tokens[currToken].charAt(tokens[currToken].length()-1) == ';') {
            return tokens[currToken].substring(0, tokens[currToken].length()-1);
        }
        if(currToken == tokens.length-1) {
            return tokens[currToken];
        }
        StringBuilder s = new StringBuilder();
        while(currToken < tokens.length) {
            if(tokens[currToken].charAt(tokens[currToken].length()-1) == ';'
                    || tokens[currToken+1].equals("AND") || tokens[currToken+1].equals("OR")) {
                s.append(tokens[currToken], 0, tokens[currToken].length()-1);
                return s.toString();
            }
            s.append(tokens[currToken]).append(' ');
            currToken++;
        }

        return s.toString();
    }

    private void checkAllFields(ArrayList<ArrayList<String>> values) {
        ArrayList<Integer> indices;
        if(allFields) {
            //return all entries
            currentTable.printAllWithConditions(values);
        } else {
            indices = returnRelevantColumnIndices();
            currentTable.printWithConditions(values, indices);
        }
    }

    private boolean isToCheckNumeric(String toCheck) {
        int colIndex = getColIndex(toCheck, currentTable);
        return isNumeric(currentTable.getData().get(1).get(colIndex));
    }

    private void checkGreaterLessThan(String[] tokens, String toCheck, boolean andEquals, boolean isGreater) {

        currToken++;
        tokens[currToken] = removeSemiColon(tokens[currToken]);
        String value = tokens[currToken].replaceAll("[()]", "");
        if(isNumeric(value) && isToCheckNumeric(toCheck)) {
            findGreaterLess(andEquals, isGreater, toCheck, tokens, value);
        } else {
            try {
                out.write("ERROR: attempting > comparison on non-numeric field" + "\n");
            } catch(IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private void findGreaterLess(boolean andEquals, boolean isGreater, String toCheck, String[] tokens, String value) {

        int ci = getColIndex(toCheck, currentTable);
        ArrayList<ArrayList<String>> toAdd = new ArrayList<>();

        for(ArrayList<String> entry : currentTable.getData()) {
            if(entry.get(ci).equals(toCheck)) {
                continue;
            }
            if(!isNumeric(entry.get(ci))) {
                return;
            }
            if(isGreater) {
                if (Integer.parseInt(entry.get(ci)) > Integer.parseInt(value)) {
                    toAdd.add(entry);
                }
            } else {
                if (Integer.parseInt(entry.get(ci)) < Integer.parseInt(value)) {
                    toAdd.add(entry);
                }
            }
            if(andEquals) {
                if(Integer.parseInt(entry.get(ci)) == Integer.parseInt(value)) {
                    toAdd.add(entry);
                }
            }
        }
        evaluationStack.push(toAdd);
        addBrackets(tokens[currToken], ')');
        currToken++;
    }

    private int getColIndex(String toCheck, Table t) {
        int colIndex = 0;
        for(String s : t.getColumns()) {
            if(s.equals(toCheck)) {
                colIndex = t.getColumns().indexOf(s);
            }
        }
        return colIndex;
    }

    private boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    private ArrayList<Integer> returnRelevantColumnIndices() {

        ArrayList<Integer> indices = new ArrayList<>();
        for(String s: currentTable.getColumns()) {
            for(String att : attributes) {
                if(att.equals(s)) {
                    indices.add(currentTable.getColumns().indexOf(s));
                }
            }
        }

        return indices;

    }

    private boolean containsAndOr(String[] tokens) {
        for(String s : tokens) {
            if(s.equals("AND") || s.equals("OR")) {
                return false;
            }
        }
        return true;
    }
    private String removeSemiColon(String s) {
        if(s.charAt(s.length()-1) == ';') {
            return s.substring(0, s.length()-1);
        }
        return s;
    }


    private int countNumBrackets(String s, char c) {
        int count = 0;
        for(int i = 0; i < s.length(); i++) {
            if(s.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    private Table assignTable(String name) {
        for(Table t : currentDatabase.getTables()) {
            if(t.getName().equals(name)) {
                return t;
            }
        }
        return null;
    }

    private Table checkExists(String name) {
        Table t = assignTable(name);
        if(t == null) {
            try {
                out.write("ERROR: Table does not exist" + "\n");
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return t;
    }

    private ArrayList<String> addValuesNotId(ArrayList<String> entry1, ArrayList<String> entry2) {
        ArrayList<String> values = new ArrayList<>();
        for(int i = 1; i < entry1.size();i++) {
            values.add(entry1.get(i));
        }
        for(int i = 1; i < entry2.size();i++) {
            values.add(entry2.get(i));
        }

        return values;
    }

    private boolean checkForMatchQuotes(String s) {
        int quoteCount = 0;
        for(int i = 0; i < s.length(); i++) {
            if(s.charAt(i) == '\'') {
                quoteCount++;
            }
        }
        return quoteCount % 2 == 0;
    }

    private boolean lastTwoChars(String s) {
        return (s.charAt(s.length()-1) == ';' && s.charAt(s.length()-2) == ')');
    }
}



