package com.example.myapplication.util;

import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

/**
 * 数据库工具类：连接数据库用、获取数据库数据用
 * 相关操作数据库的方法均可写在该类
 */
public class DBUtils {

    private static String driver = "com.mysql.jdbc.Driver";// MySql驱动
    private static String user = "test";// 用户名
    private static String password = "root@123";// 密码
    private static String ip = "192.168.1.29";
    private static String db_name = "front_crossshop";

    private static Connection connection = null;

    static{
        try {
            Class.forName(driver);// 动态加载类
            // 尝试建立到给定数据库URL的连接
            String url = "jdbc:mysql://" + ip + ":3306/" + db_name;
            connection = DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int insertUserInfo(String email, String password) {

        try {
            String sql = "insert into android_user(email,pass_word) values(?,?)";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, password);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("insertUserInfo email[" + email + "]", "异常：" + e.getMessage());
            return 0;
        }
    }


    public static HashMap<String, Object> getInfoByEmail(String email) {

        HashMap<String, Object> map = new HashMap<>();
        // 根据数据库名称，建立连接

        try {
            // mysql简单的查询语句。这里是根据MD_CHARGER表的NAME字段来查询某条记录
            String sql = "select * from android_user where email = ?";
//            String sql = "select * from MD_CHARGER";
            if (connection != null) {// connection不为null表示与数据库建立了连接
                PreparedStatement ps = connection.prepareStatement(sql);
                if (ps != null) {
                    // 设置上面的sql语句中的？的值为name
                    ps.setString(1, email);
                    // 执行sql查询语句并返回结果集
                    ResultSet rs = ps.executeQuery();
                    if (rs != null) {
                        int count = rs.getMetaData().getColumnCount();
                        Log.e("DBUtils", "列总数：" + count);
                        while (rs.next()) {
                            // 注意：下标是从1开始的
                            for (int i = 1; i <= count; i++) {
                                String field = rs.getMetaData().getColumnName(i);
                                map.put(field, rs.getString(field));
                            }
                        }
                        connection.close();
                        ps.close();
                        return map;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("getInfoByEmail email[" + email + "]", "异常：" + e.getMessage());
            return null;
        }
    }
}