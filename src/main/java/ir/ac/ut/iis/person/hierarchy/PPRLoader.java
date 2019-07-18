/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.hierarchy;

import com.diffplug.common.base.Errors;
import ir.ac.ut.iis.person.Configs;
import ir.ac.ut.iis.person.algorithms.social_textual.MySQLConnector;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.mutable.MutableDouble;

/**
 *
 * @author shayan
 */
public class PPRLoader {

    public static Map<Integer, List<Float>> map;

    public static void load(Hierarchy hier, String fileName) {
        map = new HashMap<>();
        loadAndExecute(fileName, hier, true,
                hn -> hn.getParent().usersNum() < 1000 * hn.usersNum(),
                (hn, ss) -> {
                    int parseInt = Integer.parseInt(ss[0]);
                    List<Float> get = map.get(parseInt);
                    if (get == null) {
                        get = new FloatArrayList();
                        map.put(parseInt, get);
                    }
                    get.add((float) hn.getId());
                    get.add(Float.parseFloat(ss[1]));
                },
                t -> {
                },
                (hn, s) -> {
                },
                () -> {
                });
    }

    public static void extractSelfPPRs(Hierarchy hier, String input, String output) {
        try ( OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(output)))) {
            final MutableDouble val = new MutableDouble();
            loadAndExecute(input, hier, false,
                    hn -> true,
                    (hn, ss) -> {
                        if (hn.isEqualToOrAncestorOf(hier.getUserNode(Integer.parseInt(ss[0])).getHierarchyNode())) {
                            val.add(Float.parseFloat(ss[1]));
                        }
                    },
                    t -> {
                        val.setValue(0.);
                    },
                    (hn, ss) -> {
                        try {
                        writer.write(ss + " " + (float) val.getValue().doubleValue() + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    },
                    () -> {
                    }
            );
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PPRLoader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PPRLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void extractMinimalPPRs(Hierarchy hier, String input, String output) {
        try ( OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(output)))) {
            loadAndExecute(input,
                    hier,
                    true,
                    hn -> {
                        return hn.getParent().usersNum() < 1000 * hn.usersNum();
                    },
                    (hn, ss) -> Errors.rethrow().wrap(() -> {
                        writer.write(ss[0] + " " + ss[1] + "\n");
                    }),
                    Errors.rethrow().wrap(s -> {
                        writer.write(s + "\n");
                    }),
                    (hn, s) -> {
                    },
                    () -> {
                    });
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PPRLoader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PPRLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void loadAndExecute(String input, Hierarchy hier, boolean onlyNonSelfs, Function<HierarchyNode, Boolean> checkFunction, BiConsumer<HierarchyNode, String[]> consumer, Consumer<String> initialConsumer, BiConsumer<HierarchyNode, String> postConsumer, Runnable batchRunnable) {
        try ( Scanner sc = new Scanner(new BufferedInputStream(new FileInputStream(input)))) {
            int cnt = 0;
            while (sc.hasNextLine()) {
                final String nextLine = sc.nextLine();
                String[] split = nextLine.split(" ");
                int count = Integer.parseInt(split[1]);
                String[] ids = split[0].split(":");
                HierarchyNode hn = hier.getRootNode();
                for (String id : ids) {
                    hn = hn.getChildren().get(Integer.parseInt(id));
                }
                boolean check = checkFunction.apply(hn);
                if (check) {
                    initialConsumer.accept(nextLine);
                }
                cnt++;
                if (cnt % 1000000 == 0) {
                    System.out.println(cnt);
                }
                for (int i = 0; i < count; i++) {
                    cnt++;
                    if (cnt % 1000000 == 0) {
                        System.out.println(cnt);
                        batchRunnable.run();
                    }
                    if (!sc.hasNextLine())
                        System.out.println("");
                    String nextLine2 = sc.nextLine();
                    if (check) {
                        String[] split1 = nextLine2.split(" ");
                        if (!onlyNonSelfs || !hn.isEqualToOrAncestorOf(hier.getUserNode(Integer.parseInt(split1[0])).getHierarchyNode())) {
                            consumer.accept(hn, split1);
                        }
                    }
                }
                postConsumer.accept(hn, split[0]);
            }
            batchRunnable.run();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(HierarchyNode.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void addToDatabase(Hierarchy hier, String input, int threshold) {
        Connection conn = MySQLConnector.connect(Configs.database_name);
        try {
            conn.setAutoCommit(false);
            conn.prepareStatement("DROP TABLE IF EXISTS `PPR_user_" + threshold + "`").executeUpdate();
            conn.prepareStatement("CREATE TABLE `PPR_user_" + threshold + "` ("
                    + "  `hn_id` int,"
                    + "  `node_id` int,"
                    + "  `PPR` float,"
                    + "  PRIMARY KEY (`hn_id`, `node_id`)"
                    + ") ENGINE = MyISAM PACK_KEYS = 1 ROW_FORMAT = FIXED DEFAULT CHARSET=utf8;").executeUpdate();
            PreparedStatement get;
            get = conn.prepareStatement("insert into `PPR_user_" + threshold + "` values(?,?,?)");
            loadAndExecute(input, hier, true,
                    hn -> hn.getParent().usersNum() >= threshold * hn.usersNum(),
                    (hn, ss) -> {
                        try {
                            final float parseFloat = Float.parseFloat(ss[1]);
                            get.setInt(1, hn.getId());
                            get.setInt(2, Integer.parseInt(ss[0]));
                            get.setFloat(3, parseFloat);
                            get.addBatch();
                        } catch (NumberFormatException | SQLException e) {
                            throw new RuntimeException();
                        }
                    },
                    t -> {
                    },
                    (hn, s) -> {

                    },
                    Errors.rethrow().wrap(() -> {
                        get.executeBatch();
                        get.clearBatch();
                        conn.commit();
                    }));
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(PPRLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
//        int cnt = 0;
//        try (Scanner sc = new Scanner(new FileInputStream("/home/shayan/Desktop/CSR codes for revision/CSR-master/PPRs_kol.txt"))) {
//            while (sc.hasNextLine()) {
//                String[] split = sc.nextLine().split(" ");
//                int count = Integer.parseInt(split[1]);
//                String[] ids = split[0].split(":");
//                int ii = 0;
//                for (String id : ids) {
//                    ii = Integer.valueOf(id);
//                }
//
//                for (int i = 0; i < count; i++) {
//                    sc.nextLine();
//                    if (ii < 0) {
//                        cnt++;
//                    }
//                }
//            }
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(PPRLoader.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        System.out.println(cnt);
    }

}
