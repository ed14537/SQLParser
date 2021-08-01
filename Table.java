import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

class Table {

    private String name;
    private ArrayList<ArrayList<String>> data = new ArrayList<>();
    private ArrayList<String> columns;
    private int autoIncrement = 1;
    private DB withinDatabase;
    private BufferedWriter out;

    Table(String name, ArrayList<String> columns, BufferedWriter out) {
        this.name = name;
        this.columns = columns;
        data.add(columns);
        this.out = out;
    }

    String getName() {
        return name;
    }
    ArrayList<String> getColumns() {
        return columns;
    }
    DB getWithinDatabase() {
        return withinDatabase;
    }
    void setWithinDatabase(DB db) {
        withinDatabase = db;
    }

    void addCol(String name) {
        columns.add(name);
    }

    void dropCol(String name) {
        int i = columns.indexOf(name);
        columns.remove(name);

        for (ArrayList<String> entry : data) {
            if(i < entry.size()) {
                entry.remove(i);
            }
        }
    }

    ArrayList<ArrayList<String>> getData() {
        return data;
    }


    void addValues(ArrayList<String> values) {
        if(!columns.get(0).equals("id")){
            columns.add(0, "id");
        }
        if(values.size() != columns.size()-1) {
            System.out.println("Error: mismatch between requested value input and row numbers");
            return;
        }
        values.add(0, String.valueOf(autoIncrement));

        data.add(values);
        autoIncrement++;
    }

    void addValuesFromFile(ArrayList<String> values) {
        data.add(values);
    }

    //add values passed as a string in format (value, value, value);


    void printAll() {
        for(ArrayList<String> a : data) {
            StringBuilder printRows = new StringBuilder();
            for(String s : a) {
                printRows.append(s).append("   ");
            }
            try {
                out.write(printRows.toString() + "\n");
            } catch(IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }



    void printSelected(ArrayList<String> attributes) {
        ArrayList<Integer> cols = new ArrayList<>();
        ArrayList<String> toReturn = new ArrayList<>();

        for(String s : attributes) {
            cols.add(attributes.indexOf(s));
        }

        for(ArrayList<String> a : data) {
            for(Integer c : cols) {
                toReturn.add(a.get(c));
            }
            try{
                out.write(toReturn.toString() + "\n");
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            toReturn.clear();
        }
    }


    void printAllWithConditions(ArrayList<ArrayList<String>> values) {
        try {
            out.write(data.get(0) + "\n");
            for (ArrayList<String> a : values) {
                if(!a.get(0).equals("id")) {
                    out.write(a.toString() + "\n");
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }



    void printWithConditions(ArrayList<ArrayList<String>> values, ArrayList<Integer> indices) {

        for(ArrayList<String> a : values) {
            StringBuilder dataEntry = new StringBuilder();
            for(Integer i : indices) {
                if(!a.get(i).equals(columns.get(i))) {
                    dataEntry.append(a.get(i)).append("   ");
                }
            }
            try{
                for(Integer i : indices) {
                    out.write(columns.get(i) +  "\n");
                }
                out.write(dataEntry.toString() + "\n");
            }catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }


}
