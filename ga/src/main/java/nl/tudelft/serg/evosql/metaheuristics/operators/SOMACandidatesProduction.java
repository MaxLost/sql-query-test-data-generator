package nl.tudelft.serg.evosql.metaheuristics.operators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import nl.tudelft.serg.evosql.fixture.Fixture;
import nl.tudelft.serg.evosql.fixture.FixtureRow;
import nl.tudelft.serg.evosql.fixture.FixtureTable;
import nl.tudelft.serg.evosql.fixture.VectorisedFixture;
import nl.tudelft.serg.evosql.fixture.VectorisedFixtureTable;
import nl.tudelft.serg.evosql.util.random.Randomness;

public class SOMACandidatesProduction {
	private Integer jumpsMin;

	private Integer elementsRowCount;

	public SOMACandidatesProduction(Integer jumpsMin, Integer elementsRowCount) {
		this.jumpsMin = jumpsMin;
		this.elementsRowCount = elementsRowCount;
	}

	/**
	 * Creates new elements with DSOMA algorithm
	 * @return list of possible new elements in population
	 */
	public List<VectorisedFixture> produce(VectorisedFixture leader, VectorisedFixture parent) {

		List<VectorisedFixtureTable> tables = new ArrayList<>();

		for (VectorisedFixtureTable tableFromLeader : leader.getTables()) {
			VectorisedFixtureTable tableFromParent = parent.getTable(tableFromLeader.getName());

			Integer rowCount = tableFromLeader.getRowCount();

			Integer[] coordinateDiff = new Integer[rowCount];
			for (int i = 0; i < rowCount; i++) {
				coordinateDiff[i] = Math.abs(tableFromLeader.getRowIndex(i) - tableFromParent.getRowIndex(i));
			}
			Integer mode = getArrayMode(coordinateDiff);
			Integer jumpsMax = mode > 0 ? mode : 1;
			Integer stepSize = jumpsMax >= this.jumpsMin ? (jumpsMax / (this.jumpsMin + 1)) : 1;

			Integer[][] possibleJumps = new Integer[this.jumpsMin][rowCount];
			for (int j = 0; j < rowCount; j++) {
				for (int l = 1; l <= this.jumpsMin; l++) {
					if (tableFromLeader.getRowIndex(j) > tableFromParent.getRowIndex(j)) {
						possibleJumps[l - 1][j] = Math.abs((tableFromParent.getRowIndex(j) + stepSize * l)) % this.elementsRowCount;
					} else if (tableFromLeader.getRowIndex(j) < tableFromParent.getRowIndex(j)) {
						possibleJumps[l - 1][j] = Math.abs((tableFromParent.getRowIndex(j) - stepSize * l)) % this.elementsRowCount;
					} else {
						possibleJumps[l - 1][j] = tableFromParent.getRowIndex(j);
					}
				}
			}
			int x = 1;
			for (int i = 0; i < this.jumpsMin; i++) {
				tables.add(new VectorisedFixtureTable(tableFromParent.getSchema(), tableFromParent.getRowsPool(), possibleJumps[i]));
			}
		}

		List<VectorisedFixture> offsprings = new ArrayList<>();
		for (int i = 0; i < this.jumpsMin; i++) {
			List<VectorisedFixtureTable> candidateTables = new ArrayList<>();
			for (int j = i; j < tables.size(); j += this.jumpsMin) {
				candidateTables.add(tables.get(j));
			}
			VectorisedFixture offspring = new VectorisedFixture(candidateTables);
			boolean isLeader = leader.equals(offspring);
			offsprings.add(offspring);
		}

		return offsprings;
	}

	public static Integer getArrayMode(Integer[] array) {

		int modeValue = 0, maxCount = 0;
		for (int i = 0; i < array.length; ++i) {
			int count = 0;
			for (int j = 0; j < array.length; ++j) {
				if (array[j].equals(array[i]))
					count++;
			}

			if (count > maxCount) {
				maxCount = count;
				modeValue = array[i];
			}
		}
		return modeValue;
	}

}
