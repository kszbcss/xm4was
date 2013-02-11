package com.googlecode.xm4was.commons.jmx.exporter;

import java.lang.reflect.InvocationTargetException;

abstract class StatisticUpdater {
    abstract void updateStatistic() throws IllegalAccessException, InvocationTargetException;
}
