// A Backtracking program  in Java to solve Sudoku problem
// Code derived from a Java implementation at: http://www.geeksforgeeks.org/backtracking-set-7-suduku/
/*
 * Dillon Blanksma
 * Started on 11/19/2014
 * Updated on 11/23/2014
 * Finished on
 * 
 * Dr Dan Grissom
 * Artifical Intelligence
 * 
 * Demonstrating Constraint Satisfaction Problems
 * Variables are Cells where a cell is any given location on a Sudoku grid
 * Domains are the possible values that a cell can be - given numbers have empty domains
 * Contraints are
 * 		1. All rows must have digits 1-9
 * 		2. All columns must have digits 1-9
 * 		3. All 3x3 boxes must have digits 1-9
 */
import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;
import java.io.*;
import javax.swing.JOptionPane;

public class Sudoku
{
	// Constants
	final static int UNASSIGNED = 0; //UNASSIGNED is used for empty cells in sudoku grid	
	final static int N = 9;//N is used for size of Sudoku grid. Size will be NxN
	static Cell[][] gridStatic;
	static HashMap<Integer, Cell[][]> states = new HashMap<Integer, Cell[][]>();
	static int count = 0;
	static int instance = 0;

	/////////////////////////////////////////////////////////////////////
	// Main function used to test solver.
	public static void main(String[] args) throws IOException
	{
		// 0 means unassigned cells - This is a sample sudoku puzzle.
		// You can search the internet for more test cases.
		String [] puzzles = {"easy.txt", "medium.txt", "hard.txt", "notfun.txt", "impossible.txt"};
		int gridChoice = JOptionPane.showOptionDialog(null, "Please choose your grid", 
				"Ordering", JOptionPane.DEFAULT_OPTION, 0, null, puzzles, 0);
		String file = "";
		
		switch(gridChoice)
		{
		case 0:
			file = "easy.txt";
			break;
		case 1:
			file = "medium.txt";
			break;
		case 2:
			file = "hard.txt";
			break;
		case 3:
			file = "notfun.txt";
			break;
		case 4:
			file = "worldshardest.txt";
		}
		Cell grid[][] = getSudokuGrid(file);
		gridStatic = getSudokuGrid(file);
		states.put(instance, grid);
		
		System.out.println("Unsolved sudoku puzzle:");
		printGrid(grid);
		
		String [] orderings = {"Minimum Remaining Values Heuristic", "Maximum Remaining Values Heuristic",
							 "Static Ordering 1 (normal)", "Static Ordering 2 (reverse)", "Static Ordering 3 (boxes)",
							 "Random Ordering"};
		int orderChoice = JOptionPane.showOptionDialog(null, "Please choose your ordering scheme", 
				"Ordering", JOptionPane.DEFAULT_OPTION, 0, null, orderings, 0);
		
		long endTime = 0L;
		long startTime = System.currentTimeMillis(); //get the current time before we begin solving
		
		if (SolveSudoku(grid, orderChoice) == true)
		{
			endTime = System.currentTimeMillis(); //get the time after solution has been returned
			double time = Math.abs(startTime - endTime); //computer the difference to be time elapsed.
			
			System.out.println("\n\nSolved sudoku puzzle:");
			printGrid(grid);
			System.out.println(time + " ms");
			
			if (isValid(grid))
				System.out.println("Rows & columns & boxes are all valid");
			System.out.println(count + " backtracks");
		}
		else
			System.out.println("No solution exists");
	}
	

	/////////////////////////////////////////////////////////////////////
	// Takes a partially filled-in grid and attempts to assign values to
	// all unassigned locations in such a way to meet the requirements
	// for Sudoku solution (non-duplication across rows, columns, and boxes)
	static boolean SolveSudoku(Cell grid[][], int choice)
	{
		Cell unassigned = null;
		//basic switch statement to determine which ordering scheme to run
		switch(choice)
		{
		case 0:
			unassigned = minimumRemainingValue(grid);
			break;
		case 1:
			unassigned = maximumRemainingValue(grid);
			break;
		case 2:
			unassigned = FindUnassignedLocation(grid);
			break;
		case 3:
			unassigned = FindUnassignedReverse(grid);
			break;
		case 4:
			unassigned = FindUnassignedInBoxes(grid);
			break;
		case 5:
			unassigned = RandomUnassignedLocation(grid);
			
		}
		
		// If there is no unassigned location, we are done
		if (unassigned == null)
			return true; // success!

		int row = unassigned.row;
		int col = unassigned.col;
		ArrayList<Integer> domain = unassigned.domain;

		// consider digits only within initial domain
		for (int num = 0; num < domain.size(); num++)
		{
			//if looks promising
			if (isSafe(grid, row, col, domain.get(num)))
			{
				instance++;
				// make tentative assignment
				grid[row][col].value = domain.get(num);
				
				Cell [][] gridCopy = new Cell[N][N];
				for (int i = 0; i < N; i++)
					for (int j = 0; j < N; j++)
						gridCopy[i][j] = grid[i][j].clone();
	
				states.put(instance, gridCopy); //save the current state in a map for reference if we backtrack
				updateSpecifiedDomains(grid, row, col, domain.get(num)); //removes num from all applicable domains

				
				//printGrid(grid, "Instance " + String.valueOf(instance));

				// return, if success, yay!
				if (SolveSudoku(grid, choice))
					return true;

				// failure, un-assign & try again and reassign the grid to the previous grid
				Cell [][] previousGrid = states.get(instance); //retain previous state
				for (int i = 0; i < N; i++)
					for (int j = 0; j < N; j++)
						grid[i][j] = previousGrid[i][j].clone();
				
				domain = grid[row][col].domain; //reset the domain

			}
		}
		count++;
		instance--;	//if we backtrack refer back one instance
		return false; // this triggers backtracking
	}
	
	/*
	 * Dillon Blanksma
	 * Check out the unassigned cells and update their domains.
	 * A cell's domain is initially all values [1-9] - this was chosen for harder grids
	 * It's more likely a harder grid will have a larger domain (meaning more possible values)
	 * Therefore, removals to the domain may be more optimal than adding values to the domain. - very miniscule optimization
	 */
	static Cell[][] updateAllDomains(Cell grid[][])
	{
		Cell updatedGrid[][] = new Cell[N][N];
		for (int row = 0; row < grid.length; row++)
			for (int col = 0; col < grid.length; col++)
			{
				//only focus on unassigned cells
				if (grid[row][col].value == UNASSIGNED)
					for (int i = 0; i < grid[row][col].domain.size();)
					{
						if (!isSafe(grid, row, col, grid[row][col].domain.get(i)))
							grid[row][col].domain.remove((Integer)grid[row][col].domain.get(i)); //remove values that break a rule
						else
							i++; //removal from array list will change the size. Only increment index if a value was NOT removed
					}
				else
					grid[row][col].domain.clear(); //number is ASSIGNED (not 0) so erase its domain
			}
		
		for (int row = 0; row < updatedGrid.length; row++)
			for (int col = 0; col < updatedGrid.length; col++)
				updatedGrid[row][col] = grid[row][col];
		
		return updatedGrid;
	}
	
	//to update dynamically
	static void updateSpecifiedDomains(Cell grid[][], int row, int col, int num, boolean remove)
	{
		updateRowDomains(grid, row, num, remove);
		updateColumnDomains(grid, col, num, remove);
		updateBoxDomains(grid, row - row%3, col - col%3, num, remove);
	}
	
	static void updateRowDomains(Cell grid[][], int row, int num, boolean remove)
	{
		if (remove)
		{
			for (int col = 0; col < N; col++)
				if (grid[row][col].domain.contains((Integer)num))
					grid[row][col].domain.remove((Integer)num);
		}
		else
			for (int col = 0; col < N; col++)
				if (gridStatic[row][col].domain.contains((Integer)num))
					grid[row][col].domain.add((Integer)num);
	}
	
	static void updateColumnDomains(Cell grid[][], int col, int num, boolean remove)
	{
		if(remove)
		{
			for (int row = 0; row < N; row++)
				if (grid[row][col].domain.contains((Integer)num) && remove)
					grid[row][col].domain.remove((Integer)num);
		}
		else
			for (int row = 0; row < N; row++)
				if (gridStatic[row][col].domain.contains((Integer)num))
					grid[row][col].domain.add((Integer)num);
	}
	
	static void updateBoxDomains(Cell grid[][], int boxStartRow, int boxStartCol, int num, boolean remove)
	{
		if (remove)
		{
			for (int row = 0; row < 3; row++)
				for (int col = 0; col < 3; col++)
					if (grid[row+boxStartRow][col+boxStartCol].domain.contains((Integer)num) && remove)
						grid[row][col].domain.remove((Integer)num);
		}
		else
			for (int row = 0; row < 3; row++)
				for (int col = 0; col < 3; col++)
					if (gridStatic[row][col].domain.contains((Integer)num)  && !grid[row+boxStartRow][col+boxStartCol].set && grid[row+boxStartRow][col+boxStartCol].domain.contains((Integer)num))
						grid[row+boxStartRow][col+boxStartCol].domain.add((Integer)num);
	}

	/////////////////////////////////////////////////////////////////////
	// Searches the grid to find an entry that is still unassigned. If
	// found, the reference parameters row, col will be set the location
	// that is unassigned, and true is returned. If no unassigned entries
	// remain, false is returned.
	//STATIC ORDERING 1
	static Cell FindUnassignedLocation(Cell grid[][])
	{
		for (int row = 0; row < N; row++)
			for (int col = 0; col < N; col++)
				if (grid[row][col].value == UNASSIGNED)
					return grid[row][col];
		return null;
	}
	
	static Cell FindUnassignedReverse(Cell grid[][])
	{
		for (int row = N-1; row >= 0; row--)
			for (int col = N-1; col >= 0; col--)
				if (grid[row][col].value == UNASSIGNED)
					return grid[row][col];
		return null;
	}
	/*
	 * STATIC ORDERING 2 - Dillon Blanksma
	 * This static ordering scheme looks at each individual box for unassigned coordinates
	 * I thought about the strategy I usually use when solving a Sudoku puzzle by hand: usually I focus
	 * on the boxes first then the rows and columns associated with each box
	 * 
	 * Time difference: minimal
	 */
	static Cell FindUnassignedInBoxes(Cell grid[][])
	{
		ArrayList<Cell> unassignedCoords = new ArrayList<Cell>();
		
		for (int row = 0; row < N; row += 3)
			for (int col = 0; col < N; col += 3)
			{
				unassignedCoords = getUnassignedInBox(grid, row, col);
				if (!unassignedCoords.isEmpty())
					return unassignedCoords.get(0);
			}
		return null;
	}
	
	//finding unassigned locations for a given 3x3 box
	static ArrayList<Cell> getUnassignedInBox(Cell grid[][], int i, int j)
	{
		ArrayList<Cell> unassignedCoords = new ArrayList<Cell>();
		
		for (int row = i; row < i+3; row++)
			for (int col = j; col < j+3; col++)
				if(grid[row][col].value == UNASSIGNED)
					unassignedCoords.add(grid[row][col]);
		return unassignedCoords;
	}
	
	/*
	 * RANDOM ORDERING - Dillon Blanksma
	 * Generate all the unassigned coordinates into one list and choose randomly among them
	 * Time difference: considerably worse
	 */
	static Cell RandomUnassignedLocation(Cell grid[][])
	{
		ArrayList<Cell> unassignedCoords = new ArrayList<Cell>();

		for (int row = 0; row < N; row++)
			for (int col = 0; col < N; col++)
				if (grid[row][col].value == UNASSIGNED)
					unassignedCoords.add(grid[row][col]);
		
		Cell randomCoord = null;
		Random rand = new Random();
		//kept running into errors within the Random class saying that 'n' could not be negative
		//where n is located in rand.nextInt(n)
		if (unassignedCoords.size() > 1)
			randomCoord = unassignedCoords.get(rand.nextInt(unassignedCoords.size())); // <= somehow unassignedCoords.size() was negative
		else if (unassignedCoords.size() == 1)
			randomCoord = unassignedCoords.get(0);
		
		return randomCoord;
	}
	
	/*
	 * Dillon Blanksma
	 * MINIMUM REMAINING VALUES aka MOST CONSTRAINED VARIABLE heuristic
	 * as of now this is not faster - it probably should be.
	 */
	static Cell minimumRemainingValue(Cell grid[][])
	{
		int min = 100;
		Cell mostConstrained = null;
		
		for (int row = 0; row < grid.length; row++)
			for (int col = 0; col < grid.length; col++)
				if (grid[row][col].value == UNASSIGNED)
					//compare current cell domain size to the current minimum
					//assign min to current if current is less than min
					if (grid[row][col].domain.size() < min)
					{
						min = grid[row][col].domain.size();
						mostConstrained = grid[row][col];
					}
		//Just another implementation to get minimum domain size
		/*Cell [] unassignedArr = getUnassignedAsArray(grid);
		sortByDomainSize(unassignedArr);
		if (unassignedArr.length != 0)
			mostConstrained = unassignedArr[0];*/
		return mostConstrained;
	}
	/*
	 * MAXIMUM REMAINING VALUE
	 */
	static Cell maximumRemainingValue(Cell grid[][])
	{
		int max = 0;
		Cell mostConstrained = null;
		
		for (int row = 0; row < grid.length; row++)
			for (int col = 0; col < grid.length; col++)
				if (grid[row][col].value == UNASSIGNED)
					if (grid[row][col].domain.size() > max)
					{
						max = grid[row][col].domain.size();
						mostConstrained = grid[row][col];
					}
		return mostConstrained;
	}

	/////////////////////////////////////////////////////////////////////
	// Returns a boolean which indicates whether any assigned entry
	// in the specified row matches the given number.
	static boolean UsedInRow(Cell grid[][], int row, int num)
	{
		for (int col = 0; col < N; col++)
			if (grid[row][col].value == num)
				return true;
		return false;
	}

	/////////////////////////////////////////////////////////////////////
	// Returns a boolean which indicates whether any assigned entry
	// in the specified column matches the given number.
	static boolean UsedInCol(Cell grid[][], int col, int num)
	{
		for (int row = 0; row < N; row++)
			if (grid[row][col].value == num)
				return true;
		return false;
	}

	/////////////////////////////////////////////////////////////////////
	// Returns a boolean which indicates whether any assigned entry
	// within the specified 3x3 box matches the given number.
	static boolean UsedInBox(Cell grid[][], int boxStartRow, int boxStartCol, int num)
	{
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 3; col++)
				if (grid[row+boxStartRow][col+boxStartCol].value == num)
					return true;
		return false;
	}

	/////////////////////////////////////////////////////////////////////
	// Returns a boolean which indicates whether it will be legal to assign
	// num to the given row, col location.
	static boolean isSafe(Cell grid[][], int row, int col, int num)
	{
		// Check if 'num' is not already placed in current row,
		// current column and current 3x3 box
		return !UsedInRow(grid, row, num) &&
				!UsedInCol(grid, col, num) &&
				!UsedInBox(grid, row - row%3 , col - col%3, num);
	}
	
	//Blanksma, Dillon
	//EVALUATION FUNCTION to check the validity of a presumably solved puzzle
	//check all constraints
	static boolean isValid(Cell grid[][])
	{

		if(!checkRows(grid))
			return false;
		if (!checkCols(grid))
			return false;
		
		//check each 3x3 box
		for (int row = 0; row < N; row +=3)
			for (int col = 0; col < N; col += 3)
				if (!checkBoxes(grid, row, col))
					return false;
		
		return true;
	}
	
	//check for duplicates in rows
	static boolean checkRows(Cell grid[][])
	{
		for (int row = 0; row < N; row++)
			for (int col = 0; col < N; col++)
				for (int temp = 0; temp < N; temp++)
					//compare pairs of numbers, return false if there is a duplicate AND they are not the same index
					if (grid[row][col].value == grid[row][temp].value && col != temp)
						return false;
		return true;
	}
	
	//check for duplicates in columns (same as rows just switch some indices)
	static boolean checkCols(Cell grid[][])
	{
		for (int row = 0; row < N; row++)
			for (int col = 0; col < N; col++)
				for (int temp = 0; temp < N; temp++)
					if (grid[row][col].value == grid[temp][col].value && row != temp)
						return false;
		return true;
	}
	
	//this actually checks a single box given the starting row, i, and the starting column, j
	static boolean checkBoxes(Cell grid[][], int i, int j)
	{
		ArrayList<Integer> box = new ArrayList<Integer>();
		
		for (int row = i; row < i+3; row++)
			for (int col = j; col < j+3; col++)
				box.add(grid[row][col].value); //copy elements of 3x3 box in a list
		
		//use same algorithm as row checking to check for duplicates
		for (int row = 0; row < box.size(); row++)
			for (int temp = 0; temp < box.size(); temp++)
				if (box.get(row) == box.get(temp) && row != temp)
					return false;
		
		return true;
	}
	
	/*
	 * Utility function to read the text files
	 */
	static Cell [][] getSudokuGrid(String file) throws IOException
	{
		BufferedReader bf = null;
		String line = null;
		
		Cell grid[][] = new Cell[N][N]; // <= class is not loading. Set breakpoint here then see what 'grid' variable holds 
		int rowNum = 0;

		try {
			bf = new BufferedReader(new FileReader(file));
			
			while ((line = bf.readLine()) != null)
			{
				String [] row = line.split(",");
				for (int col = 0; col < grid.length; col++)
				{
					grid[rowNum][col] = new Cell(rowNum, col);
					grid[rowNum][col].value = Integer.parseInt(row[col]);
					if (grid[rowNum][col].value != UNASSIGNED)
						grid[rowNum][col].set = true;
				}
					
				
				rowNum++;
			}
		}
		catch (FileNotFoundException fnfe){
			fnfe.printStackTrace();
		}
		catch (IOException ioe){
			ioe.printStackTrace();
		}
		finally {
			bf.close();
		}
		return updateAllDomains(grid); //update the initial domains here
	}

	/////////////////////////////////////////////////////////////////////
	// A utility function to print grid
	static void printGrid(Cell grid[][])
	{
		for (int row = 0; row < N; row++)
		{
			for (int col = 0; col < N; col++)
			{
				if (grid[row][col].value == 0)
					System.out.print("- ");
				else
					System.out.print(grid[row][col].value + " ");
			}	    	   
			System.out.print("\n");
		}
	}
	
	//Some more utility functions
	static Cell [] getUnassignedAsArray(Cell grid[][])
	{
		Cell [] unassigned = new Cell[getNumberUnassigned(grid)];
		int current = 0;
		
		for (int row = 0; row < N; row++)
			for (int col = 0; col < N; col++)
				if (grid[row][col].value == UNASSIGNED)
				{
					unassigned[current] = grid[row][col];
					unassigned[current].row = row;
					unassigned[current].col = col;
					current++;
				}
		return unassigned;
	}
	
	static int getNumberUnassigned(Cell grid[][])
	{
		int count = 0;
		for (int row = 0; row < N; row++)
			for (int col = 0; col < N; col++)
				if (grid[row][col].value == UNASSIGNED)
					count++;
		return count;
	}
	
	//bubble sort
	static Cell[] sortByDomainSize(Cell unassigned[])
	{
		boolean flag = true;
		Cell temp;
		
		while (flag)
		{
			flag = false;
			for (int i = 0; i < unassigned.length-1; i++)
			{
				if (unassigned[i].domain.size() > unassigned[i+1].domain.size())
				{
					temp = unassigned[i];
					unassigned[i] = unassigned[i+1];
					unassigned[i+1] = temp;
					flag = true;
				}
			}
		}
		Cell ordered[] = unassigned;
		return ordered;
	}
}
