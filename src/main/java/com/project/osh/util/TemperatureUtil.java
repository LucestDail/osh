package com.project.osh.util;

public class TemperatureUtil {
	private double value = 0d;

    public double getTemp(double value, char unit) {
        this.value = value;
        switch (unit) {
        case 'C':
            this.value = value;
            break;
        case 'K':
            this.value = value - 273.15;
            break;
        case 'F':
        	this.value = 5.0 / 9 * (value - 32);
            break;
        }
        return this.value;
    }
    double getInC() {
        return value;
    }
    double getInF() {
        return 9.0 / 5 * value + 32;
    }
    double getInK() {
        return value + 273.15;
    }
}