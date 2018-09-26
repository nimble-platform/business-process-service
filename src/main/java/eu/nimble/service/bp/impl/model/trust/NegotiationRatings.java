package eu.nimble.service.bp.impl.model.trust;

import eu.nimble.service.model.ubl.commonaggregatecomponents.CommentType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.EvidenceSuppliedType;

import java.util.List;

public class NegotiationRatings {

    private List<EvidenceSuppliedType> ratings;
    private List<CommentType> reviews;

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
}
