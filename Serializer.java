import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

class Serializer {
    private File toUse;
    private FileWriter writer;
    private BufferedReader reader;
    private DB currentDatabase;
    private Table currentTable;
    private ArrayList<DB> Databases = new ArrayList<>();
    private BufferedWriter out;


    Serializer() {
        toUse = new File("database.txt");
        if(!toUse.exists()) {
            try {
                if(toUse.createNewFile()) {
                    try {
                        writer = new FileWriter(toUse);
                        FileReader r = new FileReader(toUse);
                        reader = new BufferedReader(r);
                    } catch(IOException ioe) {
                        ioe.printStackTrace();
                    }
                } else {
                    System.out.println("Error: unable to create target file");
                }
            } catch(IOException ioe) {
                ioe.printStackTrace();
            }
        } else {
            Databases = readFromFile();
        }
    }

    void drop(DB db) {
        Databases.remove(db);
        currentDatabase = null;
        if(currentTable != null) {
            currentTable.setWithinDatabase(null);
        }
    }

    void setOut(BufferedWriter out) {
        this.out = out;
    }

    File getToUse() {
        return toUse;
    }

    void emptyFile() {
       try{
           PrintWriter writer = new PrintWriter("database.txt");
           writer.print("");
           writer.close();
       }catch(IOException ioe) {
           ioe.printStackTrace();
       }
    }

    void writeDatabase(DB database) {
        try {
            writer = new FileWriter(toUse);
            writer.write("Database:\t" + database.getName() + "\n");
            writer.flush();
            for(Table t: database.getTables()) {
                writeTable(t);
            }
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void writeTable(Table t) {
        try {
            writer.write("Table:\t" + t.getName() + "\n");
            writer.write("Within Database:\t" + t.getWithinDatabase().getName() + "\n");
            writer.flush();
            for(ArrayList<String> a: t.getData()) {
                StringBuilder toWrite = new StringBuilder();
                for(String s : a) {
                    toWrite.append(s).append("\t");
                }
                writer.write(toWrite.toString() + "\t\n");
                writer.flush();
            }
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }

    ArrayList<DB> readFromFile() {
        String line;
        String[] toParse;
        try {

            FileReader r = new FileReader(toUse);
            reader = new BufferedReader(r);
            line = reader.readLine();

            while(line != null) {
                toParse = parseLines(line);
                if(line.contains("Database:")) {
                    addDatabase(toParse[1]);
                } else if(line.contains("Table:")) {
                    handleTable(toParse);
                } else if(line.contains("Within Database:")) {
                    toParse = parseLines(line);
                    String database = toParse[2];
                    setCurrentDatabase(database);
                } else {
                    currentTable.addValuesFromFile(new ArrayList<>(Arrays.asList(toParse)));
                }
                line =reader.readLine();
            }
        }catch(IOException | NullPointerException ioe) {
            ioe.printStackTrace();
        }

        return Databases;
    }

    private void handleTable(String[] toParse) {
        try {
            String line;
            String name = toParse[1];
            line = reader.readLine();
            toParse = parseLines(line);
            String database = toParse[1];
            line = reader.readLine();
            toParse = parseLines(line);
            addTable(name, toParse, database);
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }

    }

    private void setCurrentDatabase(String name) {
        for(DB db : Databases) {
            if(db.getName().equals(name)) {
                currentDatabase = db;
            }
        }
    }

    private void addDatabase(String name) {
        DB newDb = new DB(name);

        currentDatabase = newDb;
        Databases.add(newDb);
    }

    private void addTable(String name, String[] rows, String database) {

        ArrayList<String> values = new ArrayList<>(Arrays.asList(rows));
        Table newT = new Table(name, values, out);

        if(!currentDatabase.getName().equals(database)) {
            for(DB db : Databases) {
                if(db.getName().equals(database)) {
                    currentDatabase = db;
                }
            }
        }
        currentDatabase.getTables().add(newT);
        newT.setWithinDatabase(currentDatabase);
        currentTable = newT;
    }

    private String[] parseLines(String line) {
        return line.split("\t");
    }


}
