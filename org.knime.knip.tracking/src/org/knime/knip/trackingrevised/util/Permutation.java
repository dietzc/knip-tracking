package org.knime.knip.trackingrevised.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Permutation {
	public static <T extends Comparable<T>> T[] nextPermutation(final T[] c) {
		// get largest k, s.t. c[k] < c[k+1]
		int first = getFirst(c);
		if (first == -1)
			return null; // no greater permutation
		// find last index toSwap, s.t. c[k] < c[toSwap]
		int toSwap = c.length - 1;
		while (c[first].compareTo(c[toSwap]) >= 0)
			--toSwap;
		// swap first and toSwap
		swap(c, first++, toSwap);
		// reverse sequence from k+1 to n (inclusive)
		toSwap = c.length - 1;
		while (first < toSwap)
			swap(c, first++, toSwap--);
		return c;
	}

	private static <T extends Comparable<T>> int getFirst(final T[] c) {
		for (int i = c.length - 2; i >= 0; i--)
			if (c[i].compareTo(c[i + 1]) < 0)
				return i;
		return -1;
	}

	private static void swap(final Object[] c, final int i, final int j) {
		final Object tmp = c[i];
		c[i] = c[j];
		c[j] = tmp;
	}

	public static <T extends Comparable<T>> List<T[]> getAllPermutations(
			final List<T> c) {
		if (c.isEmpty())
			return new LinkedList<T[]>();
		@SuppressWarnings("unchecked")
		T[] array = (T[]) Array.newInstance(c.get(0).getClass(), c.size());
		int index = 0;
		for (T t : c)
			array[index++] = t;
		return getAllPermutations(array);
	}

	public static <T extends Comparable<T>> List<T[]> getAllPermutations(
			final T[] c) {
		// always sort first!
		Arrays.sort(c);
		List<T[]> result = new LinkedList<T[]>();
		do {
			result.add(c.clone());
		} while (nextPermutation(c) != null);
		return result;
	}

	// only for testing purpose

	public static void main(String args[]) {
		Integer[] testArray = { 1, 2, 3, 4 };
		printTest(testArray);
		String testString = "Test";
		Character[] c = new Character[testString.length()];
		int index = 0;
		for (char ch : testString.toCharArray())
			c[index++] = ch;
		printTest(c);
		Integer[] testArray2 = { 3, 2, 1 };
		// must be sorted!
		Arrays.sort(testArray2);
		printTest(testArray2);

		// getAllPermutations
		printAllPermutations(testArray);
		printAllPermutations(new Integer[] { 1 });
		printAllPermutations(new Integer[] { 1, 2 });
		printAllPermutations(new Integer[] { 2, 1 });
	}

	private static <T extends Comparable<T>> void printTest(T[] c) {
		int counter = 0;
		do {
			System.out.println(Arrays.toString(c));
			counter++;
		} while ((Permutation.<T> nextPermutation(c)) != null);
		System.out.println("#: " + counter + " of " + fac(c.length));
	}

	private static long fac(int number) {
		long result = 1;
		while (number > 0) {
			result *= (number--);
		}
		return result;
	}

	private static <T extends Comparable<T>> void printAllPermutations(T[] c) {
		List<T[]> result = getAllPermutations(c);
		for (T[] array : result) {
			System.out.println(Arrays.toString(array));
		}
	}
}
