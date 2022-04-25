package privruler;

import com.google.gson.JsonArray;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class Util {

	public static String legacyJoin(CharSequence delimiter, Iterable<? extends CharSequence> elements) {
		StringBuilder sb = new StringBuilder("");
		for (CharSequence element : elements) {
			sb.append(element).append(delimiter);
		}
		return sb.toString();
	}

	public static List<String> readAllLinesFromFile(String fileName) {
		List<String> lines = new ArrayList<String>();
		try {
			FileInputStream fileInputStream = new FileInputStream(fileName);
			DataInputStream dataInputStream = new DataInputStream(fileInputStream);

			while (true) {
				String line = dataInputStream.readLine();
				if (line == null) {
					break;
				}

				// Record current line
				lines.add(line);
			}
		} catch (Exception e) {
			throw new RuntimeException("Unexpected IO error on " + fileName, e);
		}
		return lines;
	}

	public static JsonArray convertJsonArray(Collection<String> values) {
		JsonArray valueArr = new JsonArray();
		for (String value : values) {
			valueArr.add(value);
		}
		return valueArr;
	}

	public static Collection<String> containedKeywords(String str, Collection<String> keywords) {
		Collection<String> ret = new HashSet<String>();

		for (String k : keywords) {
			if (str.contains(k)) {
				ret.add(k);
			}
		}
		return ret;
	}

	/* Checks if a string is empty ("") or null. */
	public static boolean isEmpty(String s) {
		return s == null || s.length() == 0;
	}

	/* Counts how many times the substring appears in the larger string. */
	public static int countMatches(String text, String str) {
		if (isEmpty(text) || isEmpty(str)) {
			return 0;
		}

		int index = 0, count = 0;
		while (true) {
			index = text.indexOf(str, index);
			if (index != -1) {
				count++;
				index += str.length();
			} else {
				break;
			}
		}

		return count;
	}
}