package com.uniandes.jfm.timesapp;

/**
 * Created by JFM on 15/08/2015.
 */
public class AccessPoint implements Comparable<AccessPoint>{
    public String macadd;
    public int potencia;

    public AccessPoint (String macaddP, int potenciaP)
    {
        potencia=potenciaP;
        macadd=macaddP;
    }

    @Override
    public int compareTo(AccessPoint another) {
        return potencia - another.potencia;
    }
}
