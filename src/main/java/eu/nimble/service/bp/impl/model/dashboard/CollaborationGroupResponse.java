package eu.nimble.service.bp.impl.model.dashboard;

import eu.nimble.service.bp.swagger.model.CollaborationGroup;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CollaborationGroupResponse {
    private List<CollaborationGroup> collaborationGroups = new ArrayList<CollaborationGroup>();

    private Integer size = null;

    /**
     * Get collaborationGroups
     *
     * @return collaborationGroups
     **/
    @ApiModelProperty(value = "")
    public List<CollaborationGroup> getCollaborationGroups() {
        return collaborationGroups;
    }

    public void setCollaborationGroups(List<CollaborationGroup> collaborationGroups) {
        this.collaborationGroups = collaborationGroups;
    }

    public CollaborationGroupResponse size(Integer size) {
        this.size = size;
        return this;
    }

    /**
     * Get size
     *
     * @return size
     **/
    @ApiModelProperty(value = "")
    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CollaborationGroupResponse collaborationGroupResponse = (CollaborationGroupResponse) o;
        return Objects.equals(this.collaborationGroups, collaborationGroupResponse.collaborationGroups) &&
                Objects.equals(this.size, collaborationGroupResponse.size);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collaborationGroups, size);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class CollaborationGroupResponse {\n");

        sb.append("    collaborationGroups: ").append(toIndentedString(collaborationGroups)).append("\n");
        sb.append("    size: ").append(toIndentedString(size)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}

