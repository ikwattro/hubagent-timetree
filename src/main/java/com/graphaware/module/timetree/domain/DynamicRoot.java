package com.graphaware.module.timetree.domain;

/**
 * Created by cw on 6/07/15.
 */
public class DynamicRoot {

    private String rootLabel;

    private String rootPropertyNameRef;

    private String rootPropertyValueRef;

    private boolean isDefined;

    public DynamicRoot(String definition) {
        String[] split = definition.split(":");
        if (split.length < 3) {
            isDefined = false;
        } else {
            rootLabel = split[0];
            rootPropertyNameRef = split[1];
            rootPropertyValueRef = split[2];
            isDefined = true;
        }
    }

    public String getRootLabel() { return rootLabel; }

    public String getRootPropertyNameRef() { return rootPropertyNameRef; }

    public String getRootPropertyValueRef() { return rootPropertyValueRef; }

    public boolean isDefined() { return isDefined; }

    public String getDefinition() {
        return getRootLabel() + ":" + getRootPropertyNameRef() + ":" + getRootPropertyValueRef();
    }
}
