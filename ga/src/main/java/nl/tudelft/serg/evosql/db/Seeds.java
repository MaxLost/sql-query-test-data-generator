package nl.tudelft.serg.evosql.db;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.hsqldb.types.Charset;

public class Seeds {

	private ArrayList<String> longs;
	private ArrayList<String> doubles;
	private ArrayList<String> strings;
	private ArrayList<String> tempSeeds;

	public Seeds() {
		this.longs = new ArrayList<String>();
		this.doubles = new ArrayList<String>();
		this.strings = new ArrayList<String>();
		this.tempSeeds = new ArrayList<String>();
	}
	public void addSeeds(Seeds newSeeds)
	{
		longs.addAll(newSeeds.longs);
		doubles.addAll(newSeeds.doubles);
		strings.addAll(newSeeds.strings);
		tempSeeds.addAll(newSeeds.tempSeeds);
	}

	public void addLong(long n) {
		longs.add(String.valueOf(n));
	}

	public void addDouble(double n) {
		doubles.add(String.valueOf(n));
	}

	public void addString(String n) {
		this.strings.add(n);
	}

	public List<String> getLongs() {
		List<String> result = new ArrayList<String>();
		result.addAll(longs);
		result.addAll(tempSeeds);
		return result;
	}

	public List<String> getDoubles() {
		List<String> result = new ArrayList<String>();
		result.addAll(doubles);
		result.addAll(tempSeeds);
		return result;
	}
	
	public List<String> getLongsAndDoubles() {
		List<String> result = new ArrayList<String>();
		result.addAll(longs);
		result.addAll(doubles);
		result.addAll(tempSeeds);
		return result;
	}

	public List<String> getStrings() {
		List<String> result = new ArrayList<String>();
		result.addAll(strings);
		result.addAll(tempSeeds);
		return result;
	}
	
	public void addToTemp(List<String> values) {
		tempSeeds.addAll(values);
	}
	
	public void unsetTemp() {
		tempSeeds.clear();
	}
	
	public boolean hasStrings() {
		return strings.size() > 0;
	}
	
	public boolean hasLongs() {
		return longs.size() > 0;
	}
	
	public boolean hasDoubles() {
		return doubles.size() > 0;
	}

	private static Seeds empty;
	static {
		empty = new Seeds();
	}
	public static Seeds emptySeed() {
		return empty;
	}
	
	public void expand(int mod, int stringModificationsAmount) throws UnsupportedEncodingException {

		Random rand = new Random();

		ArrayList<String> newLongs = new ArrayList<>(longs.size() * 3);
		for (String seed : longs) {
			Long longSeed = Long.parseLong(seed);
			Long increasedSeed = longSeed + (rand.nextLong() % mod);
			Long decreasedSeed = longSeed - (rand.nextLong() % mod);
			newLongs.add(longSeed.toString());
			newLongs.add(increasedSeed.toString());
			newLongs.add(decreasedSeed.toString());
		}
		longs = newLongs;

		ArrayList<String> newDoubles = new ArrayList<>(doubles.size() * 3);
		for (String seed : doubles) {
			Double longSeed = Double.parseDouble(seed);
			Double increasedSeed = longSeed + rand.nextDouble() * (rand.nextInt() % mod);
			Double decreasedSeed = longSeed - rand.nextDouble() * (rand.nextInt() % mod);
			newDoubles.add(longSeed.toString());
			newDoubles.add(increasedSeed.toString());
			newDoubles.add(decreasedSeed.toString());
		}
		doubles = newDoubles;

		ArrayList<String> newStrings = new ArrayList<>(strings.size() * 2);
		for (String seed: strings) {
			byte[] byteArray = seed.getBytes("UTF-8");
			for (int i = 0; i < stringModificationsAmount; i++) {
				int index = rand.nextInt(byteArray.length);
				byteArray[index] = (byte) (rand.nextInt() % 255);
			}
			String modifiedSeed = new String(byteArray, StandardCharsets.UTF_8);
			newStrings.add(seed);
			newStrings.add(modifiedSeed);
		}
		strings = newStrings;

		ArrayList<String> newTemps = new ArrayList<>(tempSeeds.size() * 2);
		for (String seed: tempSeeds) {
			byte[] byteArray = seed.getBytes("UTF-8");
			for (int i = 0; i < stringModificationsAmount; i++) {
				int index = rand.nextInt(byteArray.length);
				byteArray[index] = (byte) (rand.nextInt() % 255);
			}
			String modifiedSeed = new String(byteArray, StandardCharsets.UTF_8);
			newTemps.add(seed);
			newTemps.add(modifiedSeed);
		}
		tempSeeds = newTemps;

		return;
	}

}
