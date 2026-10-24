package com.sheridan.gcr.modularSys.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PivotMap {
    private final Pivot root;
    private final Map<String, Pivot> pivots = new HashMap<>();
    public static final PivotMap EMPTY = new PivotMap(new Pivot(0, 0, 0, 0, 0, 0, "root", null));

    public PivotMap(Pivot root) {
        this.root = root;
        loadPivots(root);
        pivots.put(root.name, root);
    }

    public Pivot get(String name) {
        return pivots.get(name);
    }

    private void loadPivots(Pivot root) {
        Map<String, Pivot> children = root.children;
        for (Map.Entry<String, Pivot>  entry : children.entrySet()) {
            pivots.put(entry.getKey(), entry.getValue());
            loadPivots(entry.getValue());
        }
    }

    public void print() {
        for (Pivot pivot : pivots.values()) {
            System.out.println(pivot);
        }
    }

    public Map<String, Pivot> slots() {
        return pivots;
    }

    public Pivot root() {
        return root;
    }

    public List<Pivot> pivots() {
        return new ArrayList<>(pivots.values());
    }
}
