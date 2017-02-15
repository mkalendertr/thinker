package edu.yeditepe.typeclassifier;

import de.bwaldvogel.liblinear.FeatureNode;

public class TypeClassifierFeature {
    public FeatureNode[] getFeatureVector() {
        return featureVector;
    }

    public void setFeatureVector(FeatureNode[] featureVector) {
        this.featureVector = featureVector;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    private FeatureNode[] featureVector;

    private int type;

    public TypeClassifierFeature(FeatureNode[] featureVector, int type) {
        super();
        this.featureVector = featureVector;
        this.type = type;
    }
    
}
