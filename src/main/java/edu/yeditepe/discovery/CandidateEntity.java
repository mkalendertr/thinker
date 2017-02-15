package edu.yeditepe.discovery;

public class CandidateEntity {
    private String entityType;

    private String entityValue;

    private String sentencesContainingEntity;

    public CandidateEntity(String entityType, String entityValue, String sentencesContainingEntity) {
        this.entityValue = entityValue;
        this.entityType = entityType;
        this.sentencesContainingEntity = sentencesContainingEntity;
    }

    public String getType() {
        return entityType;
    }

    public String getName() {
        return entityValue;
    }

    public String getSentencesContainingEntity() {
        return sentencesContainingEntity;
    }

}
