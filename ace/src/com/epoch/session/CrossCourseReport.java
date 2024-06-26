package com.epoch.session;

import com.epoch.assgts.Assgt;
import com.epoch.db.HWRead;
import com.epoch.evals.EvalResult;
import com.epoch.exceptions.DBException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** For storing student response results from different courses.  */
public final class CrossCourseReport {

	/** Contains map of student results by Q number by assignment number by
	 * course number by student login ID. */
	final private Map<String, // student
			LinkedHashMap<Integer, // course
				LinkedHashMap<int[], // assignment ID and number
					LinkedHashMap<Integer, // Q number
						EvalResult>>>> allResults =
			new LinkedHashMap<String,
				LinkedHashMap<Integer,
					LinkedHashMap<int[],
						LinkedHashMap<Integer,
							EvalResult>>>>();

	/** Constructor. */
	public CrossCourseReport() {
		// empty
	}

	/** Adds a result to the report.
	 * @param	studentId	login ID of the student
	 * @param	courseId	course ID number
	 * @param	hwIdAndNum	ID number and 1-based serial number of the assignment
	 * @param	qNum	1-based serial number of the question in the assignment
	 * @param	result	the evaluation result
	 */
	public void addResult(String studentId, int courseId, int[] hwIdAndNum,
			int qNum, EvalResult result) {
		LinkedHashMap<Integer, LinkedHashMap<int[],
				LinkedHashMap<Integer, EvalResult>>> studentResults =
					allResults.get(studentId);
		if (studentResults == null) {
			studentResults = new LinkedHashMap<Integer, 
					LinkedHashMap<int[], LinkedHashMap<Integer, 
						EvalResult>>>();
			allResults.put(studentId, studentResults);
		} // if this is first student result
		final Integer courseIdObj = Integer.valueOf(courseId);
		LinkedHashMap<int[], LinkedHashMap<Integer, 
				EvalResult>> studentCrsResults = 
					studentResults.get(courseIdObj);
		if (studentCrsResults == null) {
			studentCrsResults = new LinkedHashMap<int[], 
					LinkedHashMap<Integer, EvalResult>>();
			studentResults.put(courseIdObj, studentCrsResults);
		} // if this is first student result for the course
		LinkedHashMap<Integer, EvalResult> studentCrsHWResults = 
				studentCrsResults.get(hwIdAndNum);
		if (studentCrsHWResults == null) {
			studentCrsHWResults = 
					new LinkedHashMap<Integer, EvalResult>();
			studentCrsResults.put(hwIdAndNum, studentCrsHWResults);
		} // if this is first student result for the course
		studentCrsHWResults.put(Integer.valueOf(qNum), result);
	} // addResult(String, int, int[], int, EvalResult)

	/** Gets the login IDs of the students.
	 * @return	list of login IDs in the order they were added to the map
	 */
	public List<String> getStudentIds() {
		return new ArrayList<String>(allResults.keySet());
	} // getStudentIds()

	/** Gets the course ID numbers of a student for which there are results.
	 * @param	studentId	login ID of the student
	 * @return	list of course IDs in the order they were added to the map
	 */
	public List<Integer> getCourseIds(String studentId) {
		final LinkedHashMap<Integer, LinkedHashMap<int[],
				LinkedHashMap<Integer, EvalResult>>> studentResults =
					allResults.get(studentId);
		return (studentResults == null ? new ArrayList<Integer>()
				: new ArrayList<Integer>(studentResults.keySet()));
	} // getCourseIds(String)

	/** Gets the assignment IDs and numbers in a course of a student for which 
	 * there are results.
	 * @param	studentId	login ID of the student
	 * @param	courseId	course ID number
	 * @return	list of assignment IDs and numbers in the order they were added
	 * to the map
	 */
	public List<int[]> getHWIdsAndNums(String studentId, int courseId) {
		final LinkedHashMap<Integer, LinkedHashMap<int[],
				LinkedHashMap<Integer, EvalResult>>> studentResults =
					allResults.get(studentId);
		if (studentResults == null) return new ArrayList<int[]>();
		final Integer crsIdObj = Integer.valueOf(courseId);
		final LinkedHashMap<int[], LinkedHashMap<Integer, EvalResult>> 
				studentCrsResults = studentResults.get(crsIdObj);
		return (studentCrsResults == null ? new ArrayList<int[]>()
				: new ArrayList<int[]>(studentCrsResults.keySet()));
	} // getHWIdsAndNums(String, int)

	/** Gets the question numbers in an assignment in a course of a student for 
	 * which there are results.
	 * @param	studentId	login ID of the student
	 * @param	courseId	course ID number
	 * @param	hwIdAndNum	assignment ID and number
	 * @return	list of question numbers in the order they were added to the
	 * map
	 */
	public List<Integer> getQNums(String studentId, int courseId, 
			int[] hwIdAndNum) {
		final LinkedHashMap<Integer, LinkedHashMap<int[],
				LinkedHashMap<Integer, EvalResult>>> studentResults =
					allResults.get(studentId);
		if (studentResults == null) return new ArrayList<Integer>();
		final Integer crsIdObj = Integer.valueOf(courseId);
		final LinkedHashMap<int[], LinkedHashMap<Integer, EvalResult>> 
				studentCrsResults = studentResults.get(crsIdObj);
		if (studentCrsResults == null) return new ArrayList<Integer>();
		final LinkedHashMap<Integer, EvalResult> studentCrsHWResults = 
				studentCrsResults.get(hwIdAndNum);
		return (studentCrsHWResults == null ? new ArrayList<Integer>()
				: new ArrayList<Integer>(studentCrsHWResults.keySet()));
	} // getQNums(String, int, int[])

	/** Gets the result of a student's response to a question.
	 * @param	studentId	login ID of the student
	 * @param	courseId	course ID number
	 * @param	hwIdAndNum	assignment ID and number
	 * @param	qNum	1-based serial number of the question in the assignment
	 * @return	result of student response
	 */
	public EvalResult getResult(String studentId, int courseId, 
			int[] hwIdAndNum, int qNum) {
		final LinkedHashMap<Integer, LinkedHashMap<int[],
				LinkedHashMap<Integer, EvalResult>>> studentResults =
					allResults.get(studentId);
		if (studentResults == null) return null;
		final Integer crsIdObj = Integer.valueOf(courseId);
		final LinkedHashMap<int[], LinkedHashMap<Integer, EvalResult>> 
				studentCrsResults = studentResults.get(crsIdObj);
		if (studentCrsResults == null) return null;
		final LinkedHashMap<Integer, EvalResult> studentCrsHWResults = 
				studentCrsResults.get(hwIdAndNum);
		if (studentCrsHWResults == null) return null;
		return studentCrsHWResults.get(Integer.valueOf(qNum));
	} // getResult(String, int, int[], int)

	/** Read assignment description with the given ID.
	 * @param	hwId	assignment ID number
	 * @return	an assignment description
	 * @throws	DBException	if there's a problem reading the database
	 */
	public Assgt getHW(int hwId) throws DBException {
		return HWRead.getHW(hwId);
	} // getHW(int)

	/** Gets if no results were found.
	 * @return	true if there are no results
	 */
	public boolean isEmpty() {
		return allResults.isEmpty();
	} // isEmpty()

} // CrossCourseReport
