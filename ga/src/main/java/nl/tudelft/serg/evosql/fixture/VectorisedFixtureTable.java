package nl.tudelft.serg.evosql.fixture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import nl.tudelft.serg.evosql.sql.ColumnSchema;
import nl.tudelft.serg.evosql.sql.TableSchema;

public class VectorisedFixtureTable {

    private TableSchema tableSchema;
    private Integer[] rowIndices;
    private Fixture rowsPool;

    public VectorisedFixtureTable(TableSchema tableSchema, Fixture rowsPool, Integer[] rowIndices) {
        this.tableSchema = tableSchema;
        this.rowIndices = rowIndices;
        this.rowsPool = rowsPool;
    }

    public Integer getRowIndex(Integer index) {
        return this.rowIndices[index];
    }

    public Fixture getRowsPool() {
        return rowsPool;
    }

    public Integer[] getRowIndices() {
        return this.rowIndices;
    }

    public List<FixtureRow> getRows() {

        ArrayList<FixtureRow> rows = new ArrayList<>();

        for (Integer rowIndex : rowIndices) {
            rows.add(rowsPool.getTable(tableSchema.getName()).getRows().get(rowIndex));
        }
        return rows;
    }

    public int getRowCount() {
        return rowIndices.length;
    }

    public List<String> getColumnValues(ColumnSchema cs) {
        List<String> result = new ArrayList<String>();

        for (Integer rowIndex : rowIndices) {
            result.add(rowsPool.getTable(tableSchema.getName()).getRows().get(rowIndex).getValueFor(cs.getName()));
        }

        return result;
    }

    public String getName() {
        return this.tableSchema.getName();
    }

    public TableSchema getSchema() {
        return this.tableSchema;
    }

    @Override
    public String toString() {
        return "FixtureTable [rows=" + rowIndices + "]";
    }

    public String getInsertSQL() {
        String sql = tableSchema.getInsertSQL() + " VALUES ";

        int size = this.rowIndices.length;
        for (int i = 0; i < size; i++) {
            FixtureRow x = rowsPool.getTable(tableSchema.getName()).getRows().get(i);
            sql += rowsPool.getTable(tableSchema.getName()).getRows().get(this.rowIndices[i]).getValuesSQL();

            if (i < size - 1) {
                sql += ", ";
            }
        }

        return sql;
    }

    public String getInsertSQL(int excludeIndex) {
        String sql = tableSchema.getInsertSQL() + " VALUES ";

        int size = this.rowIndices.length;
        for (int i = 0; i < size; i++) {
					if (i == excludeIndex) {
						continue;
					}

            sql += rowsPool.getTable(tableSchema.getName()).getRows().get(this.rowIndices[i]).getValuesSQL();

            // Only put comma if not last
            if (!((i == size - 1) || (i == size - 2 && excludeIndex == size - 1))) {
                sql += ", ";
            }
        }

        return sql;
    }

    public VectorisedFixtureTable copy() {
        Integer[] indicesClone = this.rowIndices.clone();
        VectorisedFixtureTable clone = new VectorisedFixtureTable(this.tableSchema, this.rowsPool, indicesClone);
        return clone;
    }

    @Override
    public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

        VectorisedFixtureTable that = (VectorisedFixtureTable) o;

			if (tableSchema != null ? !tableSchema.equals(that.tableSchema) : that.tableSchema != null) {
				return false;
			}
        return rowIndices != null ? rowIndices.equals(that.rowIndices) : that.rowIndices == null;
    }

    @Override
    public int hashCode() {
        int result = tableSchema != null ? tableSchema.hashCode() : 0;
        result = 31 * result + (rowIndices != null ? rowIndices.hashCode() : 0);
        return result;
    }
}
