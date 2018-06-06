package org.aksw.limes.core.io.ls;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.aksw.limes.core.datastrutures.LogicOperator;
import org.aksw.limes.core.io.parser.Parser;

/**
 * @author Mohamed Sherif (sherif@informatik.uni-leipzig.de)
 * @author Klaus Lyko (lyko@informatik.uni-leipzig.de)
 * @version Nov 12, 2015
 */
public class LinkSpecification implements ILinkSpecification {
	// Constants
	protected static final String MAX = "MAX";
	protected static final String OR = "OR";
	protected static final String ADD = "ADD";
	protected static final String MINUS = "MINUS";
	protected static final String XOR = "XOR";
	protected static final String MIN = "MIN";
	protected static final String AND = "AND";
	protected static final String LUKT = "LUKT";
	protected static final String LUKTCO = "LUKTCO";
	protected static final String LUKDIFF = "LUKDIFF";
	protected static final String ALGT = "ALGT";
	protected static final String ALGTCO = "ALGTCO";
	protected static final String ALGDIFF = "ALGDIFF";
	protected static final String EINT = "EINT";
	protected static final String EINTCO = "EINTCO";
	protected static final String EINDIFF = "EINDIFF";
	protected static final String HAMT = "HAMT";
	protected static final String HAMTCO = "HAMTCO";
	protected static final String HAMDIFF = "HAMDIFF";
	protected double threshold;
	protected LogicOperator operator;
	protected List<LinkSpecification> children; // children must be a list
	// because
	// not all operators are
	// commutative
	protected List<LinkSpecification> dependencies;// dependencies are the list
	// of
	// specs whose result set is
	// included in the result set
	// of this node
	protected String filterExpression;
	protected LinkSpecification parent;
	protected double quality = 0d;
	// If the LinkSpecification is atomic the measure and properties are this.
	// filterexpression: e.g. trigrams(s.label,t.label).
	protected String atomicMeasure = ""; // eg. trigrams...
	protected String prop1 = "";
	protected String prop2 = "";
	protected String treePath = "";
	protected String fullExpression = "";
	// just a quick hack to have lower borders for advanced threshold searches
	private double lowThreshold = 0d;
	/**
	 * Used for parameterized t-norms/t-conorms
	 */
	protected double operatorParameter;

	public LinkSpecification() {
		setOperator(null);
		setChildren(null);
		setThreshold(-1);
		parent = null;
		setDependencies(null);
	}

	/**
	 * Creates a spec with a measure read inside
	 *
	 * @param measure
	 *            String representation of the spec
	 * @param threshold
	 *            of the spec
	 */
	public LinkSpecification(String measure, double threshold) {
		setOperator(null);
		setChildren(null);
		parent = null;
		setDependencies(null);
		readSpec(measure, threshold);
	}

	public void setAtomicFilterExpression(String atomicMeasure, String prop1, String prop2) {
		setAtomicMeasure(atomicMeasure);
		this.prop1 = prop1;
		this.prop2 = prop2;
		filterExpression = atomicMeasure + "(" + prop1 + "," + prop2 + ")";
	}

	/**
	 * Adds a child to the current node of the spec
	 *
	 * @param spec
	 *            to be added
	 */
	public void addChild(LinkSpecification spec) {
		if (getChildren() == null) {
			setChildren(new ArrayList<LinkSpecification>());
		}
		getChildren().add(spec);
	}

	/**
	 * Adds a child to the current node of the spec
	 *
	 * @param spec
	 *            to be added
	 */
	public void addDependency(LinkSpecification spec) {
		if (getDependencies() == null) {
			setDependencies(new ArrayList<LinkSpecification>());
		}
		getDependencies().add(spec);
	}

	/**
	 * Removes a dependency from the list of dependencies
	 *
	 * @param spec
	 *            Input spec
	 */
	public void removeDependency(LinkSpecification spec) {
		if (getDependencies().contains(spec)) {
			getDependencies().remove(spec);
		}
		if (getDependencies().isEmpty()) {
			setDependencies(null);
		}
	}

	/**
	 * Checks whether a spec has dependencies
	 *
	 * @return true if the spec has dependencies, false otherwise
	 */
	public boolean hasDependencies() {
		if (getDependencies() == null) {
			return false;
		}
		return !getDependencies().isEmpty();
	}

	/**
	 * @return True if the spec is empty, all false
	 */
	public boolean isEmpty() {
		if (filterExpression == null && (getChildren() == null || getChildren().isEmpty())) {
			return true;
		}
		return false;
	}

	/**
	 * @return True if the spec is a leaf (has no children), else false
	 */
	public boolean isAtomic() {
		if (getChildren() == null) {
			return true;
		}
		return getChildren().isEmpty();
	}

	/**
	 * Create the path of operators for each leaf spec
	 */
	public void pathOfAtomic() {
		if (isAtomic()) {
			treePath += "";
		} else {
			if (getChildren() != null) {
				for (LinkSpecification child : getChildren()) {
					String parentPath = treePath;
					if (child == getChildren().get(0)) {
						child.treePath = parentPath + ": " + getOperator() + "->left";
					} else {
						child.treePath = parentPath + ": " + getOperator() + "->right";
					}
					child.pathOfAtomic();
				}
			}
		}

	}

	/**
	 * Reads a spec expression into its canonical form Don't forget to optimize
	 * the filters by checking (if threshold_left and threshold_right grater
	 * than or equal to theta, then theta = 0)
	 *
	 * @param spec
	 *            expression to read
	 * @param theta
	 *            Global threshold
	 */
	public void readSpec(String spec, double theta) {

		Parser p = new Parser(spec, theta);
		if (p.isAtomic()) {
			filterExpression = spec;
			setThreshold(theta);
			fullExpression = spec;
		} else {
			LinkSpecification leftSpec = new LinkSpecification();
			LinkSpecification rightSpec = new LinkSpecification();
			leftSpec.parent = this;
			rightSpec.parent = this;
			setChildren(new ArrayList<LinkSpecification>());
			getChildren().add(leftSpec);
			getChildren().add(rightSpec);

			if (p.getOperator().equalsIgnoreCase(AND)) {
				setOperator(LogicOperator.AND);
				leftSpec.readSpec(p.getLeftTerm(), p.getThreshold1());
				rightSpec.readSpec(p.getRightTerm(), p.getThreshold2());
				filterExpression = null;
				setThreshold(theta);
				fullExpression = "AND(" + leftSpec.fullExpression + "|" + p.getThreshold1() + ","
						+ rightSpec.fullExpression + "|" + p.getThreshold2() + ")";
			} else if (p.getOperator().equalsIgnoreCase(MIN)) {
				setOperator(LogicOperator.AND);
				leftSpec.readSpec(p.getLeftTerm(), theta);
				rightSpec.readSpec(p.getRightTerm(), theta);
				filterExpression = null;
				setThreshold(theta);
				fullExpression = "AND(" + leftSpec.fullExpression + "|" + p.getThreshold1() + ","
						+ rightSpec.fullExpression + "|" + p.getThreshold2() + ")";
			} else if (p.getOperator().equalsIgnoreCase(OR)) {
				setOperator(LogicOperator.OR);
				leftSpec.readSpec(p.getLeftTerm(), p.getThreshold1());
				rightSpec.readSpec(p.getRightTerm(), p.getThreshold2());
				filterExpression = null;
				setThreshold(theta);
				fullExpression = "OR(" + leftSpec.fullExpression + "|" + p.getThreshold1() + ","
						+ rightSpec.fullExpression + "|" + p.getThreshold2() + ")";
			} else if (p.getOperator().equalsIgnoreCase(MAX)) {
				setOperator(LogicOperator.OR);
				leftSpec.readSpec(p.getLeftTerm(), theta);
				rightSpec.readSpec(p.getRightTerm(), theta);
				filterExpression = null;
				setThreshold(theta);
				fullExpression = "OR(" + leftSpec.fullExpression + "|" + p.getThreshold1() + ","
						+ rightSpec.fullExpression + "|" + p.getThreshold2() + ")";
			} else if (p.getOperator().equalsIgnoreCase(XOR)) {
				setOperator(LogicOperator.XOR);
				leftSpec.readSpec(p.getLeftTerm(), p.getThreshold1());
				rightSpec.readSpec(p.getRightTerm(), p.getThreshold2());
				filterExpression = null;
				setThreshold(theta);
				fullExpression = "XOR(" + leftSpec.fullExpression + "|" + p.getThreshold1() + ","
						+ rightSpec.fullExpression + "|" + p.getThreshold2() + ")";
			} else if (p.getOperator().equalsIgnoreCase(MINUS)) {
				setOperator(LogicOperator.MINUS);
				leftSpec.readSpec(p.getLeftTerm(), p.getThreshold1());
				rightSpec.readSpec(p.getRightTerm(), p.getThreshold2());
				filterExpression = null;
				setThreshold(theta);
				fullExpression = "MINUS(" + leftSpec.fullExpression + "|" + p.getThreshold1() + ","
						+ rightSpec.fullExpression + "|" + p.getThreshold2() + ")";
			} else if (p.getOperator().equalsIgnoreCase(ADD)) {
				setOperator(LogicOperator.AND);
				leftSpec.readSpec(p.getLeftTerm(), Math.abs(theta - p.getRightCoefficient()) / p.getLeftCoefficient());
				rightSpec.readSpec(p.getRightTerm(),
						Math.abs(theta - p.getLeftCoefficient()) / p.getRightCoefficient());
				filterExpression = null;
				setThreshold(theta);
				fullExpression = "AND(" + leftSpec.fullExpression + "|"
						+ Math.abs(theta - p.getRightCoefficient()) / p.getLeftCoefficient() + ","
						+ rightSpec.fullExpression + "|"
						+ Math.abs(theta - p.getLeftCoefficient()) / p.getRightCoefficient() + ")";

			} else if (p.getOperator().equalsIgnoreCase(LUKT)) {
				setOperator(LogicOperator.LUKASIEWICZT);
				leftSpec.readSpec(p.getLeftTerm(), p.getThreshold1());
				rightSpec.readSpec(p.getRightTerm(), p.getThreshold2());
				filterExpression = null;
				setThreshold(theta);
				fullExpression = "LUKT(" + leftSpec.fullExpression + "|" + p.getThreshold1() + ","
						+ rightSpec.fullExpression + "|" + p.getThreshold2() + ")";
			} else if (p.getOperator().equalsIgnoreCase(LUKTCO)) {
				setOperator(LogicOperator.LUKASIEWICZTCO);
				leftSpec.readSpec(p.getLeftTerm(), p.getThreshold1());
				rightSpec.readSpec(p.getRightTerm(), p.getThreshold2());
				filterExpression = null;
				setThreshold(theta);
				fullExpression = "LUKTCO(" + leftSpec.fullExpression + "|" + p.getThreshold1() + ","
						+ rightSpec.fullExpression + "|" + p.getThreshold2() + ")";
			} else if (p.getOperator().equalsIgnoreCase(LUKDIFF)) {
				setOperator(LogicOperator.LUKASIEWICZDIFF);
				leftSpec.readSpec(p.getLeftTerm(), p.getThreshold1());
				rightSpec.readSpec(p.getRightTerm(), p.getThreshold2());
				filterExpression = null;
				setThreshold(theta);
				fullExpression = "LUKDIFF(" + leftSpec.fullExpression + "|" + p.getThreshold1() + ","
						+ rightSpec.fullExpression + "|" + p.getThreshold2() + ")";
			} else if (p.getOperator().equalsIgnoreCase(ALGT)) {
				setOperator(LogicOperator.ALGEBRAICT);
				leftSpec.readSpec(p.getLeftTerm(), p.getThreshold1());
				rightSpec.readSpec(p.getRightTerm(), p.getThreshold2());
				filterExpression = null;
				setThreshold(theta);
				fullExpression = "ALGT(" + leftSpec.fullExpression + "|"
						+ p.getThreshold1() + "," + rightSpec.fullExpression
						+ "|" + p.getThreshold2() + ")";
			} else if (p.getOperator().equalsIgnoreCase(ALGTCO)) {
				setOperator(LogicOperator.ALGEBRAICTCO);
				leftSpec.readSpec(p.getLeftTerm(), p.getThreshold1());
				rightSpec.readSpec(p.getRightTerm(), p.getThreshold2());
				filterExpression = null;
				setThreshold(theta);
				fullExpression = "ALGTCO(" + leftSpec.fullExpression + "|"
						+ p.getThreshold1() + "," + rightSpec.fullExpression
						+ "|" + p.getThreshold2() + ")";
			} else if (p.getOperator().equalsIgnoreCase(ALGDIFF)) {
				setOperator(LogicOperator.ALGEBRAICDIFF);
				leftSpec.readSpec(p.getLeftTerm(), p.getThreshold1());
				rightSpec.readSpec(p.getRightTerm(), p.getThreshold2());
				filterExpression = null;
				setThreshold(theta);
				fullExpression = "ALGDIFF(" + leftSpec.fullExpression + "|"
						+ p.getThreshold1() + "," + rightSpec.fullExpression
						+ "|" + p.getThreshold2() + ")";
			} else if (p.getOperator().equalsIgnoreCase(EINT)) {
				setOperator(LogicOperator.EINSTEINT);
				leftSpec.readSpec(p.getLeftTerm(), p.getThreshold1());
				rightSpec.readSpec(p.getRightTerm(), p.getThreshold2());
				filterExpression = null;
				setThreshold(theta);
				fullExpression = "EINT(" + leftSpec.fullExpression + "|"
						+ p.getThreshold1() + "," + rightSpec.fullExpression
						+ "|" + p.getThreshold2() + ")";
			} else if (p.getOperator().equalsIgnoreCase(EINTCO)) {
				setOperator(LogicOperator.EINSTEINTCO);
				leftSpec.readSpec(p.getLeftTerm(), p.getThreshold1());
				rightSpec.readSpec(p.getRightTerm(), p.getThreshold2());
				filterExpression = null;
				setThreshold(theta);
				fullExpression = "EINTCO(" + leftSpec.fullExpression + "|"
						+ p.getThreshold1() + "," + rightSpec.fullExpression
						+ "|" + p.getThreshold2() + ")";
			} else if (p.getOperator().equalsIgnoreCase(EINDIFF)) {
				setOperator(LogicOperator.EINSTEINDIFF);
				leftSpec.readSpec(p.getLeftTerm(), p.getThreshold1());
				rightSpec.readSpec(p.getRightTerm(), p.getThreshold2());
				filterExpression = null;
				setThreshold(theta);
				fullExpression = "EINDIFF(" + leftSpec.fullExpression + "|"
						+ p.getThreshold1() + "," + rightSpec.fullExpression
						+ "|" + p.getThreshold2() + ")";
			} else if (p.getOperator().toUpperCase().startsWith(HAMTCO)) {
				setOperator(LogicOperator.HAMACHERTCO);
				leftSpec.readSpec(p.getLeftTerm(), p.getThreshold1());
				rightSpec.readSpec(p.getRightTerm(), p.getThreshold2());
				filterExpression = null;
				setThreshold(theta);
				operatorParameter = getParameter(p.getOperator().toUpperCase(), HAMTCO);
				fullExpression = "HAMTCO_" + operatorParameter + "(" + leftSpec.fullExpression + "|" + p.getThreshold1()
				+ "," + rightSpec.fullExpression + "|" + p.getThreshold2() + ")";
			} else if (p.getOperator().toUpperCase().startsWith(HAMT)) {
				setOperator(LogicOperator.HAMACHERT);
				leftSpec.readSpec(p.getLeftTerm(), p.getThreshold1());
				rightSpec.readSpec(p.getRightTerm(), p.getThreshold2());
				filterExpression = null;
				setThreshold(theta);
				operatorParameter = getParameter(p.getOperator().toUpperCase(), HAMT);
				fullExpression = "HAMT_" + operatorParameter + "(" + leftSpec.fullExpression + "|" + p.getThreshold1()
				+ "," + rightSpec.fullExpression + "|" + p.getThreshold2() + ")";
			} else if (p.getOperator().toUpperCase().startsWith(HAMDIFF)) {
				setOperator(LogicOperator.HAMACHERDIFF);
				leftSpec.readSpec(p.getLeftTerm(), p.getThreshold1());
				rightSpec.readSpec(p.getRightTerm(), p.getThreshold2());
				filterExpression = null;
				setThreshold(theta);
				operatorParameter = getParameter(p.getOperator().toUpperCase(), HAMDIFF);
				fullExpression = "HAMDIFF_" + operatorParameter + "(" + leftSpec.fullExpression + "|"
						+ p.getThreshold1() + "," + rightSpec.fullExpression + "|" + p.getThreshold2() + ")";
			}
		}
	}

	private double getParameter(String opWithParameter, String op) {
		return Double.valueOf(opWithParameter.replace(op + "_", ""));
	}

	/**
	 * Returns all leaves of the link spec
	 *
	 * @return List of atomic spec, i.e., all leaves of the link spec
	 */
	public List<LinkSpecification> getAllLeaves() {
		List<LinkSpecification> allLeaves = new ArrayList<>();
		if (isAtomic()) {
			allLeaves.add(this);
		} else {
			for (LinkSpecification child : getChildren()) {
				allLeaves.addAll(child.getAllLeaves());
			}
		}
		return allLeaves;
	}

	public LinkSpecification setLeaf(LinkSpecification leaf, int n) {
		LinkSpecification clone = clone();
		clone.setLeaf(leaf, null, n);
		return clone;
	}

	private void setLeaf(LinkSpecification leaf, LinkSpecification parent, int n) {
		List<LinkSpecification> children = getChildren();
		int leftSize = children.get(0).getAllLeaves().size();
		if (n < leftSize) {
			if (children.get(0).isAtomic()) {
				leaf.setParent(this);
				children.set(0, leaf);
				setChildren(children);
				return;
			}
			children.get(0).setLeaf(leaf, this, n);
		} else {
			if (children.get(1).isAtomic()) {
				leaf.setParent(this);
				children.set(1, leaf);
				setChildren(children);
				return;
			}
			children.get(1).setLeaf(leaf, this, n - leftSize);
		}
	}

	/**
	 * Returns size of the spec, i.e., 1 for atomic spec, 0 for empty spec and
	 * else 1 + sum of size of all children
	 *
	 * @return Size of the current spec
	 */
	public int size() {
		int size = 1;
		if (isEmpty()) {
			return 0;
		}
		if (isAtomic()) {
			return 1;
		} else {
			for (LinkSpecification c : getChildren()) {
				size = size + c.size();
			}
		}
		return size;
	}

	/**
	 * Computes a hashCode for the current spec
	 *
	 * @return Hash code
	 */
	@Override
	public int hashCode() {
		int res = new Random().nextInt();
		return res;
	}

	/**
	 * Generates a clone of the current spec
	 *
	 * @return Clone of current spec
	 */
	@Override
	public LinkSpecification clone() {
		LinkSpecification clone = new LinkSpecification();
		clone.setThreshold(threshold);
		clone.setLowThreshold(lowThreshold);
		clone.setOperator(operator);
		clone.filterExpression = filterExpression;
		clone.fullExpression = fullExpression;
		clone.prop1 = prop1;
		clone.prop2 = prop2;
		clone.atomicMeasure = atomicMeasure;
		List<LinkSpecification> l = new ArrayList<>();
		LinkSpecification childCopy;
		if (getChildren() != null) {
			for (LinkSpecification c : getChildren()) {
				childCopy = c.clone();
				clone.addChild(childCopy);
				childCopy.parent = clone;
				l.add(childCopy);
			}
		}

		return clone;
	}

	/**
	 * @return A string representation of the spec
	 */
	@Override
	public String toString() {
		if (getChildren() != null) {
			String str = "(" + filterExpression + ", " + getThreshold() + ", " + getOperator() + ", null,)";
			for (LinkSpecification child : getChildren()) {

				str += "\n  ->" + child;
			}
			return str;
		}
		else {
			return "(" + filterExpression + ", " + getThreshold() + ", " + getOperator() + ", null)";
			// }
		}
	}

	public int getDepth() {
		if (parent == null) {
			return 0;
		}
		return parent.getDepth() + 1;
	}

	public String toStringPretty() {
		if (getChildren() != null) {
			String str = "(" + filterExpression + ", " + getThreshold() + ", " + getOperator() + ", null,)";
			String indent = new String(new char[getDepth()]).replace("\0", "\t");
			for (LinkSpecification child : getChildren()) {
				str += "\n  " + indent + " ->" + child.toStringPretty();
			}
			return str;
		}
		else {
			return "(" + filterExpression + ", " + getThreshold() + ", " + getOperator() + ", null)";
			// }
		}
	}

	/**
	 * @return A string representation of the spec in a single line
	 */
	public String toStringOneLine() {
		if (getChildren() != null) {
			String str = "(" + getShortendFilterExpression() + ", " + getThreshold() + ", " + getOperator()
			+ ", null,)";
			str += "{";
			for (LinkSpecification child : getChildren()) {
				str += child.toStringOneLine() + ",";
			}
			str += "}";
			return str;
		}
		else {
			return "(" + getShortendFilterExpression() + ", " + getThreshold() + ", " + getOperator() + ", null)";
			// }
		}
	}

	/**
	 * Checks whether the current node is the root of the whole spec
	 *
	 * @return True if root, else false
	 */
	public boolean isRoot() {
		return parent == null;
	}

	/**
	 * @return the filter expression implemented in the spec
	 */
	public String getMeasure() {
		if (isAtomic()) {
			return filterExpression;
		} else {
			return getOperator() + "(" + ")";
		}
	}

	@Override
	public boolean equals(Object other) {
		LinkSpecification o = (LinkSpecification) other;

		if (isAtomic() && o.isAtomic()) {
			if (filterExpression == null && o.filterExpression == null) {
				return true;
			}
			if (filterExpression != null && o.filterExpression == null) {
				return false;
			}
			if (filterExpression == null && o.filterExpression != null) {
				return false;
			}
			if (filterExpression.equalsIgnoreCase(o.filterExpression)) {
				return Math.abs(getThreshold() - o.getThreshold()) < 0.001d;
			}

		} else if (!isAtomic() && !o.isAtomic()) {
			if (getOperator() == null && o.getOperator() != null) {
				return false;
			}
			if (getOperator() != null && o.getOperator() == null) {
				return false;
			}
			if (getOperator() == null && o.getOperator() == null) {
				return true;
			}
			if (getOperator().equals(o.getOperator())) {
				if (getChildren() == null && o.getChildren() == null) {
					return true;
				}
				if (getChildren() != null && o.getChildren() == null) {
					return false;
				}
				if (getChildren() == null && o.getChildren() != null) {
					return false;
				}
				// HashSet<LinkSpecification> hs = new
				// HashSet<LinkSpecification>();
				// if (this.getChildren() != null)
				// hs.addAll(getChildren());
				// return (!hs.addAll(o.getChildren()));
				return getChildren().equals(o.getChildren());
			} // not equal operators
			return false;

		} else {
			return false;
		}
		return false;

	}

	@Override
	public int compareTo(Object o) {

		LinkSpecification other = (LinkSpecification) o;

		// logger.info("LinkSpecification.compareTo: this="+this+"
		// -other="+other);
		if (other.size() > size()) {
			return -1;
		}
		if (other.size() < size()) {
			return 1;
		}
		if (isEmpty() && other.isEmpty()) {
			return 0;
		}
		// size is equal
		// if(!this.isAtomic() && !other.isAtomic()) {
		// return 0;
		// }
		if (isAtomic() && other.isAtomic()) {
			if (getThreshold() > other.getThreshold()) {
				return 1;
			}
			if (getThreshold() < other.getThreshold()) {
				return -1;
			}
			if (filterExpression == null && other.filterExpression != null) {
				return -1;
			}
			if (filterExpression != null && other.filterExpression == null) {
				return 1;
			}
			if (filterExpression == null && other.filterExpression == null) {
				return 0;
			}
			return filterExpression.compareToIgnoreCase(other.filterExpression);
		} else { // even size, non atomic
			if (getOperator() == other.getOperator()) {

				// same operators
				if (getAllLeaves().size() == other.getAllLeaves().size()) {
					List<LinkSpecification> leaves = getAllLeaves();
					List<LinkSpecification> otherLeaves = other.getAllLeaves();
					for (int i = 0; i < leaves.size(); i++) {
						if (leaves.get(i).compareTo(otherLeaves.get(i)) != 0) {
							return leaves.get(i).compareTo(otherLeaves.get(i));
						}
					}
					return 0;
				} else {
					return getAllLeaves().size() - other.getAllLeaves().size();
				}
			} else {
				// non-atomic, different operators
				return getOperator().compareTo(other.getOperator());
			}
		}
	}

	public String getShortendFilterExpression() {
		if (filterExpression == null) {
			return null;
		}
		if (filterExpression.length() <= 0) {
			return "";
		}
		if (!isAtomic()) {
			return filterExpression;
		}
		// "trigrams(x.prop1,y.prop2)" expect something like this...
		int beginProp1 = filterExpression.indexOf("(");
		int brakeProp = filterExpression.indexOf(",");
		int endProp2 = filterExpression.indexOf(")");
		String measure = filterExpression.substring(0, beginProp1);
		String prop1 = filterExpression.substring(beginProp1 + 1, brakeProp);
		String prop2 = filterExpression.substring(brakeProp + 1, endProp2);
		if (prop1.lastIndexOf("#") != -1) {
			prop1 = prop1.substring(prop1.lastIndexOf("#") + 1);
		} else if (prop1.lastIndexOf("/") != -1) {
			prop1 = prop1.substring(prop1.lastIndexOf("/") + 1);
		}
		if (prop2.lastIndexOf("#") != -1) {
			prop2 = prop2.substring(prop2.lastIndexOf("#") + 1);
		} else if (prop2.lastIndexOf("/") != -1) {
			prop2 = prop2.substring(prop2.lastIndexOf("/") + 1);
		}
		DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ENGLISH);
		df.applyPattern("#,###,######0.00");
		return measure + "(" + prop1 + "," + prop2 + ")|" + df.format(getThreshold());
	}

	public String getFilterExpression() {
		return filterExpression;
	}

	/**
	 * @return the atomicMeasure
	 */
	public String getAtomicMeasure() {
		if (isAtomic()) {
			if (atomicMeasure.length() > 0) {
				return atomicMeasure;
			} else {
				return filterExpression.substring(0, filterExpression.indexOf("("));
			}
		} else {
			return null;
		}
	}

	/**
	 * @param atomicMeasure
	 *            the atomicMeasure to set
	 */
	public void setAtomicMeasure(String atomicMeasure) {
		this.atomicMeasure = atomicMeasure;
	}

	/**
	 * Checks of at least two leaves compare the same properties, possibly with
	 * different measures though.
	 *
	 * @return true if two leaves compare the same properties, possibly with
	 *         different measures
	 */
	public boolean containsRedundantProperties() {
		List<LinkSpecification> leaves = getAllLeaves();
		HashSet<String> props = new HashSet<>();
		for (LinkSpecification leave : leaves) {
			String propStr = leave.prop1 + "_" + leave.prop2;
			if (!props.add(propStr)) {
				return true;
			}
		}
		return false;
	}

	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public LogicOperator getOperator() {
		return operator;
	}

	public void setOperator(LogicOperator operator) {
		this.operator = operator;
	}

	public List<LinkSpecification> getChildren() {
		return children;
	}

	public void setChildren(List<LinkSpecification> children) {
		this.children = children;
	}

	public List<LinkSpecification> getDependencies() {
		return dependencies;
	}

	public void setDependencies(List<LinkSpecification> dependencies) {
		this.dependencies = dependencies;
	}

	public String getFullExpression() {
		return fullExpression;
	}

	public void setFullExpression(String fullExpression) {
		this.fullExpression = fullExpression;
	}

	public LinkSpecification getParent() {
		return parent;
	}

	public void setParent(LinkSpecification parent) {
		this.parent = parent;
	}

	public String getProperty1() {
		return prop1;
	}

	public String getProperty2() {
		return prop2;
	}

	public double getQuality() {
		return quality;
	}

	public void setQuality(double quality) {
		this.quality = quality;
	}

	public double getLowThreshold() {
		return lowThreshold;
	}

	public void setLowThreshold(double lowThreshold) {
		this.lowThreshold = lowThreshold;
	}

	public double getOperatorParameter() {
		return operatorParameter;
	}

	public void setOperatorParameter(double operatorParameter) {
		this.operatorParameter = operatorParameter;
	}

}
