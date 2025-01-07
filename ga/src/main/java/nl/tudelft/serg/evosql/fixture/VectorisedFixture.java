package nl.tudelft.serg.evosql.fixture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import nl.tudelft.serg.evosql.metaheuristics.operators.FixtureFitness;
import nl.tudelft.serg.evosql.sql.TableSchema;

public class VectorisedFixture implements Cloneable{

	/** To keep track of its fitness value **/
	private FixtureFitness fitness = null;

	private boolean isChanged = false;

	private List<VectorisedFixtureTable> tables;

	public VectorisedFixture(List<VectorisedFixtureTable> tables) {
		this.tables = tables;
	}
	
	public List<VectorisedFixtureTable> getTables() {
		return Collections.unmodifiableList(tables);
	}
	
	public VectorisedFixtureTable getTable(TableSchema ts) {
		return tables.stream().filter(ft -> ft.getSchema() == ts).findFirst().get();
	}

	public void removeTable(int idx) {
		tables.remove(idx);
	}
	
	public void addTable(VectorisedFixtureTable table) {
		tables.add(table);
	}

	@Override
	public String toString() {
		return "Fixture [tables=" + tables + "]";
	}
	
	public List<String> getInsertStatements() {
		List<String> statements = new ArrayList<String>();
		
		for (VectorisedFixtureTable ft : tables) {
			statements.add(ft.getInsertSQL());
		}
		
		return statements;
	}
	
	public List<String> getInsertStatements(String excludeTableName, int excludeIndex) {

		List<String> statements = new ArrayList<String>();
		
		for (VectorisedFixtureTable ft : tables) {
			if (ft.getName().equals(excludeTableName)) {
				statements.add(ft.getInsertSQL(excludeIndex));
			} else {
				statements.add(ft.getInsertSQL());
			}
		}
		
		return statements;
	}
	

	public FixtureFitness getFitness(){
		return fitness;
	}
	
	public void setFitness (FixtureFitness fitness){
		this.fitness = fitness;
	}
	
	public void unsetFitness() {
		this.fitness = null;
	}
	
	public VectorisedFixture copy() {
		List<VectorisedFixtureTable> cloneList = new ArrayList<>();
		for (VectorisedFixtureTable table : this.tables){
			cloneList.add(table.copy());
		}
		VectorisedFixture clone = new VectorisedFixture(cloneList);
		if (this.fitness != null)
			clone.setFitness(this.fitness.copy());
		return clone;
	}

	public String prettyPrint() {
        StringBuilder prettyFixture = new StringBuilder();

        for (VectorisedFixtureTable table : tables) {
            prettyFixture.append("-- Table: " + table.getName() + "\n");

            Iterator<FixtureRow> it = table.getRows().iterator();
            int rowCount = 1;
            while (it.hasNext()) {
                FixtureRow row = it.next();
                prettyFixture.append(" Row #" + rowCount + ": ");

                for (Map.Entry<String, String> kv : row.getValues().entrySet()) {
                    prettyFixture.append(kv.getKey() + "='" + kv.getValue() + "',");
                }

                prettyFixture.append("\n");
                rowCount++;
            }
            prettyFixture.append("\n");
        }

        return prettyFixture.toString().trim();
    }

	public int qtyOfTables() {
		return tables.size();
	}

	public VectorisedFixtureTable getTable(int index) {
		return tables.get(index);
	}

	public VectorisedFixtureTable getTable(String tableName) {
		return tables.stream().filter(x -> x.getName().equals(tableName)).findFirst().get();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		VectorisedFixture fixture = (VectorisedFixture) o;

		if (fitness != null ? !fitness.equals(fixture.fitness) : fixture.fitness != null) return false;
		return tables != null ? tables.equals(fixture.tables) : fixture.tables == null;
	}

	@Override
	public int hashCode() {
		int result = fitness != null ? fitness.hashCode() : 0;
		result = 31 * result + (tables != null ? tables.hashCode() : 0);
		return result;
	}

	public boolean isChanged() {
		return isChanged;
	}

	public void setChanged(boolean changed) {
		isChanged = changed;
	}

	public int getNumberOfTables(){
		return this.getTables().size();
	}
}
