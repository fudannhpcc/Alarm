package cn.fudannhpcc.www.alarm.commonclass;

public class Utilites {

    public static boolean isNumeric(String s) throws NumberFormatException {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
