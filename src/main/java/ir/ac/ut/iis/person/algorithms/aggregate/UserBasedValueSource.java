/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.algorithms.aggregate;

import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.algorithms.social_textual.MySQLConnector;
import ir.ac.ut.iis.person.query.Query;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.function.FunctionValues;

/**
 *
 * @author shayan
 */
public abstract class UserBasedValueSource extends MyValueSource implements Closeable {

    protected final Connection conn;
    private Map<Integer, Double> map;
    private Query queryCache = null;
    private Map<Integer, Double> mapCache = null;

    public UserBasedValueSource(String database_name) {
        conn = MySQLConnector.connect(database_name);
    }

    @Override
    public void initialize(Query query) {
        if (query.equals(queryCache)) {
            map = mapCache;
        } else {
            queryCache = query;
            map = calcWeights(query.getSearcher(), Configs.socialTextualDegree);
            mapCache = map;
        }
    }

    @Override
    public FunctionValues getValues(Map context, LeafReaderContext readerContext) throws IOException {
        return new FunctionValues() {

            @Override
            public float floatVal(int doc) {
                Double get1 = map.get(readerContext.docBase + doc);
                if (get1 == null) {
                    return 0;
                }
                return get1.floatValue();
            }

            @Override
            public String toString(int doc) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String description() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public abstract HashMap<Integer, Double> calcWeights(int userId, int degree);

    @Override
    public void close() throws IOException {
        try {
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(UserBasedValueSource.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

}
