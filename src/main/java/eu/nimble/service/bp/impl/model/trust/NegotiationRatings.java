package eu.nimble.service.bp.impl.model.trust;

import eu.nimble.service.model.ubl.commonaggregatecomponents.CommentType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.EvidenceSuppliedType;

import java.util.List;

public class NegotiationRatings {

    private String processInstanceID;
    private List<EvidenceSuppliedType> ratings;
    private List<CommentType> reviews;

    public NegotiationRatings() {
    }

    public NegotiationRatings(String processInstanceID, List<EvidenceSuppliedType> ratings, List<CommentType> reviews) {
        this.processInstanceID = processInstanceID;
        this.ratings = ratings;
        this.reviews = reviews;
    }

    public List<EvidenceSuppliedType> getRatings() {
        return ratings;
    }

    public void setRatings(List<EvidenceSuppliedType> ratings) {
        this.ratings = ratings;
    }

    public List<CommentType> getReviews() {
        return reviews;
    }

    public void setReviews(List<CommentType> reviews) {
        this.reviews = reviews;
    }

    public String getProcessInstanceID() {
        return processInstanceID;
    }

    public void setProcessInstanceID(String processInstanceID) {
        this.processInstanceID = processInstanceID;
    }
}
