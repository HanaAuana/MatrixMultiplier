// Michael Lim
//Program takes three arguments 
//1. Either U, R, or R denoting unthreaded, row-threaded, or element-threaded
//2. The number of matrices to be multiplied 
//3. The size of the matrices to be multiplied. A matrix of size n will be an n X n

//Will output the final matrix, and the number of milliseconds the operation took. 
class matrixmultiplier {

	/**
	 * Reads in command line arugments, and multiplies the designated matrices
	 */
	public static void main(String[] args) {
		String METHOD = args[0];
		int NUM_MATRICES = Integer.parseInt(args[1]);
		int SIZE = Integer.parseInt(args[2]);

		Matrix result = new Matrix(SIZE);
		Matrix left = new Matrix(SIZE);
		Matrix right = new Matrix(SIZE);
		long startTime = System.currentTimeMillis(); //Get the start time of the multiplication
		
		for(int i = 0; i < NUM_MATRICES-1; i ++){ //For the number of matrices we were passed		
			if(METHOD.equals("U")){ //We will not be using any extra threads 
				result = useUnthreaded(left,right);
			}
			else if(METHOD.equals("R")){ //We will use a thread for each row of the result matrix (n threads for an n X n matrix)
				result = useRowThreads(SIZE,left,right);
			}
			else if(METHOD.equals("E")){//We will use one thread per element (n^2 threads for an n X n matrix)
				result = useElementThreads(SIZE, left,right);
			}
			else{
				System.err.println("Unknown method, defaulting to unthreaded");
				result = useUnthreaded(left, right); //If the first argument was invalid, just use unthreaded
			}
			left = result; //Set our left matrix to the result, to allow for the next multiplication
		}
		long runTime = System.currentTimeMillis() - startTime; //Get the elapsed time
		System.out.println("Multiplication took: "+runTime+" milliseconds");
		result.printMatrix(); 
	}

	//Simple wrapper for multiplying two matrices
	private static Matrix useUnthreaded(Matrix m1, Matrix m2){
		return multiplyMatrices(m1,m2);	//Unthreaded method just multiplies the whole matrix at once
	}

	//Creates numRows number of threads, and gets the result one row of multiplication at a time
	private static Matrix useRowThreads(int numRows, Matrix m1, Matrix m2){
		Thread[] threads = new Thread[numRows];
		Matrix result = new Matrix(numRows);

		for(int i = 0; i < threads.length; i++){ //Set up one thread per row
			threads[i] = new RowThread(m1.matrix[i], m2.getMatrix(), i, result);
			threads[i].start();
		}
		
		for(int i = 0; i < numRows; i++){
			try {
				threads[i].join(); //Wait for threads to finish
			} catch (InterruptedException e) {
				System.err.println("Thread was interrupted");
			}
		}
		return result;
	}

	//Creates a thread for each element, and solves the multiplcation one element at time
	private static Matrix useElementThreads(int numRows, Matrix m1, Matrix m2){
		int numElements = numRows*numRows;
		Matrix left = new Matrix(m1.matrix, numRows);
		Matrix right = new Matrix(m2.matrix, numRows);
		Matrix result = new Matrix(numRows);
		Thread[] threads = new Thread[numElements];

		for(int i = 0; i < threads.length; i++){//Set up one thread per element

			int whichRow = (int) Math.floor(i/numRows);
			int whichCol = i%numRows;// Get the row and column for each element
			
			double[] row = left.matrix[whichRow];//Get the row and column we'll need to compute this element
			double[] col = right.getCol(whichCol);

			threads[i] = new ElementThread(row, col, whichRow, whichCol, result);
			threads[i].start();
		}
		
		for(int i = 0; i < numRows; i++){
			try {
				threads[i].join(); //Wait for threads to finish
			} catch (InterruptedException e) {
				System.err.println("Thread was interrupted");
			}
		}
		return result;
	}

	//Given two matrices, find their product
	public static Matrix multiplyMatrices(Matrix m1, Matrix m2){
		if(m1.size != m1.size){ //Matrices must be compatible, size wise
			System.err.println("Matrices are incompatible");
			System.exit(-1);
		}

		int size = m1.size;
		double[][] leftElements = m1.getMatrix();
		double[][] rightElements = m2.getMatrix();
		double[][] result = new double[size][size];

		for(int i = 0; i < size; i++){
			for( int j = 0; j < size; j++){
				for(int k = 0; k < size; k++){//Multiplies each column and row combination
					result [i][j] += (leftElements[i][k] * rightElements[k][j]); //Stores the resulting element
				}
			}
		}
		return new Matrix(result, size);//Returns the product Matrix
	}

	//Multiplies one row by a matrix to get one row's worth of the product matrix
	public static double[] multiplyRow(double[] row, double[][] matrix){
		if(row.length != matrix.length){ //Row and column length must be equal
			System.err.println("Rows are incompatible");
			System.exit(-1);
		}

		int size = row.length;
		double[] result = new double[size];

		for(int i = 0; i < size; i++){
			for(int j = 0; j< size; j++){//Multiplies each element in the row by its corresponding elements in eaach column
				result[i] += (row[j] * matrix[i][j]); //and adds them together to get one element in the product row
			}
		}
		return result;
	}

	//Given a row and a column, compute the resulting element for the product matrix
	public static double multiplyElement(int whichR, int whichC, double[] row, double[] col){
		double result = 0;

		if(row.length != col.length){//Row and column must be the same size
			System.err.println("Incompatible size");
			System.exit(-1);
		}

		int size = row.length;

		//Mutliply row elements by the corresponding column element, and add them together
		for(int i = 0; i < size; i++){
			result += (row[i] * col[i]);
		}
		return result;
	}
}


class RowThread extends Thread{
	double[] left;
	double[][] right;
	Matrix result;
	int whichRow;

	RowThread(double[] row, double[][] matrix, int whichRow, Matrix result){
		this.left = row;
		this.right = matrix;
		this.whichRow = whichRow;
		this.result = result;
	}
	//Sets the designate row in the result matrix, to the value we compute 
	public void run(){
		synchronized(this.result){
			result.setRow(whichRow, matrixmultiplier.multiplyRow(this.left, this.right));
		}
	}
}

class ElementThread extends Thread{
	double[] left;
	double[] right;
	Matrix result;
	int whichRow;
	int whichCol;

	ElementThread(double[] row, double[] col, int whichR, int whichC, Matrix result){
		this.left = row;
		this.right = col;
		this.whichRow = whichR;
		this.whichCol = whichC;
		this.result = result;
	}

	//Sets the element at the given row and column to the value we compute
	public void run(){
		synchronized(this.result){
			double newValue = matrixmultiplier.multiplyElement(whichRow, whichCol, this.left, this.right);
			result.setElement(this.whichRow, this.whichCol, newValue);
		}
	}
}

//A class to represent our matrices
class Matrix {
	double[][] matrix;
	int size;

	public Matrix(double[][] matrix, int size){
		this.size = size;
		this.matrix = matrix;
	}

	public Matrix(int size){
		this.size = size;
		this.matrix = genMatrix(this.size);
	}

	//Given an int, return the corresponding column
	public double[] getCol(int which) {
		double[] result = new double[this.size];

		for(int i = 0; i < this.size; i++){
			for(int j = 0; j < this.size; j++){
				if(j == which){ //Get the value at the given index in each row
					result[i] = this.matrix[i][j];
				}
			}
		}
		return result;
	}

	//Given a row and column, set the element there to the given double
	public void setElement(int whichRow, int whichCol, double what) {
		this.matrix[whichRow][whichCol] = what;
	}

	//Given a size n, generate an n X n matrix, with the value determined by genElement()
	private double[][] genMatrix(int size){
		double[][]  result = new double[size][size];

		for(int i = 0; i < size; i++){
			for( int j = 0; j < size; j++){//For each element in the n X n matrix
				result [i][j] = genElement(size, i,j);
			}
		}
		return result;
	}

	//Given a row, column, and row length, generate a matrix with 1.0 at each element in it's main diagonal
	private double genElement(int size, int row, int col){ //2.0 in each element one away, 3.0 in those 2 away, etc.
		int rMinusC = row-col;
		double cMinusR = col-row;

		if(rMinusC == 0){
			return 1.0;
		}
		else{
			return (Math.abs(cMinusR) +1.0);
		}
	}

	//Prints the matrix, with some basic formatting
	public void printMatrix(){
		for(int i = 0; i < size; i++){
			for( int j = 0; j < size; j++){
				System.out.print(this.matrix[i][j]+"   ");
			}
			System.out.println();
		}
	}

	//Returns the 2d double array representing the matrix
	public double[][] getMatrix(){
		return this.matrix;
	}

	//Sets the given row to the given double array
	public void setRow(int which, double[] what){
		this.matrix[which] = what;
	}
}