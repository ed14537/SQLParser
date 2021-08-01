import java.util.ArrayList;

class DB {
    private String name;
    private ArrayList<Table> Tables = new ArrayList<>();

    DB(String name) {
        this.name = name;
    }

    String getName() {
        return name;
    }

    ArrayList<Table> getTables() {
        return Tables;
    }

    void deleteTable(String name) {

        for(Table t : Tables) {
            if(t.getName().equals(name)) {
                Tables.remove(t);
                return;
            }
        }
    }


}
