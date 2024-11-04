package de.triology.cas.ldap;

public class Util {
        public static String extractGroupNameFromDn(String dn) {
        String result = dn;
        int eqindex = dn.indexOf('=');
        int coindex = dn.indexOf(',');
        if (eqindex > 0 && (coindex < 0 || eqindex < coindex) && dn.length() > eqindex + 1) {
            dn = dn.substring(eqindex + 1);
            coindex = dn.indexOf(',');
            if (coindex > 0) {
                result = dn.substring(0, coindex);
            } else {
                result = dn;
            }
        }
        return result;
    }

}
