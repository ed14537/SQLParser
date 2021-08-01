import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

class ParseConditionals {
    private ArrayList<DB> Databases = new ArrayList<>();
    private DB currentDatabase;
    private Table currentTable;
    private Table secondTable;
    private ArrayList<String> attributes;
    private boolean allFields;
    private ArrayList<ArrayList<String>> values = new ArrayList<>();
    private ArrayList<ArrayList<ArrayList<String>>> subValues = new ArrayList<>();
    private int currToken;
    private String keyWord;
    private BufferedWriter out;
    private String newValue;
    private String toAlter;
    private boolean isAnd;
    private boolean haveRead = false;
    private boolean programRunning = false;
    private boolean hasReachedInner = false;

    ParseConditionals(Table t, boolean allFields, String keyWord, BufferedWriter out, String toAlter) {
        currentTable = t;
        this.allFields = allFields;
        this.keyWord = keyWord;
        this.out = out;
        this.toAlter = toAlter;
    }


    private  ArrayList<ArrayList<String>> checkAndOperation(int index) {


        ArrayList<ArrayList<String>> operatedValues = new ArrayList<>();
        for(ArrayList<String> entry : subValues.get(index)) {
            if(subValues.get(index+1).contains(entry)) {
                operatedValues.add(entry);
            }
        }

        return operatedValues;
    }

    private ArrayList<ArrayList<String>> checkOrOperation(int index) {

        ArrayList<ArrayList<String>> operatedValues = new ArrayList<>(subValues.get(index));
        try {
            out.write(index);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        for(ArrayList<String> entry : subValues.get(index+1)) {
            if(!operatedValues.contains(entry)) {
                operatedValues.add(entry);
            }
        }

        return operatedValues;
    }

    private void handleSingleComparison(String[] tokens) {
        handleCondition(tokens);
        checkKeyWord();
        values.clear();
        subValues.clear();
    }

    private void stackComparison(String[] tokens) {
        //Create stack
        Stack<ArrayList<ArrayList<String>>> evaluationStack = new Stack<>();
        Stack<ArrayList<ArrayList<String>>> stack2 = new Stack<>();

        //if 2 parentheses add ( to stack
        //if OR / AND add to Stack
        //add evaluated expression to stack
        //while stack.pop.get(0) != (, add to second stack. if(stack == empty), a
        //if
    }

    /*private void handleComparisonStack(String[] tokens) {
        Stack<ArrayList<String>> toEvaluateOps = new Stack<>();
        Stack<ArrayList<String>> toEvaluateVals = new Stack<>();




    }*/

    /*private String[] createSubExpression(String[] tokens) {
        String[] subExp;
        tokens[tokens.length-1] = removeSemiColon(tokens[tokens.length-1]);

        if(countNumBrackets(tokens[currToken], '(') == 2)

        return subExp;
    }

    private void returnOperands() {

    }*/

    private void handleComparison(String[] tokens) {
        //currToken = first word after where
        //how do we determine if there's only one expression?

        try {
            if(!containsAndOr(tokens)) {
                handleCondition(tokens);
                if(subValues.size()>1) {
                    if(isAnd) {
                        values = checkAndOperation(0);
                        out.write("here");
                    } else {
                        values = checkOrOperation(0);
                        out.write("there");
                    }
                    subValues.add(0, values);
                    out.write(String.valueOf(subValues.get(0)));
                }
                return;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        try {
            if(containsAndOr(tokens)) {
                //currToken is now at outer and
                selectBracketMethod(tokens);
                //Step Left
                int i = currToken;
                String[] toEvaluate = parseSubExpression(tokens, -1, ')', '(');
                currToken = 0;
                out.write(Arrays.toString(toEvaluate));
                handleComparison(toEvaluate);
                //Step Right
                currToken = i+1;
                toEvaluate = parseSubExpression(tokens, 1, '(', ')');
                out.write(Arrays.toString(toEvaluate));
                currToken = 0;
                handleComparison(toEvaluate);
            }
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }

    }
    private boolean lastTwoChars(String s) {
        return (s.charAt(s.length()-1) == ';' && s.charAt(s.length()-2) == ')');
    }

    private String[] parseSubExpression(String[] tokens, int mod, char bracket, char otherBracket) {
        int i = currToken;
        String[] expression;
        tokens[tokens.length-1] = removeSemiColon(tokens[tokens.length-1]);
        if(countNumBrackets(tokens[i+mod], bracket) == 2) {
            while((tokens[i].charAt(0) != otherBracket && tokens[i].charAt(1) != otherBracket) &&
                    (tokens[i].charAt(tokens[i].length()-1) != otherBracket &&
                            tokens[i].charAt(tokens[i].length()-2) != otherBracket) ) {
                i += mod;
            }
        } else if(countNumBrackets(tokens[i+mod], bracket) == 1) {
            while(tokens[i].charAt(0) != otherBracket && tokens[i].charAt(tokens[i].length()-1) != otherBracket) {
                i += mod;
            }
        }

        if(currToken > i) {
            expression = Arrays.copyOfRange(tokens, i, currToken);
        } else {
            expression = Arrays.copyOfRange(tokens, currToken, tokens.length);
        }

        String[] toReturn = new String[expression.length];
        int j = 0;
        for(String s : expression) {
            String replaced = s.replaceAll("[()]", "");
            toReturn[j] = replaced;
            j++;
        }

        currToken = i;
        return toReturn;
    }

    private void selectBracketMethod(String[] tokens) {
        for(String s : tokens) {
            if(s.charAt(0) == '(' && s.charAt(1) == '(') {
                findOutermostComparison(tokens);
            } else if(s.charAt(0) == '('){
                findSingleAndOr(tokens);
            }
        }
    }

    private void findOutermostComparison(String[] tokens) {

        while(currToken != tokens.length) {
            if(tokens[currToken].equals("AND") || tokens[currToken].equals("OR")) {
                isAnd(tokens[currToken]);
                if((currToken != 0 && countNumBrackets(tokens[currToken-1], ')') == 2) ||
                        (currToken+1 != tokens.length && countNumBrackets(tokens[currToken+1], '(') == 2)) {
                    return;
                }
            }
            currToken++;
        }
    }


    private void checkKeyWord() {

        switch (keyWord) {
            case "DELETE":
                for (ArrayList<String> entry : subValues.get(0)) {
                    currentTable.getData().remove(entry);
                }
                break;
            case "SELECT":
                checkAllFields(subValues.get(0));
                break;
            case "UPDATE":
                if (subValues.get(0).get(0).size()-1 < getColIndex(toAlter, currentTable)) {
                    subValues.get(0).get(0).add(newValue);
                } else {
                    currentTable.getData().get(currentTable.getData().indexOf(subValues.get(0).get(0)))
                            .set(getColIndex(toAlter, currentTable), newValue);
                }
                try{
                    out.write("VALUE UPDATED TO " + newValue);
                }catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                break;
        }
        values.clear();
    }

    private void handleCondition(String[] tokens) {
        String toCheck = null;
        for(String s:currentTable.getColumns()) {
            if (tokens[currToken].equals(s)) {
                toCheck = tokens[currToken];
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
                break;
            default:
                try {
                    out.write("Do not recognise operator");
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
        }
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
        subValues.add(toAdd);
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

    private void checkGreaterLessThan(String[] tokens, String toCheck, boolean andEquals, boolean isGreater) {

        currToken++;
        int ci = getColIndex(toCheck, currentTable);
        ArrayList<ArrayList<String>> toAdd = new ArrayList<>();
        tokens[currToken] = removeSemiColon(tokens[currToken]);

        if(isNumeric(tokens[currToken])) {
            for(ArrayList<String> entry : currentTable.getData()) {
                if(tokens[currToken].charAt(tokens[currToken].length()-1) == ';') {
                    tokens[currToken] = tokens[currToken].substring(0, tokens[currToken].length()-1);
                }
                if(entry.get(ci).equals(toCheck)) {
                    continue;
                }
                if(!isNumeric(entry.get(ci))) {
                    return;
                }
                if(isGreater) {
                    if (Integer.parseInt(entry.get(ci)) > Integer.parseInt(tokens[currToken])) {
                        toAdd.add(entry);
                    }
                } else {
                    if (Integer.parseInt(entry.get(ci)) < Integer.parseInt(tokens[currToken])) {
                        toAdd.add(entry);
                    }
                }
                if(andEquals) {
                    if(Integer.parseInt(entry.get(ci)) == Integer.parseInt(tokens[currToken])) {
                        toAdd.add(entry);
                    }
                }
            }
            subValues.add(toAdd);
        } else {
            try {
                out.write("ERROR: attempting > comparison on non-numeric field");
            } catch(IOException ioe) {
                ioe.printStackTrace();
            }
        }
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

    private void findSingleAndOr(String[] tokens) {
        for(int i = 0; i < tokens.length; i++) {
            if(tokens[i].equals("AND") || tokens[i].equals("OR")) {
                isAnd(tokens[i]);
                currToken = i;
                hasReachedInner = true;
            }
        }
    }

    private void isAnd(String s) {
        isAnd = s.equals("AND");
    }

    private boolean containsAndOr(String[] tokens) {
        for(String s : tokens) {
            if(s.equals("AND") || s.equals("OR")) {
                return true;
            }
        }
        return false;
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
}

