package eu.nimble.service.bp.model.billOfMaterial;

import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BillOfMaterial {

    private List<String> catalogueUuids = new ArrayList<>();
    private List<String> lineIds = new ArrayList<>();
    private List<QuantityType> quantities = new ArrayList<>();

    /**
     * Get catalogueUuids
     *
     * @return catalogueUuids
     **/
    @ApiModelProperty(value = "")
    public List<String> getCatalogueUuids() {
        return catalogueUuids;
    }

    public void setCatalogueUuids(List<String> catalogueUuids) {
        this.catalogueUuids = catalogueUuids;
    }

    /**
     * Get lineIds
     *
     * @return lineIds
     **/
    @ApiModelProperty(value = "")
    public List<String> getLineIds() {
        return lineIds;
    }

    public void setLineIds(List<String> lineIds) {
        this.lineIds = lineIds;
    }

    /**
     * Get quantities
     *
     * @return quantities
     **/
    @ApiModelProperty(value = "")
    public List<QuantityType> getQuantities() {
        return quantities;
    }

    public void setQuantities(List<QuantityType> quantities) {
        this.quantities = quantities;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BillOfMaterial billOfMaterial = (BillOfMaterial) o;
        return Objects.equals(this.catalogueUuids, billOfMaterial.catalogueUuids) &&
                Objects.equals(this.lineIds, billOfMaterial.lineIds) &&
                Objects.equals(this.quantities, billOfMaterial.quantities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(catalogueUuids,lineIds,quantities);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class BillOfMaterial {\n");

        sb.append("    catalogueUuids: ").append(toIndentedString(catalogueUuids)).append("\n");
        sb.append("    lineIds: ").append(toIndentedString(lineIds)).append("\n");
        sb.append("    quantities: ").append(toIndentedString(quantities)).append("\n");
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
