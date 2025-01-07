package nl.tudelft.serg.evosql.metaheuristics;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import nl.tudelft.serg.evosql.EvoSQLConfiguration;
import nl.tudelft.serg.evosql.db.Seeds;
import nl.tudelft.serg.evosql.fixture.Fixture;
import nl.tudelft.serg.evosql.fixture.FixtureRow;
import nl.tudelft.serg.evosql.fixture.FixtureRowFactory;
import nl.tudelft.serg.evosql.fixture.FixtureTable;
import nl.tudelft.serg.evosql.fixture.VectorisedFixture;
import nl.tudelft.serg.evosql.fixture.VectorisedFixtureTable;
import nl.tudelft.serg.evosql.metaheuristics.operators.DifferentialEvolutionCandidateProduction;
import nl.tudelft.serg.evosql.metaheuristics.operators.FixtureComparator;
import nl.tudelft.serg.evosql.metaheuristics.operators.FixtureFitness;
import nl.tudelft.serg.evosql.metaheuristics.operators.FixtureMutation;
import nl.tudelft.serg.evosql.metaheuristics.operators.SOMACandidatesProduction;
import nl.tudelft.serg.evosql.metaheuristics.operators.TournamentSelection;
import nl.tudelft.serg.evosql.metaheuristics.operators.VectorisedFixtureComparator;
import nl.tudelft.serg.evosql.sql.TableSchema;
import nl.tudelft.serg.evosql.util.random.Randomness;

public class SOMA extends Approach {
	private boolean isInitialized;

	/** Current population as vectors **/
	private List<VectorisedFixture> population;

	/** Pool of possible rows for all tables **/
	private Fixture elementsPool;

	/** Amount of samples generated for vectorisation **/
	private Integer elementsRowCount = EvoSQLConfiguration.MAX_ROW_QTY * 6000;

	private Integer tableRowCount = 9;

	private Randomness random = new Randomness();

	/** Selection operator **/
	private TournamentSelection selection = new TournamentSelection();

	/** Row Factory **/
	private FixtureRowFactory rowFactory = new FixtureRowFactory();

	/** String comparator **/
	private Comparator<String> stringComparator = Comparator.comparing(String::toString);

	/** Comparator **/
	private VectorisedFixtureComparator fc = new VectorisedFixtureComparator();

	/** Candidate production operator **/
	private SOMACandidatesProduction candidateProducer = new SOMACandidatesProduction(3, this.elementsRowCount);

	/** Seeds store **/
	private Seeds seeds;

	private int populationSize;


	public SOMA(List<VectorisedFixture> population, Map<String, TableSchema> pTableSchemas, String pPathToBeTested, Seeds seeds){
		super(pTableSchemas, pPathToBeTested);
		
		this.seeds = seeds;
		
		// this.mutation = new FixtureMutation(rowFactory, seeds);
		this.population = population;
		this.isInitialized = false;
		
		// if it's baseline, there will be only a single generation, and population will be larger
		populationSize = EvoSQLConfiguration.POPULATION_SIZE; // EvoSQLConfiguration.POPULATION_SIZE * 5; // * (baseline ? 2 : 1);
	}

	private void initialize() throws SQLException {
		//1. Initial population
		generateInitialPopulation();
		
		calculateFitness(population);
		
		// 2. sort population by Fitness Function
		Collections.sort(population, fc);
		log.debug("Best Fitness Function in the generated population = {}", population.get(0).getFitness());
		
		isInitialized = true;
	}
	
	@Override
	public Fixture execute(long pathTime) throws SQLException {
		long startTime = System.currentTimeMillis();

		// Initialize first
		if (!isInitialized) {
			initialize();
		}

		//3. Main Loop
		while (population.get(0).getFitness().getDistance() > 0
				&& System.currentTimeMillis() - startTime < pathTime
				&& System.currentTimeMillis() - startTime < EvoSQLConfiguration.MS_MAX_PATH_EXECUTION_TIME
				//&& generations < maxGenerations
		){

			log.info("Time since start " + (System.currentTimeMillis() - startTime) + "ms");
			double distance = population.get(0).getFitness().getDistance();

			List<VectorisedFixture> newPopulation = new ArrayList<>(populationSize);

			// Population assumed to be sorted by fitness level
			VectorisedFixture leader = population.get(0);
			newPopulation.add(leader);

			for (int index = 1; index < populationSize; index++) {
				VectorisedFixture parent = population.get(index);

				List<VectorisedFixture> offsprings = this.candidateProducer.produce(leader, parent);
				//calculateFitness(offsprings);
				//offsprings.sort(fc);
				//VectorisedFixture candidate = offsprings.get(0);
				newPopulation.addAll(offsprings);

				//offsprings.add(leader);
				//offsprings.sort(fc);
				//leader = offsprings.get(0);
			}

			// Add fresh tables to population
			for (int i = 0; i < populationSize / 8; i++) {
				List<VectorisedFixtureTable> tables = new ArrayList<>();
				for (TableSchema tableSchema : tableSchemas.values()) {
					tables.add(createVectorisedFixtureTable(tableSchema, tableRowCount, tables));
				}

				VectorisedFixture fixture = new VectorisedFixture(tables);
				log.debug("Fixture created: {}", fixture);
				newPopulation.add(fixture);
			}

			calculateFitness(newPopulation);
			// Order by fitness
			newPopulation.sort(fc);
			// Set the new population as the POPULATION_SIZE best
			population = newPopulation.subList(0, populationSize);

			log.debug("Generation = {}, best Fitness Function = {}", generations, population.get(0).getFitness());// + " for fixture: " + population.get(0));
			generations++;
		}

		List<FixtureTable> solutionTables = new ArrayList<>();
		for (VectorisedFixtureTable vectorisedTable : population.get(0).getTables()) {
			FixtureTable table = new FixtureTable(vectorisedTable.getSchema(), vectorisedTable.getRows());
			solutionTables.add(table);
		}
		Fixture solution = new Fixture(solutionTables);
		calculateFitnessForFixture(solution);

		log.info("Total generations: {}", generations);
		log.info("Best Fitness Function = {}", population.get(0).getFitness());
		if (EvoSQLConfiguration.USE_MINIMIZE)
			return minimize(solution);
		else
			return solution;

	}

	private void generateInitialPopulation() {
		int currentPopulationSize = 0;
		Random rand = new Random();
		List<VectorisedFixture> newPopulation = new ArrayList<>();
		// If we have a previous population fed by EvoSQL, clone some of these
//		if (population != null && !population.isEmpty()) {
//			// Order by fitness (the previous population should have fitness values)
//			Collections.sort(population, new VectorisedFixtureComparator());
//
//			// Select random individuals
//			boolean includesSolution = false;
//			for (int i = 0; i < population.size(); i++){
//				VectorisedFixture fixture = population.get(i);
//				if ((fixture.getFitness() != null && fixture.getFitness().getDistance() == 0 && !includesSolution) || // Always add one solution if there is one
//						rand.nextDouble() <= EvoSQLConfiguration.P_CLONE_POPULATION) {
//					if (fixture.getFitness() != null && fixture.getFitness().getDistance() == 0) includesSolution = true;
//					// Get a copy of this fixture that matches the current path
//					VectorisedFixture newFixture = fixture.copy();
//					newFixture.setFitness(null); // make sure fitness is gone
//					fixFixture(newFixture); // make sure all tables are present in the individual, and no more
//					newPopulation.add(newFixture);
//					currentPopulationSize++;
//				}
//			}
//		}

		log.debug("Generating pool of possible rows");

		ArrayList<FixtureTable> poolTables = new ArrayList<>();
		for (TableSchema tableSchema : tableSchemas.values()) {
			poolTables.add(createFixtureTable(tableSchema, poolTables));
		}
		this.elementsPool = new Fixture(poolTables);

		log.debug("Generating random initial population...");

		//this.tableRowCount = (EvoSQLConfiguration.MAX_ROW_QTY - EvoSQLConfiguration.MIN_ROW_QTY);
		//this.tableRowCount = 10;
		for(; currentPopulationSize < populationSize; currentPopulationSize++) {
			List<VectorisedFixtureTable> tables = new ArrayList<>();
			for (TableSchema tableSchema : tableSchemas.values()) {
				tables.add(createVectorisedFixtureTable(tableSchema, tableRowCount, tables));
			}

			VectorisedFixture fixture = new VectorisedFixture(tables);
			log.debug("Fixture created: {}", fixture);
			newPopulation.add(fixture);
		}
		log.debug("Generated random population with {} fixtures", newPopulation.size());

		// Store the new population in the list given by EvoSQL
		population.clear();
		population.addAll(newPopulation);
	}

	private void calculateFitness(List<VectorisedFixture> solutions) throws SQLException{
		for(VectorisedFixture fixture : solutions) {
			calculateFitness(fixture);
		}
	}
	
	public void calculateFitness(VectorisedFixture fixture) throws SQLException {
		individualCount++;
		
		// Truncate tables in Instrumented DB
		for (TableSchema tableSchema : tableSchemas.values()) {
			genetic.Instrumenter.execute(tableSchema.getTruncateSQL());
		}
		
		// Insert population
		for (String sqlStatement : fixture.getInsertStatements()) {
			genetic.Instrumenter.execute(sqlStatement);
		}
		
		// Start instrumenter
		genetic.Instrumenter.startInstrumenting();
		
		// Execute the path
		genetic.Instrumenter.execute(pathToTest);
		
		FixtureFitness ff = new FixtureFitness(genetic.Instrumenter.getFitness());
		fixture.setFitness(ff);
		
		// Store exceptions
		if (!genetic.Instrumenter.getException().isEmpty() && !exceptions.contains(genetic.Instrumenter.getException())) {
			exceptions += ", " + genetic.Instrumenter.getException();
		}
		
		// Stop instrumenter
		genetic.Instrumenter.stopInstrumenting();

		// set the fixture as "not changed" to avoid future fitness function computation
		fixture.setChanged(false);
	}

	public void calculateFitnessForFixture(Fixture fixture) throws SQLException {
		// Truncate tables in Instrumented DB
		for (TableSchema tableSchema : tableSchemas.values()) {
			genetic.Instrumenter.execute(tableSchema.getTruncateSQL());
		}

		// Insert population
		for (String sqlStatement : fixture.getInsertStatements()) {
			genetic.Instrumenter.execute(sqlStatement);
		}

		// Start instrumenter
		genetic.Instrumenter.startInstrumenting();

		// Execute the path
		genetic.Instrumenter.execute(pathToTest);

		FixtureFitness ff = new FixtureFitness(genetic.Instrumenter.getFitness());
		fixture.setFitness(ff);

		// Store exceptions
		if (!genetic.Instrumenter.getException().isEmpty() && !exceptions.contains(genetic.Instrumenter.getException())) {
			exceptions += ", " + genetic.Instrumenter.getException();
		}

		// Stop instrumenter
		genetic.Instrumenter.stopInstrumenting();

		// set the fixture as "not changed" to avoid future fitness function computation
		fixture.setChanged(false);
	}

	private VectorisedFixtureTable createVectorisedFixtureTable(TableSchema tableSchema, Integer rowCount, List<VectorisedFixtureTable> tables) {

		Integer[] rowIndicesArray = new Integer[rowCount];
		Arrays.fill(rowIndicesArray, -1);
		List<Integer> rowIndicesList = Arrays.asList(rowIndicesArray);

		for (int i = 0; i < rowCount; i++) {
			Integer index = this.random.nextInt(this.elementsRowCount);
			while (rowIndicesList.contains(index)) {
				index = this.random.nextInt(this.elementsRowCount);
			}
			rowIndicesList.set(i, index);
		}

		rowIndicesList.toArray(rowIndicesArray);

		// План сведения пространства задачи к дискретному пространству
		// + 1. Нагенерировать пул строк для каждой таблицы и пронумеровать их
		// + 2. Таблица - массив индексов строк
		// + 3. Элемент популяции - список таблиц
		// 4. Связать новую структуру с вычислением целевой функции

		return new VectorisedFixtureTable(tableSchema, this.elementsPool, rowIndicesArray);
	}
	
	private FixtureTable createFixtureTable(TableSchema tableSchema, List<FixtureTable> tables) {

		List<FixtureRow> rows = new ArrayList<>();

		for (int j = 0; j < this.elementsRowCount; j++) {
			FixtureRow row = rowFactory.create(tableSchema, tables, seeds);
			//FixtureRow row = rowFactory.create(tableSchema, tables, Seeds.emptySeed());
			rows.add(row);
			log.debug("Row created: {}", row);
		}
		rows.sort((x, y) -> stringComparator.compare(x.toString(), y.toString()));
		return new FixtureTable(tableSchema, rows);
	}
	
	// Takes a fixture and removes all tables that are not in the current problem
	// Adds new FixtureTables for tables that are in the problem but not in the given fixture
	private void fixFixture(VectorisedFixture fixture) {
		List<VectorisedFixtureTable> tables = fixture.getTables();
		int tableCount = tables.size();
		
		for (int i = 0; i < tables.size(); i++) {
			// Remove table if not in tableSchemas
			if (!tableSchemas.containsKey(tables.get(i).getName())) {
				fixture.removeTable(i);
				tableCount--;
			}
		}
		
		// If we have too few tables now, we need to add the missing ones
		if (tableCount < tableSchemas.size()) {
			for (TableSchema ts : tableSchemas.values()) {
				boolean present = false;
				for (VectorisedFixtureTable ft : tables) {
					if (ft.getSchema().equals(ts)) {
						present = true;
						break;
					}
				}
				// Create a new fixture table if this schema is not present
				if (!present) {
					fixture.addTable(createVectorisedFixtureTable(ts, this.tableRowCount, tables));
				}
			}
		}
	}
}
