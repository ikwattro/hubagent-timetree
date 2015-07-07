package com.graphaware.module.timetree.domain;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by cw on 7/07/15.
 */
public class InitializeLabelsRestriction {

    private static final Logger LOG = LoggerFactory.getLogger(InitializeLabelsRestriction.class);

    private Set<Label> labels = new HashSet<>();

    public InitializeLabelsRestriction(String labelsRestriction) {
        String[] ls = labelsRestriction.split(",");
        LOG.info(labelsRestriction);
        LOG.info(ls.toString());
        if ("" != ls[0]) {
            for (String l : ls) {
                labels.add(DynamicLabel.label(l));
            }
        }
    }

    public Set getLabelsRestriction() {
        return labels;
    }

    public boolean hasLabelsRestriction() {
        return labels.size() > 0;
    }

    public int getSize() {
        return labels.size();
    }

    public String toString() {
        String s = "";
        for (Label l : labels) {
            s = s + l.toString();
        }

        return s;
    }
}
