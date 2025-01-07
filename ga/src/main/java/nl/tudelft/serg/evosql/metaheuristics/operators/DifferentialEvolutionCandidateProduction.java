package nl.tudelft.serg.evosql.metaheuristics.operators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import nl.tudelft.serg.evosql.fixture.Fixture;
import nl.tudelft.serg.evosql.fixture.FixtureRow;
import nl.tudelft.serg.evosql.fixture.FixtureTable;
import nl.tudelft.serg.evosql.util.random.Randomness;

public class DifferentialEvolutionCandidateProduction {
	private Randomness random;
	private Double threshold;

	public DifferentialEvolutionCandidateProduction(Randomness random, Double threshold) {
		this.random = random;
		this.threshold = threshold;
	}

	/**
	 * Performs crossover
	 * @return a new candidate from 2 parents
	 */
	public Fixture produce(Fixture parent1, Fixture parent2) {

		if (parent1.getTables().size() < 1 || parent2.getTables().size() < 1 )
			throw new IllegalArgumentException("Each solution must have at least one Table");

		List<FixtureTable> candidateTables = new ArrayList<>();

		if (isApplicable(parent1, parent2)) {

			for (FixtureTable tableFromP1 : parent1.getTables()) {
				// i do not trust on the unordered list, so that's why i' using table name and not indexes
				FixtureTable tableFromP2 = parent2.getTable(tableFromP1.getName());
				Iterator<FixtureRow> rowsFromTableP2 = tableFromP2.getRows().iterator();

				FixtureTable candidateTable = new FixtureTable(tableFromP1.getSchema(), new ArrayList<>());

				for (FixtureRow rowFromTableP1 : tableFromP1.getRows()) {
					if (this.random.nextDouble() < this.threshold) {
						candidateTable.addRow(rowFromTableP1);
					} else {
						candidateTable.addRow(rowsFromTableP2.hasNext() ? rowsFromTableP2.next() : rowFromTableP1);
					}
				}
			}

			return new Fixture(candidateTables);

		} else {
			return parent1;
		}
	}

	/**
	 * The crossover can be applied only to Fixture with at least two Table each
	 * @param parent1
	 * @param parent2
	 * @return true if the parents contain at least two Tables
	 */
	protected boolean isApplicable(Fixture parent1, Fixture parent2) {
		return (parent1.getNumberOfTables()>1 && parent2.getNumberOfTables()>1);
	}
}
