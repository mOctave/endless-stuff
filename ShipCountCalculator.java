import java.io.File;
import java.io.FileNotFoundException;

import java.lang.reflect.Array;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

class ShipCountCalculator {
	static List<String> filedata = new ArrayList<>();
	static List<String> reqAttributes = new ArrayList<>();
	static HashMap<String,HashMap<String, Double>> fleets = new HashMap<String,HashMap<String, Double>>();
	static List<HashMap<String, Double>> systems = new ArrayList<> ();
	static HashMap<String, Double> relativeShipCounts = new HashMap<> ();
	static int population = 0;
	static final int MAIN = 0;
	static final int FLEET = 1;
	static final int SYSTEM = 2;
	static final int SHIP = 3;


	// The main method that runs everything
	public static void main(String[] args) {
		// Get the options from the args provided
		List<String> options = Arrays.asList(args);

		if (options.contains("-h")) {
			System.out.println("-b\tOnly look at base ships, ignoring variants and rolling all their stats together.");
			System.out.println("-s\tOnly require ONE of the attributes selected, rather than requiring them all.");
		}

		// Create a new scanner for use input, get data
		Scanner s = new Scanner(System.in);
		System.out.println("Welcome to the ES Ship Count Calculator!");
		/*System.out.println("Please enter the total population: ");
		population = Integer.parseInt(s.nextLine());*/
		System.out.print("Please enter paths to files to parse (leave blank to stop adding filepaths): ");

		while (true) {
			// Add filedata
			String fp = s.nextLine();
			if (fp.trim().equals(""))
				break;
			File f = new File(fp);
			try {
				int lines = 0;
				Scanner fs = new Scanner(f);
				while (fs.hasNextLine()) {
					filedata.add(fs.nextLine());
					lines ++;
				}
				System.out.printf("%d lines added to filedata.%n",lines);
				fs.close();
			} catch (FileNotFoundException e) {
				System.out.printf("No file found at %s!%n",fp);
			}
		}

		// Add attributes
		System.out.print("Please enter attributes to require (leave blank to stop adding attributes): ");

		while (true) {
			String x = s.nextLine();
			if (x.trim().equals(""))
				break;
			reqAttributes.add(x);
		}

		s.close();

		// Parse filedata, do calculations, print out CSV table
		parseFiledata(options.contains("-b"), options.contains("-s"));
		calculateRelativeShipCounts();
		System.out.println(formatAsCSV("ship", "relative abundance", relativeShipCounts));

		System.out.println(filedata.size());
		System.out.println(filedata.contains("fleet \"Korath Raid\""));
		System.out.println(fleets.keySet().contains("Korath Raid"));
	}



	// Parse all of the filedata for fleets, systems, and ships
	public static void parseFiledata(boolean ignoreVariants, boolean onlyRequireSomeAttributes) {
		int mode = MAIN;
		int totalweight = 0;
		int weight = 0;
		String name = "";
		HashMap<String,Double> currentVariant = new HashMap<>();
		boolean allAttributes = false;
		boolean someAttributes = false;
		List<String> systemNameList = new ArrayList<>();

		for (int i = 0; i < filedata.size(); i++) {
			String l = filedata.get(i);
			if (i % 100 == 0)
				System.out.println("Line "+i);
			if (l.contains("Deep River") && !l.contains("\t"))
				System.out.println(l);
			if (l.contains("Ap'arak") && !l.contains("\t"))
				System.out.println(l);

			// Outside system, fleet, or ship node, keep looking
			if (mode == MAIN) {
				if (l.startsWith("fleet")) {
					mode = FLEET;
					totalweight = 0;
					name = ((String) Array.get(l.split(" ", 2), 1)).trim().replaceAll("\"","");
					System.out.printf("New fleet (\"%s\") on line %d%n", name, i);
					currentVariant = new HashMap<>();
				}
				else if (l.startsWith("system")) {
					mode = SYSTEM;
					name = ((String) Array.get(l.split(" ", 2), 1)).trim().replaceAll("\"","");
					currentVariant = new HashMap<> ();
					allAttributes = false;
					someAttributes = false;
				}
				/*else if (l.startsWith("ship")) {
					mode = SHIP;
				}*/
			}

			// Found fleet
			else if (mode == FLEET) {
				if (!l.startsWith("\t")) {
					// End of fleet
					System.out.println("End of fleet reached!");
					mode = MAIN;
					transferCurrentVariant(name, weight, currentVariant);
					weightFleet(name, totalweight);

				} else if (l.trim().startsWith("variant")) {
					// Add new variant to current fleet
					transferCurrentVariant(name, weight, currentVariant);
					try {
						weight = Integer.parseInt((String) Array.get(l.trim().split(" "),1));
					} catch (ArrayIndexOutOfBoundsException e) {
						weight = 1;
					}
					totalweight += weight;
					currentVariant = new HashMap<>();

				} else if (l.trim().startsWith("\"")) {
					// Add ship to current variant
					String[] x;
					if (l.contains("\""))
						x = l.trim().split("\"");
					else 
						x = l.trim().split(" ");
					if (ignoreVariants) {
						// Only add base models if the -b flag is in use
						String baseModel = ((String) Array.get(x, 1)).replaceAll("\\(.+\\)", "").trim();
						if (currentVariant.get(baseModel) == null) {
							try {
								currentVariant.put(baseModel,Double.parseDouble(((String) Array.get(x, 2)).trim()));
							} catch (ArrayIndexOutOfBoundsException e) {
								currentVariant.put(baseModel,1.);
							}
						} else {
							try {
								currentVariant.put(baseModel,currentVariant.get(baseModel) + Double.parseDouble(((String) Array.get(x, 2)).trim()));
							} catch (ArrayIndexOutOfBoundsException e) {
								currentVariant.put(baseModel,currentVariant.get(baseModel) + 1.);
							}
						}
					} else {
						// Otherwise add variants too
						try {
							currentVariant.put((String) Array.get(x, 1),Double.parseDouble(((String) Array.get(x, 2)).trim()));
						} catch (ArrayIndexOutOfBoundsException e) {
							currentVariant.put((String) Array.get(x, 1),1.);
						}
					}

				}
			}

			// Found system
			else if (mode == SYSTEM) {
				if (!l.startsWith("\t")) {
					// End of system
					System.out.println("End of system reached");
					mode = MAIN;
					if (allAttributes || (someAttributes && onlyRequireSomeAttributes) || reqAttributes.isEmpty()) {
						systems.add(currentVariant);
						systemNameList.add(name);
					} else {
						System.out.println("System did not meet all criteria!");
					}
					currentVariant = new HashMap<>();
				} else if (l.trim().startsWith("fleet")) {
					// Add new fleet to current system
					String[] x;
					if (l.contains("\""))
						x = l.trim().split("\"");
					else 
						x = l.trim().split(" ");
					currentVariant.put((String) Array.get(x, 1),60 / Double.parseDouble(((String) Array.get(x, 2)).trim()));
				} else if (l.trim().startsWith("attributes")) {
					// Check whether the system should be included
					allAttributes = true;
					for (String attr : reqAttributes) {
						if (l.contains(attr)) {
							someAttributes = true;
							System.out.println("Found "+attr+" in system "+name);
						} else {
							allAttributes = false;
							System.out.println("Did not find "+attr+" in system "+name);
						}
					}
				}

				/*// Found ship
				else if (mode == SHIP) {
					if (!l.startsWith("\t")) {
					// End of fleet
						System.out.println("End of ship reached!");
						mode = MAIN;
						weightFleet(name, totalweight);

					}
				}*/
			}
		}
		System.out.println(systemNameList);
		System.out.println(systems);
	}



	// Take all the system and fleet data and figure out how much of everything there is
	public static void calculateRelativeShipCounts() {
		System.out.println(fleets.keySet());
		for (HashMap<String, Double> system : systems) {
			for (Map.Entry<String, HashMap<String, Double>> fleet : fleets.entrySet()) {
				if (system.get(fleet.getKey()) != null) {
					for (Map.Entry<String, Double> ship : fleet.getValue().entrySet()) {
						Double x = relativeShipCounts.get(ship.getKey());
						if (x == null) x = 0.;
						relativeShipCounts.put(ship.getKey(), x + system.get(fleet.getKey()) * ship.getValue());
					}
				}
			}
		}
		System.out.println("Relative Ship Counts:");
		System.out.println(relativeShipCounts);
	}



	// Take the current fleet variant and add it to the fleet
	public static void transferCurrentVariant(String fleet, int weight, HashMap<String,Double> variant) {
		HashMap<String,Double> x = fleets.get(fleet);
		if (x == null)
			x = new HashMap<> ();
		for (String key : variant.keySet()) {
			if (x.get(key) == null)
				x.put(key, variant.get(key) * weight);
			else
				x.put(key, x.get(key) + variant.get(key) * weight);
		}
		fleets.put(fleet, x);
	}



	// Weight every variant in the fleet according to the total weight of the fleet
	public static void weightFleet(String fleet, int totalweight) {
		HashMap<String,Double> x = fleets.get(fleet);
		for (String key : x.keySet())
			x.put(key, x.get(key) / totalweight);
		fleets.put(fleet.trim(), x);
	}



	// Convert the hashmap with headers to a CSV table
	public static String formatAsCSV(String header1, String header2, HashMap<String,Double> map) {
		String x = header1 + ", " + header2 + "\n";
		for (Map.Entry<String, Double> i : map.entrySet())
			x += i.getKey() + ", " + i.getValue() + "\n";
		return x;
	}
}