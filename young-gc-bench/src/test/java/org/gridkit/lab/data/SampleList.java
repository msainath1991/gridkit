package org.gridkit.lab.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SampleList {

	private List<Sample> samples;
	
	private Map<String, Object> typedValueCache = new HashMap<String, Object>();

	
	public SampleList(List<Sample> samples) {
		this.samples = new ArrayList<Sample>(samples);
	}

	public SampleList sort(String... fields) {
		return sort(fields, false);
	}

	public SampleList sortReverse(String... fields) {
		return sort(fields, true);
	}
	
	public SampleList sort(String[] fields, boolean reverse) {
		Integer[] order = new Integer[samples.size()];
		for(int i = 0; i != order.length; ++i) {
			order[i] = i;
		}
		for(int i = fields.length - 1; i >= 0; --i) {
			sort(order, fields[i], reverse);
		}
		Sample[] result = new Sample[order.length];
		for(int i = 0; i != result.length; ++i) {
			result[i] = samples.get(order[i]);
		}
		return new SampleList(Arrays.asList(result));
	}
	
	/**
	 * Retains only first sample in each group
	 */
	public SampleList filterFirst(String... fields) {
		Set<List<Object>> tags = new HashSet<List<Object>>();
		
		List<Sample> samples = new ArrayList<Sample>();
		for(Sample sample: this.samples) {
			Object[] tag = new Object[fields.length];
			for(int i = 0; i != fields.length; ++i) {
				tag[i] = sample.get(fields[i]);
			}
			if (tags.add(Arrays.asList(tag))) {
				samples.add(sample);
			}
		}
		return new SampleList(samples);
	}

	/**
	 * Retains only first sample in each group
	 */
	public SampleList filterMedian(String... fields) {
		Map<List<Object>, List<Sample>> groups = new LinkedHashMap<List<Object>, List<Sample>>();
		
		List<Sample> samples = new ArrayList<Sample>();
		for(Sample sample: this.samples) {
			Object[] tag = new Object[fields.length];
			for(int i = 0; i != fields.length; ++i) {
				tag[i] = sample.get(fields[i]);
			}
			List<Sample> group = groups.get(Arrays.asList(tag));
			if (group == null) {
				group = new ArrayList<Sample>();
				groups.put(Arrays.asList(tag), group);
			}
			group.add(sample);
		}
		for(List<Sample> group: groups.values()) {
			samples.add(group.get(group.size() / 2));
		}
		return new SampleList(samples);
	}
	
	public SampleList filter(String field, Object value) {
		List<Sample> samples = new ArrayList<Sample>();
		for(Sample sample: this.samples) {
			if (value.equals(sample.get(field))) {
				samples.add(sample);
			}
		}
		return new SampleList(samples);
	}

	public SampleList filter(String field, String... anyOf) {
		Set<Object> values = new HashSet<Object>(Arrays.asList(anyOf));
		List<Sample> samples = new ArrayList<Sample>();
		for(Sample sample: this.samples) {
			if (values.contains(sample.get(field))) {
				samples.add(sample);
			}
		}
		return new SampleList(samples);
	}

	public SampleList filter(String field, double l, double h) {
		List<Sample> samples = new ArrayList<Sample>();
		for(Sample sample: this.samples) {
			double v = sample.getDouble(field);					
			if (v >= l && v <= h) {
				samples.add(sample);
			}
		}
		return new SampleList(samples);
	}
	
	public SampleList replace(String field, String oldValue, String newValue) {
		List<Sample> result = new ArrayList<Sample>();
		for(Sample sample: samples) {
			Sample sample2 = sample.clone();
			if (oldValue.equals(sample2.get(field))) {
				sample2.set(field, newValue);
			}
			result.add(sample2);
		}
		return new SampleList(result);
	}
	
	public Map<Object, SampleList> groupBy(String field) {
		Map<Object, SampleList> result = new HashMap<Object, SampleList>();
		for(Sample sample: samples) {
			Object val = sample.get(field);
			SampleList s = result.get(val);
			if (s == null) {
				s = new SampleList(new ArrayList<Sample>());
				result.put(val, s);
			}
			s.samples.add(sample);
		}
		return result;
	}
	
	public double[] numericSeries(String field) {
		double[] s = new double[samples.size()];
		for(int i = 0; i != s.length; ++i) {
			s[i] = ((Number)getTyped(i, field)).doubleValue();
		}
		return s;
	}

	public double[] scaledNumericSeries(String field, double factor) {
		double[] s = new double[samples.size()];
		for(int i = 0; i != s.length; ++i) {
			s[i] = factor * ((Number)getTyped(i, field)).doubleValue();
		}
		return s;
	}

	public String[] textSeries(String field) {
		String[] s = new String[samples.size()];
		for(int i = 0; i != s.length; ++i) {
			s[i] = samples.get(i).get(field);
		}
		return s;
	}

	public Object[] objectSeries(String field) {
		Object[] s = new Object[samples.size()];
		for(int i = 0; i != s.length; ++i) {
			s[i] = getTyped(i, field);
		}
		return s;
	}
	
	private Object deriveType(String field) {
		if (samples.size() == 0) {
			return String.class;
		}
		double[] dv = new double[samples.size()];
		long[] dl = new long[samples.size()];
		for(int i = 0; i != samples.size(); ++i) {
			String v = samples.get(i).get(field);
			if (v == null) {
				return String.class;
			}
			try {
				if (dl != null) {
					dl[i] = Long.parseLong(v);
					dv[i] = dl[i];
					continue;
				}
			} catch (NumberFormatException e) {
				// not a long
			}
			try {
				if (dv != null) {
					dv[i] = Double.parseDouble(v);
					continue;
				}
			} catch (NumberFormatException e) {
				// not a long
			}
			return String.class;
		}
		return dl != null ? dl : dv != null ? dv : String.class; 
	}

	private Object getTyped(int row, String field) {
		Object col = typedValueCache.get(field); 
		if (col == null) {
			col = deriveType(field);
			typedValueCache.put(field, col);
		}
		
		if (col == String.class) {
			return samples.get(row).get(field);
		}
		else if (col instanceof long[]) {
			return ((long[])col)[row];
		}
		else if (col instanceof double[]) {
			return ((double[])col)[row];
		}
		else {
			throw new RuntimeException("Broken typed cache");
		}
	}
	
	private void sort(final Integer[] order, final String field, final boolean reverse) {
		Comparator<Integer> cmp = new Comparator<Integer>() {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public int compare(Integer o1, Integer o2) {
				Object a1 = getTyped(o1, field);
				Object a2 = getTyped(o2, field);
				int n;
				if (a1 == a2) {
					n = 0;
				}
				else if (a1 == null) {
					n = -1;
				}
				else if (a2 == null) {
					n = 1;
				}
				else {
					n = ((Comparable)a1).compareTo(a2);
				}
				
				return (reverse ? -1 : 1) * n;
			}
		};
		
		Arrays.sort(order, cmp);
	}
	
	public int size() {
		return samples.size();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(Sample sample: samples) {
			if (sb.length() > 0) {
				sb.append('\n');
			}
			sb.append(sample.toString());
		}

		return sb.toString();
	}	
}