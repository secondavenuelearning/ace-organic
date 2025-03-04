package com.epoch.db;

import chemaxon.formats.MolFormatException;
import chemaxon.jchem.db.DatabaseSearchException;
import chemaxon.jchem.db.JChemSearch;
import chemaxon.jchem.db.UpdateHandler;
import chemaxon.sss.SearchConstants;
import chemaxon.sss.search.JChemSearchOptions;
import chemaxon.util.ConnectionHandler;
import com.epoch.exceptions.DBException;
import com.epoch.utils.Utils;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.tomcat.dbcp.dbcp2.DelegatingConnection;

/** Stores and retrieves results of Reactor calculations. */
public final class ReactorResultsRW extends DBCommon 
		implements SearchConstants {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** JChem table for storing the results of reactor calculations.  In
	 * addition to the columns below, it includes a whole slew of columns to 
	 * which we never refer ourselves.  */
	private static final String REACTOR_RESULTS = "reactor_results_v4";
		/** Field in REACTOR_RESULTS that is created by JChem. */
		private static final String REACT_SM_ID = "cd_id";
		/** Our own field in REACTOR_RESULTS. */
		private static final String REACT_RXN_ID = "rxn_id";
		/** Our own field in REACTOR_RESULTS. */
		private static final String REACT_CALCD_PRODS = "calcd_products";
	/** Name of JChem property table. */
	private static final String JCHEM_PROPERTY_TABLE =
			ConnectionHandler.DEFAULT_PROPERTY_TABLE;

	/** Stores the products, calculated by Reactor, of a particular set of 
	 * reaction conditions operating on a particular molecule or group of 
	 * molecules.  Called from SynthSolver.
	 * <p>According to 
	 * <a href="https://www.chemaxon.com/forum/ftopic7311.html">this</a>
	 * and
	 * <a href="https://forums.oracle.com/forums/thread.jspa?threadID=279238">this</a>
	 * discussion, we need to get a ConnectionHandler that uses the inner 
	 * OracleConnection of the pooled connection, but later we need to close 
	 * the outer pooled connection without closing the inner one.
	 * <p><b>Note</b>: Synchronization of this method requires any call to this
	 * method to be complete before another call to this method can be executed.
	 * @param	molStr	starting material(s) of the reaction
	 * @param	rxnId	the ID number of the reaction conditions operating on
	 * the molecule or group of molecules
	 * @param	calcdProdStrs	the products calculated by Reactor, as an array
	 * of strings; normally a single string that contains all the products
	 * fused, but can contain multiple strings for products that have properties
	 * that need to be preserved
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public synchronized static void addCalcdProducts(String molStr, int rxnId, 
			String[] calcdProdStrs) throws DBException {
		final String SELF = "ReactorResultsRW.addCalcdProducts: ";
		Connection con = null;
		UpdateHandler uh = null;
		try {
			con = getPoolConnection();
			final ConnectionHandler conHandler = getConHandler(con);
			if (conHandler != null) {
				final String rxnName = getName(con, rxnId);
				if (Utils.isEmpty(getCompoundIds(conHandler, molStr, rxnId))) {
					final String addlCols = 
							joinAll(REACT_RXN_ID, REACT_CALCD_PRODS).toString();
					debugPrint(SELF + "storing ", calcdProdStrs.length, 
							" calculated product(s) for starting materials ", 
							molStr, " and reaction ", rxnName, " (ID ", rxnId, 
							"):\n", calcdProdStrs);
					uh = new UpdateHandler(conHandler, UpdateHandler.INSERT, 
							REACTOR_RESULTS, addlCols);
					uh.setDuplicateFiltering(
							UpdateHandler.DUPLICATE_FILTERING_OFF);
					for (final String calcdProd : calcdProdStrs) {
						uh.setStructure(molStr);
						uh.setValueForAdditionalColumn(1, rxnId);
						uh.setValueForAdditionalColumn(2, calcdProd);
						uh.execute();
					} // for each calculated product
				} else alwaysPrint(SELF + "record already exists for starting "
						+ "materials ", molStr, " and reaction ", rxnName, 
						" (ID ", rxnId, "), so calculated products not "
						+ "written."); // shouldn't happen often
			} else {
				alwaysPrint(SELF + "can't get innermost Oracle connection; "
						+ "need to add data to table in two steps.");
				addCalcdProductsTwoSteps(molStr, rxnId, calcdProdStrs);
			} // if we got a good connection handler
		} catch (SQLException e) {
			alwaysPrint(SELF + "caught SQLException trying to store "
					+ "reaction of ", molStr, " under rxn conditions ", 
					rxnId, " giving products ", calcdProdStrs);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			alwaysPrint(SELF + "caught Exception trying to store "
					+ "reaction of ", molStr, " under rxn conditions ", 
					rxnId, " giving products ", calcdProdStrs);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			try {
				if (uh != null) uh.close();
			} catch (SQLException e) {
				debugPrint(SELF + "SQLException");
			}
			closeConnection(con);
		} // try
	} // addCalcdProducts(String, int, String[])

	/** Stores the products, calculated by Reactor, of a particular set of 
	 * reaction conditions operating on a particular molecule or group of 
	 * molecules.  Called only when addCalcdProducts() fails because we can't
	 * get the inner OracleConnection from the pool Connection.
	 * @param	molStr	starting material(s) of the reaction
	 * @param	rxnId	the ID number of the reaction conditions operating on
	 * the molecule or group of molecules
	 * @param	calcdProdStrs	the products calculated by Reactor, as strings
	 * @throws	DBException	if there's a problem writing to the database
	 */
	private synchronized static void addCalcdProductsTwoSteps(String molStr, 
			int rxnId, String[] calcdProdStrs) throws DBException {
		final String SELF = "ReactorResultsRW.addCalcdProductsTwoSteps: ";
		final String qry = 
				UPDATE + REACTOR_RESULTS 
				+ SET + REACT_CALCD_PRODS + EQUALS + QMARK
				+ WHERE + REACT_SM_ID + EQUALS + QMARK
				+ AND + REACT_RXN_ID + EQUALS + QMARK;
		debugPrint(SELF, qry);
		UpdateHandler uh = null;
		ConnectionHandler conHandler = null;
		Connection con = null;
		PreparedStatement pstmt = null;
		try {
			conHandler = getConHandler();
			con = conHandler.getConnection();
			final String rxnName = getName(conHandler, rxnId);
			if (Utils.isEmpty(getCompoundIds(conHandler, molStr, rxnId))) {
				con.setAutoCommit(false);
				pstmt = con.prepareStatement(qry);
				debugPrint(SELF + "inserting starting materials ", 
						molStr, " and reaction ", rxnName, " (ID ", 
						rxnId, ") into JChem table.");
				uh = new UpdateHandler(conHandler, UpdateHandler.INSERT, 
						REACTOR_RESULTS, REACT_RXN_ID);
				uh.setDuplicateFiltering(UpdateHandler.DUPLICATE_FILTERING_OFF);
				int batchNum = 0;
				for (final String calcdProd : calcdProdStrs) {
					uh.setStructure(molStr);
					uh.setValueForAdditionalColumn(1, rxnId);
					final int smId = uh.execute(true);
					debugPrint(SELF + "adding calculated products ", 
							Utils.isEmpty(calcdProd) ? "[none]" : calcdProd, 
							" to JChem table row with starting materials ID ", 
							smId, " and reaction ID ", rxnId);
					final StringBuilder joinedValues = setValues(pstmt, 
							calcdProd, 
							smId, 
							rxnId);
					debugPrint(SELF, "batch ", batchNum++, ": ", joinedValues); 
					pstmt.addBatch();
				} // for each calculated product
				pstmt.executeBatch();
				uh.saveUpdateLogs(); // ChemAxon requires when autoCommit is off
				con.commit();
			} else alwaysPrint(SELF + "record already exists for starting "
					+ "materials ", molStr, " and reaction ", rxnName, 
					" (ID ", rxnId, "), so calculated products not "
					+ "written."); // shouldn't happen often
		} catch (SQLException e) {
			alwaysPrint(SELF + "caught SQLException.");
			e.printStackTrace();
			rollbackConnection(con);
			throw new DBException(e.getMessage());
		} finally {
			closeConnection(null, pstmt, null);
			try {
				if (uh != null) uh.close();
			} catch (SQLException e) {
				debugPrint(SELF + "SQLException");
			}
			closeConHandler(conHandler);
		} // try
	} // addCalcdProductsTwoSteps(String, int, String[])

	/** Deletes the products, calculated by Reactor, of a particular set of 
	 * reaction conditions.  Called by SynthDataRW.setRxnCondition() when a
	 * reaction condition is being modified.
	 * @param	con	database connection
	 * @param	rxnId	the ID number of the reaction conditions operating on
	 * the molecule or group of molecules
	 * @throws	DBException	if there's a problem writing to the database
	 */
	static void deleteCalcdProducts(Connection con, int rxnId) 
			throws DBException {
		final String SELF = "ReactorResultsRW.deleteCalcdProducts: ";
		final String where = toString(WHERE + REACT_RXN_ID + EQUALS, rxnId);
		ConnectionHandler conHandler = null;
		try {
			conHandler = getConHandler(con);
			if (conHandler != null) {
				UpdateHandler.deleteRows(conHandler, REACTOR_RESULTS, where);
				alwaysPrint(SELF + "deleted calculated products of reaction ", 
						rxnId);
			} else {
				alwaysPrint(SELF + "could not delete calculated products of "
						+ "reaction ", rxnId, 
						" because connection handler is null.");
			} // if conHandler
		} catch (SQLException e) {
			alwaysPrint(SELF + "caught SQLException while trying to delete "
					+ "products of reaction ", rxnId);
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} // try
	} // deleteCalcdProducts(Connection, int)

	/** Deletes the products, calculated by Reactor, of a particular set of 
	 * reaction conditions operating on particular starting material(s). Called 
	 * by front end.
	 * @param	rxnId	the ID number of the reaction conditions operating on
	 * the starting material(s)
	 * @param	molStr	MRV of the starting material(s) whose products were
	 * calculated
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static synchronized void deleteCalcdProducts(int rxnId, 
			String molStr) throws DBException {
		final String SELF = "ReactorResultsRW.deleteCalcdProducts: ";
		ConnectionHandler conHandler = null;
		try {
			conHandler = getConHandler();
			final int[] hitCpdIds = getCompoundIds(conHandler, molStr, rxnId);
			alwaysPrint(SELF + "deleting ", hitCpdIds.length, 
					" record(s) of starting material(s) ", molStr,
					" undergoing reaction ", rxnId);
			int rowNum = 0;
			for (final int hitCpdId : hitCpdIds) {
				UpdateHandler.deleteRow(conHandler, REACTOR_RESULTS, hitCpdId);
				alwaysPrint(SELF + "deleted record ", ++rowNum, '.');
			} // for each hit
		} catch (SQLException e) {
			alwaysPrint(SELF + "caught SQLException.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConHandler(conHandler);
		} // try
	} // deleteCalcdProducts(int, String)

	/** Deletes all the products calculated by Reactor in the table.
	 * Called by admin when JChem is upgraded.
	 * @throws	DBException	if there's a problem writing to the database
	 */
	public static void deleteAllCalcdProducts() throws DBException {
		final String SELF = "ReactorResultsRW.deleteAllCalcdProducts: ";
		ConnectionHandler conHandler = null;
		try {
			conHandler = getConHandler();
			UpdateHandler.deleteRows(conHandler, REACTOR_RESULTS, "");
			alwaysPrint(SELF + "deleted calculated products of all reactions.");
		} catch (SQLException e) {
			alwaysPrint(SELF + "caught SQLException.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConHandler(conHandler);
		} // try
	} // deleteAllCalcdProducts()

	/** Finds the products, calculated by Reactor, of a particular set of 
	 * reaction conditions operating on a particular molecule or group of 
	 * molecules.  Should return an array with one or zero members.
	 * @param	molStr	a molecule or group of molecules
	 * @param	rxnId	the ID number of the reaction conditions operating on
	 * the molecule or group of molecules
	 * @return	all sets of products calculated by Reactor (should be only one)
	 * @throws	DBException	if there's a problem reading the database
	 */
	public static String[] getCalcdProducts(String molStr, int rxnId) 
			throws DBException {
		final String SELF = "ReactorResultsRW.getCalcdProducts: ";
		ConnectionHandler conHandler = null;
		try {
			conHandler = getConHandler();
			return getCalcdProducts(conHandler, molStr, rxnId);
		} catch (SQLException e) {
			alwaysPrint(SELF + "caught SQLException.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} finally {
			closeConHandler(conHandler);
		} // try
	} // getCalcdProducts(String, int)

	/** Finds the products, calculated by Reactor, of a particular set of 
	 * reaction conditions operating on a particular molecule or group of 
	 * molecules.  Should return an array with one or zero members.
	 * @param	molStr	a molecule or group of molecules
	 * @param	rxnId	the ID number of the reaction conditions operating on
	 * the molecule or group of molecules
	 * @param	conHandler	handles connection to the database
	 * @return	all sets of products calculated by Reactor
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static String[] getCalcdProducts(ConnectionHandler conHandler, 
			String molStr, int rxnId) throws DBException {
		final String SELF = "ReactorResultsRW.getCalcdProducts: ";
		final List<String> calcdProdStrs = new ArrayList<String>();
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			final int[] hitCpdIds = getCompoundIds(conHandler, molStr, rxnId);
			debugPrint(SELF + "starting materials cd_ids for rxn ", rxnId,
					": ", hitCpdIds);
			if (!Utils.isEmpty(hitCpdIds)) {
				con = conHandler.getConnection();
				final String qry = toString(
						SELECT + REACT_CALCD_PRODS
						+ FROM + REACTOR_RESULTS
						+ WHERE + REACT_RXN_ID + EQUALS + QMARK
						+ AND + REACT_SM_ID + IN, parensQMarks(hitCpdIds));
				final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
						rxnId,
						hitCpdIds);
				debugPrint(SELF, sql_vals);
				stmt = getStatement(con, sql_vals);
				rs = stmt.executeQuery();
				int prodNum = 0;
				while (rs.next()) {
					final String calcdProd = 
							rs.getString(REACT_CALCD_PRODS);
					debugPrint(SELF + "calculated product ",
							++prodNum, ":\n", calcdProd == null
								? "[none]" : calcdProd);
					calcdProdStrs.add(calcdProd == null ? "" : calcdProd);
				} // while there are results
			} // if the starting materials are in the database already
		} catch (SQLException e) {
			alwaysPrint(SELF + "caught SQLException.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (DBException e) {
			alwaysPrint(SELF + "caught DBException.");
			e.printStackTrace();
			throw e;
		} finally {
			closeStmtAndRs(stmt, rs);
		} // try
		final String rxnName = getName(conHandler, rxnId);
		debugPrint(SELF + "for starting materials ", molStr, 
				" and reaction ", rxnName, " (ID ", rxnId, 
				"), found products: ", calcdProdStrs.isEmpty() 
					? "[none]" : calcdProdStrs);
		return calcdProdStrs.toArray(new String[calcdProdStrs.size()]);
	} // getCalcdProducts(ConnectionHandler, String, int)

	/** Gets the name of a set of reaction conditions from its ID.
	 * @param	conHandler	contains the connection that will be used to
	 * query the database
	 * @param	rxnId	the ID number of the set of reaction conditions
	 * @return	the name of the set of reaction conditions
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static String getName(ConnectionHandler conHandler, int rxnId) 
			throws DBException {
		return getName(conHandler.getConnection(), rxnId);
	} // getName(ConnectionHandler, int)

	/** Gets the name of a set of reaction conditions from its ID.
	 * @param	con	the database connection
	 * @param	rxnId	the ID number of the set of reaction conditions
	 * @return	the name of the set of reaction conditions
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static String getName(Connection con, int rxnId) 
			throws DBException {
		return SynthDataRW.getRxnConditionName(con, rxnId);
	} // getName(Connection, int)

	/** Finds the (arbitrarily assigned) ID numbers of the molecule or
	 * molecules in the JChem table for the particular reaction. Should return
	 * array of one or zero members.
	 * @param	molStr	a molecule or molecules
	 * @param	conHandler	handles connection to the database
	 * @param	rxnId	the ID number of the reaction conditions operating on
	 * the molecule or group of molecules
	 * @return	array of ID numbers of the molecule or molecules associated 
	 * with a particular reaction: should be zero or one member
	 * @throws	DBException	if there's a problem reading the database
	 */
	private static int[] getCompoundIds(ConnectionHandler conHandler, 
			String molStr, int rxnId) throws DBException {
		final String SELF = "ReactorResultsRW.getCompoundIds: ";
		final JChemSearch searcher = new JChemSearch();
		try {
			searcher.setQueryStructure(molStr);
			searcher.setConnectionHandler(conHandler);
			searcher.setStructureTable(REACTOR_RESULTS);
			final JChemSearchOptions searchOptions = 
					new JChemSearchOptions(DUPLICATE);
			searchOptions.setFilterQuery(toString(
					SELECT + REACT_SM_ID
					+ FROM + REACTOR_RESULTS
					+ WHERE + REACT_RXN_ID + EQUALS, rxnId));
			searcher.setSearchOptions(searchOptions);
			searcher.setRunMode(JChemSearch.RUN_MODE_SYNCH_COMPLETE);
			searcher.run();
		} catch (MolFormatException e) {
			alwaysPrint(SELF + "caught MolFormatException.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (DatabaseSearchException e) {
			alwaysPrint(SELF + "caught DatabaseSearchException.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (IOException e) {
			alwaysPrint(SELF + "caught IOException.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (SQLException e) {
			alwaysPrint(SELF + "caught SQLException.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (NoSuchMethodError e) {
			alwaysPrint(SELF + "caught NoSuchMethodError.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} catch (Exception e) {
			alwaysPrint(SELF + "caught Exception.");
			e.printStackTrace();
			throw new DBException(e.getMessage());
		} // try
		return searcher.getResults();
	} // getCompoundIds(ConnectionHandler, String, int)

	/** Gets a connection handler for searching or updating the JChem table. 
	 * Use only if you aren't writing a CLOB to the JChem table.
	 * @return	a connection handler
	 * @throws	SQLException	if can't get a pool connection
	 */
	private static ConnectionHandler getConHandler() throws SQLException {
		return new ConnectionHandler(getPoolConnection(), JCHEM_PROPERTY_TABLE);
	} // getConHandler()

	/** Gets a connection handler for updating the JChem table.  Called only
	 * from addCalcdProducts() because that's where we write a CLOB to the JChem
	 * table.  See the discussions 
	 * <a href="https://www.chemaxon.com/forum/ftopic7311.html">here</a> and
	 * <a href="https://forums.oracle.com/forums/thread.jspa?threadID=279238">here</a>
	 * for why it's necessary to get the inner OracleConnection to write a CLOB.
	 * @param	con	a pool connection
	 * @return	a connection handler activated with an inner OracleConnection,
	 * or null if it can't get the inner OracleConnection
	 * @throws	SQLException	if can't get a pool connection
	 */
	private static ConnectionHandler getConHandler(Connection con) 
			throws SQLException {
		final String SELF = "ReactorResultsRW.getConHandler: ";
		ConnectionHandler conHandler = 
				new ConnectionHandler(con, JCHEM_PROPERTY_TABLE);
		if (con instanceof DelegatingConnection) {
			debugPrint(SELF + "connection is DelegatingConnection; need "
					+ "to get InnermostDelegate to get ConnectionHandler.");
			final Connection dc = 
					((DelegatingConnection) con).getInnermostDelegate();
			if (dc != null) {
				debugPrint(SELF + "innermost Oracle connection is not null; "
						+ "returning ConnectionHandler.");
				conHandler = 
						new ConnectionHandler(dc, JCHEM_PROPERTY_TABLE);
			} else {
				debugPrint(SELF + "innermost Oracle connection is null; "
						+ "returning conhandler made from original connection "
						+ "even though it is a delegating connection.");
				conHandler = new ConnectionHandler(con, JCHEM_PROPERTY_TABLE);
			} // if delegatingconnection is null
		} else {
			debugPrint(SELF + "connection is not DelegatingConnection.");
			conHandler = new ConnectionHandler(con, JCHEM_PROPERTY_TABLE);
		} // if connection is a DelegatingConnection
		return conHandler;
	} // getConHandler(Connection)

	/** Closes the connection inside a ConnectionHandler.
	 * @param	conHandler	a ConnectionHandler
	 */
	private static void closeConHandler(ConnectionHandler conHandler) {
		if (conHandler != null) {
			closeConnection(conHandler.getConnection());
		} // if conHandler is not null
	} // closeConHandler(ConnectionHandler)

	/** For debugging JChem Oracle calls. 
	public static void testOracle() throws SQLException, NamingException {
		Connection con = null;
		try {
			con = getPoolConnection();
			System.out.println("ReactorResultsRW.main: "
					+ "creating tempClob.");
			final CLOB tempClob = 
					CLOB.createTemporary(con, true, CLOB.DURATION_SESSION);
			System.out.println("ReactorResultsRW.main: "
					+ "created tempClob successfully.");
			final ConnectionHandler ch = new ConnectionHandler();
			ch.setDriver("oracle.jdbc.OracleDriver");
			ch.setUrl("jdbc:oracle:thin:@localhost:1521:xe");
			ch.setLoginName("aceorg15");
			ch.setPassword("groeca99");
			ch.connectToDatabase();
			final String addlCols = 
					joinAll(REACT_RXN_ID, REACT_CALCD_PRODS).toString();
			final UpdateHandler uh = new UpdateHandler(ch,
			        UpdateHandler.INSERT, REACTOR_RESULTS, addlCols);
			uh.setStructure("CC(C)=O");
			uh.setValueForAdditionalColumn(1, 1);
			uh.setValueForAdditionalColumn(2, "BrCC(C)=O");
			System.out.println("ReactorResultsRW.main: about to store data.");
			uh.execute();
			ch.close();
			System.out.println("ReactorResultsRW.main: success!");
		} catch (SQLException e) {
			System.out.println("ReactorResultsRW.main: SQL exception thrown. "
					+ e.getMessage());
		} finally {
			closeConnection(con);
		}
	} // main()
	*/

	/** Constructor to disable external instantiation. */
	private ReactorResultsRW() { }

} // ReactorResultsRW
