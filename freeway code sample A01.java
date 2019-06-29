		// freeway code sample: can I PREVENT the grid from sorting rows alphabetically?!?

		// NOTE: I pulled this code out of a much larger project (only the relevant code snippets are here)

		IGrid riskGrid;

		riskGrid = container.addGrid("Risk", new String[] {"Delta", "Gamma", "Theta", "Vega", "Downside", "ATM", "Upside", "DayTradeProfitLoss", "ProfitLoss", "OptionsDelta", "FuturesDelta", "ExpirationDate"});
		riskGrid.clear();
		log("To use, create a grid named \"Risk\" with columns \"Delta\", \"Gamma\", \"Theta\", \"Vega\", \"Downside\", \"ATM\", \"Upside\", \"DayTradeProfitLoss\", \"ProfitLoss\", \"OptionsDelta\", \"FuturesDelta\", and \"ExpirationDate\"");

		// The code below is representative of the code I use to populate the grid (for the first time and for each subsequent update).
		// My hope was that with no other options set, the rows would appear in the grid in the order I populate them. For the following, I
		// would like "OZS" to appear in the first row and "HE" in the second, but what I am seeing is that these will be automatically sorted
		// alphabetically so that "HE" will be in the first row and "OZS" in the second row.

		String rowSymbol;

		rowSymbol = "OZS";
		riskGrid.set(rowSymbol, "Delta", 1.0);
		riskGrid.set(rowSymbol, "Gamma", 2.0);
		riskGrid.set(rowSymbol, "Theta", 3.0);
		riskGrid.set(rowSymbol, "Vega", 4.0);
		riskGrid.set(rowSymbol, "DayTradeProfitLoss", -550.75);
		riskGrid.set(rowSymbol, "ProfitLoss", 645.25);

		rowSymbol = "HE";
		riskGrid.set(rowSymbol, "Delta", 11.0);
		riskGrid.set(rowSymbol, "Gamma", 22.0);
		riskGrid.set(rowSymbol, "Theta", 33.0);
		riskGrid.set(rowSymbol, "Vega", 44.0);
		riskGrid.set(rowSymbol, "DayTradeProfitLoss", -2550.75);
		riskGrid.set(rowSymbol, "ProfitLoss", 6645.25);

		// I placed a grid on the dashboard and configured the Columns to match the column names used in the definition of riskGrid.
		// There are very few options on this dashboard grid, but in particular I made sure that "Sort on Updates" is NOT checked.
