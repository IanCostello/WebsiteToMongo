package main;
/** Queue Class
 * 
 * Description: A standard purpose queue class that handles enqueueing and dequeuing of Strings. 
 * The class returns all output instead of printing it to console.
 * It provides also provides methods for printing of data and other general purpose methods.
 */

public class Queue 
{
	private static final int MAX_SIZE = 5096;
	private String[] queue; //The queue
	private int start = 0; //start
	private int end = -1; //end
	private int size = 0; //size so you don't have queueSize - 1 
	
	/** Constuctor */
	public Queue (int queueSize) 
	{
		
		queue = new String[queueSize];
	}

	/** enqueue
	 * Adds something to the queue 
	 * @param s     String to enqueue
	 * @return      nothing if successful or error message
	 */
	public String enqueue(String s) 
	{
		//Check that it isn't full
		if (!isFull()) 
		{
			end = nextI(end);
			queue[end] = s;
			size++;
			//Return nothing
			return "";
		} else {
			if (size < (MAX_SIZE/2)) {
				String[] newQueue = new String[size*2];
				System.arraycopy(newQueue, 0, newQueue, 0, size);
			}
		}
		//Return error message
		return "Queue is full; didn't add to the queue!";
	}
	
	/** dequeue
	 * Removes the first item in the queue
	 * @return		String that was dequeued or error message
	 */
	public String dequeue() 
	{
		if (!isEmpty()) 
		{
			size--;
			//get string at start of queue then increment start
			String s = queue[start];
			start = nextI(start);
			return s;
		}
		return "Queue is empty; nothing to dequeue!";
	}
	
	/** toString 
	 * Prints out the queue 
	 * @return The data in print ready form
	 */
	public String toString()
	{
		String s = "";
		//Loop through the entire queue
		for (int i = 0; i < size; i+=1) 
		{
			//Go from start to start+size 
			s += queue[(start+i)%queue.length] + '\n';
		}
		return s;
	}

	/** printRawData
	 * Prints out the queue from index 0 to end including null vars
	 * @return The the data in print ready form
	 */
	public String printRawData() 
	{
		String s = "";
		//Loop through entire array and add each String
		for (int i = 0; i < queue.length; i+=1) 
		{
			s += queue[i];
			s += '\n';
		}
		return s;
	}
	
	/** isEmpty
	 * Returns t/f on whether the queue is empty
	 * @return isEmpty
	 */
	public boolean isEmpty() 
	{
		return (size == 0);
	}
	
	
	public int nextI(int i) 
	{
		return (i+1) % queue.length;
	}
	
	/** isFull
	 * Returns t/f on whether the queue is full
	 * @return
	 */
	public boolean isFull() 
	{
		return (size >= queue.length);
	}
}
