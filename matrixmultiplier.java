// Michael Lim

// How to use:
// Pass the name of the input file via command line
// Threads will continue to run indefinitely, so
// user will need to manually kill the program
//

import java.io.File;        //Imports for file reading
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;


class matrixmultiplier {

	static String METHOD; //Global constant recording the mode of multiplication
	static int NUM_MATRICES;
	static int SIZE;

	/**
	 * Creates a DataBuffer and 4 threads, then reads, counts, numbers, and writes 
	 * lines in an input file
	 */
	public static void main(String[] args) {
		METHOD = args[0]; //Gets file name from command line argument
		NUM_MATRICES = Integer.parseInt(args[1]);
		SIZE = Integer.parseInt(args[2]);

		Matrix m1 = new Matrix(SIZE);
		//m1.printMatrix();

		Matrix m2 = new Matrix(SIZE);

		Matrix testResult = multiplyMatrices(m1,m2);
		testResult.printMatrix();
		System.out.println();
		
		Matrix test3Result = multiplyMatrices(m1,testResult);
		test3Result.printMatrix();
		System.out.println();
		
//		Matrix test4Result = multiplyMatrices(m1,test3Result);
//		test4Result.printMatrix();
//		System.out.println();

		Matrix result = new Matrix(SIZE);
		Matrix left = new Matrix(SIZE);
		Matrix right = new Matrix(SIZE);

		for(int i = 0; i < NUM_MATRICES-1; i ++){
			System.out.println("Setting up matrices "+i+" and "+(i+1));
			//System.out.println("Left at start ");
			//left.printMatrix();
			//System.out.println("Right at start");
			//right.printMatrix();
			

			if(METHOD.equals("U")){
				result = useUnthreaded(left,right);
			}
			else if(METHOD.equals("R")){
				result = useRowThreads(SIZE,left,right);
			}
			else if(METHOD.equals("E")){
				result = useElementThreads(SIZE, left,right);
			}
			else{
				System.err.println("Unknown method, defaulting to unthreaded");
				result = useUnthreaded(left, right);
			}
			System.out.println("Left at end ");
			left.printMatrix();
			left = result;
			System.out.println("Result at end ");
			left.printMatrix();
			System.out.println("");
		}
		result.printMatrix();
		
		
	}

	private static Matrix useUnthreaded(Matrix m1, Matrix m2){
		return multiplyMatrices(m1,m2);	
	}

	private static Matrix useRowThreads(int numRows, Matrix m1, Matrix m2){

		Thread[] threads = new Thread[numRows]; //Set up an array for our threads
		Matrix result = new Matrix(numRows);

		for(int i = 0; i < threads.length; i++){ //For each thread,
			threads[i] = new RowThread(m1.matrix[i], m2.getMatrix(), i, result);
			threads[i].start();
		}
		for(int i = 0; i < numRows; i++){ //For each thread,
			try {
				threads[i].join(); //wait for them all to finish
			} catch (InterruptedException e) {
				System.err.println("Thread was interrupted");
			}
		}
		return result;

	}

	private static Matrix useElementThreads(int numRows, Matrix m1, Matrix m2){
		int numElements = numRows*numRows;
		Matrix left = new Matrix(m1.matrix, numRows);
		Matrix right = new Matrix(m2.matrix, numRows);
		Matrix result = new Matrix(numRows);

		Thread[] threads = new Thread[numElements]; //Set up an array for our threads

		for(int i = 0; i < threads.length; i++){ //For each thread,

			int whichRow = (int) Math.floor(i/numRows);
			int whichCol = i%numRows;
			//System.out.println("Setting up thread for element "+i+" at "+whichRow+", "+whichCol);

			threads[i] = new ElementThread(left.matrix[whichRow], right.getCol(whichCol), whichRow, whichCol, result);
			threads[i].start();
		}
		for(int i = 0; i < numRows; i++){ //For each thread,
			try {
				threads[i].join(); //wait for them all to finish
			} catch (InterruptedException e) {
				System.err.println("Thread was interrupted");
			}
		}
		return result;

	}

	public static Matrix multiplyMatrices(Matrix m1, Matrix m2){

		if(m1.size != m1.size){
			System.err.println("Matrices are incompatible");
			System.exit(-1);
		}

		int size = m1.size;

		double[][] leftElements = m1.getMatrix();
		double[][] rightElements = m2.getMatrix();

		double[][] result = new double[size][size];

		for(int i = 0; i < size; i++){
			for( int j = 0; j < size; j++){
				for(int k = 0; k < size; k++){
					result [i][j] += (leftElements[i][k] * rightElements[k][j]);
				}
			}
		}

		return new Matrix(result, size);
	}

	public static double[] multiplyRow(double[] row, double[][] matrix){

		if(row.length != matrix.length){
			System.err.println("Rows are incompatible");
			System.exit(-1);
		}

		int size = row.length;


		double[] result = new double[size];

		for(int i = 0; i < size; i++){
			for(int j = 0; j< size; j++){
				result[i] += (row[j] * matrix[i][j]);

			}

		}

		return result;
	}

	public static double multiplyElement(int whichR, int whichC, double[] row, double[] col){
		

		double result = 0;

		if(row.length != col.length){
			System.err.println("Incompatible size");
			System.exit(-1);
		}
		
		//System.out.println("Multiplying row "+whichR);
		for(int i = 0; i < row.length; i++){
			//System.out.print(row[i]+" ");
		}
		//System.out.println();
		//System.out.println("By Column "+whichC);
		for(int i = 0; i < col.length; i++){
			//System.out.println(col[i]);
			//System.out.println();
		}

		int size = row.length;

		for(int i = 0; i < size; i++){
			result += (row[i] * col[i]);
		}

		//System.out.println("Result for "+whichR+", "+whichC);
		for(int i = 0; i < 1; i++){
			//System.out.print(result);
		}
		//System.out.println();
		return result;
	}
}

class RowThread extends Thread{ //Used this for help with Threads http://docs.oracle.com/javase/tutorial/essential/concurrency/runthread.html

	double[] left;
	double[][] right;
	Matrix result;
	int whichRow;

	RowThread(double[] row, double[][] matrix, int whichRow, Matrix result){ //Take a command as a parameter
		this.left = row;
		this.right = matrix;
		this.whichRow = whichRow;
		this.result = result;
	}

	public void run(){ //On run, simply execute a command as usual, but now wrapped in a thread
		synchronized(this.result){
			result.setRow(whichRow, matrixmultiplier.multiplyRow(this.left, this.right));
		}
	}
}

class ElementThread extends Thread{ //Used this for help with Threads http://docs.oracle.com/javase/tutorial/essential/concurrency/runthread.html

	double[] left;
	double[] right;
	Matrix result;
	int whichRow;
	int whichCol;

	ElementThread(double[] row, double[] col, int whichR, int whichC, Matrix result){ //Take a command as a parameter
		this.left = row;
		this.right = col;
		this.whichRow = whichR;
		this.whichCol = whichC;
		this.result = result;
	}

	public void run(){ //On run, simply execute a command as usual, but now wrapped in a thread
		synchronized(this.result){
			double newValue = matrixmultiplier.multiplyElement(whichRow, whichCol, this.left, this.right);
			result.setElement(this.whichRow, this.whichCol, newValue);
		}
	}
}

class Matrix {

	double[][] matrix;
	int size;

	public Matrix(double[][] matrix, int size){
		this.size = size;
		this.matrix = matrix;

	}

	public double[] getCol(int which) {
		double[] result = new double[this.size];

		for(int i = 0; i < this.size; i++){
			for(int j = 0; j < this.size; j++){
				if(j == which){
					result[i] = this.matrix[i][j];
				}
			}
		}
		return result;
	}

	public void setElement(int whichRow, int whichCol, double what) {
		this.matrix[whichRow][whichCol] = what;

	}

	public Matrix(int size){
		this.size = size;
		this.matrix = genMatrix(this.size);

	}

	private double[][] genMatrix(int size){
		double[][]  result = new double[size][size];

		for(int i = 0; i < size; i++){
			for( int j = 0; j < size; j++){
				result [i][j] = genElement(size, i,j);
			}
		}

		return result;
	}

	private double genElement(int size, int row, int col){
		int rMinusC = row-col;
		double cMinusR = col-row;

		if(rMinusC == 0){
			return 1.0;
		}
		else{
			return (Math.abs(cMinusR) +1.0);
		}
	}

	public void printMatrix(){

		for(int i = 0; i < size; i++){
			for( int j = 0; j < size; j++){
				System.out.print(this.matrix[i][j]+"    ");
			}
			System.out.println();
		}
	}



	public double[][] getMatrix(){
		return this.matrix;
	}

	public void setRow(int which, double[] what){
		this.matrix[which] = what;
	}
}

